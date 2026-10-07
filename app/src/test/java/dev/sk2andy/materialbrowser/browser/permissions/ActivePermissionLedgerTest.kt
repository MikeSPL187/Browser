package dev.sk2andy.materialbrowser.browser.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivePermissionLedgerTest {
    private val site = PermissionSiteKey("profile-a", "https://example.com")

    @Test
    fun parallelGrantsAreUnionedAndCanceledByExactToken() {
        val firstRequest = Any()
        val secondRequest = Any()
        val ledger = ActivePermissionLedger()
        ledger.record(
            firstRequest,
            ActivePermissionGrant("tab-a", site, setOf(SitePermission.Camera)),
        )
        ledger.record(
            secondRequest,
            ActivePermissionGrant("tab-a", site, setOf(SitePermission.Microphone)),
        )

        assertEquals(
            setOf(SitePermission.Camera, SitePermission.Microphone),
            ledger.permissions("tab-a", site),
        )
        assertTrue(ledger.drop(secondRequest))
        assertEquals(setOf(SitePermission.Camera), ledger.permissions("tab-a", site))
        assertFalse(ledger.drop(secondRequest))
    }

    @Test
    fun unrelatedDeniedOrOldRequestCannotEraseActiveGrant() {
        val activeRequest = Any()
        val unrelatedRequest = Any()
        val ledger = ActivePermissionLedger()
        ledger.record(
            activeRequest,
            ActivePermissionGrant("tab-a", site, setOf(SitePermission.Camera)),
        )

        assertFalse(ledger.drop(unrelatedRequest))
        assertTrue(ledger.has("tab-a", site, SitePermission.Camera))
        assertFalse(ledger.has("tab-b", site, SitePermission.Camera))
    }

    @Test
    fun tabCloseRemovesOnlyThatTabsGrants() {
        val firstRequest = Any()
        val secondRequest = Any()
        val ledger = ActivePermissionLedger()
        ledger.record(
            firstRequest,
            ActivePermissionGrant("tab-a", site, setOf(SitePermission.Camera)),
        )
        ledger.record(
            secondRequest,
            ActivePermissionGrant("tab-b", site, setOf(SitePermission.Microphone)),
        )

        assertTrue(ledger.dropTab("tab-a"))
        assertTrue(ledger.has("tab-b", site, SitePermission.Microphone))
        assertFalse(ledger.has("tab-a", site, SitePermission.Camera))
    }

    @Test
    fun tabsForFindsEverySiteTabHoldingTheAccess() {
        val ledger = ActivePermissionLedger()
        val other = PermissionSiteKey("profile-a", "https://other.example")
        ledger.record(Any(), ActivePermissionGrant("tab-a", site, setOf(SitePermission.Camera)))
        ledger.record(Any(), ActivePermissionGrant("tab-b", site, setOf(SitePermission.Camera)))
        ledger.record(Any(), ActivePermissionGrant("tab-c", site, setOf(SitePermission.Location)))
        ledger.record(Any(), ActivePermissionGrant("tab-d", other, setOf(SitePermission.Camera)))

        assertEquals(setOf("tab-a", "tab-b"), ledger.tabsFor(site, SitePermission.Camera))
        assertEquals(setOf("tab-a", "tab-b", "tab-c"), ledger.tabsFor(site))
        assertEquals(emptySet<String>(), ledger.tabsFor(site, SitePermission.Microphone))
    }

    @Test
    fun blockingRevokesTheAccessInEveryIncludedTab() {
        val ledger = ActivePermissionLedger()
        ledger.record(Any(), ActivePermissionGrant("tab-a", site, setOf(SitePermission.Camera)))
        ledger.record(Any(), ActivePermissionGrant("tab-b", site, setOf(SitePermission.Camera)))
        ledger.record(Any(), ActivePermissionGrant("private", site, setOf(SitePermission.Camera)))

        val plan = ledger.revocationPlan(site, SitePermission.Camera) { tabId -> tabId != "private" }

        assertEquals(setOf("tab-a", "tab-b"), plan.reloadNow)
        assertTrue(plan.reloadAfterNotificationSync.isEmpty())
    }

    @Test
    fun resetReloadsLiveAccessAtOnceAndNotificationOnlyTabsAfterSync() {
        val ledger = ActivePermissionLedger()
        ledger.record(
            Any(),
            ActivePermissionGrant(
                "tab-a",
                site,
                setOf(SitePermission.Microphone, SitePermission.Notifications),
            ),
        )
        ledger.record(
            Any(),
            ActivePermissionGrant("tab-b", site, setOf(SitePermission.Notifications)),
        )

        val reset = ledger.revocationPlan(site, permission = null) { true }
        val blockNotifications = ledger.revocationPlan(site, SitePermission.Notifications) { true }

        assertEquals(setOf("tab-a"), reset.reloadNow)
        assertEquals(setOf("tab-b"), reset.reloadAfterNotificationSync)
        assertEquals(setOf("tab-a", "tab-b"), reset.tabs)
        assertTrue(blockNotifications.reloadNow.isEmpty())
        assertEquals(setOf("tab-a", "tab-b"), blockNotifications.reloadAfterNotificationSync)
    }
}
