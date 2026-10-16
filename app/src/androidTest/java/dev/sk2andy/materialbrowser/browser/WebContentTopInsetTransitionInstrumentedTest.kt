package dev.sk2andy.materialbrowser.browser

import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Both tests run with and without system animations. Whether animators run is read inside the
 * activity: a test process learns the system's animator scale only when its first window opens,
 * so before that ValueAnimator.areAnimatorsEnabled() is true even with animations off (CI).
 */
@RunWith(AndroidJUnit4::class)
class WebContentTopInsetTransitionInstrumentedTest {
    @Test
    fun nativeHeaderTransitionKeepsTopEdgeAndSettlesAtFinalLayout() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var content: View
            scenario.onActivity { activity ->
                val root = FrameLayout(activity)
                content = View(activity)
                root.addView(
                    content,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                activity.setContentView(root)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity {
                val layoutParams = content.layoutParams as FrameLayout.LayoutParams
                layoutParams.topMargin = 96
                content.layoutParams = layoutParams
                content.smoothWebContentTopInsetChange(
                    previousTopInsetPx = 0,
                    nextTopInsetPx = 96,
                    animateChange = true,
                )

                // Without animations the content takes its new place at once.
                val startTranslationY = if (ValueAnimator.areAnimatorsEnabled()) -96f else 0f
                assertEquals(startTranslationY, content.translationY, 0.5f)
            }

            SystemClock.sleep(WebContentTopInsetTransitionRules.DURATION_MILLIS + 100L)
            scenario.onActivity {
                assertEquals(0f, content.translationY, 0.5f)
                assertEquals(96, (content.layoutParams as FrameLayout.LayoutParams).topMargin)
            }
        }
    }

    @Test
    fun nonHeaderInsetChangeCancelsRunningTransitionAndSnaps() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var content: View
            scenario.onActivity { activity ->
                val root = FrameLayout(activity)
                content = View(activity)
                root.addView(
                    content,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                activity.setContentView(root)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity {
                val layoutParams = content.layoutParams as FrameLayout.LayoutParams
                layoutParams.topMargin = 96
                content.layoutParams = layoutParams
                content.smoothWebContentTopInsetChange(
                    previousTopInsetPx = 0,
                    nextTopInsetPx = 96,
                    animateChange = true,
                )

                layoutParams.topMargin = 0
                content.layoutParams = layoutParams
                content.smoothWebContentTopInsetChange(
                    previousTopInsetPx = 96,
                    nextTopInsetPx = 0,
                    animateChange = false,
                )

                assertEquals(0f, content.translationY, 0f)
                assertEquals(0, layoutParams.topMargin)
            }
        }
    }
}
