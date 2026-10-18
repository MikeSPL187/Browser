package dev.sk2andy.materialbrowser.browser.transfer

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferFailure
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferOfferState
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairing
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairingRules
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferProtocol
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferReceiveResult
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLocalTransferPortTest {
    private val payload = LocalTransferPayload(
        deviceName = "Pixel 8",
        tabs = listOf(LocalTransferTab("https://example.com/a", "A")),
        bookmarks = listOf(LocalTransferBookmark("https://example.org/", "Example", "Work")),
    )
    private val wifi = InetAddress.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 1, 20)) as Inet4Address

    @Test
    fun hmacMatchesRfc4231() {
        val mac = AndroidLocalTransferCrypto.hmacSha256("Jefe".toByteArray(), "what do ya want for nothing?".toByteArray())

        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", mac.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun gcmOpensOnlyWithTheSameKeyNonceAndAad() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { 7 }
        val sealed = AndroidLocalTransferCrypto.aesGcmSeal(key, nonce, "hello".toByteArray(), "aad".toByteArray())

        assertArrayEquals("hello".toByteArray(), AndroidLocalTransferCrypto.aesGcmOpen(key, nonce, sealed, "aad".toByteArray()))
        assertNull(AndroidLocalTransferCrypto.aesGcmOpen(key, nonce, sealed, "other".toByteArray()))
        assertNull(AndroidLocalTransferCrypto.aesGcmOpen(ByteArray(32), nonce, sealed, "aad".toByteArray()))
        sealed[0] = (sealed[0].toInt() xor 1).toByte()
        assertNull(AndroidLocalTransferCrypto.aesGcmOpen(key, nonce, sealed, "aad".toByteArray()))
    }

    @Test
    fun httpRoundTripsOneBoundedRequestAndAnswer() {
        val request = ByteArrayOutputStream().also { LocalTransferHttp.writeRequest(it, "192.168.1.20", 4_000, "/v1/abc", "{\"v\":1}") }
        val read = LocalTransferHttp.readRequest(ByteArrayInputStream(request.toByteArray()), 1_024)!!
        assertEquals("POST", read.method)
        assertEquals("/v1/abc", read.path)
        assertEquals("{\"v\":1}", read.body)

        val answer = ByteArrayOutputStream().also { LocalTransferHttp.writeResponse(it, 410) }
        val response = LocalTransferHttp.readResponse(ByteArrayInputStream(answer.toByteArray()), 1_024)!!
        assertEquals(410, response.status)
        assertEquals("", response.body)
    }

    @Test
    fun httpRefusesUnboundedInput() {
        fun read(raw: String) = LocalTransferHttp.readRequest(ByteArrayInputStream(raw.toByteArray()), 16)

        assertNull("body over the limit", read("POST / HTTP/1.1\r\nContent-Length: 17\r\n\r\n" + "x".repeat(17)))
        assertNull("no length", read("POST / HTTP/1.1\r\n\r\n"))
        assertNull("chunked", read("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\nContent-Length: 1\r\n\r\nx"))
        assertNull("length twice", read("POST / HTTP/1.1\r\nContent-Length: 1\r\nContent-Length: 1\r\n\r\nx"))
        assertNull("short body", read("POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\nx"))
        assertNull("long line", read("POST /" + "a".repeat(2_000) + " HTTP/1.1\r\nContent-Length: 0\r\n\r\n"))
        assertNull("bare newline", read("POST / HTTP/1.1\nContent-Length: 0\n\n"))
        assertNull("other version", read("POST / HTTP/1.0\r\nContent-Length: 0\r\n\r\n"))
        assertNull("too many headers", read("POST / HTTP/1.1\r\n" + "X: 1\r\n".repeat(40) + "Content-Length: 0\r\n\r\n"))
    }

    @Test
    fun anOfferAnswersOneProvenRequestOnce() {
        val pairing = pairing()
        val offer = LocalTransferOffer(pairing, payload, AndroidLocalTransferCrypto)
        val request = LocalTransferProtocol.request(pairing, AndroidLocalTransferCrypto)

        val reply = offer.answer(LocalTransferHttp.Request("POST", LocalTransferProtocol.path(pairing), request.body))

        assertEquals(200, reply.status)
        assertEquals(LocalTransferOffer.Outcome.Sent, reply.outcome)
        assertEquals(LocalTransferOffer.Outcome.Sent, offer.outcome)
        assertEquals(payload, LocalTransferProtocol.open(pairing, request.nonce, reply.body, AndroidLocalTransferCrypto))
        assertEquals(410, offer.answer(LocalTransferHttp.Request("POST", LocalTransferProtocol.path(pairing), request.body)).status)
    }

    @Test
    fun wrongProofsDoNotUseTheOfferUpUntilTooMany() {
        val pairing = pairing()
        val offer = LocalTransferOffer(pairing, payload, AndroidLocalTransferCrypto)
        val path = LocalTransferProtocol.path(pairing)
        val stranger = LocalTransferPairing(pairing.host, pairing.port, pairing.sessionId, ByteArray(32) { 1 }, pairing.expiresAtEpochSeconds)

        assertEquals(400, offer.answer(null).status)
        assertEquals(404, offer.answer(LocalTransferHttp.Request("POST", "/v1/other", "")).status)
        assertEquals(405, offer.answer(LocalTransferHttp.Request("GET", path, "")).status)
        val forged = offer.answer(LocalTransferHttp.Request("POST", path, LocalTransferProtocol.request(stranger, AndroidLocalTransferCrypto).body))
        assertEquals(403, forged.status)
        assertEquals(LocalTransferOffer.Outcome.Open, forged.outcome)

        repeat(LocalTransferOffer.MAX_WRONG_REQUESTS - 2) { offer.answer(LocalTransferHttp.Request("POST", path, "{}")) }
        assertEquals(LocalTransferOffer.Outcome.Open, offer.outcome)
        val last = offer.answer(LocalTransferHttp.Request("POST", path, "{}"))
        assertEquals(LocalTransferOffer.Outcome.TooManyAttempts, last.outcome)
        val proven = LocalTransferProtocol.request(pairing, AndroidLocalTransferCrypto).body
        assertEquals(410, offer.answer(LocalTransferHttp.Request("POST", path, proven)).status)
    }

    @Test
    fun requestsThatDoNotNameTheSessionCannotCloseTheOffer() {
        val pairing = pairing()
        val offer = LocalTransferOffer(pairing, payload, AndroidLocalTransferCrypto)
        val path = LocalTransferProtocol.path(pairing)

        // A scan of the Wi-Fi, a browser's prefetch, a stranger's probe: none of it knows the session.
        repeat(50) {
            offer.answer(null)
            offer.answer(LocalTransferHttp.Request("GET", "/", ""))
            offer.answer(LocalTransferHttp.Request("POST", "/v1/other", "{}"))
            offer.answer(LocalTransferHttp.Request("GET", path, ""))
        }

        assertEquals(LocalTransferOffer.Outcome.Open, offer.outcome)
        val proven = LocalTransferProtocol.request(pairing, AndroidLocalTransferCrypto).body
        assertEquals(200, offer.answer(LocalTransferHttp.Request("POST", path, proven)).status)
        assertEquals(LocalTransferOffer.Outcome.Sent, offer.outcome)
    }

    @Test
    fun aPayloadMovesBetweenTwoPortsOverTheNetwork() {
        val port = loopbackPort()
        val states = CopyOnWriteArrayList<LocalTransferOfferState>()
        val sent = CountDownLatch(1)
        val handle = port.offer(payload) { state ->
            states += state
            if (state !is LocalTransferOfferState.Waiting) sent.countDown()
        }
        val waiting = states.first() as LocalTransferOfferState.Waiting
        assertTrue(waiting.code.startsWith("vola-pair:v1?h=192.168.1.20&"))

        val received = CountDownLatch(1)
        var result: LocalTransferReceiveResult? = null
        port.receive(waiting.code) { result = it; received.countDown() }

        assertTrue(received.await(10, TimeUnit.SECONDS))
        assertEquals(LocalTransferReceiveResult.Received(payload), result)
        assertTrue(sent.await(10, TimeUnit.SECONDS))
        assertEquals(LocalTransferOfferState.Sent, states.last())
        handle.cancel()
    }

    @Test
    fun aSlowClientCannotHoldTheOfferPastItsBudget() {
        val port = loopbackPort(requestBudgetMillis = 500)
        val states = CopyOnWriteArrayList<LocalTransferOfferState>()
        val handle = port.offer(payload) { states += it }
        val waiting = states.first() as LocalTransferOfferState.Waiting
        val pairing = LocalTransferPairingRules.parse(waiting.code, NOW)!!
        // Another device on the Wi-Fi opens a connection first and sends one byte every 100 ms: every
        // read is well inside the socket timeout, but the request never ends.
        val stop = AtomicBoolean(false)
        val dripper = thread(isDaemon = true) {
            runCatching {
                Socket(InetAddress.getLoopbackAddress(), pairing.port).use { socket ->
                    val out = socket.getOutputStream()
                    while (!stop.get()) {
                        out.write('G'.code)
                        out.flush()
                        Thread.sleep(100)
                    }
                }
            }
        }
        Thread.sleep(200)

        val received = CountDownLatch(1)
        var result: LocalTransferReceiveResult? = null
        port.receive(waiting.code) { result = it; received.countDown() }

        assertTrue("the receiver is served once the slow client's budget is spent", received.await(4, TimeUnit.SECONDS))
        assertEquals(LocalTransferReceiveResult.Received(payload), result)
        stop.set(true)
        dripper.join(2_000)
        handle.cancel()
    }

    @Test
    fun aCancelledOfferIsUnreachable() {
        val port = loopbackPort()
        var code = ""
        port.offer(payload) { state -> if (state is LocalTransferOfferState.Waiting) code = state.code }.cancel()

        val received = CountDownLatch(1)
        var result: LocalTransferReceiveResult? = null
        port.receive(code) { result = it; received.countDown() }

        assertTrue(received.await(10, TimeUnit.SECONDS))
        assertEquals(LocalTransferReceiveResult.Failed(LocalTransferFailure.Unreachable), result)
    }

    @Test
    fun badCodesAndMissingWifiFailAtOnce() {
        val code = LocalTransferPairingRules.format(pairing())
        var result: LocalTransferReceiveResult? = null
        var state: LocalTransferOfferState? = null

        loopbackPort(now = NOW + 600).receive(code) { result = it }
        assertEquals(LocalTransferReceiveResult.Failed(LocalTransferFailure.Expired), result)
        loopbackPort().receive("https://example.com") { result = it }
        assertEquals(LocalTransferReceiveResult.Failed(LocalTransferFailure.InvalidCode), result)
        AndroidLocalTransferPort(localAddress = { null }, postToMain = { it() }).offer(payload) { state = it }
        assertEquals(LocalTransferOfferState.Failed(LocalTransferFailure.Unavailable), state)
    }

    @Test
    fun aSocketThatCannotBeMadeOnTheWifiFailsTheReceiveWithoutThrowing() {
        val code = LocalTransferPairingRules.format(pairing())
        val results = CopyOnWriteArrayList<LocalTransferReceiveResult>()

        // The Wi-Fi went away between the scan and the connection: binding the socket throws.
        AndroidLocalTransferPort(
            localAddress = { wifi },
            nowEpochSeconds = { NOW },
            postToMain = { it() },
            clientSocket = { throw IOException("The network is gone") },
        ).receive(code) { results += it }
        // No Wi-Fi network at all (mobile data, a VPN): there is no socket to make, and the request
        // must not leave through the default route instead.
        AndroidLocalTransferPort(
            localAddress = { wifi },
            nowEpochSeconds = { NOW },
            postToMain = { it() },
            clientSocket = { null },
        ).receive(code) { results += it }

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (results.size < 2 && System.nanoTime() < deadline) Thread.sleep(10)
        assertEquals(
            listOf(
                LocalTransferReceiveResult.Failed(LocalTransferFailure.Unavailable),
                LocalTransferReceiveResult.Failed(LocalTransferFailure.Unavailable),
            ),
            results.toList(),
        )
    }

    private fun loopbackPort(now: Long = NOW, requestBudgetMillis: Int = 5_000) = AndroidLocalTransferPort(
        localAddress = { wifi },
        nowEpochSeconds = { now },
        postToMain = { it() },
        socketAddress = { InetAddress.getLoopbackAddress() },
        requestBudgetMillis = requestBudgetMillis,
    )

    private fun pairing() = LocalTransferPairing(
        host = "192.168.1.20",
        port = 40_000,
        sessionId = ByteArray(16) { it.toByte() },
        key = ByteArray(32) { (it * 3).toByte() },
        expiresAtEpochSeconds = NOW + LocalTransferPairingRules.LIFETIME_SECONDS,
    )

    private companion object {
        // System clock in the end-to-end tests: the server's expiry check reads the same clock.
        val NOW = System.currentTimeMillis() / 1_000
    }
}
