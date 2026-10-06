package dev.sk2andy.materialbrowser.browser.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionRadarRepositoryTest {
    private val site = PermissionSiteKey("personal", "https://example.com")

    @Test
    fun decisionsStayIsolatedByProfileAndOrigin() {
        val persistence = RecordingPersistence()
        val repository = PermissionRadarRepository(persistence)

        repository.setDecision(site, SitePermission.Camera, SitePermissionDecision.Allow, false)

        assertEquals(
            SitePermissionDecision.Allow,
            repository.decision(site, SitePermission.Camera, false),
        )
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(site.copy(profileId = "work"), SitePermission.Camera, false),
        )
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(site.copy(origin = "https://other.example"), SitePermission.Camera, false),
        )
    }

    @Test
    fun privateChoicesAndSessionAllowsNeverReachPersistence() {
        val persistence = RecordingPersistence()
        val repository = PermissionRadarRepository(persistence)

        repository.setDecision(site, SitePermission.Microphone, SitePermissionDecision.Block, true)
        repository.allowOnce(site, setOf(SitePermission.Location), true)

        assertTrue(persistence.saved.isEmpty())
        assertEquals(
            SitePermissionDecision.Block,
            repository.decision(site, SitePermission.Microphone, true),
        )
        assertTrue(repository.isAllowedForSession(site, SitePermission.Location, true))

        repository.clearPrivateSession()

        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(site, SitePermission.Microphone, true),
        )
        assertFalse(repository.isAllowedForSession(site, SitePermission.Location, true))
    }

    @Test
    fun resettingSinglePermissionAndSiteReturnToAsk() {
        val repository = PermissionRadarRepository(RecordingPersistence())
        repository.setDecision(site, SitePermission.Camera, SitePermissionDecision.Allow, false)
        repository.setDecision(site, SitePermission.Location, SitePermissionDecision.Block, false)

        repository.setDecision(site, SitePermission.Camera, SitePermissionDecision.Ask, false)
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(site, SitePermission.Camera, false),
        )
        assertEquals(
            SitePermissionDecision.Block,
            repository.decision(site, SitePermission.Location, false),
        )

        repository.resetSite(site, false)
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(site, SitePermission.Location, false),
        )
    }

    @Test
    fun newestDecisionSurvivesRestartWhenTheStoreIsFull() {
        val persistence = RecordingPersistence()
        val repository = PermissionRadarRepository(persistence)
        val sites = (0 until PermissionRadarRepository.MAX_SITES).map { index ->
            PermissionSiteKey("personal", "https://s%03d.example".format(index))
        }
        sites.forEach { full ->
            repository.setDecision(full, SitePermission.Camera, SitePermissionDecision.Block, false)
        }
        val newest = PermissionSiteKey("personal", "https://zzz.example")

        repository.setDecision(newest, SitePermission.Camera, SitePermissionDecision.Block, false)
        val restored = PermissionRadarRepository(persistence)

        assertEquals(PermissionRadarRepository.MAX_SITES, persistence.saved.size)
        assertEquals(
            SitePermissionDecision.Block,
            restored.decision(newest, SitePermission.Camera, false),
        )
        assertEquals(
            SitePermissionDecision.Ask,
            restored.decision(sites.first(), SitePermission.Camera, false),
        )
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(sites.first(), SitePermission.Camera, false),
        )
    }

    @Test
    fun changingASiteMovesItAwayFromEviction() {
        val persistence = RecordingPersistence()
        val repository = PermissionRadarRepository(persistence)
        val sites = (0 until PermissionRadarRepository.MAX_SITES).map { index ->
            PermissionSiteKey("personal", "https://s%03d.example".format(index))
        }
        sites.forEach { full ->
            repository.setDecision(full, SitePermission.Camera, SitePermissionDecision.Block, false)
        }

        repository.setDecision(sites.first(), SitePermission.Location, SitePermissionDecision.Allow, false)
        repository.setDecision(site, SitePermission.Camera, SitePermissionDecision.Allow, false)

        assertEquals(
            SitePermissionDecision.Block,
            repository.decision(sites.first(), SitePermission.Camera, false),
        )
        assertEquals(
            SitePermissionDecision.Ask,
            repository.decision(sites[1], SitePermission.Camera, false),
        )
        assertEquals(sites.first(), persistence.saved.keys.elementAt(persistence.saved.size - 2))
        assertEquals(site, persistence.saved.keys.last())
    }
}

private class RecordingPersistence : PermissionDecisionPersistence {
    var saved: Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>> = emptyMap()

    override fun load(): Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>> = saved

    override fun save(
        decisions: Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>>,
    ) {
        saved = decisions
    }
}
