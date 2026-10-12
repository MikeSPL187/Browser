package dev.sk2andy.materialbrowser.ui.v5

import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/*
 * How the v5 new tab (V13a, canvas screen «а», system-v5.md section 11) lays out pinned sites and
 * labels the report card's week.
 */
object V5NewTabRules {
    /** One cell of the pinned grid: a site, or «Add» after the last one. */
    sealed interface Cell {
        data class Site(val index: Int) : Cell
        data object Add : Cell
    }

    /**
     * [count] pinned sites and, when [canAdd], «Add» after them, in rows of
     * [V5Sizes.PINNED_COLUMNS]; the last row may be short.
     */
    fun rows(count: Int, canAdd: Boolean, columns: Int = V5Sizes.PINNED_COLUMNS): List<List<Cell>> {
        val cells = List(count) { Cell.Site(it) } + if (canAdd) listOf(Cell.Add) else emptyList()
        return cells.chunked(columns.coerceAtLeast(1))
    }

    /** Tile colors for a site without an icon: the vivid ones, never gray. */
    private val letterTiles = V5Tile.entries.filterNot { it == V5Tile.Gray }

    /** The same site always gets the same color. */
    fun letterTile(key: String): V5Tile = letterTiles[Math.floorMod(key.hashCode(), letterTiles.size)]

    /** The letter of a site without an icon: the first letter or digit of its name. */
    fun letter(label: String): String =
        label.firstOrNull(Char::isLetterOrDigit)?.uppercaseChar()?.toString() ?: "•"

    /** Short weekday names under the report card's bars, the oldest first, ending on [lastEpochDay]. */
    fun weekdays(lastEpochDay: Long, days: Int, locale: Locale): List<String> =
        (days - 1 downTo 0).map { back ->
            LocalDate.ofEpochDay(lastEpochDay - back).dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { it.titlecase(locale) }
        }
}
