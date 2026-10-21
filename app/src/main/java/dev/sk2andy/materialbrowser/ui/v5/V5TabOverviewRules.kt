package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.TabSearchRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
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

/** What an empty grid says, with its one way on (О-3). */
enum class V5OverviewEmpty {
    /** A search found nothing: it names the query. */
    NoMatches,

    /** The private segment has no tabs yet: it says what one is, with «New private tab». */
    NoPrivateTabs,

    /** The profile has no private tabs at all (a synced profile, an engine without them). */
    PrivateUnavailable,

    /** The tabs segment is empty: «New tab». */
    NoTabs,
}

object V5TabOverviewRules {
    /** Why the grid is empty: a search first, then the segment and whether it can have tabs. */
    fun empty(query: String, privateSegment: Boolean, canOpenPrivate: Boolean): V5OverviewEmpty = when {
        query.isNotBlank() -> V5OverviewEmpty.NoMatches
        privateSegment && !canOpenPrivate -> V5OverviewEmpty.PrivateUnavailable
        privateSegment -> V5OverviewEmpty.NoPrivateTabs
        else -> V5OverviewEmpty.NoTabs
    }

    /** Whether «+» adds a tab to the segment in front: a private one only where the profile has them (О-2). */
    fun canAddTab(privateSegment: Boolean, canOpenPrivate: Boolean): Boolean = !privateSegment || canOpenPrivate

    /**
     * What «Close all» closes (О-1): the cards in front of the user, so the count it asks about is
     * the one on screen. Another segment, tabs behind the private lock and pinned tabs stay.
     */
    fun closeAllIds(cards: List<V5TabCard>): List<String> = cards.filter(V5TabCard::closable).map(V5TabCard::id)

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

/** A tab group as the v5 overview draws it (V12, canvas screen «г»). */
data class V5OverviewGroup(
    val id: String,
    val name: String,
    val tabIds: List<String>,
    val collapsed: Boolean,
    val tile: V5Tile,
)

/** One line of the overview grid: a group's header, a collapsed group, «No group», or a card. */
sealed interface V5OverviewItem {
    val key: String

    data class GroupHeader(val group: V5OverviewGroup, val count: Int) : V5OverviewItem {
        override val key: String get() = "group:${group.id}"
    }

    data class CollapsedGroup(val group: V5OverviewGroup, val count: Int, val previewIds: List<String>) : V5OverviewItem {
        override val key: String get() = "collapsed:${group.id}"
    }

    data object UngroupedHeader : V5OverviewItem {
        override val key: String get() = "ungrouped"
    }

    data class Card(val id: String, val group: V5OverviewGroup?) : V5OverviewItem {
        override val key: String get() = id
    }
}

object V5OverviewGroupRules {
    /** A collapsed group shows a fan of up to this many of its pages. */
    const val COLLAPSED_PREVIEWS = 3

    /**
     * The overview's lines for the [visibleIds] cards in tab order: each group with a visible tab as
     * a header and its cards, or one row when collapsed, in group order; then «No group» and the
     * rest. Without groups, and while searching, the cards stay one flat grid.
     */
    fun items(visibleIds: List<String>, groups: List<V5OverviewGroup>, searching: Boolean): List<V5OverviewItem> {
        if (searching) return visibleIds.map { V5OverviewItem.Card(it, null) }
        val visible = visibleIds.toSet()
        val shown = groups.mapNotNull { group ->
            val members = group.tabIds.filter { it in visible }
            if (members.isEmpty()) null else group to members.sortedBy(visibleIds::indexOf)
        }
        if (shown.isEmpty()) return visibleIds.map { V5OverviewItem.Card(it, null) }
        val grouped = shown.flatMapTo(hashSetOf()) { (_, members) -> members }
        return buildList {
            shown.forEach { (group, members) ->
                if (group.collapsed) {
                    add(V5OverviewItem.CollapsedGroup(group, members.size, members.take(COLLAPSED_PREVIEWS)))
                } else {
                    add(V5OverviewItem.GroupHeader(group, members.size))
                    members.forEach { add(V5OverviewItem.Card(it, group)) }
                }
            }
            val rest = visibleIds.filterNot { it in grouped }
            if (rest.isNotEmpty()) {
                add(V5OverviewItem.UngroupedHeader)
                rest.forEach { add(V5OverviewItem.Card(it, null)) }
            }
        }
    }

    /** Where the grid starts so [tabId]'s card, or its collapsed group, is on screen. */
    fun indexOf(items: List<V5OverviewItem>, tabId: String?): Int {
        if (tabId == null) return 0
        val index = items.indexOfFirst { item ->
            when (item) {
                is V5OverviewItem.Card -> item.id == tabId
                is V5OverviewItem.CollapsedGroup -> tabId in item.group.tabIds
                else -> false
            }
        }
        if (index < 0) return 0
        // Start at the group's header so the card is seen with its group.
        val header = (index downTo 0).firstOrNull { items[it] is V5OverviewItem.GroupHeader }
        val card = items[index]
        return if (card is V5OverviewItem.Card && card.group != null && header != null) header else index
    }
}
