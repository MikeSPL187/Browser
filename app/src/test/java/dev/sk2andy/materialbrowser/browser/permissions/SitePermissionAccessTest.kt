package dev.sk2andy.materialbrowser.browser.permissions

import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SitePermissionAccessTest {
    private val origin = "https://meet.example"
    private val tabs = mutableListOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "personal", url = "$origin/room"),
        BrowserTab(id = "b", lastAccessedAt = 2, profileId = "personal", url = "$origin/other"),
    )
    private var selected = "a"
    private var resumed = true
    private val grantedRuntime = mutableSetOf<String>()
    private val requestedRuntime = mutableListOf<Set<String>>()
    private val reloads = mutableListOf<String>()
    private val notificationDecisions = mutableListOf<Pair<String, SitePermissionDecision>>()
    private val repository = PermissionRadarRepository(InMemoryPersistence())

    private val access = SitePermissionAccess(
        repository = repository,
        host = object : SitePermissionAccess.Host {
            override val tabs get() = this@SitePermissionAccessTest.tabs
            override val selectedTabId get() = selected
            override val isActivityResumed get() = resumed
            override val isActivityStarted = true
            override val isDestroyed = false
            override fun pageUrl(tabId: String): String? = null
            override fun navigationGeneration(tabId: String): Int = 1
            override fun hasEngineSession(tabId: String) = true
            override fun setNotificationPermission(
                tabId: String,
                origin: String,
                decision: SitePermissionDecision,
                onComplete: (() -> Unit)?,
            ): Boolean {
                notificationDecisions += tabId to decision
                onComplete?.invoke()
                return true
            }
            override fun reload(tabId: String) {
                reloads += tabId
            }
            override fun hasRuntimePermission(permission: String) = permission in grantedRuntime
            override fun requestRuntimePermissions(permissions: Set<String>) {
                requestedRuntime += permissions
            }
            override fun clearExternalNavigationAuthorization(tabId: String) = Unit
            override fun scheduleResidentSessionTrim() = Unit
        },
    )

    private val granted = mutableListOf<Set<SitePermission>>()
    private var denied = 0

    private fun request(tabId: String = "a", vararg permissions: SitePermission, token: Any = Any()) {
        val identity = requireNotNull(access.requestIdentity(tabId, origin))
        access.begin(
            identity = identity,
            site = PermissionSiteKey(identity.profileId, identity.origin),
            requested = permissions.toSet(),
            requestToken = token,
            grant = { granted += it },
            deny = { denied++ },
        )
    }

    @Test
    fun anUndecidedPermissionAsksAndAllowAlwaysRemembersAndGrants() {
        grantedRuntime += SitePermission.Camera.runtimePermissions
        request(permissions = arrayOf(SitePermission.Camera))

        val prompt = access.prompt!!
        assertEquals(setOf(SitePermission.Camera), prompt.permissions)
        assertTrue(access.hasActivity("a"))

        access.respondToPrompt(prompt.id, PermissionPromptChoice.AllowAlways)

        assertNull(access.prompt)
        assertEquals(listOf(setOf(SitePermission.Camera)), granted)
        assertEquals(
            SitePermissionDecision.Allow,
            repository.decision(PermissionSiteKey("personal", origin), SitePermission.Camera, false),
        )
    }

    @Test
    fun blockDeniesAndAnAlreadyBlockedPermissionDoesNotAskAgain() {
        request(permissions = arrayOf(SitePermission.Microphone))
        access.respondToPrompt(access.prompt!!.id, PermissionPromptChoice.Block)
        assertEquals(1, denied)

        request(permissions = arrayOf(SitePermission.Microphone))
        assertNull(access.prompt)
        assertEquals(2, denied)
    }

    @Test
    fun anAllowedPermissionWaitsForAndroidAndGrantsWhatAndroidGave() {
        access.setDecision("a", origin, SitePermission.Camera, SitePermissionDecision.Allow)
        request(permissions = arrayOf(SitePermission.Camera))

        assertEquals(listOf(SitePermission.Camera.runtimePermissions), requestedRuntime)
        assertTrue(access.isBusy)

        access.onRuntimePermissionResult(SitePermission.Camera.runtimePermissions.associateWith { true })
        assertEquals(listOf(setOf(SitePermission.Camera)), granted)
        assertFalse(access.isBusy)
    }

    @Test
    fun aRequestFromATabNoLongerShownIsDeniedOnTheAnswer() {
        request(permissions = arrayOf(SitePermission.Location))
        val prompt = access.prompt!!
        selected = "b"

        access.respondToPrompt(prompt.id, PermissionPromptChoice.AllowOnce)

        assertEquals(1, denied)
        assertTrue(granted.isEmpty())
        assertNull(access.prompt)
    }

    @Test
    fun cancellingOnlyTouchesTheGivenTabsRequest() {
        request(permissions = arrayOf(SitePermission.Location))
        access.cancelPending("b")
        assertTrue(access.isBusy)

        access.cancelPending("a")
        assertFalse(access.isBusy)
        assertEquals(1, denied)
    }

    @Test
    fun aWithdrawnRequestIsDroppedWithoutAnAnswer() {
        val token = Any()
        request(permissions = arrayOf(SitePermission.Location), token = token)
        access.dropCanceled(Any())
        assertTrue(access.isBusy)

        access.dropCanceled(token)
        assertFalse(access.isBusy)
        assertEquals(0, denied)
    }

    @Test
    fun blockingAnActiveGrantRevokesItAndReloadsTheTab() {
        grantedRuntime += SitePermission.Camera.runtimePermissions
        access.setDecision("a", origin, SitePermission.Camera, SitePermissionDecision.Allow)
        request(permissions = arrayOf(SitePermission.Camera))
        assertTrue(access.hasActivity("a"))

        access.setDecision("a", origin, SitePermission.Camera, SitePermissionDecision.Block)

        assertFalse(access.hasActivity("a"))
        assertEquals(listOf("a"), reloads)
    }

    @Test
    fun resettingASiteAsksAgainAndTellsTheEngineAboutNotifications() {
        access.setDecision("a", origin, SitePermission.Notifications, SitePermissionDecision.Allow)
        notificationDecisions.clear()

        assertTrue(access.resetSite("a", origin))

        assertEquals(listOf("a" to SitePermissionDecision.Ask), notificationDecisions)
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(PermissionSiteKey("personal", origin), SitePermission.Notifications, false),
        )
    }

    @Test
    fun theRadarShowsWhatIsPendingAndAnUnknownTabNothing() {
        request(permissions = arrayOf(SitePermission.Location))

        val entry = access.radarSnapshot("a", null).entries.first { it.permission == SitePermission.Location }
        assertEquals(SitePermissionActivity.Pending, entry.activity)
        assertEquals(PermissionRadarSnapshot.Empty, access.radarSnapshot("missing", null))
    }

    private class InMemoryPersistence : PermissionDecisionPersistence {
        var saved: Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>> = emptyMap()

        override fun load() = saved

        override fun save(decisions: Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>>) {
            saved = decisions
        }
    }
}
