package dev.sk2andy.materialbrowser.browser

import java.net.IDN
import java.net.URI
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Locale

/**
 * The certificate of the page's own site, as Site info shows it (board W-SiteInfo, Q10b). Engine
 * neutral: GeckoView and the System WebView both hand over an [X509Certificate].
 */
data class SiteCertificate(
    val host: String,
    val subjectName: String?,
    val issuerName: String?,
    val validFromMillis: Long?,
    val validUntilMillis: Long?,
    val sha256Fingerprint: String?,
)

internal object SiteCertificateRules {
    fun fromX509(host: String?, certificate: X509Certificate?): SiteCertificate? {
        val safeHost = host?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return null
        val cert = certificate ?: return null
        val issuer = cert.issuerX500Principal.name
        return SiteCertificate(
            host = safeHost,
            subjectName = x500Attribute(cert.subjectX500Principal.name, "CN"),
            issuerName = x500Attribute(issuer, "O") ?: x500Attribute(issuer, "CN"),
            validFromMillis = cert.notBefore?.time,
            validUntilMillis = cert.notAfter?.time,
            sha256Fingerprint = runCatching { fingerprint(cert.encoded) }.getOrNull(),
        )
    }

    /** One attribute of an RFC 2253 name («CN=example.com,O=Example\, Inc.»), unescaped. */
    fun x500Attribute(name: String, key: String): String? {
        var start = 0
        var escaped = false
        val parts = mutableListOf<String>()
        name.forEachIndexed { index, char ->
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == ',' || char == '+' -> {
                    parts += name.substring(start, index)
                    start = index + 1
                }
            }
        }
        parts += name.substring(start)
        return parts.firstNotNullOfOrNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0 || !part.substring(0, separator).trim().equals(key, ignoreCase = true)) {
                null
            } else {
                unescape(part.substring(separator + 1).trim()).takeIf(String::isNotEmpty)
            }
        }
    }

    /** SHA-256 of the certificate, as browsers print it: «AB:CD:…». */
    fun fingerprint(encoded: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(encoded)
            .joinToString(":") { byte -> "%02X".format(byte.toInt() and 0xFF) }

    /** The fingerprint in lines of [bytesPerLine] bytes, so a narrow screen never splits a byte. */
    fun fingerprintLines(fingerprint: String, bytesPerLine: Int = 8): String =
        fingerprint.split(':').chunked(bytesPerLine).joinToString("\n") { it.joinToString(":") }

    /** A certificate is shown only for the page it came with: a stale one from another host is not. */
    fun forPage(certificate: SiteCertificate?, pageUrl: String): SiteCertificate? {
        val uri = runCatching { URI(pageUrl.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        val pageHost = uri.host ?: return null
        return certificate?.takeIf { asciiHost(it.host) == asciiHost(pageHost) }
    }

    fun isExpired(certificate: SiteCertificate, nowMillis: Long): Boolean =
        certificate.validUntilMillis?.let { nowMillis > it } == true

    private fun asciiHost(host: String): String =
        runCatching { IDN.toASCII(host) }.getOrDefault(host).lowercase(Locale.ROOT)

    private fun unescape(value: String): String {
        val text = value.removeSurrounding("\"")
        if ('\\' !in text) return text
        val result = StringBuilder(text.length)
        var escaped = false
        text.forEach { char ->
            if (escaped) {
                result.append(char)
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }
}
