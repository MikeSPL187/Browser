package dev.sk2andy.materialbrowser.reader

import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageCaptureTest {
    private val storyUrl = "https://news.example/story"
    private val tabs = mutableListOf(
        BrowserTab(id = "a", lastAccessedAt = 1, profileId = "home", url = storyUrl),
        BrowserTab(id = "p", lastAccessedAt = 2, profileId = "home", url = storyUrl, isIncognito = true),
        BrowserTab(id = "blank", lastAccessedAt = 3, profileId = "home", url = "about:blank"),
    )
    private var selected = "a"
    private val generations = mutableMapOf("a" to 1, "p" to 1)
    private val sessions = mutableMapOf("a" to "session-a", "p" to "session-p", "blank" to "session-blank")
    private val pendingExtractions = ArrayDeque<(String?) -> Unit>()
    private var saveWorks = true
    private val saved = mutableListOf<ReaderDocument>()

    private val capture = ReaderPageCapture(
        object : ReaderPageCapture.Host<String> {
            override val isDestroyed = false
            override val selectedTab get() = tabs.first { it.id == selected }
            override fun tab(tabId: String) = tabs.firstOrNull { it.id == tabId }
            override fun navigationGeneration(tabId: String) = generations.getOrDefault(tabId, 0)
            override fun session(tabId: String) = sessions[tabId]
            override fun extractPage(session: String, onComplete: (String?) -> Unit) {
                pendingExtractions.addLast(onComplete)
            }
            override fun saveToLibrary(document: ReaderDocument, onSaved: (Boolean) -> Unit) {
                if (saveWorks) saved += document
                onSaved(saveWorks)
            }
        },
    )

    private fun article(sourceUrl: String = storyUrl): String = JSONObject()
        .put("title", "Story")
        .put("sourceUrl", sourceUrl)
        .put("siteName", "Example News")
        .put(
            "blocks",
            JSONArray().put(
                JSONObject()
                    .put("kind", "paragraph")
                    .put(
                        "text",
                        "A long readable paragraph with enough useful article text " +
                            "to satisfy the extraction contract and stay pleasant.",
                    )
                    .put("level", 0)
                    .put("links", JSONArray()),
            ),
        )
        .toString()

    private val invalid = ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse)

    @Test
    fun theSelectedPageBecomesAReaderDocument() {
        var result: ReaderExtractionResult? = null
        capture.extractSelected { result = it }
        pendingExtractions.removeFirst()(article())

        assertEquals("Story", (result as ReaderExtractionResult.Success).document.title)
    }

    @Test
    fun aPageThatIsNotTheWebIsNotRead() {
        selected = "blank"
        var result: ReaderExtractionResult? = null

        capture.extractSelected { result = it }

        assertEquals(ReaderExtractionResult.Failure(ReaderExtractionFailure.UnsupportedPage), result)
        assertTrue(pendingExtractions.isEmpty())
    }

    @Test
    fun anAnswerAfterTheTabOrPageChangedIsAFailure() {
        var result: ReaderExtractionResult? = null
        capture.extractSelected { result = it }
        generations["a"] = 2
        pendingExtractions.removeFirst()(article())
        assertEquals(invalid, result)

        capture.extractSelected { result = it }
        sessions["a"] = "session-a2"
        pendingExtractions.removeFirst()(article())
        assertEquals(invalid, result)
    }

    @Test
    fun savingKeepsTheDocumentInTheLibrary() {
        var result: ReaderExtractionResult? = null
        capture.saveSelected { result = it }
        pendingExtractions.removeFirst()(article())

        assertTrue(result is ReaderExtractionResult.Success)
        assertEquals(listOf("Story"), saved.map(ReaderDocument::title))
    }

    @Test
    fun aPrivateTabIsNeverSaved() {
        selected = "p"
        var result: ReaderExtractionResult? = null

        capture.saveSelected { result = it }

        assertEquals(ReaderExtractionResult.Failure(ReaderExtractionFailure.UnsupportedPage), result)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun aDocumentFromAnotherAddressOrAFailedSaveIsAFailure() {
        var result: ReaderExtractionResult? = null
        capture.saveSelected { result = it }
        pendingExtractions.removeFirst()(article(sourceUrl = "https://elsewhere.example/"))
        assertEquals(invalid, result)
        assertTrue(saved.isEmpty())

        saveWorks = false
        capture.saveSelected { result = it }
        pendingExtractions.removeFirst()(article())
        assertEquals(invalid, result)
    }
}
