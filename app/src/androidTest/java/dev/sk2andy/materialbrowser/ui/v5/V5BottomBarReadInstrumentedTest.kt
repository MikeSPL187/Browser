package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SiteTints
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * П-6 (audit 20.10): «Read» joins the capsule when a page reads as an article, and the domain
 * must not move while it comes in or after: a mirror grows on the other side on the same spring.
 * Measured on every frame of the animation, not only at rest, on the text itself (unmerged): the
 * capsule around it never moves, so measuring the merged node would pass for nothing.
 */
@RunWith(AndroidJUnit4::class)
class V5BottomBarReadInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var readable by mutableStateOf(false)

    private val gestures = V5BarGestures(
        tabSwipeEnabled = false,
        onTabDrag = {},
        onTabDragStopped = {},
        overviewEnabled = false,
        overviewProgress = mutableFloatStateOf(0f),
        onOverviewProgress = {},
        onOverviewStarted = {},
        onOverviewCancelled = {},
    )

    @Test
    fun theDomainStaysInPlaceWhileReadComesInAndGoes() {
        composeRule.setContent {
            VolaV5Theme(dark = false) {
                Box(Modifier.fillMaxSize()) {
                    V5BottomBar(
                        state = V5BarState(
                            tabId = "tab",
                            domain = DOMAIN,
                            isLoading = false,
                            progress = 0f,
                            tabCount = 3,
                            siteAvailable = true,
                            permissionActivity = false,
                            connectionKind = SiteConnectionKind.Https,
                            blockedCount = 0,
                            readerAvailable = readable,
                        ),
                        gestures = gestures,
                        tint = V5SiteTints.of(null, V5Theme.colors.chrome, V5Theme.colors.label),
                        collapsed = false,
                        onExpand = {},
                        onAddress = {},
                        onShield = {},
                        onReloadOrStop = {},
                        onTabs = {},
                        onBarPositioned = { _, _ -> },
                        menuButton = { IconButton(onClick = {}) { Icon(VolaIcons.MoreVert, contentDescription = null) } },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val atRest = domainCenter()

        composeRule.mainClock.autoAdvance = false
        readable = true
        repeat(FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            assertInPlace(atRest, "while «Read» comes in, frame $it")
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(V5BarTestTags.Read).assertExists()
        assertInPlace(atRest, "with «Read» in")

        composeRule.mainClock.autoAdvance = false
        readable = false
        repeat(FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            assertInPlace(atRest, "while «Read» goes, frame $it")
        }
        composeRule.mainClock.autoAdvance = true
    }

    private fun domainCenter(): Float {
        val bounds = composeRule.onNodeWithText(DOMAIN, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return (bounds.left + bounds.right) / 2f
    }

    private fun assertInPlace(expected: Float, moment: String) {
        val center = domainCenter()
        assertTrue("The domain moved by ${center - expected} px $moment", abs(center - expected) <= TOLERANCE_PX)
    }

    private companion object {
        const val DOMAIN = "polar-notes.example"
        const val FRAMES = 30
        const val TOLERANCE_PX = 1.5f
    }
}
