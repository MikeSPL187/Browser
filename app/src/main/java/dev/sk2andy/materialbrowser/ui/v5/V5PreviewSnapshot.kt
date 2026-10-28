package dev.sk2andy.materialbrowser.ui.v5

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

/* A stand-in page snapshot for previews of v5 glass (system-v5.md section 4). */

/** A page in bands of tile colors: a header, a picture and the text under it. */
internal fun v5PreviewPageSnapshot(): Bitmap {
    val bitmap = Bitmap.createBitmap(PREVIEW_SNAPSHOT_WIDTH, PREVIEW_SNAPSHOT_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val band = PREVIEW_SNAPSHOT_HEIGHT / PREVIEW_SNAPSHOT_BANDS.size
    PREVIEW_SNAPSHOT_BANDS.forEachIndexed { index, tile ->
        canvas.drawRect(
            0f,
            (index * band).toFloat(),
            PREVIEW_SNAPSHOT_WIDTH.toFloat(),
            ((index + 1) * band).toFloat(),
            Paint().apply { color = tile.color.toArgb() },
        )
    }
    return bitmap
}

private const val PREVIEW_SNAPSHOT_WIDTH = 108
private const val PREVIEW_SNAPSHOT_HEIGHT = 216
private val PREVIEW_SNAPSHOT_BANDS = listOf(V5Tile.Teal, V5Tile.Blue, V5Tile.Green, V5Tile.Orange)
