package dev.sk2andy.materialbrowser.shared.ui.settings

import dev.sk2andy.materialbrowser.ui.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsHomeRulesTest {
    @Test
    fun homeFollowsTheBoardCardByCard() {
        val items = SettingsHomeRules.items(hasFirefoxExtensions = true)

        assertEquals(
            listOf(
                listOf(SettingsDestination.ProtectionAndData, SettingsDestination.Sync),
                listOf(
                    SettingsDestination.Appearance,
                    SettingsDestination.TabsAndGestures,
                    SettingsDestination.Search,
                ),
                listOf(
                    SettingsDestination.Userscripts,
                    null,
                    SettingsDestination.SiteCapsules,
                    SettingsDestination.Downloads,
                ),
                listOf(SettingsDestination.Browser, SettingsDestination.AboutLegal),
            ),
            SettingsHomeRules.cards(items).map { card -> card.map(SettingsHomeItem::destination) },
        )
        assertTrue(items.single { it.destination == null }.isFirefoxExtensionsAction)
    }

    @Test
    fun everyPageOfTheHomeIsOnIt() {
        val destinations = SettingsHomeRules.items(
            hasFirefoxExtensions = true,
            hasDeveloperOptions = true,
        ).mapNotNull(SettingsHomeItem::destination).toSet()

        val pagesOfTheHome = SettingsDestination.entries.toSet() - setOf(
            SettingsDestination.Home,
            // Opened from inside other pages, not from the home.
            SettingsDestination.AddressBarLongPressActions,
            SettingsDestination.AddressBarActions,
            SettingsDestination.MenuActions,
            SettingsDestination.LinkPeekActions,
            SettingsDestination.Themes,
            SettingsDestination.ToppingCatalog,
        )
        assertEquals(pagesOfTheHome, destinations)
    }

    @Test
    fun cardsKeepTheirOrderAndSkipEmptyOnes() {
        val items = SettingsHomeRules.items(hasFirefoxExtensions = false)
            .filter { it.card != SettingsHomeCard.Personalization }

        assertEquals(
            listOf(SettingsHomeCard.Protection, SettingsHomeCard.Features, SettingsHomeCard.About),
            SettingsHomeRules.cards(items).map { card -> card.first().card },
        )
    }

    @Test
    fun unsupportedPlatformOmitsFirefoxExtensionAction() {
        val items = SettingsHomeRules.items(hasFirefoxExtensions = false)

        assertFalse(items.any(SettingsHomeItem::isFirefoxExtensionsAction))
        assertNull(items.firstOrNull { it.destination == null })
    }

    @Test
    fun developerOptionsAppearOnlyAfterUnlock() {
        assertFalse(
            SettingsHomeRules.items(hasFirefoxExtensions = false)
                .any { it.destination == SettingsDestination.DeveloperOptions },
        )
        val unlocked = SettingsHomeRules.items(
            hasFirefoxExtensions = false,
            hasDeveloperOptions = true,
        )

        assertEquals(
            SettingsDestination.DeveloperOptions,
            unlocked[unlocked.lastIndex - 1].destination,
        )
        assertEquals(SettingsDestination.AboutLegal, unlocked.last().destination)
    }
}
