package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The workspace aura behind the framed shell (`.aura` in vola4.css). Light themes run the three
 * aura stops at 172°; the dark theme runs from the first two stops down into pure black.
 */
internal object VolaAura {
    const val LIGHT_ANGLE_DEGREES = 172f
    const val DARK_ANGLE_DEGREES = 180f
    const val LIGHT_MIDDLE_STOP = 0.48f
    const val DARK_MIDDLE_STOP = 0.38f

    fun brush(colors: VolaExtendedColors, dark: Boolean): ShaderBrush = if (dark) {
        AngledLinearGradient(
            colors = listOf(colors.aura1, colors.aura2, Color.Black),
            stops = listOf(0f, DARK_MIDDLE_STOP, 1f),
            angleDegrees = DARK_ANGLE_DEGREES,
        )
    } else {
        AngledLinearGradient(
            colors = colors.aura,
            stops = listOf(0f, LIGHT_MIDDLE_STOP, 1f),
            angleDegrees = LIGHT_ANGLE_DEGREES,
        )
    }
}

/**
 * A CSS-style `linear-gradient(<angle>, …)`: 0° points up, 90° right, and the gradient line is as
 * long as needed for the first and last stop to touch the box corners.
 */
@Immutable
internal data class AngledLinearGradient(
    val colors: List<Color>,
    val stops: List<Float>,
    val angleDegrees: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
        val halfLength = (abs(size.width * direction.x) + abs(size.height * direction.y)) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        return LinearGradientShader(
            from = center - direction * halfLength,
            to = center + direction * halfLength,
            colors = colors,
            colorStops = stops,
        )
    }
}

/** Whether the app theme is dark; the aura and other shell surfaces follow it. */
internal val LocalVolaDarkTheme = staticCompositionLocalOf { false }

internal val VolaTheme.auraBrush: ShaderBrush
    @Composable
    @ReadOnlyComposable
    get() = VolaAura.brush(extendedColors, LocalVolaDarkTheme.current)
