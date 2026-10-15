package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/**
 * The sides v5 keeps its content clear of: a phone turned sideways has its camera cutout on one
 * side and, with three-button navigation, the navigation bar on the other. Backgrounds still run
 * edge to edge; only what can be read or touched moves in.
 */
internal val V5SafeSides: WindowInsets
    @Composable get() = WindowInsets.displayCutout
        .union(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Horizontal)
