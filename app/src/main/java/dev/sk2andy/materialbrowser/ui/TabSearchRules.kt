package dev.sk2andy.materialbrowser.ui

import java.util.Locale

/** Finding a tab in the overview by what the user remembers: words of its title or its address. */
internal object TabSearchRules {
    /** Every word of [query] appears in the title or the address, ignoring case. */
    fun matches(title: String, url: String, query: String): Boolean {
        val words = words(query)
        if (words.isEmpty()) return true
        val haystack = "${title.lowercase(Locale.ROOT)} ${url.lowercase(Locale.ROOT)}"
        return words.all(haystack::contains)
    }

    /** [items] in their order; all of them while the query is blank. */
    fun <T> filter(
        items: List<T>,
        query: String?,
        title: (T) -> String,
        url: (T) -> String,
    ): List<T> {
        if (query.isNullOrBlank()) return items
        return items.filter { item -> matches(title(item), url(item), query) }
    }

    private fun words(query: String): List<String> =
        query.lowercase(Locale.ROOT).split(' ', '\t', '\n').filter(String::isNotBlank)
}
