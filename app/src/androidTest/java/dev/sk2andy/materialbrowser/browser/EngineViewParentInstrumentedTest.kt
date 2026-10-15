package dev.sk2andy.materialbrowser.browser

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An engine view that holds focus moves into a host Compose attaches inside a measure pass, as the
 * fullscreen video host in BoxWithConstraints does. A plain removeView hands the focus to the root,
 * Compose's focus search lays out the lazy list mid-measure and the app crashes ("performMeasure
 * AndLayout called during measure layout", PiP test, 16.10).
 */
@RunWith(AndroidJUnit4::class)
class EngineViewParentInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun focusedEngineViewMovesIntoHostAttachedDuringMeasure() {
        val showDestination = mutableStateOf(false)
        lateinit var engineView: View
        lateinit var destination: FrameLayout
        composeRule.setContent {
            Column {
                LazyColumn(Modifier.fillMaxWidth().height(160.dp)) {
                    items(count = 40) { Box(Modifier.size(48.dp).focusable()) }
                }
                AndroidView(
                    factory = { context ->
                        FrameLayout(context).apply {
                            engineView = View(context).apply {
                                isFocusable = true
                                isFocusableInTouchMode = true
                            }
                            addView(engineView)
                        }
                    },
                    modifier = Modifier.size(120.dp),
                )
                if (showDestination.value) {
                    BoxWithConstraints {
                        AndroidView(
                            factory = { context -> FrameLayout(context).also { destination = it } },
                            update = { container ->
                                if (engineView.parent !== container) {
                                    engineView.removeFromParentWithoutFocusHandoff()
                                    container.addView(engineView)
                                }
                            },
                            modifier = Modifier.size(120.dp),
                        )
                    }
                }
            }
        }
        lateinit var root: ViewGroup
        var rootFocusability = 0
        composeRule.runOnIdle {
            assertTrue("engine view did not take focus", engineView.requestFocus())
            root = engineView.rootView as ViewGroup
            rootFocusability = root.descendantFocusability
            showDestination.value = true
        }

        composeRule.runOnIdle {
            assertSame(destination, engineView.parent)
            assertFalse("the moved view kept focus", engineView.isFocused)
            assertEquals(rootFocusability, root.descendantFocusability)
        }
    }

    @Test
    fun unfocusedEngineViewMovesAsBefore() {
        composeRule.runOnIdle {
            val activity = composeRule.activity
            val source = FrameLayout(activity)
            val destination = FrameLayout(activity)
            val engineView = View(activity)
            source.addView(engineView)
            activity.setContentView(FrameLayout(activity).apply {
                addView(source)
                addView(destination)
            })

            engineView.removeFromParentWithoutFocusHandoff()
            destination.addView(engineView)

            assertEquals(0, source.childCount)
            assertSame(destination, engineView.parent)
        }
    }
}
