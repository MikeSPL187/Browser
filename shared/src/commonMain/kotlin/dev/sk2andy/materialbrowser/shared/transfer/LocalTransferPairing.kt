package dev.sk2andy.materialbrowser.shared.transfer

import dev.sk2andy.materialbrowser.sync.SyncBase64

/**
 * What the QR code of a local transfer (V26) carries: where the sending device listens on the
 * Wi-Fi ([host], [port]), which one-shot session it serves ([sessionId]), the session's 256-bit
 * [key] and when the offer ends ([expiresAtEpochSeconds]). The key travels only through the
 * screen, never over the network; see `docs/vola/research/v26-local-transfer.md`.
 */
class LocalTransferPairing(
    val host: String,
    val port: Int,
    val sessionId: ByteArray,
    val key: ByteArray,
    val expiresAtEpochSeconds: Long,
) {
    init {
        require(LocalTransferPairingRules.isLocalHost(host))
        require(port in LocalTransferPairingRules.PORTS)
        require(sessionId.size == LocalTransferPairingRules.SESSION_ID_BYTES)
        require(key.size == LocalTransferPairingRules.KEY_BYTES)
    }

    /** Never prints the key, so a log line cannot leak it. */
    override fun toString(): String = "LocalTransferPairing(host=$host, port=$port, expires=$expiresAtEpochSeconds)"
}

object LocalTransferPairingRules {
    const val SCHEME = "vola-pair:v1"
    const val KEY_BYTES = 32
    const val SESSION_ID_BYTES = 16

    /** How long an offer stays open (owner's decision, V26). */
    const val LIFETIME_SECONDS = 120L

    /** Clock difference between the two phones that a code still survives. */
    const val CLOCK_SKEW_SECONDS = 30L

    /** Unprivileged ports only; the sender binds an ephemeral one. */
    val PORTS = 1_024..65_535

    private const val MAX_CODE_CHARS = 512

    /** The text the sending device shows as a QR code. */
    fun format(pairing: LocalTransferPairing): String = buildString {
        append(SCHEME)
        append("?h=").append(pairing.host)
        append("&p=").append(pairing.port)
        append("&s=").append(SyncBase64.encode(pairing.sessionId))
        append("&k=").append(SyncBase64.encode(pairing.key))
        append("&e=").append(pairing.expiresAtEpochSeconds)
    }

    /**
     * The pairing in a scanned or typed [code], checked against the receiving phone's clock
     * [nowEpochSeconds]: null for anything that is not a well-formed, unexpired local offer.
     * Every field must be there exactly once; nothing else is accepted.
     */
    fun parse(code: String, nowEpochSeconds: Long): LocalTransferPairing? {
        val text = code.trim()
        if (text.length > MAX_CODE_CHARS || !text.startsWith("$SCHEME?")) return null
        val fields = text.removePrefix("$SCHEME?").split('&').map { field ->
            val name = field.substringBefore('=', missingDelimiterValue = "")
            val value = field.substringAfter('=', missingDelimiterValue = "")
            if (name.isEmpty() || value.isEmpty()) return null
            name to value
        }
        if (fields.map { it.first }.sorted() != listOf("e", "h", "k", "p", "s")) return null
        val values = fields.toMap()
        val port = values.getValue("p").toIntOrNull()?.takeIf { it in PORTS } ?: return null
        val expires = values.getValue("e").toLongOrNull() ?: return null
        if (!isOpen(expires, nowEpochSeconds)) return null
        val sessionId = runCatching { SyncBase64.decode(values.getValue("s"), expectedBytes = SESSION_ID_BYTES) }
            .getOrNull() ?: return null
        val key = runCatching { SyncBase64.decode(values.getValue("k"), expectedBytes = KEY_BYTES) }
            .getOrNull() ?: return null
        val host = values.getValue("h").takeIf { isLocalHost(it) } ?: return null
        return LocalTransferPairing(host, port, sessionId, key, expires)
    }

    /**
     * Whether an offer ending at [expiresAtEpochSeconds] is still open at [nowEpochSeconds]. An
     * end further away than one lifetime (plus skew) is refused too: no code lives longer.
     */
    fun isOpen(expiresAtEpochSeconds: Long, nowEpochSeconds: Long): Boolean =
        nowEpochSeconds <= expiresAtEpochSeconds + CLOCK_SKEW_SECONDS &&
            expiresAtEpochSeconds - nowEpochSeconds <= LIFETIME_SECONDS + CLOCK_SKEW_SECONDS

    /**
     * Whether [host] is a private IPv4 address of a home or office network (10/8, 172.16/12,
     * 192.168/16): the transfer never leaves the Wi-Fi, and a code cannot point a phone at a
     * server on the internet, at itself (loopback) or at a link-local address.
     */
    fun isLocalHost(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { part ->
            if (part.isEmpty() || part.length > 3 || !part.all(Char::isDigit) || (part.length > 1 && part[0] == '0')) {
                return false
            }
            part.toInt().takeIf { it in 0..255 } ?: return false
        }
        return when {
            octets[0] == 10 -> true
            octets[0] == 172 -> octets[1] in 16..31
            octets[0] == 192 -> octets[1] == 168
            else -> false
        }
    }
}
