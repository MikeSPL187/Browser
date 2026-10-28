package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The listening mini player (canvas «Reader»): time said so far, the speed capsule, pause and stop. */
@RunWith(AndroidJUnit4::class)
class V5ListenRowInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private fun show(state: V5ListenRowState) {
        composeRule.setContent {
            VolaV5Theme {
                V5ListenRow(
                    state = state,
                    onPlayPause = { calls += "play" },
                    onClose = { calls += "close" },
                    onSpeed = { calls += "speed" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theSpeedCapsuleAndTheTimeSpeakForThemselves() {
        show(
            V5ListenRowState(
                title = "Article",
                speaking = true,
                timing = V5ListenTiming(progress = 0.37f, text = "3:12 / 8:40", description = "3:12 of about 8:40"),
                speed = "1.2",
            ),
        )

        composeRule.onNodeWithTag(V5ListenRowTestTags.Speed)
            .assertContentDescriptionEquals(context.getString(R.string.v5_listen_speed_description, "1.2"))
            .performClick()
        composeRule.onNodeWithTag(V5ListenRowTestTags.Progress).assertContentDescriptionEquals("3:12 of about 8:40")
        composeRule.onNodeWithTag(V5ListenRowTestTags.PlayPause).performClick()
        composeRule.onNodeWithTag(V5ListenRowTestTags.Close).performClick()

        assertEquals(listOf("speed", "play", "close"), calls)
    }

    @Test
    fun aMessageLeavesOutTheTimeAndTheSpeed() {
        show(V5ListenRowState(title = "Article", speaking = false, canPlay = false, message = "Preparing"))

        composeRule.onNodeWithTag(V5ListenRowTestTags.Speed).assertDoesNotExist()
        composeRule.onNodeWithTag(V5ListenRowTestTags.Progress).assertDoesNotExist()
        composeRule.onNodeWithTag(V5ListenRowTestTags.PlayPause).assertDoesNotExist()
    }
}
