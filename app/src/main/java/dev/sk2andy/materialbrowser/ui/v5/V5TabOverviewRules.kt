package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.TabSearchRules

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
}
