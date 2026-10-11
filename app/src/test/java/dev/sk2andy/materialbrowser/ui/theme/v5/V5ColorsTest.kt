package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Colors.highContrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The contrast promises of system-v5.md, sections 1 and 8, checked on the shipped values. */
class V5ColorsTest {
    private val light = V5Colors.light()
    private val dark = V5Colors.dark()
    private val privateScheme = V5Colors.privateBrowsing()

    @Test
    fun `primary and secondary text are readable on every surface`() {
        for (scheme in listOf(light, dark, privateScheme)) {
            for (surface in scheme.textSurfaces()) {
                assertAtLeast(TEXT, scheme.label, surface, "label")
                assertAtLeast(TEXT, scheme.labelSecondary, surface, "labelSecondary")
            }
        }
    }

    @Test
    fun `tertiary text reaches the non-text minimum on background and surface`() {
        for (scheme in listOf(light, dark, privateScheme)) {
            assertAtLeast(NON_TEXT, scheme.labelTertiary, scheme.background, "labelTertiary")
            assertAtLeast(NON_TEXT, scheme.labelTertiary, scheme.surface, "labelTertiary")
        }
    }

    @Test
    fun `status colors are readable as text`() {
        for (scheme in listOf(light, dark)) {
            for (status in listOf(scheme.success, scheme.warning, scheme.danger)) {
                assertAtLeast(TEXT, status, scheme.background, "status")
                assertAtLeast(TEXT, status, scheme.surface, "status")
            }
        }
    }

    @Test
    fun `every accent keeps white on its fill and reads as text in both themes`() {
        for (accent in V5Accent.entries) {
            for (scheme in listOf(V5Colors.light(accent), V5Colors.dark(accent))) {
                assertAtLeast(TEXT, scheme.onAccentFill, scheme.accentFill, "$accent fill")
                assertAtLeast(TEXT, scheme.accent, scheme.background, "$accent text")
                assertAtLeast(TEXT, scheme.accent, scheme.surface, "$accent text")
            }
            assertEquals("light accent doubles as its fill", V5Colors.light(accent).accent,
                V5Colors.light(accent).accentFill)
        }
    }

    @Test
    fun `private palette has its own accent and readable fill`() {
        assertTrue(privateScheme.isDark && privateScheme.isPrivate)
        assertAtLeast(TEXT, privateScheme.accent, privateScheme.background, "private accent")
        assertAtLeast(TEXT, privateScheme.onAccentFill, privateScheme.accentFill, "private fill")
        assertFalse(privateScheme.background == dark.background)
    }

    @Test
    fun `dark theme is dark gray, never pure black`() {
        assertEquals(Color(0xFF1C1C1E), dark.background)
        for (scheme in listOf(dark, privateScheme)) {
            assertTrue(scheme.background != Color.Black)
            // Each level above the background is lighter.
            val steps = listOf(scheme.background, scheme.surface, scheme.surfaceElevated, scheme.surfaceHighest)
            assertEquals(steps, steps.sortedBy { it.luminance() })
        }
    }

    @Test
    fun `white glyphs stand out on every settings tile`() {
        for (tile in V5Tile.entries) {
            assertAtLeast(NON_TEXT, tile.glyph, tile.color, "tile $tile")
        }
    }

    @Test
    fun `increased contrast promotes secondary text and darkens separators`() {
        for (scheme in listOf(light, dark)) {
            val strong = scheme.highContrast()
            assertEquals(scheme.label, strong.labelSecondary)
            assertTrue(contrast(strong.separator, scheme.background) > contrast(scheme.separator, scheme.background))
        }
    }

    @Test
    fun `selected rows stay readable`() {
        for (accent in V5Accent.entries) {
            for (scheme in listOf(V5Colors.light(accent), V5Colors.dark(accent))) {
                assertAtLeast(TEXT, scheme.label, scheme.accentSoft, "$accent accentSoft")
            }
        }
    }

    @Test
    fun `material mapping puts the accent fill on primary`() {
        val material = dark.toMaterial()
        assertEquals(dark.accentFill, material.primary)
        assertEquals(dark.onAccentFill, material.onPrimary)
        assertEquals(dark.background, material.background)
        assertEquals(dark.label, material.onSurface)
    }

    private fun V5ColorScheme.textSurfaces() = listOf(background, surface, surfaceElevated, chrome)

    private fun assertAtLeast(minimum: Float, foreground: Color, background: Color, what: String) {
        val ratio = contrast(foreground, background)
        assertTrue("$what: ${"%.2f".format(ratio)} : 1 < $minimum : 1", ratio >= minimum)
    }

    private fun contrast(a: Color, b: Color): Float {
        val (high, low) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (high + 0.05f) / (low + 0.05f)
    }

    private companion object {
        const val TEXT = 4.5f
        const val NON_TEXT = 3f
    }
}
