package dev.sk2andy.materialbrowser.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsDestinationTest {
    @Test
    fun subpagesReturnToThePageTheyOpenFrom() {
        assertEquals(SettingsDestination.Appearance, SettingsDestination.Themes.parent)
        assertEquals(SettingsDestination.Userscripts, SettingsDestination.ToppingCatalog.parent)
        listOf(
            SettingsDestination.AddressBarLongPressActions,
            SettingsDestination.AddressBarActions,
            SettingsDestination.MenuActions,
            SettingsDestination.LinkPeekActions,
        ).forEach { subpage ->
            assertEquals(SettingsDestination.TabsAndGestures, subpage.parent)
        }
    }

    @Test
    fun pagesOfTheHomeReturnToTheHome() {
        listOf(
            SettingsDestination.Home,
            SettingsDestination.Search,
            SettingsDestination.Appearance,
            SettingsDestination.AboutLegal,
        ).forEach { page ->
            assertEquals(SettingsDestination.Home, page.parent)
        }
    }
}
