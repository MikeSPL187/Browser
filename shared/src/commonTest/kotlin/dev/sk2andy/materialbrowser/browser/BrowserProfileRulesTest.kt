package dev.sk2andy.materialbrowser.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowserProfileRulesTest {
    @Test
    fun `create normalizes icon and uses supported isolation`() {
        val profile = BrowserProfileRules.create(
            draft = BrowserProfileDraft(emoji = "  💼  ", isolationRequested = true),
            profileId = " profile-id ",
            isolationSupported = true,
        )

        assertEquals("profile-id", profile?.id)
        assertEquals("💼", profile?.emoji)
        assertTrue(profile?.isolationEnabled == true)
    }

    @Test
    fun `create disables unsupported isolation and rejects empty values`() {
        val profile = BrowserProfileRules.create(
            draft = BrowserProfileDraft(emoji = "🏠", isolationRequested = true),
            profileId = "home",
            isolationSupported = false,
        )

        assertFalse(requireNotNull(profile).isolationEnabled)
        assertNull(
            BrowserProfileRules.create(
                draft = BrowserProfileDraft(emoji = " ", isolationRequested = false),
                profileId = "home",
                isolationSupported = true,
            ),
        )
    }

    @Test
    fun `local profile can change icon and isolation`() {
        val profile = BrowserProfile(id = "work", emoji = "💼")

        assertEquals("⭐", BrowserProfileRules.updateEmoji(profile, " ⭐ ")?.emoji)
        assertTrue(
            BrowserProfileRules.updateIsolation(
                profile = profile,
                enabled = true,
                isolationSupported = true,
            )?.isolationEnabled == true,
        )
    }

    @Test
    fun `synced profile cannot mutate local icon or isolation`() {
        val profile = BrowserProfile(
            id = "synced:phone",
            emoji = "📱",
            syncedDeviceId = "phone",
        )

        assertNull(BrowserProfileRules.updateEmoji(profile, "⭐"))
        assertNull(
            BrowserProfileRules.updateIsolation(
                profile = profile,
                enabled = true,
                isolationSupported = true,
            ),
        )
    }

    @Test
    fun `profile protection is local supported and bounded`() {
        val profile = BrowserProfile(id = "work", emoji = "💼")

        val protected = BrowserProfileRules.updateProtection(
            profile = profile,
            protection = ProfileProtection(
                lockTrigger = ProfileLockTrigger.Cooldown,
                cooldownMinutes = 10_000,
            ),
            protectionSupported = true,
        )

        assertEquals(
            ProfileProtectionRules.MAX_COOLDOWN_MINUTES,
            protected?.protection?.cooldownMinutes,
        )
        assertNull(
            BrowserProfileRules.updateProtection(
                profile = profile,
                protection = ProfileProtection(ProfileLockTrigger.AppClosed),
                protectionSupported = false,
            ),
        )
        assertNull(
            BrowserProfileRules.updateProtection(
                profile = profile.copy(syncedDeviceId = "phone"),
                protection = ProfileProtection(ProfileLockTrigger.AppClosed),
                protectionSupported = true,
            ),
        )
    }

    @Test
    fun `lock timing distinguishes background close and cooldown`() {
        assertTrue(
            ProfileProtectionRules.shouldLockAfterBackground(
                ProfileProtection(ProfileLockTrigger.AppBackgrounded),
                elapsedBackgroundMillis = 0L,
            ),
        )
        assertFalse(
            ProfileProtectionRules.shouldLockAfterBackground(
                ProfileProtection(ProfileLockTrigger.AppClosed),
                elapsedBackgroundMillis = Long.MAX_VALUE,
            ),
        )
        assertFalse(
            ProfileProtectionRules.shouldLockAfterBackground(
                ProfileProtection(ProfileLockTrigger.Cooldown, cooldownMinutes = 3),
                elapsedBackgroundMillis = 179_999L,
            ),
        )
        assertTrue(
            ProfileProtectionRules.shouldLockAfterBackground(
                ProfileProtection(ProfileLockTrigger.Cooldown, cooldownMinutes = 3),
                elapsedBackgroundMillis = 180_000L,
            ),
        )
    }

    @Test
    fun `create keeps a normalized workspace name and the chosen accent`() {
        val profile = BrowserProfileRules.create(
            draft = BrowserProfileDraft(
                emoji = "💼",
                isolationRequested = false,
                name = "  Work \n  projects  ",
                accent = WorkspaceAccent.Coral,
            ),
            profileId = "work",
            isolationSupported = true,
        )

        assertEquals("Work projects", profile?.name)
        assertEquals(WorkspaceAccent.Coral, profile?.accent)
    }

    @Test
    fun `workspace names are capped and blank means the default name`() {
        assertEquals(
            WorkspaceNameRules.MAX_LENGTH,
            WorkspaceNameRules.normalize("x".repeat(100)).length,
        )
        assertEquals("", WorkspaceNameRules.normalize("   "))
    }

    @Test
    fun `name cap never splits a surrogate pair`() {
        val rocket = "\uD83D\uDE80"
        val capped = WorkspaceNameRules.cap("x".repeat(WorkspaceNameRules.MAX_LENGTH - 1) + rocket)

        assertEquals("x".repeat(WorkspaceNameRules.MAX_LENGTH - 1), capped)
        assertFalse(capped.last().isHighSurrogate())
        assertEquals(
            "x".repeat(WorkspaceNameRules.MAX_LENGTH - 2) + rocket,
            WorkspaceNameRules.cap("x".repeat(WorkspaceNameRules.MAX_LENGTH - 2) + rocket + "y"),
        )
        assertEquals(
            "x".repeat(WorkspaceNameRules.MAX_LENGTH - 1),
            WorkspaceNameRules.normalize("x".repeat(WorkspaceNameRules.MAX_LENGTH - 1) + rocket),
        )
    }

    @Test
    fun `rename and recolor change only local workspaces`() {
        val local = BrowserProfile(id = "home", emoji = "🏠")
        val synced = local.copy(syncedDeviceId = "device")

        assertEquals("Home", BrowserProfileRules.updateName(local, " Home ")?.name)
        assertNull(BrowserProfileRules.updateName(local, ""))
        assertEquals(
            WorkspaceAccent.Green,
            BrowserProfileRules.updateAccent(local, WorkspaceAccent.Green)?.accent,
        )
        assertNull(BrowserProfileRules.updateAccent(local, WorkspaceAccent.Default))
        assertNull(BrowserProfileRules.updateName(synced, "Home"))
        assertNull(BrowserProfileRules.updateAccent(synced, WorkspaceAccent.Green))
    }

    @Test
    fun `accent wire values round trip and unknown values use the default`() {
        WorkspaceAccent.entries.forEach { accent ->
            assertEquals(accent, WorkspaceAccent.fromWireValue(accent.wireValue))
        }
        assertEquals(WorkspaceAccent.Default, WorkspaceAccent.fromWireValue("neon"))
        assertEquals(WorkspaceAccent.Default, WorkspaceAccent.fromWireValue(null))
    }
}
