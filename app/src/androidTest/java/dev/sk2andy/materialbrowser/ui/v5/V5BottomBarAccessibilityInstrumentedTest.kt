package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.AddressBarTestTags
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** System-v5.md 8: the capsule's swipes are TalkBack actions, offered only where they lead. */
@RunWith(AndroidJUnit4::class)
class V5BottomBarAccessibilityInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private fun show(tabSwipeEnabled: Boolean, hasTab: (Int) -> Boolean) {
        val gestures = V5BarGestures(
            tabSwipeEnabled = tabSwipeEnabled,
            onTabDrag = {},
            onTabDragStopped = {},
            overviewEnabled = true,
            overviewProgress = mutableFloatStateOf(0f),
            onOverviewProgress = {},
            onOverviewStarted = {},
            onOverviewCancelled = {},
            hasTab = hasTab,
            onStepTab = { step -> calls += "step $step" },
        )
        composeRule.setContent {
            VolaV5Theme(dark = false) {
                Box(Modifier.fillMaxSize()) {
                    V5BottomBar(
                        state = V5BarState(
                            tabId = "tab",
                            domain = "polar-notes.example",
                            isLoading = false,
                            progress = 0f,
                            tabCount = 3,
                            siteAvailable = true,
                            permissionActivity = false,
                            connectionKind = SiteConnectionKind.Https,
                            blockedCount = 0,
                            readerAvailable = false,
                        ),
                        gestures = gestures,
                        tint = V5SiteTints.of(null, V5Theme.colors.chrome, V5Theme.colors.label),
                        collapsed = false,
                        onExpand = {},
                        onAddress = {},
                        onShield = {},
                        onReloadOrStop = {},
                        onTabs = { calls += "tabs" },
                        onBarPositioned = { _, _ -> },
                        menuButton = { IconButton(onClick = {}) { Icon(VolaIcons.MoreVert, contentDescription = null) } },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun actions() = composeRule.onNodeWithTag(AddressBarTestTags.PrimaryField).fetchSemanticsNode()
        .config.getOrElse(SemanticsActions.CustomActions) { emptyList() }

    @Test
    fun aMiddleTabOffersBothNeighboursAndTheOverview() {
        show(tabSwipeEnabled = true) { true }

        val actions = actions()
        assertEquals(
            listOf(
                context.getString(R.string.v5_bar_next_tab),
                context.getString(R.string.v5_bar_previous_tab),
                context.getString(R.string.v5_bar_open_tabs),
            ),
            actions.map { it.label },
        )
        actions.forEach { composeRule.runOnIdle { it.action() } }
        assertEquals(listOf("step 1", "step -1", "tabs"), calls)
    }

    @Test
    fun theLastTabOffersOnlyThePreviousOne() {
        show(tabSwipeEnabled = true) { step -> step < 0 }

        assertEquals(
            listOf(context.getString(R.string.v5_bar_previous_tab), context.getString(R.string.v5_bar_open_tabs)),
            actions().map { it.label },
        )
    }

    @Test
    fun withoutTabSwipesOnlyTheOverviewStays() {
        show(tabSwipeEnabled = false) { true }

        assertEquals(listOf(context.getString(R.string.v5_bar_open_tabs)), actions().map { it.label })
    }
}
