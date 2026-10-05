package dev.sk2andy.materialbrowser.shared.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowserFeatureMenuRulesTest {
    @Test
    fun `full menu has stable section and action order`() {
        val items = BrowserFeatureMenuRules.items(
            state = BrowserFeatureMenuState(
                hasPage = true,
                canDockAddressBar = true,
                overflowPageActions = listOf(
                    BrowserFeatureMenuAction.ShowTabs,
                    BrowserFeatureMenuAction.NewTab,
                    BrowserFeatureMenuAction.CloseTab,
                ),
            ),
        )

        assertEquals(
            listOf(
                BrowserFeatureMenuAction.Back,
                BrowserFeatureMenuAction.Forward,
                BrowserFeatureMenuAction.Reload,
                BrowserFeatureMenuAction.ToggleFavorite,
                BrowserFeatureMenuAction.Share,
                BrowserFeatureMenuAction.ShowTabs,
                BrowserFeatureMenuAction.NewTab,
                BrowserFeatureMenuAction.FindInPage,
                BrowserFeatureMenuAction.OpenReader,
                BrowserFeatureMenuAction.TranslatePage,
                BrowserFeatureMenuAction.ToggleDesktopView,
                BrowserFeatureMenuAction.Print,
                BrowserFeatureMenuAction.OpenMore,
                BrowserFeatureMenuAction.DuplicateTab,
                BrowserFeatureMenuAction.TogglePinned,
                BrowserFeatureMenuAction.CloseTab,
                BrowserFeatureMenuAction.OpenExternal,
                BrowserFeatureMenuAction.SnoozeTab,
                BrowserFeatureMenuAction.OpenCandyTrail,
                BrowserFeatureMenuAction.AddSiteCapsule,
                BrowserFeatureMenuAction.Summarize,
                BrowserFeatureMenuAction.ToggleDomainMute,
                BrowserFeatureMenuAction.DockAddressBar,
                BrowserFeatureMenuAction.OpenSnoozedTabs,
                BrowserFeatureMenuAction.OpenDownloads,
                BrowserFeatureMenuAction.OpenHistory,
                BrowserFeatureMenuAction.OpenFavorites,
                BrowserFeatureMenuAction.OpenPasswords,
                BrowserFeatureMenuAction.OpenSettings,
            ),
            items.map(BrowserFeatureMenuItem::action),
        )
    }

    @Test
    fun `first view stays short and the rest waits behind More`() {
        val items = BrowserFeatureMenuRules.items(
            BrowserFeatureMenuState(hasPage = true, isSplitView = false, isCompactMode = false),
        )
        val tiles = items.filter { it.section == BrowserFeatureMenuSection.Page }

        // Up to nine tiles plus «More»: three rows of four at most.
        assertTrue(tiles.size <= 10)
        assertEquals(BrowserFeatureMenuAction.NewTab, tiles.first().action)
        assertEquals(BrowserFeatureMenuAction.OpenMore, tiles.last().action)
        assertEquals(BrowserFeatureMenuLabelKey.More, tiles.last().labelKey)
        assertEquals(5, items.count { it.section == BrowserFeatureMenuSection.Toolbar })
        assertTrue(
            items.single { it.action == BrowserFeatureMenuAction.TogglePinned }.section ==
                BrowserFeatureMenuSection.More,
        )
    }

    @Test
    fun `More is kept whatever the layout hides`() {
        val layout = BrowserMenuLayout(
            BrowserMenuEntry.entries.associateWith { BrowserMenuLocation.Nowhere },
        )
        val visible = BrowserMenuLayoutRules.visibleItems(
            items = BrowserFeatureMenuRules.items(BrowserFeatureMenuState()),
            layout = layout,
            surface = BrowserMenuSurface.Tab,
        )

        assertEquals(listOf(BrowserFeatureMenuAction.OpenMore), visible.map(BrowserFeatureMenuItem::action))
        assertNull(BrowserMenuLayoutRules.entryForAction(BrowserFeatureMenuAction.OpenMore))
        assertEquals(
            BrowserMenuEntry.OpenPasswords,
            BrowserMenuLayoutRules.entryForAction(BrowserFeatureMenuAction.OpenPasswords),
        )
    }

    @Test
    fun `duplicate tab is shared and follows page availability`() {
        val unavailable = BrowserFeatureMenuRules.items(BrowserFeatureMenuState(hasPage = false))
            .single { it.action == BrowserFeatureMenuAction.DuplicateTab }
        val available = BrowserFeatureMenuRules.items(BrowserFeatureMenuState(hasPage = true))
            .single { it.action == BrowserFeatureMenuAction.DuplicateTab }

        assertFalse(unavailable.enabled)
        assertTrue(available.enabled)
    }

    @Test
    fun `loading replaces reload with stop`() {
        val toolbar = BrowserFeatureMenuRules.items(
            state = BrowserFeatureMenuState(isLoading = true),
        ).filter { it.section == BrowserFeatureMenuSection.Toolbar }

        assertEquals(BrowserFeatureMenuAction.Stop, toolbar[2].action)
        assertEquals(BrowserFeatureMenuLabelKey.StopLoading, toolbar[2].labelKey)
        assertTrue(toolbar[2].enabled)
    }

    @Test
    fun `toggle and selectable toolbar commands expose checked state`() {
        val items = BrowserFeatureMenuRules.items(
            state = BrowserFeatureMenuState(
                isDesktopView = true,
                isFavorite = true,
                isPinned = true,
            ),
        )

        val desktop = items.single { it.action == BrowserFeatureMenuAction.ToggleDesktopView }
        val favorite = items.single { it.action == BrowserFeatureMenuAction.ToggleFavorite }
        val pinned = items.single { it.action == BrowserFeatureMenuAction.TogglePinned }
        val share = items.single { it.action == BrowserFeatureMenuAction.Share }
        assertTrue(desktop.checked == true)
        assertTrue(favorite.checked == true)
        assertTrue(pinned.checked == true)
        assertNull(share.checked)
    }

    @Test
    fun `compact mode is a tile only where the browser can switch it`() {
        assertTrue(
            BrowserFeatureMenuRules.items(BrowserFeatureMenuState())
                .none { it.action == BrowserFeatureMenuAction.ToggleCompactMode },
        )
        val compact = BrowserFeatureMenuRules.items(BrowserFeatureMenuState(isCompactMode = true))
            .single { it.action == BrowserFeatureMenuAction.ToggleCompactMode }
        assertTrue(compact.checked == true)
        assertTrue(compact.enabled)
        assertEquals(
            BrowserMenuEntry.CompactMode,
            BrowserMenuLayoutRules.entryForAction(BrowserFeatureMenuAction.ToggleCompactMode),
        )
    }

    @Test
    fun `firefox extensions are Android additive capability`() {
        val common = BrowserFeatureMenuRules.items(BrowserFeatureMenuState())
        val android = BrowserFeatureMenuRules.items(
            state = BrowserFeatureMenuState(),
            capabilities = BrowserFeatureMenuCapabilities(supportsFirefoxExtensions = true),
        )

        assertFalse(common.any { it.action == BrowserFeatureMenuAction.OpenFirefoxExtensions })
        assertTrue(android.any { it.action == BrowserFeatureMenuAction.OpenFirefoxExtensions })
        assertEquals(BrowserFeatureMenuAction.OpenSettings, android.last().action)
    }

    @Test
    fun `topping commands preserve dynamic identity caption and script`() {
        val item = BrowserFeatureMenuRules.items(
            BrowserFeatureMenuState(
                toppingCommands = listOf(
                    BrowserToppingMenuCommand(
                        scriptId = "reader-tools",
                        commandId = "focus",
                        caption = "Focus mode",
                        scriptName = "Reader Tools",
                        documentId = "frame-7",
                    ),
                ),
            ),
        ).single { it.section == BrowserFeatureMenuSection.Toppings }

        assertEquals("topping:reader-tools:focus:frame-7", item.stableId)
        assertEquals("Focus mode", item.dynamicLabel)
        assertEquals("Reader Tools", item.supportingText)
        assertEquals("reader-tools", item.toppingScriptId)
        assertEquals("focus", item.toppingCommandId)
        assertEquals("frame-7", item.toppingDocumentId)
    }

    @Test
    fun `park-right overflow replaces dock action`() {
        val items = BrowserFeatureMenuRules.items(
            BrowserFeatureMenuState(
                canDockAddressBar = true,
                overflowPageActions = listOf(BrowserFeatureMenuAction.ParkAddressBarRight),
            ),
        )

        assertTrue(items.any { it.action == BrowserFeatureMenuAction.ParkAddressBarRight })
        assertFalse(items.any { it.action == BrowserFeatureMenuAction.DockAddressBar })
    }

    @Test
    fun `split view is a tile only where the browser can open it`() {
        assertTrue(
            BrowserFeatureMenuRules.items(BrowserFeatureMenuState())
                .none { it.action == BrowserFeatureMenuAction.ToggleSplitView },
        )
        val split = BrowserFeatureMenuRules.items(BrowserFeatureMenuState(isSplitView = false))
            .single { it.action == BrowserFeatureMenuAction.ToggleSplitView }
        assertFalse(split.checked == true)
        assertTrue(split.enabled)
        assertEquals(
            BrowserMenuEntry.SplitView,
            BrowserMenuLayoutRules.entryForAction(BrowserFeatureMenuAction.ToggleSplitView),
        )
    }
}
