package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * v5 shapes (system-v5.md, section 3). Corners are smoothed like a superellipse: each corner is a
 * cubic run-in, a circular arc whose radius is the token, and a cubic run-out, so the curvature
 * grows gradually instead of jumping where the straight side meets the arc. Smoothing 0.6 spreads
 * a corner over up to 1.6 radii of each side; a short side lowers the smoothing first, then the
 * radius. `full` is always a plain capsule.
 */

/** Smoothing of every v5 corner. */
const val V5_CORNER_SMOOTHING = 0.6f

@Immutable
data class V5ShapeScale(
    /** 8 dp: settings icon tiles, small tags. */
    val xs: Shape,
    /** 12 dp: grouped lists, the search field, keys. */
    val s: Shape,
    /** 16 dp: tab cards, pinned sites, blocks inside a sheet. */
    val m: Shape,
    /** 24 dp: cards on the new tab, the link preview. */
    val l: Shape,
    /** 32 dp: the top of sheets, the lock screen window. */
    val xl: Shape,
    /** Radius = height / 2: everything in the bar, buttons, chips, switches. */
    val full: Shape,
    /** A sheet: [xl] on top, square at the bottom edge. */
    val sheet: Shape,
)

object V5Shapes {
    val radiusXs: Dp = 8.dp
    val radiusS: Dp = 12.dp
    val radiusM: Dp = 16.dp
    val radiusL: Dp = 24.dp
    val radiusXl: Dp = 32.dp

    val default: V5ShapeScale = V5ShapeScale(
        xs = SmoothCornerShape(radiusXs),
        s = SmoothCornerShape(radiusS),
        m = SmoothCornerShape(radiusM),
        l = SmoothCornerShape(radiusL),
        xl = SmoothCornerShape(radiusXl),
        full = RoundedCornerShape(percent = 50),
        sheet = SmoothCornerShape(topStart = radiusXl, topEnd = radiusXl, bottomEnd = 0.dp, bottomStart = 0.dp),
    )

    /** Concentric radius of a shape inset by [padding] in a parent of [parentRadius] (never below 0). */
    fun concentric(parentRadius: Dp, padding: Dp): Dp = (parentRadius - padding).coerceAtLeast(0.dp)
}

/** A rounded rectangle with smoothed (superellipse-like) corners. */
@Immutable
class SmoothCornerShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val smoothing: Float = V5_CORNER_SMOOTHING,
) : Shape {
    constructor(radius: Dp, smoothing: Float = V5_CORNER_SMOOTHING) :
        this(radius, radius, radius, radius, smoothing)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val radii = with(density) {
            listOf(topStart, topEnd, bottomEnd, bottomStart).map { it.toPx() }
        }
        // Clockwise from the top-left corner, in physical (left/right) order.
        val (topLeft, topRight, bottomRight, bottomLeft) =
            if (ltr) radii else listOf(radii[1], radii[0], radii[3], radii[2])
        if (topLeft + topRight + bottomRight + bottomLeft == 0f) {
            return Outline.Rectangle(size.toRect())
        }
        return Outline.Generic(smoothRectPath(size, topLeft, topRight, bottomRight, bottomLeft, smoothing))
    }

    override fun equals(other: Any?): Boolean = other is SmoothCornerShape &&
        topStart == other.topStart && topEnd == other.topEnd && bottomEnd == other.bottomEnd &&
        bottomStart == other.bottomStart && smoothing == other.smoothing

    override fun hashCode(): Int =
        listOf(topStart, topEnd, bottomEnd, bottomStart, smoothing).hashCode()

    override fun toString(): String =
        "SmoothCornerShape(topStart=$topStart, topEnd=$topEnd, bottomEnd=$bottomEnd, " +
            "bottomStart=$bottomStart, smoothing=$smoothing)"
}

/**
 * The measurements of one smoothed corner, along its two sides from the corner point.
 * [extent] is how far the corner reaches along each side; [a], [b] are the run-in handles, [c], [d]
 * the run-in end point; [arcDegrees] the sweep of the circular part.
 */
internal data class SmoothCorner(
    val radius: Float,
    val extent: Float,
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val arcDegrees: Float,
)

