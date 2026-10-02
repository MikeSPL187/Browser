package dev.sk2andy.materialbrowser.ui

import java.net.URI
import java.util.Locale

/** What TalkBack reads for a tab card: its title and site, then whether it is current or pinned. */
internal object TabCardDescriptionRules {
    /** The site of a web page without `www.`; `null` for blank tabs and pages not on the web. */
    fun site(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        return uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.takeIf(String::isNotBlank)
    }

    /** The title, then the site unless the title already is the site. */
    fun parts(title: String, url: String): List<String> {
        val cleanTitle = title.trim()
        val site = site(url)?.takeUnless { it.equals(cleanTitle, ignoreCase = true) }
        return listOfNotNull(cleanTitle.takeIf(String::isNotEmpty), site)
    }

    fun join(parts: List<String>): String = parts.joinToString(separator = ", ")
}
