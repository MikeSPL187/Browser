package dev.sk2andy.materialbrowser.recall

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.RecallRepository
import org.json.JSONObject

/**
 * Puts a page that finished loading into Recall, the local full-text memory of visited pages
 * (PRO28-03). The text comes from the engines' reader extraction, which Gecko and System WebView
 * both have; the page is kept only if, when the text comes back, it is still the same page of the
 * same tab, Recall is still on and nothing was cleared in between.
 */
internal object RecallPageCapture {
    /**
     * Asks [extract] for the page of [current] and indexes it. [current] is read again when the text
     * arrives: a different answer (another page, private, Recall off) drops the text.
     */
    fun capture(
        repository: RecallRepository,
        extract: ((String?) -> Unit) -> Unit,
        current: () -> RecallExtractionIdentity?,
        nowMillis: () -> Long = System::currentTimeMillis,
    ) {
        val identity = current() ?: return
        val cleanupEpoch = repository.captureCleanupEpoch()
        val visitedAt = nowMillis()
        extract { rawJson ->
            if (current() != identity) return@extract
            repository.indexReaderPage(rawJson, identity, cleanupEpoch, visitedAt)
        }
    }

    /** Which page [tab] shows, or null when it must not be remembered (private, not a web page). */
    fun identity(
        tab: BrowserTab?,
        pageUrl: String?,
        navigationGeneration: Int?,
        allowed: Boolean,
    ): RecallExtractionIdentity? {
        if (!allowed || tab == null || tab.isIncognito || navigationGeneration == null) return null
        val url = pageUrl?.let(RecallRules::canonicalUrl) ?: return null
        return RecallExtractionIdentity(tab.id, tab.profileId, url, navigationGeneration)
    }

    /**
     * The reader extraction ([dev.sk2andy.materialbrowser.reader.ReaderExtractionScript]) as the
     * Recall document of [identity]'s page: the article's blocks, or the page's visible text when it
     * has no article. Null for another page, an error or no text.
     */
    fun document(rawJson: String?, identity: RecallExtractionIdentity, visitedAt: Long): RecallDocument? {
        val json = rawJson?.takeIf { it.length <= MAX_JSON_CHARS } ?: return null
        return runCatching {
            val root = JSONObject(json)
            val blocks = root.optJSONArray("blocks")
            val article = buildString {
                for (index in 0 until (blocks?.length() ?: 0)) {
                    val text = blocks?.optJSONObject(index)?.optString("text").orEmpty()
                    if (text.isBlank()) continue
                    if (isNotEmpty()) append(' ')
                    append(text)
                    if (length >= RecallRules.MAX_DOCUMENT_CHARS) break
                }
            }
            val document = RecallRules.sanitizeDocument(
                RecallDocument(
                    profileId = identity.profileId,
                    url = root.optString("sourceUrl"),
                    title = root.optString("title"),
                    text = article.ifBlank { root.optString("visibleText") },
                    visitedAt = visitedAt,
                ),
            )
            document?.takeIf { it.url == identity.url }
        }.getOrNull()
    }

    private const val MAX_JSON_CHARS = 2_000_000
}
