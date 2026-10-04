package dev.sk2andy.materialbrowser.shared.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/** Spacing of the shared settings pages, passed in no other way since shared code has no tokens. */
object SettingsLayoutTokens {
    /** A row's description under it, lined up with its title. */
    val summaryPadding = PaddingValues(start = 18.dp, top = 6.dp, end = 18.dp)

    /** The «More settings» row that folds the advanced settings away. */
    val foldPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    val foldGap = 12.dp
}
