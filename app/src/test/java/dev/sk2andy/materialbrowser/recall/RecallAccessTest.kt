package dev.sk2andy.materialbrowser.recall

import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecallAccessTest {
    private val match = RecallMatch(
        profileId = "home",
        url = "https://docs.example/guide",
        title = "Guide",
        excerpt = "Kotlin coroutines guide",
        visitedAt = 1,
        score = 1.0,
    )
    private val tabs = mutableListOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "home"),
        BrowserTab(id = "private", lastAccessedAt = 2, profileId = "home", isIncognito = true),
    )
    private var selected = "a"
    private var historySuggestions = true
    private var clearingData = false
    private val saved = mutableListOf<Boolean>()
    private val searches = mutableListOf<Triple<Set<String>, String, Int>>()
    private val pendingSearches = mutableListOf<(List<RecallMatch>) -> Unit>()
    private val pendingClears = mutableListOf<(Boolean) -> Unit>()

    private val recall = RecallAccess(
        saveEnabled = { saved += it },
        search = { profileIds, query, limit, onComplete ->
            searches += Triple(profileIds, query, limit)
            pendingSearches += onComplete
        },
        clearInBackground = { pendingClears += it },
        host = object : RecallAccess.Host {
            override val isDestroyed = false
            override val isBrowsingDataClearPending get() = clearingData
            override val isHistorySuggestionsEnabled get() = historySuggestions
            override val selectedTab get() = tabs.first { it.id == selected }
            override fun tab(tabId: String) = tabs.firstOrNull { it.id == tabId }
        },
    )

    private fun search(query: String): List<RecallMatch>? {
        var result: List<RecallMatch>? = null
        recall.searchFromAddress(query) { result = it }
        pendingSearches.removeFirstOrNull()?.invoke(listOf(match))
        return result
    }

    @Test
    fun turningOnSavesAtOnce() {
        recall.requestEnabled(true)

        assertTrue(recall.isEnabled)
        assertTrue(recall.isActive)
        assertEquals(listOf(true), saved)
    }

    @Test
    fun turningOffWaitsForTheEraseAndStaysOnIfItFails() {
        recall.restore(enabled = true)

        recall.requestEnabled(false)
        assertTrue(recall.isEnabled)
        assertFalse(recall.isActive)
        recall.requestEnabled(true)
        pendingClears.single()(false)

        assertTrue(recall.isEnabled)
        assertTrue(recall.isActive)
        assertTrue(saved.isEmpty())

        recall.requestEnabled(false)
        pendingClears.last()(true)
        assertFalse(recall.isEnabled)
        assertEquals(listOf(false), saved)
    }

    @Test
    fun anAddressSearchLooksInTheTabsWorkspace() {
        recall.restore(enabled = true)

        assertEquals(listOf(match), search("coroutines guide"))
        assertEquals(Triple(setOf("home"), "coroutines guide", RecallRules.MAX_ADDRESS_RESULTS), searches.single())
    }

    @Test
    fun theExplicitCommandAsksForMoreAndWorksWithoutHistorySuggestions() {
        recall.restore(enabled = true)
        historySuggestions = false

        assertEquals(listOf(match), search(">recall guide"))
        assertEquals(RecallRules.MAX_COMMAND_RESULTS, searches.single().third)
        assertEquals(emptyList<RecallMatch>(), search("coroutines guide"))
    }

    @Test
    fun offPrivateOrClearingSearchesNothing() {
        assertEquals(emptyList<RecallMatch>(), search("coroutines guide"))

        recall.restore(enabled = true)
        selected = "private"
        assertEquals(emptyList<RecallMatch>(), search("coroutines guide"))

        selected = "a"
        clearingData = true
        assertEquals(emptyList<RecallMatch>(), search("coroutines guide"))
        assertTrue(searches.isEmpty())
    }

    @Test
    fun anAnswerAfterTheTabChangedIsDropped() {
        recall.restore(enabled = true)
        tabs += BrowserTab(id = "b", lastAccessedAt = 3, profileId = "home")
        var result: List<RecallMatch>? = null
        recall.searchFromAddress("coroutines guide") { result = it }

        selected = "b"
        pendingSearches.single()(listOf(match))

        assertEquals(emptyList<RecallMatch>(), result)
    }
}
