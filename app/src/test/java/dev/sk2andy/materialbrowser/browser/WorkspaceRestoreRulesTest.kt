package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceRestoreRulesTest {
    private val homeTab = BrowserTab(id = "a", lastAccessedAt = 1, profileId = "candy")
    private val workTab = BrowserTab(
        id = "b",
        lastAccessedAt = 2,
        profileId = "work",
        title = "Payroll",
        url = "https://payroll.example.com/",
    )

    @Test
    fun `orphan tabs move to the fallback when every workspace was read`() {
        val restored = WorkspaceRestoreRules.assignOwners(
            tabs = listOf(homeTab, workTab),
            profileIds = setOf("candy"),
            fallbackProfileId = "candy",
            storedProfilesUnreadable = false,
        )

        assertEquals(listOf("candy", "candy"), restored.live.map(BrowserTab::profileId))
        assertTrue(restored.held.isEmpty())
    }

    @Test
    fun `orphan tabs are held back when a stored workspace was unreadable`() {
        val restored = WorkspaceRestoreRules.assignOwners(
            tabs = listOf(homeTab, workTab),
            profileIds = setOf("candy"),
            fallbackProfileId = "candy",
            storedProfilesUnreadable = true,
        )

        assertEquals(listOf(homeTab), restored.live)
        assertEquals(listOf(workTab), restored.held)
    }
}
