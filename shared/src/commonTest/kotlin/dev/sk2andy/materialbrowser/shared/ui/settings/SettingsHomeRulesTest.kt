package dev.sk2andy.materialbrowser.shared.ui.settings

import dev.sk2andy.materialbrowser.ui.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsHomeRulesTest {
    @Test
    fun homeFollowsTheBoardsCards() {
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
                    null,
                    SettingsDestination.Userscripts,
                    SettingsDestination.SiteCapsules,
                    SettingsDestination.Downloads,
                    SettingsDestination.Browser,
                ),
                listOf(SettingsDestination.AboutLegal),
            ),
            SettingsHomeRules.cards(items).map { card -> card.map(SettingsHomeItem::destination) },
        )
        assertTrue(items.single { it.destination == null }.isFirefoxExtensionsAction)
    }

    @Test
    fun liveSummaryJoinsKnownPartsOnly() {
        assertEquals("Frame · theme auto", SettingsHomeRules.joinSummary(listOf("Frame", null, " theme auto ")))
        assertNull(SettingsHomeRules.joinSummary(listOf(null, " ")))
        assertNull(SettingsHomeRules.joinSummary(emptyList()))
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
