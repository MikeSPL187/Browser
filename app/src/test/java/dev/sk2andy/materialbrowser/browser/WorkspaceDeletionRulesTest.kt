package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceDeletionRulesTest {
    private val local = listOf("home", "work", "anime")

    @Test
    fun `deleting an inactive workspace keeps the active local one as fallback`() {
        assertEquals(
            "home",
            WorkspaceDeletionRules.fallbackProfileId(local, activeProfileId = "home", deletedProfileId = "work"),
        )
    }

    @Test
    fun `deleting the active workspace falls back to the first remaining local one`() {
        assertEquals(
            "home",
            WorkspaceDeletionRules.fallbackProfileId(local, activeProfileId = "work", deletedProfileId = "work"),
        )
        assertEquals(
            "work",
            WorkspaceDeletionRules.fallbackProfileId(local, activeProfileId = "home", deletedProfileId = "home"),
        )
    }

    @Test
    fun `an active remote workspace is never the fallback`() {
        assertEquals(
            "home",
            WorkspaceDeletionRules.fallbackProfileId(
                local,
                activeProfileId = "synced:desktop",
                deletedProfileId = "work",
            ),
        )
        assertEquals(
            "work",
            WorkspaceDeletionRules.fallbackProfileId(
                local,
                activeProfileId = "synced:desktop",
                deletedProfileId = "home",
            ),
        )
    }

    @Test
    fun `the last local workspace has no fallback`() {
        assertNull(
            WorkspaceDeletionRules.fallbackProfileId(
                listOf("home"),
                activeProfileId = "synced:desktop",
                deletedProfileId = "home",
            ),
        )
        assertNull(
            WorkspaceDeletionRules.fallbackProfileId(emptyList(), activeProfileId = "home", deletedProfileId = "home"),
        )
    }
}
