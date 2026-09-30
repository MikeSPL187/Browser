package dev.sk2andy.materialbrowser.shared.ui.icons

import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolaIconsTest {
    @Test
    fun everyIconIsOneFilledPathOnTheSymbolGrid() {
        VolaIcons.all.forEach { icon ->
            assertEquals(24.dp, icon.defaultWidth, icon.name)
            assertEquals(24.dp, icon.defaultHeight, icon.name)
            assertEquals(960f, icon.viewportWidth, icon.name)
            assertEquals(960f, icon.viewportHeight, icon.name)
            val path = icon.root.single() as VectorPath
            assertTrue(path.pathData.size > 2, icon.name)
            absolutePoints(path.pathData).forEach { (x, y) ->
                assertTrue(x in 0f..960f && y in 0f..960f, "${icon.name}: $x, $y")
            }
        }
    }

    @Test
    fun namesAreUnique() {
        val names = VolaIcons.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun directionalIconsMirrorInRightToLeftLayouts() {
        listOf(
            VolaIcons.ArrowBack,
            VolaIcons.ArrowForward,
            VolaIcons.KeyboardArrowLeft,
            VolaIcons.KeyboardArrowRight,
        ).forEach { assertTrue(it.autoMirror, it.name) }
        listOf(VolaIcons.Close, VolaIcons.Search, VolaIcons.KeyboardArrowDown)
            .forEach { assertFalse(it.autoMirror, it.name) }
    }

    private fun absolutePoints(nodes: List<PathNode>): List<Pair<Float, Float>> =
        nodes.mapNotNull { node ->
            when (node) {
                is PathNode.MoveTo -> node.x to node.y
                is PathNode.LineTo -> node.x to node.y
                is PathNode.QuadTo -> node.x2 to node.y2
                is PathNode.ReflectiveQuadTo -> node.x to node.y
                is PathNode.CurveTo -> node.x3 to node.y3
                is PathNode.ReflectiveCurveTo -> node.x2 to node.y2
                is PathNode.ArcTo -> node.arcStartX to node.arcStartY
                else -> null
            }
        }
}
