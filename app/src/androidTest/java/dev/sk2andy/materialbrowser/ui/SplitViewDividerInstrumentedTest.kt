package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserContentFrame
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.SplitViewRules
import dev.sk2andy.materialbrowser.browser.SplitViewState
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaSplit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplitViewDividerInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pillSwapsClosesAndSnapsFromAccessibilityActions() {
        val events = mutableListOf<String>()
        composeRule.setContent {
            MaterialBrowserTheme {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val card = BrowserContentFrame(leftPx = 0, topPx = 0, rightPx = 0, bottomPx = 0)
                    val heightPx = constraints.maxHeight.toFloat()
                    SplitViewDivider(
                        layout = SplitViewLayout(
                            state = SplitViewState(companionTabId = "b"),
                            companion = BrowserTab(id = "b", lastAccessedAt = 0),
                            card = card,
                            frames = SplitViewRules.frames(
                                card = card,
                                heightPx = heightPx.toInt(),
                                dividerPx = with(density) { VolaSplit.dividerHeight.roundToPx() },
                                topRatio = SplitViewRules.HALF,
                            ),
                        ),
                        heightPx = heightPx,
                        onRatioChange = { ratio -> events += "ratio:$ratio" },
                        onSwap = { events += "swap" },
                        onClose = { events += "close" },
                    )
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithTag(SplitViewTestTags.Swap).performClick()
        composeRule.onNodeWithTag(SplitViewTestTags.Close).performClick()
        val handle = composeRule.onNodeWithContentDescription(
            context.getString(R.string.split_view_resize),
        )
        val actions = handle.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("33%", "50%", "67%"), actions.map { it.label })
        composeRule.runOnIdle { actions.first().action() }

        assertEquals(listOf("swap", "close", "ratio:${SplitViewRules.THIRD}"), events)
    }
}
