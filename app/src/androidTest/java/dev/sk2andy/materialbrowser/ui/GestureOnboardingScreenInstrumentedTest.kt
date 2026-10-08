package dev.sk2andy.materialbrowser.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GestureOnboardingScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyGestureMustBePerformedInOrder() {
        val completed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialBrowserTheme {
                GestureOnboardingScreen(onCompleted = { completed.set(true) })
            }
        }

        startTutorial()

        composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs))
            .performTouchInput { swipeDown() }
        composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs)).assertIsDisplayed()

        composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs))
            .performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag(tag(GestureOnboardingStep.OpenTabOverview))
            .performScrollTo()
            .performTouchInput {
                swipe(
                    start = center,
                    end = center + Offset(0f, -320f),
                )
            }
        composeRule.onNodeWithTag(tag(GestureOnboardingStep.CloseTab))
            .performScrollTo()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(tag(GestureOnboardingStep.CloseTab))
            .performTouchInput { swipeUp() }

        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("gesture_onboarding_celebration").assertIsDisplayed()
        assertFalse(completed.get())
        composeRule.onAllNodesWithTag("gesture_onboarding_finish").assertCountEquals(0)
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.onAllNodesWithTag("gesture_onboarding_finish").assertCountEquals(0)
        composeRule.mainClock.advanceTimeBy(50)
        composeRule.onNodeWithTag("gesture_onboarding_finish").assertIsDisplayed()
        composeRule.onNodeWithTag("gesture_onboarding_finish").performClick()
        composeRule.mainClock.advanceTimeBy(360)
        assertFalse(completed.get())
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitUntil(timeoutMillis = 2_000) { completed.get() }
        assertTrue(completed.get())
    }

    @Test
    fun accessibilityCompletionActionIsExposed() {
        composeRule.setContent {
            MaterialBrowserTheme {
                GestureOnboardingScreen(onCompleted = {})
            }
        }

        startTutorial()

        composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun openOverviewAcceptsGestureOnFirstFrameAfterStepChange() {
        composeRule.setContent {
            MaterialBrowserTheme {
                GestureOnboardingScreen(onCompleted = {})
            }
        }

        startTutorial()
        composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs)).assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs))
                .performTouchInput { swipeLeft() }
            composeRule.mainClock.advanceTimeByFrame()

            composeRule.onNodeWithTag(tag(GestureOnboardingStep.OpenTabOverview))
                .performTouchInput {
                    swipe(
                        start = center,
                        end = center + Offset(0f, -320f),
                    )
                }
            composeRule.mainClock.advanceTimeByFrame()

            composeRule.onNodeWithTag(tag(GestureOnboardingStep.CloseTab)).assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun skipCompletesFromTheWelcomePage() {
        val completed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialBrowserTheme {
                GestureOnboardingScreen(onCompleted = { completed.set(true) })
            }
        }

        composeRule.onNodeWithTag("gesture_onboarding_skip").performClick()

        composeRule.waitUntil(timeoutMillis = 2_000) { completed.get() }
        assertTrue(completed.get())
    }

    @Test
    fun backCallsTheBackHandlerInsteadOfBeingSwallowed() {
        val backs = AtomicInteger(0)
        composeRule.setContent {
            MaterialBrowserTheme {
                GestureOnboardingScreen(onCompleted = {}, onBack = { backs.incrementAndGet() })
            }
        }

        startTutorial()
        pressBack()

        composeRule.waitUntil(timeoutMillis = 2_000) { backs.get() == 1 }
    }

    @Test
    fun backSkipsTheLessonOnItsOwn() {
        val completed = AtomicBoolean(false)
        composeRule.setContent {
            MaterialBrowserTheme {
                GestureOnboardingScreen(onCompleted = { completed.set(true) })
            }
        }

        pressBack()

        composeRule.waitUntil(timeoutMillis = 2_000) { completed.get() }
    }

    /**
     * With double font size the lesson title took the whole header row and Skip got no width:
     * the lesson could not be skipped (found by the screen audit).
     */
    @Test
    fun skipStaysReachableWithLargeFont() {
        val completed = AtomicBoolean(false)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialBrowserTheme {
                    GestureOnboardingScreen(onCompleted = { completed.set(true) })
                }
            }
        }

        val start = composeRule.onNodeWithTag("gesture_onboarding_start")
        runCatching { start.performScrollTo() }
        start.performClick()
        // The welcome has a Skip of its own: wait until only the lesson's is left.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag(GestureOnboardingStep.SwitchTabs))
                .fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("gesture_onboarding_skip").fetchSemanticsNodes().size == 1
        }

        val skip = composeRule.onNodeWithTag("gesture_onboarding_skip").assertIsDisplayed()
        val width = skip.fetchSemanticsNode().boundsInWindow.width
        val minWidth = with(composeRule.density) { 48.dp.toPx() } - 1f
        assertTrue("Skip is $width px wide with a large font", width >= minWidth)
        skip.performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) { completed.get() }
    }

    private fun startTutorial() {
        composeRule.onNodeWithTag("gesture_onboarding_welcome").assertIsDisplayed()
        composeRule.onAllNodesWithTag(tag(GestureOnboardingStep.SwitchTabs))
            .assertCountEquals(0)
        composeRule.onNodeWithTag("gesture_onboarding_start").performClick()
        composeRule.onNodeWithTag(tag(GestureOnboardingStep.SwitchTabs))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(pointerTag(GestureOnboardingStep.SwitchTabs))
            .assertCountEquals(1)
    }

    // Espresso's pressBack waits for an idle, focused root, which the floating welcome hero never gives.
    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun tag(step: GestureOnboardingStep): String = "gesture_onboarding_${step.name}"

    private fun pointerTag(step: GestureOnboardingStep): String =
        "gesture_onboarding_pointer_${step.name}"
}
