package dev.sk2andy.materialbrowser.browser.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferCrypto
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferFailure
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferHandle
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferOfferState
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairing
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairingRules
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayloadCodec
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPort
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferProtocol
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferReceiveResult
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * [LocalTransferPort] on plain sockets in the phone's Wi-Fi (V26). An offer binds an ephemeral
 * port on the Wi-Fi address only, answers one proven request and closes — after sending, after
 * two minutes, after too many wrong requests or on [LocalTransferHandle.cancel]. Nothing runs
 * when no offer is open. Callbacks are posted to the main thread.
 */
internal class AndroidLocalTransferPort(
    private val localAddress: () -> Inet4Address?,
    private val crypto: LocalTransferCrypto = AndroidLocalTransferCrypto,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
    private val postToMain: (() -> Unit) -> Unit = mainThreadPoster(),
    /** The socket address behind a code's host; tests map the private address to loopback. */
    private val socketAddress: (String) -> InetAddress = InetAddress::getByName,
    /**
     * A socket for the receiving side, bound to the Wi-Fi so a VPN cannot carry the request
     * elsewhere; null when there is no Wi-Fi network to bind it to. Called off the main thread, and
     * it may throw [IOException] when the network goes away while the socket is bound.
     */
    private val clientSocket: () -> Socket? = ::Socket,
    /** How long one connection may take to send its request, in all; tests shorten it. */
    private val requestBudgetMillis: Int = REQUEST_BUDGET_MILLIS,
) : LocalTransferPort {
    override fun offer(payload: LocalTransferPayload, onState: (LocalTransferOfferState) -> Unit): LocalTransferHandle {
        val address = localAddress()?.hostAddress?.takeIf(LocalTransferPairingRules::isLocalHost)
            ?: return failOffer(onState, LocalTransferFailure.Unavailable)
        if (LocalTransferPayloadCodec.encode(payload).encodeToByteArray().size > LocalTransferPayloadCodec.MAX_PAYLOAD_BYTES) {
            return failOffer(onState, LocalTransferFailure.TooLarge)
        }
        val server = try {
            ServerSocket().apply { bind(InetSocketAddress(socketAddress(address), 0), BACKLOG) }
        } catch (_: IOException) {
            return failOffer(onState, LocalTransferFailure.Unavailable)
        }
        val expires = nowEpochSeconds() + LocalTransferPairingRules.LIFETIME_SECONDS
        val pairing = LocalTransferPairing(
            host = address,
            port = server.localPort,
            sessionId = crypto.randomBytes(LocalTransferPairingRules.SESSION_ID_BYTES),
            key = crypto.randomBytes(LocalTransferPairingRules.KEY_BYTES),
            expiresAtEpochSeconds = expires,
        )
        val cancelled = AtomicBoolean(false)
        val deliver = { state: LocalTransferOfferState -> postToMain { if (!cancelled.get()) onState(state) } }
        deliver(LocalTransferOfferState.Waiting(LocalTransferPairingRules.format(pairing), expires))
        val offer = LocalTransferOffer(pairing, payload, crypto)
        Thread({
            runCatching { serve(server, offer, expires, cancelled, deliver) }
                .onFailure { deliver(LocalTransferOfferState.Failed(LocalTransferFailure.Unavailable)) }
        }, "vola-transfer-offer")
            .apply { isDaemon = true }
            .start()
        return LocalTransferHandle {
            cancelled.set(true)
            runCatching { server.close() }
        }
    }

    override fun receive(code: String, onResult: (LocalTransferReceiveResult) -> Unit): LocalTransferHandle {
        val now = nowEpochSeconds()
        val pairing = LocalTransferPairingRules.parse(code, now)
        if (pairing == null) {
            val reason = if (isExpired(code, now)) LocalTransferFailure.Expired else LocalTransferFailure.InvalidCode
            postToMain { onResult(LocalTransferReceiveResult.Failed(reason)) }
            return LocalTransferHandle.Done
        }
        val cancelled = AtomicBoolean(false)
        val socketInUse = AtomicReference<Socket?>(null)
        Thread({
            val result = runCatching {
                val socket = try {
                    clientSocket()
                } catch (_: IOException) {
                    null
                } ?: return@runCatching LocalTransferReceiveResult.Failed(LocalTransferFailure.Unavailable)
                socketInUse.set(socket)
                // Cancelled while the socket was being made: nobody closed it yet.
                if (cancelled.get()) runCatching { socket.close() }
                fetch(socket, pairing)
            }.getOrElse { LocalTransferReceiveResult.Failed(LocalTransferFailure.Corrupt) }
            postToMain { if (!cancelled.get()) onResult(result) }
        }, "vola-transfer-receive").apply { isDaemon = true }.start()
        return LocalTransferHandle {
            cancelled.set(true)
            runCatching { socketInUse.get()?.close() }
        }
    }

    private fun serve(
        server: ServerSocket,
        offer: LocalTransferOffer,
        expires: Long,
        cancelled: AtomicBoolean,
        deliver: (LocalTransferOfferState) -> Unit,
    ) {
        server.use {
            server.soTimeout = ACCEPT_POLL_MILLIS
            while (!cancelled.get()) {
                if (nowEpochSeconds() > expires) return deliver(LocalTransferOfferState.Failed(LocalTransferFailure.Expired))
                val connection = try {
                    server.accept()
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: IOException) {
                    return
                }
                val reply = connection.use { client ->
                    runCatching {
                        client.soTimeout = IO_TIMEOUT_MILLIS
                        val request = LocalTransferHttp.readRequest(
                            DeadlineSocketInput(client, requestBudgetMillis),
                            LocalTransferProtocol.MAX_REQUEST_CHARS,
                        )
                        offer.answer(request).also { LocalTransferHttp.writeResponse(client.getOutputStream(), it.status, it.body) }
                    }.getOrNull()
                } ?: when (offer.outcome) {
                    LocalTransferOffer.Outcome.Open -> continue
                    // The payload was sealed but the answer could not be written: the offer is spent.
                    LocalTransferOffer.Outcome.Sent -> return deliver(LocalTransferOfferState.Failed(LocalTransferFailure.Unreachable))
                    LocalTransferOffer.Outcome.TooManyAttempts ->
                        return deliver(LocalTransferOfferState.Failed(LocalTransferFailure.TooManyAttempts))
                }
                when (reply.outcome) {
                    LocalTransferOffer.Outcome.Open -> Unit
                    LocalTransferOffer.Outcome.Sent -> return deliver(LocalTransferOfferState.Sent)
                    LocalTransferOffer.Outcome.TooManyAttempts ->
                        return deliver(LocalTransferOfferState.Failed(LocalTransferFailure.TooManyAttempts))
                }
            }
        }
    }

    private fun fetch(socket: Socket, pairing: LocalTransferPairing): LocalTransferReceiveResult {
        val request = LocalTransferProtocol.request(pairing, crypto)
        val response = try {
            socket.use {
                socket.connect(InetSocketAddress(socketAddress(pairing.host), pairing.port), IO_TIMEOUT_MILLIS)
                socket.soTimeout = IO_TIMEOUT_MILLIS
                LocalTransferHttp.writeRequest(socket.getOutputStream(), pairing.host, pairing.port, LocalTransferProtocol.path(pairing), request.body)
                LocalTransferHttp.readResponse(DeadlineSocketInput(socket, RESPONSE_BUDGET_MILLIS), LocalTransferProtocol.MAX_RESPONSE_CHARS)
            }
        } catch (_: IOException) {
            return LocalTransferReceiveResult.Failed(LocalTransferFailure.Unreachable)
        } ?: return LocalTransferReceiveResult.Failed(LocalTransferFailure.Corrupt)
        return when (response.status) {
            200 -> LocalTransferProtocol.open(pairing, request.nonce, response.body, crypto)
                ?.let { payload -> LocalTransferReceiveResult.Received(payload) }
                ?: LocalTransferReceiveResult.Failed(LocalTransferFailure.Corrupt)
            410 -> LocalTransferReceiveResult.Failed(LocalTransferFailure.Expired)
            429 -> LocalTransferReceiveResult.Failed(LocalTransferFailure.TooManyAttempts)
            else -> LocalTransferReceiveResult.Failed(LocalTransferFailure.Corrupt)
        }
    }

    private fun failOffer(onState: (LocalTransferOfferState) -> Unit, reason: LocalTransferFailure): LocalTransferHandle {
        postToMain { onState(LocalTransferOfferState.Failed(reason)) }
        return LocalTransferHandle.Done
    }

    companion object {
        private const val BACKLOG = 4
        private const val ACCEPT_POLL_MILLIS = 500
        private const val IO_TIMEOUT_MILLIS = 5_000

        /** All the time a stranger's connection gets to send its request: a request is a few hundred bytes. */
        private const val REQUEST_BUDGET_MILLIS = 5_000

        /** All the time a sender gets to answer, whatever it trickles: the payload is at most 16 MB on the Wi-Fi. */
        private const val RESPONSE_BUDGET_MILLIS = 30_000

        /** Whether [code] is a well-formed offer whose time has run out, to tell that apart from a bad code. */
        internal fun isExpired(code: String, nowEpochSeconds: Long): Boolean {
            val expires = code.substringAfter("&e=", missingDelimiterValue = "").substringBefore('&').toLongOrNull() ?: return false
            return LocalTransferPairingRules.parse(code, expires) != null && !LocalTransferPairingRules.isOpen(expires, nowEpochSeconds)
        }

        fun create(context: Context): LocalTransferPort {
            val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)
                ?: return LocalTransferPort.Unavailable
            return AndroidLocalTransferPort(
                localAddress = { wifiAddress(connectivity) },
                clientSocket = {
                    // Without a Wi-Fi network the request would leave through the default route, a VPN
                    // among them, to an address that only a code decided: so there is no socket then.
                    wifiNetwork(connectivity)?.let { network -> Socket().also { socket -> network.bindSocket(socket) } }
                },
            )
        }

        /** The active network when it is Wi-Fi or Ethernet; null on mobile data, a VPN-only route or offline. */
        private fun wifiNetwork(connectivity: ConnectivityManager): Network? {
            val network = connectivity.activeNetwork ?: return null
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
            val local = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            return network.takeIf { local }
        }

        /** The phone's private IPv4 address on Wi-Fi or Ethernet; null on mobile data or without a network. */
        private fun wifiAddress(connectivity: ConnectivityManager): Inet4Address? {
            val network = wifiNetwork(connectivity) ?: return null
            return connectivity.getLinkProperties(network)?.linkAddresses
                ?.map { it.address }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { address -> address.hostAddress?.let(LocalTransferPairingRules::isLocalHost) == true }
        }
    }
}

private fun mainThreadPoster(): (() -> Unit) -> Unit {
    val handler = Handler(Looper.getMainLooper())
    return { task -> handler.post { task() } }
}
