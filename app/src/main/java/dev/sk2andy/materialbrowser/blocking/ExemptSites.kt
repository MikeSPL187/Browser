package dev.sk2andy.materialbrowser.blocking

import java.net.URI

/**
 * The bank, payment and government sites Vola's own Gecko blocker leaves alone
 * (`financial_sites.txt`, read by the extension as well): one domain per line, covering its
 * subdomains.
 */
internal class ExemptSites private constructor(private val sites: Set<String>) {
    fun containsHost(host: String?): Boolean {
        var candidate = CandyHostCanonicalizer.canonicalHost(host ?: return false) ?: return false
        while (true) {
            if (candidate in sites) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }

    fun containsUrl(url: String?): Boolean {
        val uri = runCatching { URI(url ?: return false) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in setOf("http", "https") && containsHost(uri.host)
    }

    companion object {
        val None = ExemptSites(emptySet())

        fun parse(text: String): ExemptSites = ExemptSites(
            text.lineSequence()
                .map { line -> line.trim().lowercase() }
                .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                .toSet(),
        )
    }
}
