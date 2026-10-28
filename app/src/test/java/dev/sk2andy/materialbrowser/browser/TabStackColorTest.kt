package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabStackColorTest {
    @Test
    fun `every color reads back from what is stored, an unknown one as grape`() {
        TabStackColor.entries.forEach { color ->
            assertEquals(color, TabStackColor.fromWireValue(color.wireValue))
        }
        assertEquals(TabStackColor.Grape, TabStackColor.fromWireValue("violet"))
        assertEquals(TabStackColor.Grape, TabStackColor.fromWireValue(null))
    }

    @Test
    fun `stored values of v4's colors stay as they were`() {
        assertEquals(
            listOf("grape", "cherry", "lime", "blueberry"),
            TabStackColor.classic.map(TabStackColor::wireValue),
        )
    }

    @Test
    fun `the v5 palette has the seven colors of the canvas once each, v4 keeps its four`() {
        assertEquals(
            listOf(
                TabStackColor.Orange,
                TabStackColor.Teal,
                TabStackColor.Grape,
                TabStackColor.Blueberry,
                TabStackColor.Cherry,
                TabStackColor.Lime,
                TabStackColor.Slate,
            ),
            TabStackColor.palette,
        )
        assertEquals(TabStackColor.entries.toSet(), TabStackColor.palette.toSet())
        assertTrue(TabStackColor.palette.containsAll(TabStackColor.classic))
    }
}
