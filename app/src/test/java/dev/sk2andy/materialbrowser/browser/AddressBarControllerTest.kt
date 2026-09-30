package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.data.AddressBarActionLayout
import dev.sk2andy.materialbrowser.data.AddressBarDockEdge
import dev.sk2andy.materialbrowser.data.AddressBarDockPlacement
import dev.sk2andy.materialbrowser.shared.browser.AddressBarLongPressAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressBarControllerTest {
    private val leftMiddle = AddressBarDockPlacement(AddressBarDockEdge.Left, 0.5f)

    @Test
    fun `restore drops a saved dock placement while docking is off`() {
        val store = FakeStore(dockPlacement = leftMiddle, dockingEnabled = false)
        val controller = controller(store)

        controller.restore()

        assertNull(controller.dockPlacement)
        assertFalse(controller.isDocked)
        assertEquals(listOf<AddressBarDockPlacement?>(null), store.savedDockPlacements)
        assertEquals(AddressBarDockEdge.Left, controller.lastDockEdge)
    }

    @Test
    fun `restore keeps the saved dock placement and preferences`() {
        val store = FakeStore(
            dockPlacement = leftMiddle,
            longPressAction = AddressBarLongPressAction.entries.last(),
            startupFocusMode = StartupAddressFocusMode.entries.last(),
        )
        val controller = controller(store)

        controller.restore()

        assertEquals(leftMiddle, controller.dockPlacement)
        assertEquals(AddressBarLongPressAction.entries.last(), controller.longPressAction)
        assertEquals(StartupAddressFocusMode.entries.last(), controller.startupFocusMode)
        assertTrue(store.savedDockPlacements.isEmpty())
    }

    @Test
    fun `docking returns to the last placement and undocking forgets only the current one`() {
        val store = FakeStore(lastDockPlacement = leftMiddle)
        val host = FakeHost()
        val controller = controller(store, host)
        controller.restore()

        controller.updateDocked(true)
        assertEquals(leftMiddle, controller.dockPlacement)

        controller.updateDocked(false)
        assertNull(controller.dockPlacement)
        assertEquals(AddressBarDockEdge.Left, controller.lastDockEdge)
        assertEquals(listOf(leftMiddle, null), store.savedDockPlacements)
        assertEquals(2, host.dockPlacementChanges)
    }

    @Test
    fun `parking moves to the right edge at the last height`() {
        val controller = controller(FakeStore(lastDockPlacement = leftMiddle))
        controller.restore()

        controller.parkOnRight()

        assertEquals(AddressBarDockPlacement(AddressBarDockEdge.Right, 0.5f), controller.dockPlacement)
    }

    @Test
    fun `nothing docks while docking is off`() {
        val store = FakeStore(dockingEnabled = false)
        val host = FakeHost()
        val controller = controller(store, host)
        controller.restore()

        controller.updateDocked(true)
        controller.parkOnRight()
        controller.updateDockPlacement(leftMiddle)

        assertNull(controller.dockPlacement)
        assertEquals(listOf<AddressBarDockPlacement?>(), store.savedDockPlacements)
        assertEquals(0, host.dockPlacementChanges)
    }

    @Test
    fun `turning docking off undocks the bar`() {
        val store = FakeStore(dockPlacement = leftMiddle)
        val controller = controller(store)
        controller.restore()

        controller.updateDockingEnabled(false)

        assertFalse(controller.isDockingEnabled)
        assertNull(controller.dockPlacement)
        assertEquals(listOf(false), store.savedDockingEnabled)
        assertEquals(listOf<AddressBarDockPlacement?>(null), store.savedDockPlacements)
    }

    @Test
    fun `placements are normalized and unchanged values are not saved again`() {
        val store = FakeStore()
        val controller = controller(store)
        controller.restore()

        controller.updateDockPlacement(AddressBarDockPlacement(AddressBarDockEdge.Left, 7f))
        controller.updateDockPlacement(AddressBarDockPlacement(AddressBarDockEdge.Left, 1f))

        assertEquals(AddressBarDockPlacement(AddressBarDockEdge.Left, 1f), controller.dockPlacement)
        assertEquals(1, store.savedDockPlacements.size)
    }

    @Test
    fun `preferences are saved only when they change`() {
        val store = FakeStore()
        val controller = controller(store)
        controller.restore()
        val action = AddressBarLongPressAction.entries.last()
        val mode = StartupAddressFocusMode.entries.last()

        repeat(2) {
            controller.updateLongPressAction(action)
            controller.updateStartupFocusMode(mode)
            controller.updateActionLayout(AddressBarActionLayout.Default)
        }

        assertEquals(listOf(action), store.savedLongPressActions)
        assertEquals(listOf(mode), store.savedStartupFocusModes)
        assertTrue(store.savedActionLayouts.isEmpty())
    }

    @Test
    fun `auto-dock probe needs the selected tab and an engine session`() {
        val posted = mutableListOf<Runnable>()
        val host = FakeHost(selectedTabId = "a")
        val controller = AddressBarController(
            store = FakeStore(),
            host = host,
            postDelayed = { runnable, _ -> posted += runnable },
            removeCallbacks = { runnable -> posted -= runnable },
        )
        controller.restore()

        controller.scheduleAutoDockProbe(tabId = "b", url = "https://example.com")
        controller.scheduleAutoDockProbe(tabId = "a", url = "https://example.com")

        assertTrue("no probe without a session or for another tab", posted.isEmpty())
    }

    private fun controller(
        store: FakeStore,
        host: FakeHost = FakeHost(),
    ) = AddressBarController(
        store = store,
        host = host,
        postDelayed = { _, _ -> },
        removeCallbacks = {},
    )

    private class FakeHost(
        override val selectedTabId: String = "tab",
    ) : AddressBarController.Host {
        var dockPlacementChanges = 0

        override val selectedTab: BrowserTab = BrowserTab(id = selectedTabId, lastAccessedAt = 0L)

        override fun tab(tabId: String): BrowserTab? = selectedTab.takeIf { it.id == tabId }

        override fun engineSession(tabId: String): AndroidBrowserEngineSessionPort? = null

        override fun navigationGeneration(tabId: String): Int = 0

        override fun isPageImeVisible(): Boolean = false

        override fun onDockPlacementChanging() {
            dockPlacementChanges++
        }
    }

    private class FakeStore(
        private val dockPlacement: AddressBarDockPlacement? = null,
        private val lastDockPlacement: AddressBarDockPlacement? = null,
        private val dockingEnabled: Boolean = true,
        private val longPressAction: AddressBarLongPressAction = AddressBarLongPressAction.Default,
        private val startupFocusMode: StartupAddressFocusMode = StartupAddressFocusMode.Default,
    ) : AddressBarPreferenceStore {
        val savedDockPlacements = mutableListOf<AddressBarDockPlacement?>()
        val savedDockingEnabled = mutableListOf<Boolean>()
        val savedLongPressActions = mutableListOf<AddressBarLongPressAction>()
        val savedActionLayouts = mutableListOf<AddressBarActionLayout>()
        val savedStartupFocusModes = mutableListOf<StartupAddressFocusMode>()

        override fun loadAddressBarDockPlacement() = dockPlacement

        override fun loadLastAddressBarDockPlacement() = lastDockPlacement

        override fun saveAddressBarDockPlacement(placement: AddressBarDockPlacement?) {
            savedDockPlacements += placement
        }

        override fun loadAddressBarDockingEnabled() = dockingEnabled

        override fun saveAddressBarDockingEnabled(enabled: Boolean) {
            savedDockingEnabled += enabled
        }

        override fun loadAddressBarLongPressAction() = longPressAction

        override fun saveAddressBarLongPressAction(action: AddressBarLongPressAction) {
            savedLongPressActions += action
        }

        override fun loadAddressBarActionLayout() = AddressBarActionLayout.Default

        override fun saveAddressBarActionLayout(layout: AddressBarActionLayout) {
            savedActionLayouts += layout
        }

        override fun loadStartupAddressFocusMode() = startupFocusMode

        override fun saveStartupAddressFocusMode(mode: StartupAddressFocusMode) {
            savedStartupFocusModes += mode
        }
    }
}
