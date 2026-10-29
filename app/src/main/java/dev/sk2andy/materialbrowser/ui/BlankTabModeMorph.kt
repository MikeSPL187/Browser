package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.math.hypot
import kotlin.math.max

internal object BlankTabModeMorphRules {
    const val DURATION_MILLIS = 360
    const val HERO_SHADOW_ELEVATION_DP = 14
    const val HERO_SHADOW_CLEARANCE_DP = 32

    fun target(incognito: Boolean): Float = if (incognito) 1f else 0f

    fun bounded(progress: Float): Float =
        if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f

    fun interpolate(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * bounded(progress)

    fun regularIconAlpha(progress: Float): Float = 1f - bounded(progress)

    fun incognitoIconAlpha(progress: Float): Float = bounded(progress)

    fun iconScale(alpha: Float): Float = interpolate(0.82f, 1f, alpha)

    fun controlCornerRadiusDp(progress: Float): Float = interpolate(14f, 20f, progress)

    fun heroCornerRadiusDp(progress: Float): Float = interpolate(48f, 30f, progress)

    fun maxRevealRadius(
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
    ): Float {
        val safeWidth = width.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val safeHeight = height.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val safeOriginX = originX.takeIf { it.isFinite() } ?: safeWidth / 2f
        val safeOriginY = originY.takeIf { it.isFinite() } ?: safeHeight / 2f
        val farthestX = max(safeOriginX, safeWidth - safeOriginX)
        val farthestY = max(safeOriginY, safeHeight - safeOriginY)
        return hypot(farthestX, farthestY)
    }

    fun revealRadius(
        progress: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
    ): Float = maxRevealRadius(originX, originY, width, height) * bounded(progress)
}

@Composable
internal fun rememberBlankTabModeProgress(tabId: String, incognito: Boolean): Float {
    val progress = remember(tabId) {
        Animatable(BlankTabModeMorphRules.target(incognito))
    }
    LaunchedEffect(progress, incognito) {
        progress.animateTo(
            targetValue = BlankTabModeMorphRules.target(incognito),
            animationSpec = tween(
                durationMillis = BlankTabModeMorphRules.DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )
    }
    return BlankTabModeMorphRules.bounded(progress.value)
}
