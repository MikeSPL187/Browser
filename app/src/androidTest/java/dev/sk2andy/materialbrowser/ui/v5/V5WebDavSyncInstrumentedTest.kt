package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * «Own server» beyond its first page (WD-08): «Connect» only when everything is right, what a server
 * said (WD-03), the connected status with its devices, «Sync now», and the two ways to disconnect.
 */
@RunWith(AndroidJUnit4::class)
class V5WebDavSyncInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val filled = V5WebDavConnectState(
        address = "https://cloud.example/remote.php/dav/files/ann/",
        login = "ann",
        password = "app-password",
        phrase = "aurora-over-baikal",
        phraseRepeat = "aurora-over-baikal",
    )

    private val connectActions = V5WebDavConnectActions(
        onBack = { calls += "back" },
        onAddress = {},
        onLogin = {},
        onPassword = {},
        onPhrase = {},
        onPhraseRepeat = {},
        onConnect = { calls += "connect" },
    )

    private fun showConnect(state: V5WebDavConnectState) {
        composeRule.setContent { VolaV5Theme { V5WebDavConnect(state = state, actions = connectActions) } }
        composeRule.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    @Test
    fun connectWaitsForMatchingPhrases() {
        showConnect(filled.copy(phraseRepeat = "aurora-over-baika"))

        composeRule.onNodeWithText(text(R.string.v5_webdav_phrase_differs)).assertExists()
        composeRule.onNodeWithTag(V5WebDavTestTags.ConnectButton).assertIsNotEnabled()
    }

    @Test
    fun aFilledFormConnectsAndAServersAnswerIsSaid() {
        showConnect(filled.copy(error = V5WebDavError.ServerFull))

        composeRule.onNodeWithText(text(R.string.v5_webdav_error_full)).assertExists()
        composeRule.onNodeWithTag(V5WebDavTestTags.ConnectButton).assertIsEnabled().performClick()
        assertEquals(listOf("connect"), calls)
    }

    @Test
    fun theStatusListsDevicesAndSyncsOnRequest() {
        composeRule.setContent {
            VolaV5Theme {
                V5WebDavStatus(
                    state = V5WebDavStatusState(
                        host = "cloud.example",
                        lastSynced = "Today at 13:42",
                        syncing = false,
                        error = null,
                        devices = listOf(V5WebDavDevice("tablet", "Tablet", tabs = 3, updated = "Yesterday")),
                    ),
                    onSyncNow = { calls += "sync" },
                    onOpenDevice = { calls += "device $it" },
                )
            }
        }

        composeRule.onNodeWithTag(V5WebDavTestTags.SyncNow).performClick()
        composeRule.onNodeWithTag(V5WebDavTestTags.device("tablet")).performClick()
        assertEquals(listOf("sync", "device tablet"), calls)
    }

    @Test
    fun whileSyncingSyncNowWaits() {
        composeRule.setContent {
            VolaV5Theme {
                V5WebDavStatus(
                    state = V5WebDavStatusState("cloud.example", lastSynced = null, syncing = true, error = null, devices = emptyList()),
                    onSyncNow = { calls += "sync" },
                    onOpenDevice = {},
                )
            }
        }

        composeRule.onNodeWithTag(V5WebDavTestTags.SyncNow).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.v5_webdav_no_devices)).assertExists()
    }

    @Test
    fun disconnectOffersToKeepOrDeleteTheServersData() {
        composeRule.setContent {
            VolaV5Theme {
                V5WebDavDisconnectSheet(
                    visible = true,
                    host = "cloud.example",
                    onDisconnect = { calls += "disconnect" },
                    onDisconnectAndDelete = { calls += "delete" },
                    onDismiss = { calls += "dismiss" },
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.v5_webdav_disconnect_delete)).performClick()
        assertEquals(listOf("delete"), calls.filter { it != "dismiss" })
    }
}
