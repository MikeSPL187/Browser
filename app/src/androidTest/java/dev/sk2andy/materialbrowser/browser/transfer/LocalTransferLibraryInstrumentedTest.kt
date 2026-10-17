package dev.sk2andy.materialbrowser.browser.transfer

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferMergeRules
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalTransferLibraryInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var controller: BrowserController? = null

    private val received = LocalTransferPayload(
        deviceName = "Pixel 8",
        tabs = listOf(LocalTransferTab("https://example.com/a", "Page A")),
        bookmarks = listOf(
            LocalTransferBookmark("https://example.org/", "Example", "Work"),
            LocalTransferBookmark("https://example.net/", "Net"),
        ),
    )

    @After
    fun tearDown() {
        activityRule.scenario.onActivity { activity ->
            controller?.onPause()
            controller?.onStop()
            controller?.destroy()
            controller = null
            clearState(activity)
        }
    }

    @Test
    fun aConfirmedTransferLandsAndUndoTakesItBack() {
        createController()
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            val tabCount = browserController.tabs.size

            val applied = browserController.applyTransfer(planFor(browserController, received))

            assertEquals(1, applied.tabsOpened)
            assertEquals(2, applied.bookmarksAdded)
            assertEquals(0, applied.bookmarksSkipped)
            assertEquals("Pixel 8", browserController.tabStacks.single { it.id == applied.groupId }.name)
            assertEquals(setOf("Example", "Net"), browserController.favoriteLibrary.favorites.map { it.title }.toSet())
            assertTrue(browserController.favoriteLibrary.folders.any { it.title == "Work" })
            assertEquals(tabCount + 1, browserController.tabs.size)

            assertTrue(applied.undo())

            assertTrue(browserController.favoriteLibrary.favorites.isEmpty())
            assertTrue(browserController.favoriteLibrary.folders.isEmpty())
            assertEquals(tabCount, browserController.tabs.size)
            assertTrue("undo twice is harmless", applied.undo())
        }
    }

    @Test
    fun bookmarksThisPhoneHasAreNotAddedTwice() {
        createController()
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            browserController.applyTransfer(planFor(browserController, received))

            val again = browserController.applyTransfer(planFor(browserController, received.copy(tabs = emptyList())))

            assertEquals(0, again.bookmarksAdded)
            assertEquals(0, again.tabsOpened)
            assertNull(again.groupId)
            assertEquals(2, browserController.favoriteLibrary.favorites.size)
        }
    }

    @Test
    fun thePayloadCarriesThisPhonesWebTabsBookmarksAndName() {
        createController()
        activityRule.scenario.onActivity { _ ->
            val browserController = requireNotNull(controller)
            browserController.applyTransfer(planFor(browserController, received))

            val payload = browserController.localTransferPayload("  My phone  ")

            assertEquals("My phone", payload.deviceName)
            assertTrue(payload.tabs.any { it.url == "https://example.com/a" })
            assertEquals(setOf("Work", ""), payload.bookmarks.map { it.folder }.toSet())
        }
    }

    private fun planFor(browserController: BrowserController, payload: LocalTransferPayload) =
        LocalTransferMergeRules.plan(payload, browserController.favoriteLibrary.favorites.map { it.url })

    private fun createController() {
        activityRule.scenario.onActivity { activity ->
            clearState(activity)
            controller = BrowserController(activity).also { browserController ->
                browserController.onStart()
                browserController.onResume()
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun clearState(context: Context) {
        context.getSharedPreferences(BrowserSessionStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
