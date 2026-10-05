package dev.sk2andy.materialbrowser.shared.credentials

enum class TotpAlgorithm(val wireName: String) {
    Sha1("SHA1"),
    Sha256("SHA256"),
    Sha512("SHA512"),
}

/** One two-factor key: the shared secret and how codes are made from it (RFC 6238). */
data class TotpConfig(
    val secretBase32: String,
    val algorithm: TotpAlgorithm = TotpAlgorithm.Sha1,
    val digits: Int = TotpRules.DEFAULT_DIGITS,
    val period: Int = TotpRules.DEFAULT_PERIOD,
) {
    override fun toString(): String = "TotpConfig(secret=<redacted>, algorithm=$algorithm, digits=$digits, period=$period)"
}

/**
 * Two-factor codes (Q21c). A key comes as an `otpauth://totp/…` link or as the bare Base32 key a
 * site shows next to its QR code; the vault keeps it as one canonical link without the label. The
 * HMAC comes in from outside ([hmac]), so the rules are tested without a platform.
 */
object TotpRules {
    const val DEFAULT_DIGITS = 6
    const val DEFAULT_PERIOD = 30
    private const val MIN_SECRET_BYTES = 10
    private const val MAX_SECRET_BYTES = 128
    private const val MAX_INPUT_LENGTH = 2_048
    private const val SCHEME = "otpauth://totp/"
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DIGIT_RANGE = 6..8
    private val PERIOD_RANGE = 10..300

    /** The key in [input], a link or a bare key; null when it is not a usable TOTP key. */
    fun parse(input: String): TotpConfig? {
        val text = input.trim()
        if (text.isEmpty() || text.length > MAX_INPUT_LENGTH) return null
        if (!text.lowercase().startsWith("otpauth://")) return config(text, null, null, null)
        if (!text.lowercase().startsWith(SCHEME)) return null
        val query = text.substringAfter('?', missingDelimiterValue = "")
        val parameters = query.split('&').mapNotNull { pair ->
            val name = pair.substringBefore('=').lowercase()
            val value = pair.substringAfter('=', missingDelimiterValue = "")
            name.takeIf(String::isNotEmpty)?.let { it to percentDecode(value) }
        }.toMap()
        return config(
            secret = parameters["secret"] ?: return null,
            algorithm = parameters["algorithm"],
            digits = parameters["digits"],
            period = parameters["period"],
        )
    }

    /** How the vault keeps [config]: one link, no label, so nothing but the key is stored. */
    fun canonical(config: TotpConfig): String =
        "${SCHEME}?secret=${config.secretBase32}&algorithm=${config.algorithm.wireName}" +
            "&digits=${config.digits}&period=${config.period}"

    fun counter(timeMillis: Long, period: Int): Long = timeMillis / 1_000 / period

    fun secondsLeft(timeMillis: Long, period: Int): Int = period - ((timeMillis / 1_000) % period).toInt()

    /** The code for [timeMillis] (RFC 6238 over RFC 4226's dynamic truncation). */
    fun code(
        config: TotpConfig,
        timeMillis: Long,
        hmac: (algorithm: TotpAlgorithm, key: ByteArray, message: ByteArray) -> ByteArray,
    ): String {
        val key = base32Decode(config.secretBase32) ?: return ""
        val counter = counter(timeMillis, config.period)
        val message = ByteArray(8) { index -> (counter ushr (8 * (7 - index))).toByte() }
        val digest = hmac(config.algorithm, key, message)
        key.fill(0)
        val offset = digest.last().toInt() and 0x0f
        val binary = ((digest[offset].toInt() and 0x7f) shl 24) or
            ((digest[offset + 1].toInt() and 0xff) shl 16) or
            ((digest[offset + 2].toInt() and 0xff) shl 8) or
            (digest[offset + 3].toInt() and 0xff)
        var modulus = 1
        repeat(config.digits) { modulus *= 10 }
        return (binary % modulus).toString().padStart(config.digits, '0')
    }

    /** «123 456» for reading aloud and typing; the copied code has no space. */
    fun grouped(code: String): String = when (code.length) {
        6 -> code.substring(0, 3) + " " + code.substring(3)
        8 -> code.substring(0, 4) + " " + code.substring(4)
        else -> code
    }

    fun base32Decode(text: String): ByteArray? {
        val clean = normalizeSecret(text) ?: return null
        val bytes = ArrayList<Byte>(clean.length * 5 / 8)
        var buffer = 0
        var bits = 0
        for (char in clean) {
            buffer = (buffer shl 5) or BASE32.indexOf(char)
            bits += 5
            if (bits >= 8) {
                bits -= 8
                bytes += (buffer shr bits).toByte()
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return bytes.toByteArray()
    }

    private fun config(secret: String, algorithm: String?, digits: String?, period: String?): TotpConfig? {
        val clean = normalizeSecret(secret) ?: return null
        val size = base32Decode(clean)?.size ?: return null
        if (size !in MIN_SECRET_BYTES..MAX_SECRET_BYTES) return null
        val parsedAlgorithm = if (algorithm == null) {
            TotpAlgorithm.Sha1
        } else {
            TotpAlgorithm.entries.firstOrNull { it.wireName.equals(algorithm, ignoreCase = true) } ?: return null
        }
        val parsedDigits = digits?.toIntOrNull()?.takeIf { it in DIGIT_RANGE } ?: if (digits == null) DEFAULT_DIGITS else return null
        val parsedPeriod = period?.toIntOrNull()?.takeIf { it in PERIOD_RANGE } ?: if (period == null) DEFAULT_PERIOD else return null
        return TotpConfig(clean, parsedAlgorithm, parsedDigits, parsedPeriod)
    }

    /** Upper case, without spaces, dashes and padding; null when another character is left. */
    private fun normalizeSecret(text: String): String? {
        val clean = text.uppercase().filterNot { it == ' ' || it == '-' || it == '=' }
        return clean.takeIf { it.isNotEmpty() && it.all { char -> char in BASE32 } }
    }

    private fun percentDecode(value: String): String {
        val bytes = ArrayList<Byte>(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '%' && index + 2 <= value.lastIndex) {
                val hex = value.substring(index + 1, index + 3).toIntOrNull(16)
                if (hex != null) {
                    bytes += hex.toByte()
                    index += 3
                    continue
                }
            }
            char.toString().encodeToByteArray().forEach { bytes += it }
            index++
        }
        return bytes.toByteArray().decodeToString()
    }
}
