package dev.sk2andy.materialbrowser.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSwipeRulesTest {
    private val ids = listOf("work", "anime", "home")

    @Test
    fun `a drag to the left brings the next workspace, to the right the previous`() {
        assertEquals("home", WorkspaceSwipeRules.neighbor(ids, "anime", direction = -1))
        assertEquals("work", WorkspaceSwipeRules.neighbor(ids, "anime", direction = 1))
        assertEquals(-1, WorkspaceSwipeRules.direction(-40f))
        assertEquals(1, WorkspaceSwipeRules.direction(40f))
    }

    @Test
    fun `nothing waits past either end of the dock`() {
        assertNull(WorkspaceSwipeRules.neighbor(ids, "work", direction = 1))
        assertNull(WorkspaceSwipeRules.neighbor(ids, "home", direction = -1))
        assertNull(WorkspaceSwipeRules.neighbor(ids, "gone", direction = -1))
        assertNull(WorkspaceSwipeRules.neighbor(ids, "work", direction = 0))
    }

    @Test
    fun `at an end the content follows the finger four times slower`() {
        assertEquals(100f, WorkspaceSwipeRules.visibleOffset(100f, hasNeighbor = true))
        assertEquals(25f, WorkspaceSwipeRules.visibleOffset(100f, hasNeighbor = false))
    }

    @Test
    fun `a release switches when the drag went far enough or was flung that way`() {
        fun commit(offset: Float, velocity: Float = 0f, neighbor: Boolean = true) =
            WorkspaceSwipeRules.shouldCommit(offset, 1000f, velocity, 2000f, neighbor)

        assertTrue(commit(-300f))
        assertFalse(commit(-299f))
        assertTrue(commit(-50f, velocity = -2500f))
        assertFalse(commit(-50f, velocity = 2500f))
        assertFalse(commit(-600f, neighbor = false))
        assertFalse(commit(0f))
    }

    @Test
    fun `a tap in the dock slides from the side the workspace sits on`() {
        assertEquals(-1, WorkspaceSwipeRules.directionTo(ids, "work", "home"))
        assertEquals(1, WorkspaceSwipeRules.directionTo(ids, "home", "work"))
    }

    @Test
    fun `progress runs from 0 to 1 across the width`() {
        assertEquals(0.5f, WorkspaceSwipeRules.progress(-500f, 1000f))
        assertEquals(1f, WorkspaceSwipeRules.progress(3000f, 1000f))
        assertEquals(0f, WorkspaceSwipeRules.progress(10f, 0f))
    }
}
