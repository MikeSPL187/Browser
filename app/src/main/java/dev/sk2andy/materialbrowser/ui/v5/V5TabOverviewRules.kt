package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.TabSearchRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import kotlin.math.abs
import kotlin.math.sign

/*
 * What the v5 tab overview shows (system-v5.md, section 10): the «Tabs» or «Private» segment of the
 * profile's tabs, narrowed by the search at the top. Locked private tabs never show.
 */

/** One tab as the overview needs it, without Android or engine types. */
data class V5OverviewTab(
    val id: String,
    val title: String,
    val url: String,
    val isPrivate: Boolean,
)

data class V5OverviewCounts(val regular: Int, val private: Int)

object V5TabOverviewRules {
    /** Tabs and private tabs the segment chips count; hidden (locked) tabs do not count. */
    fun counts(tabs: List<V5OverviewTab>, hidden: (V5OverviewTab) -> Boolean): V5OverviewCounts {
        val visible = tabs.filterNot(hidden)
        return V5OverviewCounts(
            regular = visible.count { !it.isPrivate },
            private = visible.count { it.isPrivate },
        )
    }

    /** The cards of one segment that match [query], in tab order. */
    fun visibleTabs(
        tabs: List<V5OverviewTab>,
        privateSegment: Boolean,
        query: String,
        hidden: (V5OverviewTab) -> Boolean,
    ): List<V5OverviewTab> = TabSearchRules.filter(
        items = tabs.filter { it.isPrivate == privateSegment && !hidden(it) },
        query = query,
        title = V5OverviewTab::title,
        url = V5OverviewTab::url,
    )

    /**
     * The segment to open on: the selected tab's, so the overview starts where the user is. With
     * no private tabs left the private segment falls back to tabs.
     */
    fun initialSegmentIsPrivate(selectedIsPrivate: Boolean, counts: V5OverviewCounts): Boolean =
        selectedIsPrivate && counts.private > 0

    /**
     * Whether a card let go at [offsetPx] with [velocityPxPerSecond] closes: past 40 % of its
     * [widthPx], or flung at 1000 dp/s in the direction it was dragged.
     */
    fun closes(offsetPx: Float, velocityPxPerSecond: Float, widthPx: Float, density: Float): Boolean {
        if (widthPx <= 0f || offsetPx == 0f) return false
        if (abs(offsetPx) >= widthPx * V5Motion.CARD_CLOSE_COMMIT_FRACTION) return true
        val fling = abs(velocityPxPerSecond) >= V5Motion.TAB_SWIPE_COMMIT_VELOCITY_DP * density
        return fling && sign(velocityPxPerSecond) == sign(offsetPx)
    }

    /** A swiped card fades as it travels, never below 1 − [V5Motion.CARD_CLOSE_FADE]. */
    fun swipeAlpha(offsetPx: Float, widthPx: Float): Float {
        if (widthPx <= 0f) return 1f
        return 1f - (abs(offsetPx) / widthPx).coerceIn(0f, 1f) * V5Motion.CARD_CLOSE_FADE
    }
}
