package dev.sk2andy.materialbrowser.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarRepository
import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionRadarStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences by lazy {
        context.getSharedPreferences(PermissionRadarStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun regularDecisionRoundTripsButPrivateDecisionDoesNotPersist() {
        val regularSite = PermissionSiteKey("personal", "https://example.com")
        val privateSite = PermissionSiteKey("personal", "https://private.example")
        val repository = PermissionRadarRepository(PermissionRadarStore(context))
        repository.setDecision(
            regularSite,
            SitePermission.Camera,
            SitePermissionDecision.Allow,
            isPrivate = false,
        )
        repository.setDecision(
            privateSite,
            SitePermission.Microphone,
            SitePermissionDecision.Block,
            isPrivate = true,
        )

        val restored = PermissionRadarRepository(PermissionRadarStore(context))

        assertEquals(
            SitePermissionDecision.Allow,
            restored.decision(regularSite, SitePermission.Camera, isPrivate = false),
        )
        assertEquals(
            SitePermissionDecision.Ask,
            restored.decision(privateSite, SitePermission.Microphone, isPrivate = true),
        )
        assertFalse(preferences.getString(PermissionRadarStore.KEY_DECISIONS, "").orEmpty()
            .contains("private.example"))
    }

    @Test
    fun damagedEntryKeepsValidDecisionsAndUnreadableRootIsBackedUp() {
        val site = PermissionSiteKey("personal", "https://example.com")
        preferences.edit().putString(
            PermissionRadarStore.KEY_DECISIONS,
            """[{"profileId":"personal","origin":"https://example.com",""" +
                """"permissions":{"Camera":"Block"}},7]""",
        ).commit()

        val repository = PermissionRadarRepository(PermissionRadarStore(context))
        repository.setDecision(
            site.copy(origin = "https://other.example"),
            SitePermission.Location,
            SitePermissionDecision.Allow,
            isPrivate = false,
        )
        val restored = PermissionRadarRepository(PermissionRadarStore(context))

        assertEquals(
            SitePermissionDecision.Block,
            restored.decision(site, SitePermission.Camera, isPrivate = false),
        )

        val unreadable = "[{\"profileId\":"
        preferences.edit().putString(PermissionRadarStore.KEY_DECISIONS, unreadable).commit()
        PermissionRadarRepository(PermissionRadarStore(context)).setDecision(
            site,
            SitePermission.Microphone,
            SitePermissionDecision.Block,
            isPrivate = false,
        )

        assertEquals(
            unreadable,
            preferences.getString(PermissionRadarStore.KEY_UNREADABLE_BACKUP, null),
        )
    }
}
