package dev.sk2andy.materialbrowser.ui.theme.v5

import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * v5 depth (system-v5.md, sections 4 and 5): shadows and materials. Blur is only ever drawn over
 * Vola's own pixels or a snapshot of the page, never over the live GeckoView SurfaceView, which no
 * Compose effect can see.
 */

/** One shadow layer: offset down by [y], blurred by [blur]. */
@Immutable
data class V5Shadow(val y: Dp, val blur: Dp, val color: Color)

/** Shadow layers plus an optional hairline outline (the dark theme's way to lift a card). */
@Immutable
data class V5Lift(val shadows: List<V5Shadow>, val outline: Color? = null)

/** One elevation level in both themes. */
@Immutable
data class V5ElevationLevel(val light: V5Lift, val dark: V5Lift) {
    fun forTheme(dark: Boolean): V5Lift = if (dark) this.dark else light
}

object V5Elevation {
    private val none = V5Lift(emptyList())

    /** Lists and backgrounds. */
    val e0 = V5ElevationLevel(none, none)

    /** Cards and tiles. */
    val e1 = V5ElevationLevel(
        light = V5Lift(listOf(V5Shadow(1.dp, 3.dp, Color(0x0F000000)), V5Shadow(2.dp, 8.dp, Color(0x0A000000)))),
        dark = V5Lift(emptyList(), outline = Color(0x0FFFFFFF)),
    )

    /** The bar, floating buttons, the page card on a tablet. */
    val e2 = V5ElevationLevel(
        light = V5Lift(listOf(V5Shadow(2.dp, 6.dp, Color(0x14000000)), V5Shadow(8.dp, 24.dp, Color(0x14000000)))),
        dark = V5Lift(listOf(V5Shadow(8.dp, 24.dp, Color(0x66000000)))),
    )

    /** Sheets, the link preview, menus. */
    val e3 = V5ElevationLevel(
        light = V5Lift(listOf(V5Shadow(12.dp, 40.dp, Color(0x29000000)))),
        dark = V5Lift(listOf(V5Shadow(16.dp, 48.dp, Color(0x8C000000)))),
    )
}

/**
 * A translucent material. [blur] applies from Android 13; on 12 Haze causes extra invalidations, so
 * there and below the tint alone is drawn at [fallbackAlpha]. Battery saver, "Reduce transparency"
 * and increased contrast use it opaque.
 */
@Immutable
data class V5Material(
    val blur: Dp,
    val lightTint: Color,
    val darkTint: Color,
    val tintAlpha: Float,
    val fallbackAlpha: Float,
) {
    fun tint(dark: Boolean, blurAvailable: Boolean, reduceTransparency: Boolean): Color {
        val base = if (dark) darkTint else lightTint
        val alpha = when {
            reduceTransparency -> 1f
            blurAvailable -> tintAlpha
            else -> fallbackAlpha
        }
        return base.copy(alpha = alpha)
    }
}

object V5Materials {
    /** Overview backdrop, plates on thumbnails. */
    val thin = V5Material(12.dp, Color(0xFFFFFFFF), Color(0xFF2A2A2D), 0.72f, 0.92f)

    /** The overview's bottom bar, the tablet sidebar. */
    val regular = V5Material(24.dp, Color(0xFFF9F9FB), Color(0xFF262628), 0.84f, 0.94f)

    /** Sheets and address suggestions (over a snapshot). Dark is based on background, not surface. */
    val thick = V5Material(40.dp, Color(0xFFF4F4F6), Color(0xFF1E1E20), 0.92f, 0.97f)

    /** The 0.5 dp top edge under every material: light, dark. */
    val edgeLight = Color(0x14000000)
    val edgeDark = Color(0x0FFFFFFF)

    /** Live blur from Android 13 (API 33); see [V5Material]. */
    val blurSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
