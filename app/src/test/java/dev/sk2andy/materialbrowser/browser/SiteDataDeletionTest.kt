package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteDataDeletionRulesTest {
    @Test
    fun `delete is offered for a web site when the engine can, never from a private tab`() {
        assertTrue(SiteDataDeletionRules.offers(supported = true, baseDomain = "example.com", isPrivate = false))
        assertFalse(SiteDataDeletionRules.offers(supported = false, baseDomain = "example.com", isPrivate = false))
        assertFalse(SiteDataDeletionRules.offers(supported = true, baseDomain = null, isPrivate = false))
        assertFalse(SiteDataDeletionRules.offers(supported = true, baseDomain = "example.com", isPrivate = true))
    }

    @Test
    fun `only the page still on that site reloads`() {
        assertTrue(SiteDataDeletionRules.reloadsSelected("example.com", "example.com"))
        assertFalse(SiteDataDeletionRules.reloadsSelected("example.com", "other.org"))
        assertFalse(SiteDataDeletionRules.reloadsSelected("example.com", null))
    }

    @Test
    fun `a new load starts from zero with the last failure cleared`() {
        val failed = BrowserTab(
            id = "tab",
            lastAccessedAt = 1L,
            title = "Example",
            url = "https://example.com/",
            progress = 100,
            error = "offline",
            httpStatusCode = 500,
            failureKind = BrowserEngineFailureKind.entries.first(),
        )
        val reloading = failed.startingLoad()
        assertEquals(failed.copy(isLoading = true, progress = 0, error = null, failureKind = null, httpStatusCode = null), reloading)
        assertEquals("https://example.org/", failed.startingLoad(url = "https://example.org/").url)
        assertEquals("Example", reloading.title)
    }
}

class SiteDataDeletionTest {
    private val cleared = mutableListOf<String>()
    private var clearResult = true
    private var selectedDomain: String? = "example.com"
    private var reloads = 0
    private val scheduled = mutableListOf<Pair<Runnable, Long>>()

    private val deletion = SiteDataDeletion(
        clearSiteData = { domain, done ->
            cleared += domain
            done(clearResult)
        },
        selectedBaseDomain = { selectedDomain },
        reloadSelected = { reloads++ },
        postDelayed = { task, delay -> scheduled += task to delay },
        removeCallbacks = { task -> scheduled.removeAll { it.first === task } },
    )

    @Test
    fun `nothing is deleted before the undo window runs out`() {
        deletion.request("example.com")
        assertEquals("example.com", deletion.pending?.baseDomain)
        assertTrue(cleared.isEmpty())
        assertEquals(SiteDataDeletionRules.UNDO_WINDOW_MILLIS, scheduled.single().second)
        scheduled.single().first.run()
        assertEquals(listOf("example.com"), cleared)
        assertNull(deletion.pending)
        assertEquals(1, reloads)
    }

    @Test
    fun `undo keeps the data`() {
        deletion.request("example.com")
        deletion.undo(deletion.pending!!)
        assertNull(deletion.pending)
        assertTrue(scheduled.isEmpty())
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun `an old snackbar cannot undo a newer deletion`() {
        deletion.request("example.com")
        val first = deletion.pending!!
        deletion.request("other.org")
        assertEquals(listOf("example.com"), cleared)
        deletion.undo(first)
        assertEquals("other.org", deletion.pending?.baseDomain)
    }

    @Test
    fun `leaving the app carries the deletion out at once`() {
        deletion.request("example.com", undoWindowMillis = 20_000L)
        assertEquals(20_000L, scheduled.single().second)
        deletion.commit()
        assertEquals(listOf("example.com"), cleared)
        assertTrue(scheduled.isEmpty())
        deletion.commit()
        assertEquals(1, cleared.size)
    }

    @Test
    fun `a page on another site, or a failed deletion, does not reload`() {
        selectedDomain = "other.org"
        deletion.request("example.com")
        deletion.commit()
        assertEquals(0, reloads)
        selectedDomain = "example.com"
        clearResult = false
        deletion.request("example.com")
        deletion.commit()
        assertEquals(0, reloads)
    }

    @Test
    fun `a failed deletion is kept for retry, and retry asks the engine again`() {
        clearResult = false
        deletion.request("example.com")
        deletion.commit()
        val failure = deletion.failed!!
        assertEquals("example.com", failure.baseDomain)
        assertEquals(0, reloads)
        clearResult = true
        deletion.retry(failure)
        assertEquals(listOf("example.com", "example.com"), cleared)
        assertNull(deletion.failed)
        assertEquals(1, reloads)
        deletion.retry(failure)
        assertEquals(2, cleared.size)
    }

    @Test
    fun `a dismissed or stale failure does nothing`() {
        clearResult = false
        deletion.request("example.com")
        deletion.commit()
        val first = deletion.failed!!
        deletion.retry(first)
        val second = deletion.failed!!
        assertTrue(second != first)
        deletion.dismissFailure(first)
        assertEquals(second, deletion.failed)
        deletion.dismissFailure(second)
        assertNull(deletion.failed)
        deletion.retry(second)
        assertEquals(2, cleared.size)
    }
}
