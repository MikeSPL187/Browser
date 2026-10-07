package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The library: history and favorites (boards W-History, W-Favorites). */
internal object VolaLibrary {
    val sidePadding = VolaSpacing.x4
    val sectionGap = VolaSpacing.x3

    /** The search pill under the header. */
    val searchHeight = 52.dp
    val searchShape = RoundedCornerShape(26.dp)

    /** The pill has no underline. */
    val searchIndicator = Color.Transparent

    /** A day of history or «No folder» is one card; its rows are split by hairlines. */
    val groupRadius = 24.dp
    val rowMinHeight = 64.dp
    val rowPadding = PaddingValues(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp)
    val rowGap = 14.dp
    val textGap = 2.dp
    val dividerThickness = 1.dp
    val dividerStartInset = 68.dp
    val dividerEndInset = 16.dp

    /** A site's letter tile or favicon. */
    val tileSize = 40.dp
    val tileShape = RoundedCornerShape(13.dp)
    val tileIconSize = 22.dp

    /** The workspace's gem after the time. */
    val gemSize = 20.dp
    val trailingGap = VolaSpacing.x2

    /** Section titles: «Today», «No folder». */
    val sectionLabelPadding = PaddingValues(start = 4.dp, top = 8.dp, bottom = 4.dp)

    /** Folder cards, two in a row. */
    val folderCardShape = RoundedCornerShape(22.dp)
    val folderCardPadding = 14.dp
    val folderCardGap = 10.dp
    val folderTileShape = RoundedCornerShape(14.dp)
    val folderEmojiSize = 20.sp
    const val FOLDER_COLUMNS = 2

    /** A folder without its own icon shows its first four sites' favicons, two by two. */
    val folderMosaicPadding = 5.dp
    val folderMosaicGap = 2.dp
    val folderMosaicCellShape = RoundedCornerShape(4.dp)
    const val FOLDER_MOSAIC_COLUMNS = 2

    /** Room under the list for the «Folder» button. */
    val fabClearance = 96.dp

    /**
     * Soft tiles for letters and folders, one per site by a stable hash: light in the light theme,
     * deep on pure black in the dark one (board W-History).
     */
    private val tiles = listOf(
        Tile(Color(0xFFD4F1EC), Color(0xFF00665A), Color(0xFF00443C), Color(0xFFA6E9DD)),
        Tile(Color(0xFFE6DEFF), Color(0xFF4A3A9E), Color(0xFF332A66), Color(0xFFD9CFFF)),
        Tile(Color(0xFFD8EEFF), Color(0xFF1E5E8C), Color(0xFF143F5E), Color(0xFFBEE1FF)),
        Tile(Color(0xFFFFEBC2), Color(0xFF6E4F00), Color(0xFF4A3500), Color(0xFFFFDC94)),
        Tile(Color(0xFFFFDDEA), Color(0xFF962F59), Color(0xFF63203B), Color(0xFFFFC1D6)),
        Tile(Color(0xFFFFE1D6), Color(0xFF9A3A1E), Color(0xFF662714), Color(0xFFFFC6B3)),
        Tile(Color(0xFFD3F0D9), Color(0xFF2B6C3F), Color(0xFF1D4729), Color(0xFFB4E6C0)),
        Tile(Color(0xFF2F6B5F), Color(0xFFFFFFFF), Color(0xFF2F6B5F), Color(0xFFFFFFFF)),
    )
    val tileCount: Int get() = tiles.size

    @Composable
    fun tile(index: Int): LibraryTileColors =
        tiles[Math.floorMod(index, tiles.size)].colors(LocalVolaDarkTheme.current)

    private class Tile(
        private val lightContainer: Color,
        private val lightContent: Color,
        private val darkContainer: Color,
        private val darkContent: Color,
    ) {
        fun colors(dark: Boolean): LibraryTileColors = if (dark) {
            LibraryTileColors(darkContainer, darkContent)
        } else {
            LibraryTileColors(lightContainer, lightContent)
        }
    }
}

internal data class LibraryTileColors(val container: Color, val content: Color)
