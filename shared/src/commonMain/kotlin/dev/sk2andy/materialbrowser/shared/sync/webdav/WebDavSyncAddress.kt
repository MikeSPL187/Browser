package dev.sk2andy.materialbrowser.shared.sync.webdav

/** A typed server address checked before any request. */
sealed interface WebDavAddressCheck {
    /** [base] is the HTTPS address with its path ending in `/`; [host] is for the screens. */
    data class Valid(val base: String, val host: String) : WebDavAddressCheck

    data object NotHttps : WebDavAddressCheck

    /** Not an address: no host, spaces, a login inside it, a query or a fragment. */
    data object Invalid : WebDavAddressCheck
}

/**
 * Where Vola's files live on a typed server (V27). The person types the server's address; for
 * Nextcloud that is usually the bare address, and the files' WebDAV root is
 * `remote.php/dav/files/<login>/` under it. Vola keeps everything in [FOLDER] there.
 */
object WebDavSyncAddressRules {
    const val FOLDER = "Vola/"
    const val MAX_ADDRESS_CHARS = 2_048

    private const val NEXTCLOUD_FILES = "remote.php/dav/files/"
    private const val NEXTCLOUD_LEGACY = "remote.php/webdav/"

    /** Whether [address] can be used: HTTPS only, the certificate checked by the system. */
    fun check(address: String): WebDavAddressCheck {
        val trimmed = address.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_ADDRESS_CHARS) return WebDavAddressCheck.Invalid
        val scheme = trimmed.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme == "http") return WebDavAddressCheck.NotHttps
        if (scheme != "https") return WebDavAddressCheck.Invalid
        if (trimmed.any { it.isWhitespace() || it.isISOControl() || it == '?' || it == '#' || it == '\\' }) {
            return WebDavAddressCheck.Invalid
        }
        val rest = trimmed.substring("https://".length)
        val authority = rest.substringBefore('/')
        if (authority.isEmpty() || '@' in authority) return WebDavAddressCheck.Invalid
        val host = authority.substringBefore(':').lowercase()
        val port = authority.substringAfter(':', missingDelimiterValue = "")
        if (host.isEmpty() || (authority.contains(':') && port.toIntOrNull()?.takeIf { it in 1..65_535 } == null)) {
            return WebDavAddressCheck.Invalid
        }
        val path = rest.substring(authority.length).ifEmpty { "/" }
        if (path.split('/').any { it == "." || it == ".." }) return WebDavAddressCheck.Invalid
        val base = "https://${authority.lowercase()}${path.removeSuffix("/")}/"
        return WebDavAddressCheck.Valid(base, host)
    }

    /**
     * The WebDAV roots to try for [base] and [login], in order: Nextcloud's files root first
     * unless the address already names a WebDAV path, then the address itself.
     */
    fun roots(base: String, login: String): List<String> {
        val lower = base.lowercase()
        if (NEXTCLOUD_FILES in lower || NEXTCLOUD_LEGACY in lower) return listOf(base)
        return listOf("$base$NEXTCLOUD_FILES${encodePathSegment(login)}/", base)
    }

    fun folder(root: String): String = "$root$FOLDER"

    /** The header with the key's salt; readable without the phrase, it holds no secret. */
    fun header(root: String): String = "${folder(root)}${WebDavSyncFormat.HEADER_FILE}"

    fun devices(root: String): String = "${folder(root)}${WebDavSyncFormat.DEVICES_FOLDER}"

    fun device(root: String, deviceId: String): String = "${devices(root)}$deviceId${WebDavSyncFormat.DEVICE_FILE_SUFFIX}"

    /** [value] as one URL path segment: UTF-8, everything but the unreserved characters escaped. */
    fun encodePathSegment(value: String): String = buildString {
        value.encodeToByteArray().forEach { byte ->
            val char = (byte.toInt() and 0xFF).toChar()
            if (char.isLetterOrDigit() && char.code < 0x80 || char in "-._~") {
                append(char)
            } else {
                append('%')
                append(HEX[byte.toInt() shr 4 and 0xF])
                append(HEX[byte.toInt() and 0xF])
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
}
