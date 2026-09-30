package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAddressBarColorPreset
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserColorPalette
import dev.sk2andy.materialbrowser.data.BrowserShapeStyle
import dev.sk2andy.materialbrowser.data.BrowserSurfaceStyle
import dev.sk2andy.materialbrowser.ui.theme.BrowserChromeSurfaceRole
import dev.sk2andy.materialbrowser.ui.theme.CandyChromeTreatment
import dev.sk2andy.materialbrowser.ui.theme.CandyDesignLanguage
import dev.sk2andy.materialbrowser.ui.theme.CandyTheme
import dev.sk2andy.materialbrowser.ui.theme.LocalCandyDesignLanguage
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaSchemes
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor
import dev.sk2andy.materialbrowser.ui.theme.browserChromeSurfaceTokens
import dev.sk2andy.materialbrowser.ui.theme.BrowserChromeSurfaceTokens
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialBrowserThemeInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun translucentSurfaceInheritsThemeAwareContentColor() {
        val expected = AtomicReference<Color>()
        val actual = AtomicReference<Color>()

        composeRule.setContent {
            MaterialBrowserTheme {
                expected.set(MaterialTheme.colorScheme.onSurface)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                ) {
                    actual.set(LocalContentColor.current)
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(expected.get(), actual.get())
    }

    @Test
    fun darkUsesBlackRootSurfacesInEveryPalette() {
        val surfaces = BrowserColorPalette.entries.associateWith { AtomicReference<Color>() }
        val backgrounds = BrowserColorPalette.entries.associateWith { AtomicReference<Color>() }

        composeRule.setContent {
            BrowserColorPalette.entries.forEach { palette ->
                MaterialBrowserTheme(
                    settings = AppearanceSettings(
                        appearanceMode = BrowserAppearanceMode.Dark,
                        colorPalette = palette,
                        surfaceStyle = BrowserSurfaceStyle.Frosted,
                    ),
                ) {
                    surfaces.getValue(palette).set(MaterialTheme.colorScheme.surface)
                    backgrounds.getValue(palette).set(MaterialTheme.colorScheme.background)
                }
            }
        }
        composeRule.waitForIdle()

        BrowserColorPalette.entries.forEach { palette ->
            assertEquals(Color.Black, surfaces.getValue(palette).get())
            assertEquals(Color.Black, backgrounds.getValue(palette).get())
        }
    }

    @Test
    fun workspaceAccentDrivesVolaColors() {
        val tealPrimary = AtomicReference<Color>()
        val coralPrimary = AtomicReference<Color>()
        val coralAura = AtomicReference<Color>()

        composeRule.setContent {
            MaterialBrowserTheme(
                settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.Light),
                workspaceAccent = WorkspaceAccent.Teal,
            ) {
                tealPrimary.set(MaterialTheme.colorScheme.primary)
            }
            MaterialBrowserTheme(
                settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.Light),
                workspaceAccent = WorkspaceAccent.Coral,
            ) {
                coralPrimary.set(MaterialTheme.colorScheme.primary)
                coralAura.set(VolaTheme.extendedColors.aura1)
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color(VolaSchemes.Teal.light.primary), tealPrimary.get())
        assertEquals(Color(VolaSchemes.Coral.light.primary), coralPrimary.get())
        assertEquals(Color(VolaSchemes.Coral.light.aura1), coralAura.get())
    }

    @Test
    fun palettesAndShapesProduceDistinctThemeTokens() {
        val volaPrimary = AtomicReference<Color>()
        val neutralPrimary = AtomicReference<Color>()
        val angularShape = AtomicReference<Any>()
        val extraRoundedShape = AtomicReference<Any>()

        composeRule.setContent {
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    appearanceMode = BrowserAppearanceMode.Light,
                    colorPalette = BrowserColorPalette.Vola,
                    shapeStyle = BrowserShapeStyle.Angular,
                ),
            ) {
                volaPrimary.set(MaterialTheme.colorScheme.primary)
                angularShape.set(MaterialTheme.shapes.large)
            }
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    appearanceMode = BrowserAppearanceMode.Light,
                    colorPalette = BrowserColorPalette.Neutral,
                    shapeStyle = BrowserShapeStyle.ExtraRounded,
                ),
            ) {
                neutralPrimary.set(MaterialTheme.colorScheme.primary)
                extraRoundedShape.set(MaterialTheme.shapes.large)
            }
        }
        composeRule.waitForIdle()

        assertNotEquals(volaPrimary.get(), neutralPrimary.get())
        assertNotEquals(angularShape.get(), extraRoundedShape.get())
    }

    @Test
    fun frostedChromeIsTranslucentInLightAndDark() {
        val light = AtomicReference<Color>()
        val dark = AtomicReference<Color>()

        composeRule.setContent {
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    appearanceMode = BrowserAppearanceMode.Light,
                    surfaceStyle = BrowserSurfaceStyle.Frosted,
                ),
            ) {
                light.set(browserChromeColor(Color.Red))
            }
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    appearanceMode = BrowserAppearanceMode.Dark,
                    surfaceStyle = BrowserSurfaceStyle.Frosted,
                ),
            ) {
                dark.set(browserChromeColor(Color.Red))
            }
        }
        composeRule.waitForIdle()

        assertEquals(0.82f, light.get().alpha, 0.001f)
        assertEquals(0.82f, dark.get().alpha, 0.001f)
    }

    @Test
    fun frostedGeneralAndAddressBarTransparencyAreIndependent() {
        val general = AtomicReference<Color>()
        val addressBar = AtomicReference<Color>()

        composeRule.setContent {
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    surfaceStyle = BrowserSurfaceStyle.Frosted,
                    frostedTransparencyPercent = 70,
                    frostedAddressBarTransparencyPercent = 50,
                ),
            ) {
                general.set(browserChromeColor(Color.Red))
                addressBar.set(
                    browserChromeColor(
                        color = Color.Red,
                        role = BrowserChromeSurfaceRole.AddressBar,
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(0.41f, general.get().alpha, 0.005f)
        assertEquals(0.683f, addressBar.get().alpha, 0.005f)
    }

    @Test
    fun customAddressBarColorPreservesClearAndFrostedTreatments() {
        val clear = AtomicReference<BrowserChromeSurfaceTokens>()
        val frosted = AtomicReference<BrowserChromeSurfaceTokens>()
        val colorSettings = AppearanceSettings(
            addressBarColorPreset = BrowserAddressBarColorPreset.Custom,
            addressBarCustomColorHex = "#123456",
        )

        composeRule.setContent {
            MaterialBrowserTheme(settings = colorSettings) {
                clear.set(browserChromeSurfaceTokens(BrowserChromeSurfaceRole.AddressBar))
            }
            MaterialBrowserTheme(
                settings = colorSettings.copy(
                    surfaceStyle = BrowserSurfaceStyle.Frosted,
                    frostedAddressBarTransparencyPercent = 50,
                ),
            ) {
                frosted.set(browserChromeSurfaceTokens(BrowserChromeSurfaceRole.AddressBar))
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color(0xFF123456), clear.get().containerColor)
        assertEquals(1f, clear.get().containerColor.alpha, 0f)
        assertTrue(frosted.get().containerColor.alpha >= 0.5f)
        assertTrue(frosted.get().fieldContainerColor.alpha >= 0.183f)
        assertNotEquals(0f, frosted.get().blurRadiusPx)
    }

    @Test
    fun resetThemePresetUsesExistingAddressBarTokens() {
        val defaultTokens = AtomicReference<BrowserChromeSurfaceTokens>()
        val resetTokens = AtomicReference<BrowserChromeSurfaceTokens>()

        composeRule.setContent {
            MaterialBrowserTheme(settings = AppearanceSettings()) {
                defaultTokens.set(browserChromeSurfaceTokens(BrowserChromeSurfaceRole.AddressBar))
            }
            MaterialBrowserTheme(
                settings = AppearanceSettings(
                    addressBarColorPreset = BrowserAddressBarColorPreset.Theme,
                    addressBarCustomColorHex = "",
                ),
            ) {
                resetTokens.set(browserChromeSurfaceTokens(BrowserChromeSurfaceRole.AddressBar))
            }
        }
        composeRule.waitForIdle()

        assertEquals(defaultTokens.get(), resetTokens.get())
    }

    @Test
    fun candyThemeProvidesLiquidGlassTokensWithoutChangingComponentApi() {
        val designLanguage = AtomicReference<CandyDesignLanguage>()
        val treatment = AtomicReference<CandyChromeTreatment>()
        val renderedShape = AtomicReference<Shape>()
        val addressFieldColor = AtomicReference<Color>()

        composeRule.setContent {
            CandyTheme(
                settings = AppearanceSettings(
                    appearanceMode = BrowserAppearanceMode.Light,
                    surfaceStyle = BrowserSurfaceStyle.Clear,
                ),
                designLanguage = CandyDesignLanguage.LiquidGlass,
                chromeSurfaceRenderer = CandyChromeSurfaceRenderer {
                        _,
                        _,
                        modifier,
                        shape,
                        _,
                        _,
                        _,
                        content,
                    ->
                    renderedShape.set(shape)
                    Box(modifier = modifier) {
                        content()
                    }
                },
            ) {
                designLanguage.set(LocalCandyDesignLanguage.current)
                val tokens = browserChromeSurfaceTokens(BrowserChromeSurfaceRole.AddressBar)
                treatment.set(tokens.treatment)
                addressFieldColor.set(tokens.fieldContainerColor)
                CandyChromeSurface(
                    backdropSource = null,
                    tokens = tokens,
                    modifier = Modifier.size(120.dp),
                ) { }
            }
        }
        composeRule.waitForIdle()

        assertEquals(CandyDesignLanguage.LiquidGlass, designLanguage.get())
        assertEquals(CandyChromeTreatment.PlatformNative, treatment.get())
        assertEquals(RoundedCornerShape(28.dp), renderedShape.get())
        assertEquals(Color.Transparent, addressFieldColor.get())
    }
}
