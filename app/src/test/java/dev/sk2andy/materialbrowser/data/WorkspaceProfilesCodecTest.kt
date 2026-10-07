package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ProfileLockTrigger
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceProfilesCodecTest {
    private val home = BrowserProfile(id = "candy", emoji = "🏠")
    private val work = BrowserProfile(
        id = "work",
        emoji = "💼",
        name = "Work",
        accent = WorkspaceAccent.Teal,
        protection = ProfileProtection(lockTrigger = ProfileLockTrigger.AppClosed),
    )

    @Test
    fun `round trip keeps every workspace`() {
        val decoded = WorkspaceProfilesCodec.decode(WorkspaceProfilesCodec.encode(listOf(home, work)))

        assertEquals(listOf(home, work), decoded.profiles)
        assertFalse(decoded.hasUnreadableEntries)
    }

    @Test
    fun `one damaged entry keeps the protected workspaces next to it`() {
        val raw = JSONArray(WorkspaceProfilesCodec.encode(listOf(home, work))).put(7).toString()

        val decoded = WorkspaceProfilesCodec.decode(raw)

        assertEquals(listOf(home, work), decoded.profiles)
        assertEquals(ProfileLockTrigger.AppClosed, decoded.profiles[1].protection?.lockTrigger)
        assertTrue(decoded.hasUnreadableEntries)
        assertEquals(listOf<Any>(7), decoded.unreadableEntries)
    }

    @Test
    fun `entries without an id or icon count as unreadable`() {
        val raw = """[{"id":"candy","emoji":"🏠"},{"id":"","emoji":"💼"},{"id":"x"},null,"text"]"""

        val decoded = WorkspaceProfilesCodec.decode(raw)

        assertEquals(listOf("candy"), decoded.profiles.map(BrowserProfile::id))
        assertEquals(4, decoded.unreadableEntries.size)
    }

    @Test
    fun `unreadable entries are written back unchanged`() {
        val raw = JSONArray(WorkspaceProfilesCodec.encode(listOf(home, work))).put(7).toString()
        val decoded = WorkspaceProfilesCodec.decode(raw)

        val saved = WorkspaceProfilesCodec.encode(listOf(home), decoded.unreadableEntries)

        val reread = WorkspaceProfilesCodec.decode(saved)
        assertEquals(listOf(home), reread.profiles)
        assertEquals(listOf<Any>(7), reread.unreadableEntries)
    }

    @Test
    fun `a value that is not an array is kept whole`() {
        val decoded = WorkspaceProfilesCodec.decode("{broken")

        assertTrue(decoded.profiles.isEmpty())
        assertEquals(listOf<Any>("{broken"), decoded.unreadableEntries)
        assertEquals(
            listOf<Any>("{broken"),
            WorkspaceProfilesCodec.decode(
                WorkspaceProfilesCodec.encode(listOf(home), decoded.unreadableEntries),
            ).unreadableEntries,
        )
    }

    @Test
    fun `nothing stored is not damage`() {
        val decoded = WorkspaceProfilesCodec.decode(null)

        assertTrue(decoded.profiles.isEmpty())
        assertFalse(decoded.hasUnreadableEntries)
    }

    @Test
    fun `unknown lock trigger fails closed`() {
        val raw = """[{"id":"work","emoji":"💼","protection":{"lockTrigger":"on_full_moon"}}]"""

        assertEquals(
            WorkspaceProfilesCodec.FAIL_CLOSED_PROTECTION,
            WorkspaceProfilesCodec.decode(raw).profiles.single().protection,
        )
        assertEquals(ProfileLockTrigger.AppBackgrounded, WorkspaceProfilesCodec.FAIL_CLOSED_PROTECTION.lockTrigger)
    }

    @Test
    fun `protection of the wrong type fails closed and null means none`() {
        val wrongType = """[{"id":"work","emoji":"💼","protection":"app_closed"}]"""
        val none = """[{"id":"work","emoji":"💼","protection":null},{"id":"home","emoji":"🏠"}]"""

        assertEquals(
            WorkspaceProfilesCodec.FAIL_CLOSED_PROTECTION,
            WorkspaceProfilesCodec.decode(wrongType).profiles.single().protection,
        )
        WorkspaceProfilesCodec.decode(none).profiles.forEach { profile -> assertNull(profile.protection) }
    }

    @Test
    fun `duplicate ids keep the first entry`() {
        val raw = """[{"id":"work","emoji":"💼"},{"id":"work","emoji":"⭐"}]"""

        val decoded = WorkspaceProfilesCodec.decode(raw)

        assertEquals("💼", decoded.profiles.single().emoji)
        assertFalse(decoded.hasUnreadableEntries)
    }
}
