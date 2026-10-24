package dev.sk2andy.materialbrowser.reader

import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy

/**
 * The selected page as a reader document (#500): read from its engine session for the reader view,
 * listening or saving, and kept in the reader library. An answer that arrives after the tab, its
 * page or its session changed is a failure, never another page's text. [S] is the engine session.
 */
internal class ReaderPageCapture<S : Any>(private val host: Host<S>) {
    interface Host<S> {
        val isDestroyed: Boolean
        val selectedTab: BrowserTab

        fun tab(tabId: String): BrowserTab?

        fun navigationGeneration(tabId: String): Int

        fun session(tabId: String): S?

        /** The page's reader extraction as JSON, or null. */
        fun extractPage(session: S, onComplete: (String?) -> Unit)

        /** Keeps [document] in the reader library; answers whether it was saved. */
        fun saveToLibrary(document: ReaderDocument, onSaved: (Boolean) -> Unit)
    }

    fun extractSelected(onResult: (ReaderExtractionResult) -> Unit) {
        val tab = host.selectedTab
        if (
            !tab.url.startsWith("https://", ignoreCase = true) &&
            !tab.url.startsWith("http://", ignoreCase = true)
        ) {
            onResult(ReaderExtractionResult.Failure(ReaderExtractionFailure.UnsupportedPage))
            return
        }
        val session = host.session(tab.id) ?: run {
            onResult(ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse))
            return
        }
        val expectedUrl = tab.url
        val expectedGeneration = host.navigationGeneration(tab.id)
        host.extractPage(session) { result ->
            if (
                host.isDestroyed ||
                host.selectedTab.id != tab.id ||
                host.tab(tab.id)?.url != expectedUrl ||
                host.navigationGeneration(tab.id) != expectedGeneration ||
                host.session(tab.id) !== session
            ) {
                onResult(ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse))
            } else {
                onResult(ReaderExtractionParser.parseJson(result))
            }
        }
    }

    /** Extracts the selected page and keeps it in the library; a private tab is never kept. */
    fun saveSelected(onResult: (ReaderExtractionResult) -> Unit) {
        val selected = host.selectedTab
        val expectedUrl = BrowserUriPolicy.normalizeHttpUrl(selected.url)
        if (selected.isIncognito || expectedUrl == null) {
            onResult(ReaderExtractionResult.Failure(ReaderExtractionFailure.UnsupportedPage))
            return
        }
        extractSelected { result ->
            val document = (result as? ReaderExtractionResult.Success)?.document
            if (document == null) {
                onResult(result)
                return@extractSelected
            }
            if (BrowserUriPolicy.normalizeHttpUrl(document.sourceUrl) != expectedUrl) {
                onResult(ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse))
                return@extractSelected
            }
            host.saveToLibrary(document) { saved ->
                onResult(if (saved) result else ReaderExtractionResult.Failure(ReaderExtractionFailure.InvalidResponse))
            }
        }
    }
}
