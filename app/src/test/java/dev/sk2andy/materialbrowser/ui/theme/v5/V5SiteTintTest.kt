package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The examples table of system-v5.md, section 1.3, computed by the same rule as the canvas. */
class V5SiteTintTest {
    private val chrome = V5Colors.light().chrome
    private val label = V5Colors.light().label

    private fun tint(rgb: Int) = V5SiteTints.of(rgb, chrome, label)

    private fun Color.rgb(): Int = toArgb() and 0xFFFFFF

    @Test
    fun `the spec examples come out as in the table`() {
        val white = 0xFFFFFF
        val dark = 0x111114
        val expected = listOf(
            // site color, bar color, text color
            Triple(0x1F4E5F, 0x1F4E5F, white),
            Triple(0x1DA1F2, 0x007BC6, white),
            Triple(0xFF6600, 0xC65111, white),
            Triple(0xFF4500, 0xCC4C2A, white),
            Triple(0xE50914, 0xCC473C, white),
            Triple(0x34C759, 0x56C269, dark),
            Triple(0xFFCC00, 0xFACD42, dark),
            Triple(0xF3E4CC, 0xF3E4CC, dark),
        )
        for ((site, bar, text) in expected) {
            val result = tint(site)
            assertFalse(result.neutral)
            assertEquals("bar for %06X".format(site), bar, result.container.rgb())
            assertEquals("text for %06X".format(site), text, result.onTint.rgb())
        }
    }

    @Test
    fun `text on the bar always reaches 4_5 to 1`() {
        val samples = listOf(0x000000, 0xFFFFFF, 0x808080, 0x00FF00, 0xFF00FF, 0x0000FF, 0xFFFF00, 0x00FFFF, 0x7F3F00)
        for (site in samples) {
            val result = tint(site)
            if (result.neutral) continue
            val ratio = V5SiteTints.contrast(result.container.rgb(), result.onTint.rgb())
            assertTrue("%06X: %.2f".format(site, ratio), ratio >= 4.5)
        }
    }

    @Test
    fun `no color or a color close to the bar keeps the neutral bar`() {
        assertTrue(V5SiteTints.of(null, chrome, label).neutral)
        assertTrue(tint(0xF8F8FA).neutral)
        assertEquals(chrome, V5SiteTints.of(null, chrome, label).container)
    }

    @Test
    fun `field and pressed are onTint over the color`() {
        val result = tint(0x1F4E5F)
        assertTrue(result.field != result.container)
        assertTrue(result.pressed != result.field)
        assertEquals(0.72f, result.onTintSecondary.alpha, 0.01f)
    }
}
