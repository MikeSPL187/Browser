package dev.sk2andy.materialbrowser.blocking

import android.content.res.AssetManager
import dev.sk2andy.materialbrowser.browser.PopupFilterDecision
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * The uAssets `$popup` and `$popunder` rules for Vola's own Gecko blocker. System WebView asks its
 * [ContentBlocker]; the Gecko build never loads that snapshot (its host lists live in the extension),
 * so it reads only the advanced rules, a 70 KB list parsed off the main thread.
 */
internal class PopupListFilter(
    private val loadWaitMillis: Long = LOAD_WAIT_MILLIS,
    load: () -> String,
) {
    private val rules = CompletableFuture.supplyAsync {
        runCatching { AdvancedFilterRules.parse(load()) }.getOrDefault(AdvancedFilterRules.Empty)
    }

    fun decidePopup(targetUrl: String, openerUrl: String): PopupFilterDecision =
        (loaded()?.decidePopup(targetUrl, openerUrl)).toPopupFilterDecision()

    fun decidePopunder(openerTargetUrl: String, childUrl: String): PopupFilterDecision =
        (loaded()?.decidePopunder(openerTargetUrl, childUrl)).toPopupFilterDecision()

    // A popup opened before the list is ready waits for it briefly, then goes through unfiltered.
    private fun loaded(): AdvancedFilterRules? =
        runCatching { rules.get(loadWaitMillis, TimeUnit.MILLISECONDS) }.getOrNull()

    private fun AdvancedFilterAction?.toPopupFilterDecision(): PopupFilterDecision = when (this) {
        AdvancedFilterAction.Block -> PopupFilterDecision.Block
        AdvancedFilterAction.Allow -> PopupFilterDecision.Allow
        null -> PopupFilterDecision.NoMatch
    }

    companion object {
        const val ASSET = "uassets_advanced_filters.txt"
        const val LOAD_WAIT_MILLIS = 300L

        fun fromAssets(assets: AssetManager): PopupListFilter = PopupListFilter {
            BlockingListAssets.open(assets, ASSET).bufferedReader().use { it.readText() }
        }
    }
}
