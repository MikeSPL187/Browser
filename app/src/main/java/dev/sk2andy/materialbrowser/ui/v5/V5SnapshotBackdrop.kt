package dev.sk2andy.materialbrowser.ui.v5

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Material
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5OpaqueMaterials
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * A material over a snapshot of the page (system-v5.md section 4). GeckoView draws the page in its
 * own SurfaceView, which no blur inside Compose can see, so the glass lies over a picture of the
 * page instead. The picture does not move, so it is blurred once: shrunk to a few dozen pixels and
 * stretched back with bilinear filtering. Nothing is redrawn per frame, unlike a live blur.
 */
object V5SnapshotBackdropRules {
    /** The shrunk snapshot's width: at 40 dp of blur on a phone, each pixel spans a dozen dp. */
    const val BLURRED_WIDTH_PX = 32

    /** Blur from Android 13 (section 4); opaque on request, and without a snapshot. */
    fun showsSnapshot(hasSnapshot: Boolean, blurSupported: Boolean, opaque: Boolean): Boolean =
        hasSnapshot && blurSupported && !opaque

    /** The shrunk size for a [width] × [height] snapshot: [BLURRED_WIDTH_PX] wide, the ratio kept. */
    fun blurredSize(width: Int, height: Int): Pair<Int, Int> {
        val targetWidth = BLURRED_WIDTH_PX.coerceAtMost(width)
        val targetHeight = max(1, (height.toFloat() * targetWidth / width).roundToInt())
        return targetWidth to targetHeight
    }
}

/**
 * [material] over the blurred [snapshot], filling the space. Without a snapshot (a new tab, a
 * private tab, nothing captured yet), on Android 12 and below, or with battery saver or
 * high-contrast text, only the material's tint is drawn, as before.
 */
@Composable
internal fun V5SnapshotBackdrop(
    snapshot: Bitmap?,
    material: V5Material,
    modifier: Modifier = Modifier,
    /**
     * Which part of the page lies under it: the top for a full-screen editor; a sheet at the bottom
     * takes the page's bottom ([ContentScale.FillWidth], [Alignment.BottomCenter]).
     */
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopCenter,
) {
    val colors = V5Theme.colors
    val opaque = rememberV5OpaqueMaterials()
    val shown = V5SnapshotBackdropRules.showsSnapshot(
        hasSnapshot = snapshot != null,
        blurSupported = V5Materials.blurSupported,
        opaque = opaque,
    )
    // Shrunk on the main thread, where the controller recycles replaced snapshots: never a recycled one.
    val blurred: ImageBitmap? = remember(snapshot, shown) {
        snapshot?.takeIf { shown && !it.isRecycled && it.width > 0 && it.height > 0 }?.let { source ->
            val (width, height) = V5SnapshotBackdropRules.blurredSize(source.width, source.height)
            Bitmap.createScaledBitmap(source, width, height, true).asImageBitmap()
        }
    }
    Box(modifier.clearAndSetSemantics {}) {
        if (blurred != null) {
            Image(
                bitmap = blurred,
                contentDescription = null,
                contentScale = contentScale,
                alignment = alignment,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(material.tint(dark = colors.isDark, blurAvailable = blurred != null, reduceTransparency = opaque)),
        )
    }
}
