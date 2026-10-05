package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.BrowserColorPalette

/**
 * One generated color scheme as ARGB values: the Material roles plus Vola's own roles. The values
 * come from VolaSchemes.kt, which gencss.mjs writes; plain numbers keep them testable on the JVM.
 */
@Immutable
internal data class VolaSchemeTokens(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val inversePrimary: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val outline: Long,
    val outlineVariant: Long,
    val scrim: Long,
    val surfaceBright: Long,
    val surfaceDim: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long,
    val aura1: Long,
    val aura2: Long,
    val aura3: Long,
    val ok: Long,
    val onOk: Long,
    val okContainer: Long,
    val onOkContainer: Long,
    val warn: Long,
    val onWarn: Long,
    val warnContainer: Long,
    val onWarnContainer: Long,
    val card: Long,
)

/** The four schemes of one seed color. Dark schemes always sit on pure black. */
@Immutable
internal data class VolaSchemeSet(
    val seed: Long,
    val light: VolaSchemeTokens,
    val dark: VolaSchemeTokens,
    val lightHighContrast: VolaSchemeTokens,
    val darkHighContrast: VolaSchemeTokens,
) {
    val all: List<VolaSchemeTokens>
        get() = listOf(light, dark, lightHighContrast, darkHighContrast)

    fun select(dark: Boolean, highContrast: Boolean): VolaSchemeTokens = when {
        dark && highContrast -> darkHighContrast
        dark -> this.dark
        highContrast -> lightHighContrast
        else -> light
    }
}

/**
 * What a theme (board W-Themes) brings to the shell: its neutrals, aura and card. The accent keeps
 * its primary, secondary, tertiary, error, success and warning roles. Values come from
 * VolaThemeSchemes.kt, which gencss.mjs writes.
 */
@Immutable
internal data class VolaThemeTokens(
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val outline: Long,
    val outlineVariant: Long,
    val scrim: Long,
    val surfaceBright: Long,
    val surfaceDim: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long,
    val aura1: Long,
    val aura2: Long,
    val aura3: Long,
    val card: Long,
)

/** The four variants of one theme, picked the same way as a [VolaSchemeSet]. */
@Immutable
internal data class VolaThemeSet(
    val light: VolaThemeTokens,
    val dark: VolaThemeTokens,
    val lightHighContrast: VolaThemeTokens,
    val darkHighContrast: VolaThemeTokens,
) {
    val all: List<VolaThemeTokens>
        get() = listOf(light, dark, lightHighContrast, darkHighContrast)

    fun select(dark: Boolean, highContrast: Boolean): VolaThemeTokens = when {
        dark && highContrast -> darkHighContrast
        dark -> this.dark
        highContrast -> lightHighContrast
        else -> light
    }
}

/** The accent's scheme on a theme's neutrals: what the shell shows for a theme. */
internal fun VolaSchemeTokens.withTheme(theme: VolaThemeTokens): VolaSchemeTokens = copy(
    surface = theme.surface,
    onSurface = theme.onSurface,
    surfaceVariant = theme.surfaceVariant,
    onSurfaceVariant = theme.onSurfaceVariant,
    inverseSurface = theme.inverseSurface,
    inverseOnSurface = theme.inverseOnSurface,
    outline = theme.outline,
    outlineVariant = theme.outlineVariant,
    scrim = theme.scrim,
    surfaceBright = theme.surfaceBright,
    surfaceDim = theme.surfaceDim,
    surfaceContainerLowest = theme.surfaceContainerLowest,
    surfaceContainerLow = theme.surfaceContainerLow,
    surfaceContainer = theme.surfaceContainer,
    surfaceContainerHigh = theme.surfaceContainerHigh,
    surfaceContainerHighest = theme.surfaceContainerHighest,
    aura1 = theme.aura1,
    aura2 = theme.aura2,
    aura3 = theme.aura3,
    card = theme.card,
)

