package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuAction
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuCapabilities
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuRules
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserMainMenuIconRulesTest {
    private val items = BrowserFeatureMenuRules.items(
        state = BrowserFeatureMenuState(
            hasPage = true,
            canToggleDesktopView = true,
            isCompactMode = false,
            isSplitView = false,
        ),
        capabilities = BrowserFeatureMenuCapabilities(),
    )

    @Test
    fun `page tiles use purpose-specific icons`() {
        val expectedIcons = mapOf(
            BrowserFeatureMenuAction.ToggleDesktopView to R.drawable.ic_symbol_desktop,
            BrowserFeatureMenuAction.ToggleCompactMode to R.drawable.ic_symbol_compact_mode,
            BrowserFeatureMenuAction.ToggleSplitView to R.drawable.ic_symbol_split_view,
            BrowserFeatureMenuAction.Print to R.drawable.ic_symbol_print,
        )

        val actualIcons = items
            .filter { it.action in expectedIcons }
            .associate { it.action to it.androidDrawableResource() }

        assertEquals(expectedIcons, actualIcons)
    }

    @Test
    fun `More and Passwords draw vector icons`() {
        listOf(BrowserFeatureMenuAction.OpenMore, BrowserFeatureMenuAction.OpenPasswords).forEach { action ->
            assertNull(items.single { it.action == action }.androidDrawableResource())
        }
    }
}
