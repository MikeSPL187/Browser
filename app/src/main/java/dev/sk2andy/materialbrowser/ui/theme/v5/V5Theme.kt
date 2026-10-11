package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/*
 * The v5 theme. Screens of the new interface (behind the ui.v5 flag) read every value through
 * [V5Theme]; Material 3 components inside get the same roles mapped onto their ColorScheme,
 * Typography and Shapes, so a stock Switch or Button already looks like v5.
 */

private val LocalV5Colors = staticCompositionLocalOf { V5Colors.light() }
private val LocalV5Type = staticCompositionLocalOf { V5Type.default }
private val LocalV5Shapes = staticCompositionLocalOf { V5Shapes.default }

/** Access to the current v5 tokens, like MaterialTheme for Material roles. */
object V5Theme {
    val colors: V5ColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalV5Colors.current

    val type: V5Typography
        @Composable @ReadOnlyComposable
        get() = LocalV5Type.current

    val shapes: V5ShapeScale
        @Composable @ReadOnlyComposable
        get() = LocalV5Shapes.current
}

/**
 * Applies v5 to [content]: light or dark from the system unless [dark] says otherwise, the
 * profile's [accent], and the private palette (always dark) when [privateBrowsing] is set.
 */
@Composable
fun VolaV5Theme(
    dark: Boolean = isSystemInDarkTheme(),
    accent: V5Accent = V5Accent.Default,
    privateBrowsing: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = remember(dark, accent, privateBrowsing, highContrast) {
        val base = when {
            privateBrowsing -> V5Colors.privateBrowsing()
            dark -> V5Colors.dark(accent)
            else -> V5Colors.light(accent)
        }
        if (highContrast) with(V5Colors) { base.highContrast() } else base
    }
    val materialColors = remember(colors) { colors.toMaterial() }
    CompositionLocalProvider(
        LocalV5Colors provides colors,
        LocalV5Type provides V5Type.default,
        LocalV5Shapes provides V5Shapes.default,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}

/** Material roles from v5 roles: primary is the accent fill, surfaces follow the neutral steps. */
internal fun V5ColorScheme.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accentFill,
        onPrimary = onAccentFill,
        primaryContainer = accentSoft,
        onPrimaryContainer = label,
        secondary = accent,
        onSecondary = onAccentFill,
        secondaryContainer = accentSoft,
        onSecondaryContainer = label,
        tertiary = accent,
        onTertiary = onAccentFill,
        background = background,
        onBackground = label,
        surface = surface,
        onSurface = label,
        surfaceVariant = surfaceElevated,
        onSurfaceVariant = labelSecondary,
        surfaceTint = accentFill,
        inverseSurface = label,
        inverseOnSurface = background,
        inversePrimary = accent,
        error = danger,
        onError = onAccentFill,
        outline = labelTertiary,
        outlineVariant = separator,
        scrim = scrim,
        surfaceBright = surfaceElevated,
        surfaceDim = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceElevated,
        surfaceContainerHighest = surfaceHighest,
    )
}

private val MaterialTypography: Typography = with(V5Type.default) {
    Typography(
        displayLarge = largeTitle,
        displayMedium = largeTitle,
        displaySmall = largeTitle,
        headlineLarge = largeTitle,
        headlineMedium = title,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = headline,
        titleSmall = footnote,
        bodyLarge = body,
        bodyMedium = callout,
        bodySmall = footnote,
        labelLarge = headline,
        labelMedium = footnote,
        labelSmall = caption,
    )
}

/** Material's Shapes take plain rounded corners; v5 components use the smoothed [V5Theme.shapes]. */
private val MaterialShapes: Shapes = with(V5Shapes) {
    Shapes(
        extraSmall = RoundedCornerShape(radiusXs),
        small = RoundedCornerShape(radiusS),
        medium = RoundedCornerShape(radiusM),
        large = RoundedCornerShape(radiusL),
        extraLarge = RoundedCornerShape(radiusXl),
    )
}