/** Vola roles that Material's ColorScheme has no slot for. */
@Immutable
internal data class VolaExtendedColors(
    val aura1: Color,
    val aura2: Color,
    val aura3: Color,
    val ok: Color,
    val onOk: Color,
    val okContainer: Color,
    val onOkContainer: Color,
    val warn: Color,
    val onWarn: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
    val card: Color,
) {
    /** The workspace aura as gradient stops, from the top of the shell down. */
    val aura: List<Color>
        get() = listOf(aura1, aura2, aura3)
}

internal fun VolaSchemeTokens.toColorScheme(dark: Boolean): ColorScheme =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(surface),
        onBackground = Color(onSurface),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
    )

internal fun VolaSchemeTokens.toExtendedColors(): VolaExtendedColors = VolaExtendedColors(
    aura1 = Color(aura1),
    aura2 = Color(aura2),
    aura3 = Color(aura3),
    ok = Color(ok),
    onOk = Color(onOk),
    okContainer = Color(okContainer),
    onOkContainer = Color(onOkContainer),
    warn = Color(warn),
    onWarn = Color(onWarn),
    warnContainer = Color(warnContainer),
    onWarnContainer = Color(onWarnContainer),
    card = Color(card),
)

/** Resolves the colors of the shell from the palette setting and the selected workspace. */
internal object VolaColorRules {
    fun schemeSet(accent: WorkspaceAccent, privateMode: Boolean): VolaSchemeSet =
        if (privateMode) VolaSchemes.Private else VolaSchemes.forAccent(accent)

    /**
     * The accent that colors the shell: the one picked for every workspace if there is one, the
     * workspace's own otherwise.
     */
    fun accent(workspace: WorkspaceAccent, override: WorkspaceAccent?): WorkspaceAccent =
        override ?: workspace

    /** The neutrals of a theme; null for Vola (the accent's own) and the system palette. */
    fun themeSet(palette: BrowserColorPalette): VolaThemeSet? = when (palette) {
        BrowserColorPalette.Vola, BrowserColorPalette.Dynamic -> null
        BrowserColorPalette.Ice -> VolaThemeSchemes.Ice
        BrowserColorPalette.Dusk -> VolaThemeSchemes.Dusk
        BrowserColorPalette.Paper -> VolaThemeSchemes.Paper
        BrowserColorPalette.Mono -> VolaThemeSchemes.Mono
    }

    /** The generated tokens of [palette] over [accentTokens]; null for the system palette. */
    fun tokens(
        palette: BrowserColorPalette,
        accentTokens: VolaSchemeTokens,
        dark: Boolean,
        highContrast: Boolean,
    ): VolaSchemeTokens? = when (palette) {
        BrowserColorPalette.Dynamic -> null
        else -> themeSet(palette)
            ?.let { theme -> accentTokens.withTheme(theme.select(dark, highContrast)) }
            ?: accentTokens
    }

    /**
     * Vola roles for a palette. Generated palettes take every role from their [tokens]. The system
     * palette has no generated aura, so its comes from its own containers; success and warning stay
     * the accent's, which already pass 4.5:1.
     */
    fun extendedColors(
        palette: BrowserColorPalette,
        colorScheme: ColorScheme,
        tokens: VolaSchemeTokens,
        dark: Boolean,
    ): VolaExtendedColors {
        val generated = tokens.toExtendedColors()
        if (palette != BrowserColorPalette.Dynamic) return generated
        return generated.copy(
            aura1 = colorScheme.primaryContainer,
            aura2 = colorScheme.secondaryContainer,
            aura3 = colorScheme.surfaceContainerLow,
            card = if (dark) colorScheme.surfaceContainer else colorScheme.surfaceContainerLowest,
        )
    }
}

/** Dark surfaces of the system palette, moved onto pure black like Vola's own. */
internal fun ColorScheme.withPureBlackSurfaces(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
)

internal val LocalVolaExtendedColors = staticCompositionLocalOf {
    VolaSchemes.forAccent(WorkspaceAccent.Default).light.toExtendedColors()
}

/** Entry point to Vola's theme values that MaterialTheme has no slot for. */
internal object VolaTheme {
    val extendedColors: VolaExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVolaExtendedColors.current
}
