package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToInt

/** The halves of the phone's Split View (board W-Split): one above the other. */
enum class SplitPane {
    Top,
    Bottom,
    ;

    val other: SplitPane get() = if (this == Top) Bottom else Top
}

/**
 * Split View: the selected tab fills [activePane] and the bar drives it; [companionTabId] fills
 * the other pane. [topRatio] is the top pane's share of the height the two panes split.
 */
@Immutable
data class SplitViewState(
    val companionTabId: String,
    val activePane: SplitPane = SplitPane.Top,
    val topRatio: Float = SplitViewRules.HALF,
) {
    val companionPane: SplitPane get() = activePane.other
}

/** Where the two cards sit inside the single page card, as page-host frames. */
@Immutable
data class SplitViewFrames(
    val top: BrowserContentFrame,
    val bottom: BrowserContentFrame,
) {
    fun of(pane: SplitPane): BrowserContentFrame = if (pane == SplitPane.Top) top else bottom
}

object SplitViewRules {
    const val THIRD = 1f / 3f
    const val HALF = 0.5f
    const val TWO_THIRDS = 2f / 3f

    /** Stops the divider snaps to when let go. */
    val SNAP_RATIOS = listOf(THIRD, HALF, TWO_THIRDS)

    /** Let go closer than this to an edge, the divider closes Split View. */
    const val EXIT_EDGE = 0.18f

    /** While dragging, the divider stays this far from the edges. */
    const val DRAG_EDGE = 0.08f

    fun dragRatio(ratio: Float): Float = ratio.coerceIn(DRAG_EDGE, 1f - DRAG_EDGE)

    /** The ratio the divider settles at, or null when letting go there closes Split View. */
    fun settle(ratio: Float): Float? {
        if (ratio < EXIT_EDGE || ratio > 1f - EXIT_EDGE) return null
        return SNAP_RATIOS.minBy { snap -> abs(snap - ratio) }
    }

    /**
     * Splits [card] (the frame of the single page card in a window [heightPx] tall) into a top
     * and a bottom card with a [dividerPx] gap between them, the top one [topRatio] of the rest.
     */
    fun frames(
        card: BrowserContentFrame,
        heightPx: Int,
        dividerPx: Int,
        topRatio: Float,
    ): SplitViewFrames {
        val available = (heightPx - card.topPx - card.bottomPx - dividerPx).coerceAtLeast(0)
        val topHeight = (available * topRatio.coerceIn(0f, 1f)).roundToInt()
        val bottomHeight = available - topHeight
        return SplitViewFrames(
            top = card.copy(bottomPx = card.bottomPx + dividerPx + bottomHeight),
            bottom = card.copy(topPx = card.topPx + topHeight + dividerPx),
        )
    }

    /**
     * Whether [companion] may share the screen with [selected]: another tab of the same kind, so a
     * private page never sits next to a regular one, where «Lock on exit» would not hide it.
     */
    fun pairs(selected: BrowserTab, companion: BrowserTab): Boolean =
        companion.id != selected.id && companion.isIncognito == selected.isIncognito

    /**
     * The tab to put next to [selectedTabId] when Split View opens: [requestedTabId] when it is
     * another tab, or else the most recently used other tab with a page. Either way it is of the
     * same kind (private or not); a requested tab of the other kind opens nothing.
     */
    fun companionFor(
        tabs: List<BrowserTab>,
        selectedTabId: String,
        requestedTabId: String? = null,
    ): String? {
        val selected = tabs.firstOrNull { tab -> tab.id == selectedTabId } ?: return null
        tabs.firstOrNull { tab -> tab.id == requestedTabId && tab.id != selectedTabId }
            ?.let { requested -> return requested.id.takeIf { pairs(selected, requested) } }
        return tabs
            .filter { tab -> tab.url != BLANK_URL && pairs(selected, tab) }
            .maxByOrNull(BrowserTab::lastAccessedAt)
            ?.id
    }

    /**
     * The state that still holds after the tabs changed, or null when Split View must close: its
     * companion went, became the selected tab, or is of another kind than [selectedTab].
     */
    fun reconcile(
        state: SplitViewState?,
        activeTabs: List<BrowserTab>,
        selectedTab: BrowserTab,
    ): SplitViewState? = state?.takeIf { current ->
        activeTabs.firstOrNull { tab -> tab.id == current.companionTabId }
            ?.let { companion -> pairs(selectedTab, companion) } == true
    }
}

/**
 * Split View's state. The selected tab stays the one the address bar, find in page and the menu
 * work on; the companion is the tab in the other pane, kept visible and active.
 */
class SplitViewController(
    /** A tab left the companion pane because Split View closed. */
    private val onCompanionLeft: (String) -> Unit = {},
) {
    var state by mutableStateOf<SplitViewState?>(null)
        private set

    val companionTabId: String? get() = state?.companionTabId

    /** Counts the times Split View was opened, so the screen can leave the tab overview. */
    var openRequests by mutableIntStateOf(0)
        private set

    fun open(companionTabId: String) {
        state = SplitViewState(companionTabId = companionTabId)
        openRequests++
    }

    fun close() {
        val companion = companionTabId ?: return
        state = null
        onCompanionLeft(companion)
    }

    /** The two tabs trade places; the selected tab stays selected. */
    fun swap() {
        state = state?.let { current -> current.copy(activePane = current.companionPane) }
    }

    fun updateRatio(topRatio: Float) {
        state = state?.copy(topRatio = topRatio)
    }

    /**
     * The other pane becomes the active one: its tab is selected, and the tab that was selected
     * stays in its pane as the companion. [select] selects a tab in the browser.
     */
    fun activateCompanion(selectedTabId: String, select: (String) -> Unit) {
        val current = state ?: return
        state = current.copy(companionTabId = selectedTabId, activePane = current.companionPane)
        select(current.companionTabId)
    }

    fun reconcile(activeTabs: List<BrowserTab>, selectedTab: BrowserTab) {
        if (SplitViewRules.reconcile(state, activeTabs, selectedTab) == null) close()
    }
}
