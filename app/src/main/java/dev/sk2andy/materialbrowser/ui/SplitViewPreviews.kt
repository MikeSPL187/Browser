package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.browser.BrowserContentFrame
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.SplitPane
import dev.sk2andy.materialbrowser.browser.SplitViewRules
import dev.sk2andy.materialbrowser.browser.SplitViewState
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSplit
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

/** The two cards with the active one ringed and the divider pill between them (board W-Split). */
@Composable
private fun SplitViewPreview(activePane: SplitPane, topRatio: Float) {
    MaterialBrowserTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(VolaTheme.auraBrush),
        ) {
            val density = LocalDensity.current
            val heightPx = constraints.maxHeight.toFloat()
            val card = with(density) {
                BrowserContentFrame(
                    leftPx = 6.dp.roundToPx(),
                    topPx = 40.dp.roundToPx(),
                    rightPx = 6.dp.roundToPx(),
                    bottomPx = 92.dp.roundToPx(),
                )
            }
            val state = SplitViewState(
                companionTabId = "docs",
                activePane = activePane,
                topRatio = topRatio,
            )
            val layout = SplitViewLayout(
                state = state,
                companion = BrowserTab(id = "docs", lastAccessedAt = 0),
                card = card,
                frames = SplitViewRules.frames(
                    card = card,
                    heightPx = heightPx.toInt(),
                    dividerPx = with(density) { VolaSplit.dividerHeight.roundToPx() },
                    topRatio = topRatio,
                ),
            )
            listOf(SplitPane.Top, SplitPane.Bottom).forEach { pane ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .contentFramePadding(layout.frames.of(pane))
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
            BrowserCardsMask(
                frames = listOf(layout.frames.top, layout.frames.bottom),
                highlighted = activePane.ordinal,
                modifier = Modifier.fillMaxSize(),
            )
            SplitViewDivider(
                layout = layout,
                heightPx = heightPx,
                onRatioChange = {},
                onSwap = {},
                onClose = {},
            )
        }
    }
}

@VolaPreviews
@Composable
private fun SplitViewHalvesPreview() {
    SplitViewPreview(activePane = SplitPane.Top, topRatio = SplitViewRules.HALF)
}

/** The lower card active, the divider at a third. */
@VolaPreviews
@Composable
private fun SplitViewThirdPreview() {
    SplitViewPreview(activePane = SplitPane.Bottom, topRatio = SplitViewRules.THIRD)
}
