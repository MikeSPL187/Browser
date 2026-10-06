package dev.sk2andy.materialbrowser.shared.ui

import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuAction
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuRules
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuSection
import dev.sk2andy.materialbrowser.shared.browser.BrowserFeatureMenuState
import dev.sk2andy.materialbrowser.shared.browser.BrowserToppingMenuCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserMainMenuTileRulesTest {
    private val items = BrowserFeatureMenuRules.items(
        state = BrowserFeatureMenuState(
            hasPage = true,
            canOpenReader = true,
            canToggleDesktopView = true,
            isDesktopView = true,
            canSnooze = true,
            toppingCommands = listOf(
                BrowserToppingMenuCommand(
                    scriptId = "script",
                    commandId = "command",
                    caption = "Run",
                    scriptName = "Topping",
                ),
            ),
        ),
    )

    @Test
    fun `page actions become tiles, the rest keeps its rows`() {
        val tiles = items.filter(BrowserMainMenuTileRules::isTile)

        assertEquals(
            setOf(BrowserFeatureMenuSection.Page),
            tiles.map { item -> item.section }.toSet(),
        )
        val rest = items.filterNot(BrowserMainMenuTileRules::isTile).map { item -> item.section }.toSet()
        assertEquals(
            setOf(
                BrowserFeatureMenuSection.Toolbar,
                BrowserFeatureMenuSection.Toppings,
                BrowserFeatureMenuSection.More,
                BrowserFeatureMenuSection.Browser,
            ),
            rest,
        )
    }

    @Test
    fun `rows keep menu order and leave the last row short`() {
        assertEquals(
            listOf(listOf(1, 2, 3, 4), listOf(5, 6, 7, 8), listOf(9)),
            BrowserMainMenuTileRules.rows((1..9).toList(), columns = 4),
        )
        assertEquals(listOf(listOf(1), listOf(2)), BrowserMainMenuTileRules.rows(listOf(1, 2), columns = 0))
    }

    @Test
    fun `large text gets fewer, wider columns`() {
        assertEquals(4, BrowserMainMenuTileRules.columnsFor(baseColumns = 4, fontScale = 1f))
        assertEquals(3, BrowserMainMenuTileRules.columnsFor(baseColumns = 4, fontScale = 1.3f))
        assertEquals(2, BrowserMainMenuTileRules.columnsFor(baseColumns = 4, fontScale = 2f))
        assertEquals(2, BrowserMainMenuTileRules.columnsFor(baseColumns = 2, fontScale = 1.5f))
    }

    @Test
    fun `only toggles with a state switch in place`() {
        val desktop = items.first { item -> item.action == BrowserFeatureMenuAction.ToggleDesktopView }
        val reader = items.first { item -> item.action == BrowserFeatureMenuAction.OpenReader }

        assertTrue(BrowserMainMenuTileRules.togglesInPlace(desktop))
        assertFalse(BrowserMainMenuTileRules.togglesInPlace(reader))
        assertFalse(BrowserMainMenuTileRules.togglesInPlace(desktop.copy(checked = null)))
    }
}
