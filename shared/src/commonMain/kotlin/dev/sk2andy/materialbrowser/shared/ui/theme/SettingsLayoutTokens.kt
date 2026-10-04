package dev.sk2andy.materialbrowser.shared.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Spacing of the shared settings pages, passed in no other way since shared code has no tokens. */
object SettingsLayoutTokens {
    /** A row's description under it, lined up with its title. */
    val summaryPadding = PaddingValues(start = 18.dp, top = 6.dp, end = 18.dp)

    /** The «More settings» row that folds the advanced settings away. */
    val foldPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    val foldGap = 12.dp
}

/** A card of settings rows (board W-Settings): rows that open a page, one under another. */
object SettingsCardTokens {
    val shape = RoundedCornerShape(24.dp)
    val cardGap = 12.dp

    val rowMinHeight = 64.dp
    val rowPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val rowGap = 16.dp
    val textGap = 2.dp

    /** The page's icon on a tile in its own color. */
    val tileSize = 40.dp
    val tileShape = RoundedCornerShape(14.dp)
    val tileIconSize = 22.dp
    val chevronSize = 20.dp

    /** Between two rows, from the tile's middle to the card's edge. */
    val dividerThickness = 1.dp
    val dividerStartInset = 56.dp
    val dividerEndInset = 16.dp
}
