package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionRadarCodecTest {
    private val site = PermissionSiteKey("p", "https://example.com")

    @Test
    fun damagedEntryIsSkippedWithoutLosingTheValidOnes() {
        val raw = """
            [
              {"profileId":"p","origin":"https://example.com","permissions":{"Camera":"Block"}},
              7,
              {"profileId":"p","origin":"https://other.example","permissions":"broken"},
              null,
              {"profileId":"p","origin":"https://later.example","permissions":{"Location":"Allow"}}
            ]
        """.trimIndent()

        val decoded = PermissionRadarCodec.decode(raw, maxSites = 512)

        assertEquals(
            mapOf(
                site to mapOf(SitePermission.Camera to SitePermissionDecision.Block),
                PermissionSiteKey("p", "https://later.example") to
                    mapOf(SitePermission.Location to SitePermissionDecision.Allow),
            ),
            decoded,
        )
    }

    @Test
    fun unreadableRootIsReportedInsteadOfLookingEmpty() {
        assertNull(PermissionRadarCodec.decode("{not json", maxSites = 512))
        assertNull(PermissionRadarCodec.decode("""{"profileId":"p"}""", maxSites = 512))
        assertEquals(emptyMap<Any, Any>(), PermissionRadarCodec.decode("[]", maxSites = 512))
    }

    @Test
    fun roundTripKeepsOrderAndOnlyTheNewestSites() {
        val decisions = linkedMapOf(
            PermissionSiteKey("p", "https://old.example") to
                mapOf(SitePermission.Camera to SitePermissionDecision.Block),
            site to mapOf(
                SitePermission.Microphone to SitePermissionDecision.Allow,
                SitePermission.Location to SitePermissionDecision.Ask,
            ),
            PermissionSiteKey("p", "https://new.example") to
                mapOf(SitePermission.Location to SitePermissionDecision.Block),
        )

        val decoded = PermissionRadarCodec.decode(
            PermissionRadarCodec.encode(decisions, maxSites = 2),
            maxSites = 2,
        )

        assertEquals(
            listOf(site, PermissionSiteKey("p", "https://new.example")),
            decoded?.keys?.toList(),
        )
        assertEquals(
            mapOf(SitePermission.Microphone to SitePermissionDecision.Allow),
            decoded?.get(site),
        )
    }
}
