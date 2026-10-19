package dev.sk2andy.materialbrowser.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The page blocks a person hid with «Hide distracting items» (v5 V20), as CSS selectors per site.
 * A site is the page host without `www.`. The most recently changed site and the newest selector
 * come last, so the limits drop what was hidden longest ago.
 */
internal data class HiddenDistractions(val sites: Map<String, List<String>> = emptyMap()) {
    fun selectorsFor(host: String?): List<String> =
        HiddenDistractionsRules.siteOf(host)?.let(sites::get).orEmpty()
}

internal object HiddenDistractionsRules {
    const val MAX_SELECTORS_PER_SITE = 32
    const val MAX_SITES = 200
    const val MAX_SELECTOR_LENGTH = 256

    // What `hide_distractions.js` writes: ids, tags, stable classes, `:nth-of-type(n)` and ` > `.
    // Anything else, such as a brace that would end the rule, never reaches a page's style sheet.
    private val SELECTOR = Regex("^[A-Za-z0-9_\\-#.>:() ]+$")
    private val HOST = Regex("^[a-z0-9.-]+$")

    fun siteOf(host: String?): String? = host?.trim()?.trimEnd('.')?.lowercase()
        ?.removePrefix("www.")
        ?.takeIf { it.length in 1..253 && HOST.matches(it) }

    fun isValidSelector(selector: String): Boolean =
        selector.length in 1..MAX_SELECTOR_LENGTH && selector.isNotBlank() && SELECTOR.matches(selector)

    fun hide(state: HiddenDistractions, host: String?, selector: String): HiddenDistractions {
        val site = siteOf(host) ?: return state
        if (!isValidSelector(selector)) return state
        val selectors = (state.sites[site].orEmpty() - selector + selector).takeLast(MAX_SELECTORS_PER_SITE)
        val sites = state.sites.entries.filter { it.key != site }.takeLast(MAX_SITES - 1)
            .associate { it.key to it.value } + (site to selectors)
        return HiddenDistractions(sites)
    }

    /** Shows [selector] on [host]'s site again; a site left with nothing hidden goes from the list. */
    fun show(state: HiddenDistractions, host: String?, selector: String): HiddenDistractions {
        val site = siteOf(host) ?: return state
        val selectors = state.sites[site] ?: return state
        if (selector !in selectors) return state
        val rest = selectors - selector
        return HiddenDistractions(if (rest.isEmpty()) state.sites - site else state.sites + (site to rest))
    }

    fun showAll(state: HiddenDistractions, host: String?): HiddenDistractions {
        val site = siteOf(host) ?: return state
        return if (site in state.sites) HiddenDistractions(state.sites - site) else state
    }
}

/** `{"sites":[{"h":"example.com","s":["#promo"]}]}`; the list keeps the order of the limits. */
internal object HiddenDistractionsCodec {
    fun encode(state: HiddenDistractions): String = JSONObject().put(
        "sites",
        JSONArray().also { sites ->
            state.sites.forEach { (site, selectors) ->
                sites.put(JSONObject().put("h", site).put("s", JSONArray(selectors)))
            }
        },
    ).toString()

    /** Anything unreadable or past the limits is left out rather than failing the whole list. */
    fun decode(text: String?): HiddenDistractions {
        if (text.isNullOrBlank()) return HiddenDistractions()
        val sites = runCatching { JSONObject(text).optJSONArray("sites") }.getOrNull()
            ?: return HiddenDistractions()
        var state = HiddenDistractions()
        for (index in 0 until sites.length()) {
            val site = sites.optJSONObject(index) ?: continue
            val host = site.optString("h")
            val selectors = site.optJSONArray("s") ?: continue
            for (selector in 0 until selectors.length()) {
                state = HiddenDistractionsRules.hide(state, host, selectors.optString(selector))
            }
        }
        return state
    }
}
