package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSiteDomainsTest {
    private var stored: Map<String, Set<String>> = mapOf("home" to setOf("video.example"), "gone" to setOf("old.example"))
    private var saves = 0
    private val domains = ProfileSiteDomains(
        load = { stored },
        save = {
            stored = it
            saves++
        },
    )

    @Test
    fun storedMarksApplyToTheirSite() {
        assertTrue(domains.contains("home", isPrivate = false, url = "https://video.example/watch"))
        assertFalse(domains.contains("home", isPrivate = true, url = "https://video.example/"))
        assertFalse(domains.contains("work", isPrivate = false, url = "https://video.example/"))
    }

    @Test
    fun aRegularTabsMarkIsSavedAndAPrivateTabsIsNot() {
        assertTrue(domains.set("home", isPrivate = false, domain = "news.example", marked = true))
        assertEquals(setOf("video.example", "news.example"), stored["home"])
        assertEquals(1, saves)

        assertTrue(domains.set("home", isPrivate = true, domain = "secret.example", marked = true))
        assertTrue(domains.contains("home", isPrivate = true, url = "https://secret.example/"))
        assertEquals(1, saves)
    }

    @Test
    fun anUnchangedMarkReportsNoChangeAndSavesNothing() {
        assertFalse(domains.set("home", isPrivate = false, domain = "video.example", marked = true))
        assertEquals(0, saves)
    }

    @Test
    fun unmarkingTheLastSiteDropsTheWorkspace() {
        domains.set("home", isPrivate = false, domain = "video.example", marked = false)

        assertFalse("home" in stored)
    }

    @Test
    fun missingWorkspacesAreDroppedAndAForgottenOneLeavesNothing() {
        domains.keepProfiles(setOf("home"))
        assertEquals(setOf("home"), stored.keys)

        domains.set("home", isPrivate = true, domain = "secret.example", marked = true)
        domains.forgetProfile("home")
        assertTrue(stored.isEmpty())
        assertFalse(domains.contains("home", isPrivate = true, url = "https://secret.example/"))
    }

    @Test
    fun closingPrivateTabsKeepsRegularMarksAndClearingDataDropsAll() {
        domains.set("home", isPrivate = true, domain = "secret.example", marked = true)

        domains.clearPrivate()
        assertFalse(domains.contains("home", isPrivate = true, url = "https://secret.example/"))
        assertTrue(domains.contains("home", isPrivate = false, url = "https://video.example/"))

        domains.clearAll()
        assertFalse(domains.contains("home", isPrivate = false, url = "https://video.example/"))
        assertTrue(stored.isEmpty())
    }
}
