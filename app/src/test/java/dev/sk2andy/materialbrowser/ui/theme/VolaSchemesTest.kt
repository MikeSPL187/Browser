package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.ui.graphics.Color
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.BrowserColorPalette
import dev.sk2andy.materialbrowser.shared.ui.theme.NeutralDarkColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.reflect.KProperty1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VolaSchemesTest {
    private val schemeSets: Map<String, VolaSchemeSet> =
        WorkspaceAccent.entries.associate { accent ->
            accent.name to VolaSchemes.forAccent(accent)
        } + ("Private" to VolaSchemes.Private)

    private val schemes: List<Pair<String, VolaSchemeTokens>> =
        schemeSets.flatMap { (name, set) ->
            listOf(
                "$name.light" to set.light,
                "$name.dark" to set.dark,
                "$name.lightHighContrast" to set.lightHighContrast,
                "$name.darkHighContrast" to set.darkHighContrast,
            )
        }

    @Test
    fun `every text and background pair reaches 4_5 to 1 in every scheme`() {
        val failures = schemes.flatMap { (name, tokens) ->
            TEXT_ON_BACKGROUND.mapNotNull { (text, background) ->
                val ratio = contrastRatio(text.get(tokens), background.get(tokens))
                if (ratio < MINIMUM_CONTRAST) {
                    "$name ${text.name}/${background.name} %.2f".format(ratio)
                } else {
                    null
                }
            }
        }

        assertEquals(36, schemes.size)
        assertTrue(failures.joinToString(separator = "\n"), failures.isEmpty())
    }

    @Test
    fun `dark schemes sit on pure black`() {
        schemeSets.forEach { (name, set) ->
            listOf(set.dark, set.darkHighContrast).forEach { tokens ->
                assertEquals(name, BLACK, tokens.surface)
                assertEquals(name, BLACK, tokens.surfaceDim)
                assertEquals(name, BLACK, tokens.surfaceContainerLowest)
                val colorScheme = tokens.toColorScheme(dark = true)
                assertEquals(name, Color.Black, colorScheme.background)
                assertEquals(name, Color.Black, colorScheme.surface)
            }
            listOf(set.light, set.lightHighContrast).forEach { tokens ->
                assertNotEquals(name, BLACK, tokens.surface)
            }
        }
    }

    @Test
    fun `workspace seeds are the accent tones of WorkspaceAccents`() {
        WorkspaceAccent.entries.forEach { accent ->
            assertEquals(
                accent.name,
                WorkspaceAccentTokens.color(accent = accent, dark = false),
                Color(VolaSchemes.forAccent(accent).seed),
            )
        }
        assertEquals(
            WorkspaceAccent.entries.size,
            WorkspaceAccent.entries.map { VolaSchemes.forAccent(it).light.primary }.toSet().size,
        )
    }

    @Test
    fun `color scheme maps every generated role`() {
        val tokens = VolaSchemes.Teal.light
        val colorScheme = tokens.toColorScheme(dark = false)

        assertEquals(Color(tokens.primary), colorScheme.primary)
        assertEquals(Color(tokens.onPrimaryContainer), colorScheme.onPrimaryContainer)
        assertEquals(Color(tokens.tertiaryContainer), colorScheme.tertiaryContainer)
        assertEquals(Color(tokens.surface), colorScheme.background)
        assertEquals(Color(tokens.onSurfaceVariant), colorScheme.onSurfaceVariant)
        assertEquals(Color(tokens.surfaceContainerHighest), colorScheme.surfaceContainerHighest)
        assertEquals(Color(tokens.errorContainer), colorScheme.errorContainer)
        assertEquals(Color(tokens.outlineVariant), colorScheme.outlineVariant)
    }

    @Test
    fun `scheme set picks the variant for darkness and contrast`() {
        val set = VolaSchemes.Violet

        assertSame(set.light, set.select(dark = false, highContrast = false))
        assertSame(set.dark, set.select(dark = true, highContrast = false))
        assertSame(set.lightHighContrast, set.select(dark = false, highContrast = true))
        assertSame(set.darkHighContrast, set.select(dark = true, highContrast = true))
        assertSame(VolaSchemes.Private, VolaColorRules.schemeSet(WorkspaceAccent.Teal, true))
        assertSame(VolaSchemes.Teal, VolaColorRules.schemeSet(WorkspaceAccent.Teal, false))
    }

    @Test
    fun `cards are white on light and raised grey on black`() {
        schemeSets.forEach { (name, set) ->
            assertEquals(name, set.light.surfaceContainerLowest, set.light.card)
            assertEquals(name, set.dark.surfaceContainer, set.dark.card)
            assertNotEquals(name, BLACK, set.dark.card)
        }
    }

    @Test
    fun `extended colors follow the workspace in the Vola palette`() {
        val tokens = VolaSchemes.Coral.dark
        val colorScheme = tokens.toColorScheme(dark = true)

        val vola = VolaColorRules.extendedColors(
            palette = BrowserColorPalette.Vola,
            colorScheme = colorScheme,
            workspaceTokens = tokens,
            dark = true,
        )
        val neutral = VolaColorRules.extendedColors(
            palette = BrowserColorPalette.Neutral,
            colorScheme = NeutralDarkColors.withPureBlackSurfaces(),
            workspaceTokens = tokens,
            dark = true,
        )

        assertEquals(tokens.toExtendedColors(), vola)
        assertEquals(Color(tokens.ok), neutral.ok)
        assertEquals(NeutralDarkColors.primaryContainer, neutral.aura1)
        assertEquals(NeutralDarkColors.surfaceContainer, neutral.card)
    }

    @Test
    fun `other palettes also turn pure black in the dark theme`() {
        val colorScheme = NeutralDarkColors.withPureBlackSurfaces()

        assertEquals(Color.Black, colorScheme.background)
        assertEquals(Color.Black, colorScheme.surface)
        assertEquals(Color.Black, colorScheme.surfaceContainerLowest)
        assertEquals(NeutralDarkColors.surfaceContainer, colorScheme.surfaceContainer)
    }

    private fun contrastRatio(first: Long, second: Long): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** WCAG 2 relative luminance, the same formula as tools/contrast.py and gencss.mjs. */
    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 0xFF) / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private companion object {
        const val MINIMUM_CONTRAST = 4.5
        const val BLACK = 0xFF000000

        private fun onSurfaceOver(
            background: KProperty1<VolaSchemeTokens, Long>,
        ): List<Pair<KProperty1<VolaSchemeTokens, Long>, KProperty1<VolaSchemeTokens, Long>>> =
            listOf(
                VolaSchemeTokens::onSurface to background,
                VolaSchemeTokens::onSurfaceVariant to background,
            )

        private fun accentsOver(
            background: KProperty1<VolaSchemeTokens, Long>,
        ): List<Pair<KProperty1<VolaSchemeTokens, Long>, KProperty1<VolaSchemeTokens, Long>>> =
            listOf(
                VolaSchemeTokens::primary to background,
                VolaSchemeTokens::error to background,
                VolaSchemeTokens::ok to background,
                VolaSchemeTokens::warn to background,
            )

        /** Same list as contrastPairs in gencss.mjs. */
        val TEXT_ON_BACKGROUND = listOf(
            VolaSchemeTokens::onPrimary to VolaSchemeTokens::primary,
            VolaSchemeTokens::onPrimaryContainer to VolaSchemeTokens::primaryContainer,
            VolaSchemeTokens::onSecondary to VolaSchemeTokens::secondary,
            VolaSchemeTokens::onSecondaryContainer to VolaSchemeTokens::secondaryContainer,
            VolaSchemeTokens::onTertiary to VolaSchemeTokens::tertiary,
            VolaSchemeTokens::onTertiaryContainer to VolaSchemeTokens::tertiaryContainer,
            VolaSchemeTokens::onError to VolaSchemeTokens::error,
            VolaSchemeTokens::onErrorContainer to VolaSchemeTokens::errorContainer,
            VolaSchemeTokens::onOk to VolaSchemeTokens::ok,
            VolaSchemeTokens::onOkContainer to VolaSchemeTokens::okContainer,
            VolaSchemeTokens::onWarn to VolaSchemeTokens::warn,
            VolaSchemeTokens::onWarnContainer to VolaSchemeTokens::warnContainer,
            VolaSchemeTokens::inverseOnSurface to VolaSchemeTokens::inverseSurface,
            VolaSchemeTokens::inversePrimary to VolaSchemeTokens::inverseSurface,
        ) + listOf(
            VolaSchemeTokens::surface,
            VolaSchemeTokens::surfaceBright,
            VolaSchemeTokens::surfaceDim,
            VolaSchemeTokens::surfaceContainerLowest,
            VolaSchemeTokens::surfaceContainerLow,
            VolaSchemeTokens::surfaceContainer,
            VolaSchemeTokens::surfaceContainerHigh,
            VolaSchemeTokens::surfaceContainerHighest,
            VolaSchemeTokens::surfaceVariant,
            VolaSchemeTokens::card,
            VolaSchemeTokens::aura1,
            VolaSchemeTokens::aura2,
            VolaSchemeTokens::aura3,
        ).flatMap(::onSurfaceOver) + listOf(
            VolaSchemeTokens::surface,
            VolaSchemeTokens::surfaceContainerLowest,
            VolaSchemeTokens::card,
        ).flatMap(::accentsOver)
    }
}