/**
 * Corner geometry for [radius] with [smoothing] when the corner may reach at most [budget] along
 * each side. Too short a side lowers the smoothing first, then the radius.
 */
internal fun smoothCorner(radius: Float, smoothing: Float, budget: Float): SmoothCorner {
    val r = min(radius, budget).coerceAtLeast(0f)
    if (r == 0f) return SmoothCorner(0f, 0f, 0f, 0f, 0f, 0f, 0f)
    val s = min(smoothing.coerceIn(0f, 1f), budget / r - 1f).coerceAtLeast(0f)
    val extent = (1f + s) * r
    val arcDegrees = 90f * (1f - s)
    val arcChord = sin(rad(arcDegrees / 2f)) * r * sqrt(2f)
    val alpha = (90f - arcDegrees) / 2f
    val p3ToP4 = r * tan(rad(alpha / 2f))
    val beta = 45f * s
    val c = p3ToP4 * cos(rad(beta))
    val d = c * tan(rad(beta))
    val b = (extent - arcChord - c - d) / 3f
    val a = 2f * b
    return SmoothCorner(r, extent, a, b, c, d, arcDegrees)
}

private fun rad(degrees: Float): Float = (degrees * Math.PI / 180.0).toFloat()

/**
 * The path, clockwise in screen coordinates. Each corner is written in its own frame: `along`
 * runs back along the incoming side from the corner point, `out` forward along the outgoing side.
 */
internal fun smoothRectPath(
    size: Size,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    smoothing: Float,
): Path {
    val budget = min(size.width, size.height) / 2f
    val w = size.width
    val h = size.height
    // Corner point, direction of travel into the corner, direction of travel out of it.
    val corners = listOf(
        Triple(Offset(w, 0f), Offset(1f, 0f), Offset(0f, 1f)) to topRight,
        Triple(Offset(w, h), Offset(0f, 1f), Offset(-1f, 0f)) to bottomRight,
        Triple(Offset(0f, h), Offset(-1f, 0f), Offset(0f, -1f)) to bottomLeft,
        Triple(Offset(0f, 0f), Offset(0f, -1f), Offset(1f, 0f)) to topLeft,
    )
    val path = Path()
    val topLeftCorner = smoothCorner(topLeft, smoothing, budget)
    path.moveTo(topLeftCorner.extent, 0f)
    for ((frame, radius) in corners) {
        val (point, into, out) = frame
        val corner = smoothCorner(radius, smoothing, budget)
        fun at(along: Float, outward: Float) = point - into * along + out * outward
        val start = at(corner.extent, 0f)
        path.lineTo(start.x, start.y)
        if (corner.radius == 0f) continue
        val e = corner.extent
        val runInEnd = at(e - corner.a - corner.b - corner.c, corner.d)
        path.cubicTo(at(e - corner.a, 0f), at(e - corner.a - corner.b, 0f), runInEnd)
        val runOutStart = at(corner.d, e - corner.a - corner.b - corner.c)
        arcTo(path, runInEnd, runOutStart, point - into * corner.radius + out * corner.radius,
            corner.radius, corner.arcDegrees)
        path.cubicTo(at(0f, e - corner.a - corner.b), at(0f, e - corner.a), at(0f, e))
    }
    path.close()
    return path
}

/** A clockwise circular arc from [from] to [to] around [center], as one cubic. */
private fun arcTo(path: Path, from: Offset, to: Offset, center: Offset, radius: Float, sweepDegrees: Float) {
    if (sweepDegrees <= 0f) {
        path.lineTo(to.x, to.y)
        return
    }
    val handle = 4f / 3f * tan(rad(sweepDegrees / 4f)) * radius
    path.cubicTo(from + clockwiseTangent(from - center) * handle, to - clockwiseTangent(to - center) * handle, to)
}

/** The unit tangent of a clockwise (screen coordinates) circle at a point [radial] from its center. */
private fun clockwiseTangent(radial: Offset): Offset {
    val length = radial.getDistance()
    return if (length == 0f) Offset.Zero else Offset(-radial.y / length, radial.x / length)
}

private fun Path.cubicTo(first: Offset, second: Offset, end: Offset) =
    cubicTo(first.x, first.y, second.x, second.y, end.x, end.y)
