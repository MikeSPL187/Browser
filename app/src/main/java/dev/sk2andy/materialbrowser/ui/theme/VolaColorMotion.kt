package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The whole scheme springs to [target] instead of jumping: switching between a regular and a
 * private tab, or between workspaces, recolors the shell without a flash. With animations off
 * the colors change at once.
 */
@Composable
internal fun animateColorScheme(target: ColorScheme, animate: Boolean): ColorScheme {
    val spec: AnimationSpec<Color> = if (animate) VolaMotion.effects() else snap()

    @Composable
    fun Color.animated(): Color = animateColorAsState(this, spec, label = "scheme").value

    return target.copy(
        primary = target.primary.animated(),
        onPrimary = target.onPrimary.animated(),
        primaryContainer = target.primaryContainer.animated(),
        onPrimaryContainer = target.onPrimaryContainer.animated(),
        inversePrimary = target.inversePrimary.animated(),
        secondary = target.secondary.animated(),
        onSecondary = target.onSecondary.animated(),
        secondaryContainer = target.secondaryContainer.animated(),
        onSecondaryContainer = target.onSecondaryContainer.animated(),
        tertiary = target.tertiary.animated(),
        onTertiary = target.onTertiary.animated(),
        tertiaryContainer = target.tertiaryContainer.animated(),
        onTertiaryContainer = target.onTertiaryContainer.animated(),
        background = target.background.animated(),
        onBackground = target.onBackground.animated(),
        surface = target.surface.animated(),
        onSurface = target.onSurface.animated(),
        surfaceVariant = target.surfaceVariant.animated(),
        onSurfaceVariant = target.onSurfaceVariant.animated(),
        surfaceTint = target.surfaceTint.animated(),
        inverseSurface = target.inverseSurface.animated(),
        inverseOnSurface = target.inverseOnSurface.animated(),
        error = target.error.animated(),
        onError = target.onError.animated(),
        errorContainer = target.errorContainer.animated(),
        onErrorContainer = target.onErrorContainer.animated(),
        outline = target.outline.animated(),
        outlineVariant = target.outlineVariant.animated(),
        scrim = target.scrim.animated(),
        surfaceBright = target.surfaceBright.animated(),
        surfaceDim = target.surfaceDim.animated(),
        surfaceContainer = target.surfaceContainer.animated(),
        surfaceContainerHigh = target.surfaceContainerHigh.animated(),
        surfaceContainerHighest = target.surfaceContainerHighest.animated(),
        surfaceContainerLow = target.surfaceContainerLow.animated(),
        surfaceContainerLowest = target.surfaceContainerLowest.animated(),
    )
}

/** The Vola roles of [target], animated like [animateColorScheme]. */
@Composable
internal fun animateExtendedColors(target: VolaExtendedColors, animate: Boolean): VolaExtendedColors {
    val spec: AnimationSpec<Color> = if (animate) VolaMotion.effects() else snap()

    @Composable
    fun Color.animated(): Color = animateColorAsState(this, spec, label = "vola").value

    return target.copy(
        aura1 = target.aura1.animated(),
        aura2 = target.aura2.animated(),
        aura3 = target.aura3.animated(),
        card = target.card.animated(),
    )
}
