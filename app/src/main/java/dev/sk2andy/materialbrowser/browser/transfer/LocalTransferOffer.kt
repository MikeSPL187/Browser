package dev.sk2andy.materialbrowser.browser.transfer

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferCrypto
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPairing
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferProtocol

/**
 * What the sending phone answers on its one-shot server, without the sockets: only a request
 * that proves the code's key gets the sealed payload, and only once. Wrong requests do not use
 * the offer up. Those that name the session (its path, a POST) and fail the proof are counted, and
 * after [MAX_WRONG_REQUESTS] of them the offer closes. Anything else — a scan of the Wi-Fi, a
 * stray GET, a probe of another path — only gets its error: it does not know the session, so it
 * cannot be guessing the key, and counting it would let anyone on the network close the offer with
 * a few requests. Not thread-safe: the server handles one connection at a time.
 */
internal class LocalTransferOffer(
    private val pairing: LocalTransferPairing,
    private val payload: LocalTransferPayload,
    private val crypto: LocalTransferCrypto,
) {
    enum class Outcome { Open, Sent, TooManyAttempts }

    class Reply(val status: Int, val body: String, val outcome: Outcome)

    private val path = LocalTransferProtocol.path(pairing)
    private var wrongRequests = 0
    /** [Outcome.Open] while the offer still answers. */
    var outcome = Outcome.Open
        private set

    fun answer(request: LocalTransferHttp.Request?): Reply {
        if (outcome != Outcome.Open) return Reply(410, "", outcome)
        val nonce = request
            ?.takeIf { it.method == "POST" && it.path == path }
            ?.let { LocalTransferProtocol.checkRequest(pairing, it.body, crypto) }
        if (nonce == null) {
            val namesTheSession = request != null && request.method == "POST" && request.path == path
            if (namesTheSession) {
                wrongRequests++
                if (wrongRequests >= MAX_WRONG_REQUESTS) outcome = Outcome.TooManyAttempts
            }
            val status = when {
                request == null -> 400
                request.path != path -> 404
                request.method != "POST" -> 405
                else -> 403
            }
            return Reply(status, "", outcome)
        }
        outcome = Outcome.Sent
        return Reply(200, LocalTransferProtocol.seal(pairing, nonce, payload, crypto), outcome)
    }

    companion object {
        const val MAX_WRONG_REQUESTS = 5
    }
}
