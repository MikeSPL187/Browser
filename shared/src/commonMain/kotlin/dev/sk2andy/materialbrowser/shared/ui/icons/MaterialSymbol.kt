package dev.sk2andy.materialbrowser.shared.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Material Symbols are drawn on a 960 unit grid and shown at 24 dp. */
private const val SYMBOL_VIEWPORT = 960f

/**
 * Builds one Material Symbols icon from path data on the 0 0 960 960 viewport. The fill is
 * black because Icon tints it with the content color.
 */
internal fun materialSymbol(
    name: String,
    pathData: String,
    autoMirror: Boolean = false,
): ImageVector = ImageVector.Builder(
    name = "VolaIcons.$name",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = SYMBOL_VIEWPORT,
    viewportHeight = SYMBOL_VIEWPORT,
    autoMirror = autoMirror,
).addPath(
    pathData = addPathNodes(pathData),
    fill = SolidColor(Color.Black),
).build()
