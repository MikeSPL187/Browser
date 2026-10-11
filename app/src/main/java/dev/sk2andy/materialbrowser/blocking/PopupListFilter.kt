package dev.sk2andy.materialbrowser.blocking

import android.content.res.AssetManager
import dev.sk2andy.materialbrowser.browser.PopupFilterDecision
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * The uAssets `$popup` and `$popunder` rules for Vola's own Gecko blocker. System WebView asks its
 * [ContentBlocker]; the Gecko build never loads that snapshot (its host lists live in the extension),
 * so it reads only the advanced rules, a 70 KB list parsed off the main thread from the moment the
 * filter exists: the controller creates it with the first policy of Vola's own blocker, well before
 * the first popup. A popup that a bank or payment site opens, or that opens one (a 3-D Secure
 * check, a payment window), is let through: the extension leaves those sites alone too.
 */
internal class PopupListFilter(
    private val loadWaitMillis: Long = 0,
    loadExemptSites: () -> String = { "" },
    load: () -> String,
) {
    private class Lists(val rules: AdvancedFilterRules, val exemptSites: ExemptSites)

    private val lists = CompletableFuture.supplyAsync {
        Lists(
            rules = runCatching { AdvancedFilterRules.parse(load()) }.getOrDefault(AdvancedFilterRules.Empty),
            exemptSites = runCatching { ExemptSites.parse(loadExemptSites()) }.getOrDefault(ExemptSites.None),
        )
    }

    fun decidePopup(targetUrl: String, openerUrl: String): PopupFilterDecision =
        decide(targetUrl, openerUrl) { rules -> rules.decidePopup(targetUrl, openerUrl) }

    fun decidePopunder(openerTargetUrl: String, childUrl: String): PopupFilterDecision =
        decide(openerTargetUrl, childUrl) { rules -> rules.decidePopunder(openerTargetUrl, childUrl) }

    private fun decide(
        firstUrl: String,
        secondUrl: String,
        decision: (AdvancedFilterRules) -> AdvancedFilterAction?,
    ): PopupFilterDecision {
        val loaded = loaded() ?: return PopupFilterDecision.NoMatch
        if (loaded.exemptSites.containsUrl(firstUrl) || loaded.exemptSites.containsUrl(secondUrl)) {
            return PopupFilterDecision.Allow
        }
        return decision(loaded.rules).toPopupFilterDecision()
    }

    /** Nothing to do: creating the filter has started reading the list. */
    fun preload() = Unit

    // Decisions run on the main thread, which never waits: a popup opened before the list is ready
    // goes through unfiltered. Only tests wait ([loadWaitMillis]).
    private fun loaded(): Lists? = if (loadWaitMillis <= 0) {
        lists.getNow(null)
    } else {
        runCatching { lists.get(loadWaitMillis, TimeUnit.MILLISECONDS) }.getOrNull()
    }

    private fun AdvancedFilterAction?.toPopupFilterDecision(): PopupFilterDecision = when (this) {
        AdvancedFilterAction.Block -> PopupFilterDecision.Block
        AdvancedFilterAction.Allow -> PopupFilterDecision.Allow
        null -> PopupFilterDecision.NoMatch
    }

    companion object {
        const val ASSET = "uassets_advanced_filters.txt"
        const val EXEMPT_SITES_ASSET = "financial_sites.txt"

        fun fromAssets(assets: AssetManager): PopupListFilter {
            fun read(name: String) = BlockingListAssets.open(assets, name).bufferedReader().use { it.readText() }
            return PopupListFilter(loadExemptSites = { read(EXEMPT_SITES_ASSET) }) { read(ASSET) }
        }
    }
}
