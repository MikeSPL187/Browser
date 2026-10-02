package dev.sk2andy.materialbrowser.ui

import kotlin.math.floor

/** Where a dragged tile lands in the grid; pure, so the arithmetic is tested on the JVM. */
internal object EssentialsGridRules {
    /** The slot under ([x], [y]), the dragged tile's center in grid coordinates. */
    fun slotAt(
        x: Float,
        y: Float,
        pitchX: Float,
        pitchY: Float,
        columns: Int,
        count: Int,
        rtl: Boolean,
    ): Int {
        if (count <= 0 || pitchX <= 0f || pitchY <= 0f) return 0
        val rawColumn = floor(x / pitchX).toInt().coerceIn(0, columns - 1)
        val column = if (rtl) columns - 1 - rawColumn else rawColumn
        val row = floor(y / pitchY).toInt().coerceAtLeast(0)
        return (row * columns + column).coerceIn(0, count - 1)
    }
}
