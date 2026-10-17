package dev.sk2andy.materialbrowser.browser.translation

import androidx.compose.runtime.mutableStateMapOf

/**
 * On-device translation of the browser's tabs (v5 V23): what each tab's page is written in,
 * whether it is translated, and the two actions the v5 screens offer — translate and show the
 * original. The browser [attach]es a tab's engine port when the tab gets a page and [detach]es it
 * when the page closes; screens read [stateOf], which recomposes them as the engine reports.
 * [appLanguageTags] are the languages the user reads, the app's first.
 *
 * Every call and callback is on the main thread, as the engine ports promise.
 */
internal class OnDeviceTranslationController(
    private val models: TranslationModelsPort,
    private val appLanguageTags: () -> List<String>,
) {
    private val ports = mutableMapOf<String, PageTranslationPort>()
    private val states = mutableStateMapOf<String, OnDeviceTranslationUiState>()

    /** Starts following [tabId]'s page through [port]; a port it had before is let go. */
    fun attach(tabId: String, port: PageTranslationPort) {
        detach(tabId)
        ports[tabId] = port
        port.setListener(
            object : PageTranslationListener {
                override fun onStateChanged(state: PageTranslationState) {
                    update(tabId) { current ->
                        current.copy(
                            page = state,
                            // The engine's own word ends the wait as well as the request's answer.
                            inProgress = current.inProgress && !state.isTranslated && !state.hasError,
                        )
                    }
                }
            },
        )
    }

    /** Stops following [tabId]: its page closed. */
    fun detach(tabId: String) {
        ports.remove(tabId)?.setListener(null)
        states.remove(tabId)
    }

    /** What [tabId]'s page is in and how far its translation got; read from a screen, it follows changes. */
    fun stateOf(tabId: String): OnDeviceTranslationUiState = states[tabId] ?: OnDeviceTranslationUiState()

    /** The pair the menu offers for [tabId]'s page; null when the page is in a language the user reads. */
    fun suggestedPair(tabId: String): TranslationPair? =
        OnDeviceTranslationRules.suggestedPair(stateOf(tabId).page, appLanguageTags())

    /** Languages pages can be translated from and to, for the sheet's lists; null when the engine could not say. */
    fun languages(onResult: (TranslationLanguages?) -> Unit) = models.languages(onResult)

    /** Bytes to download before [pair] can be translated, so the sheet can say so first; null when unknown. */
    fun downloadSize(pair: TranslationPair, onResult: (Long?) -> Unit) = models.downloadSize(pair, onResult)

    /** Translates [tabId]'s page; a missing model is downloaded only when [allowModelDownload]. */
    fun translate(
        tabId: String,
        pair: TranslationPair,
        allowModelDownload: Boolean,
        onResult: (PageTranslationResult) -> Unit = {},
    ) {
        val port = ports[tabId] ?: return onResult(PageTranslationResult.Unsupported)
        update(tabId) { it.copy(pair = pair, inProgress = true, error = null) }
        port.translate(pair, allowModelDownload) { result ->
            update(tabId) { it.copy(inProgress = false, error = (result as? PageTranslationResult.Failed)?.error) }
            onResult(result)
        }
    }

    /** Shows [tabId]'s page as it was written. */
    fun showOriginal(tabId: String, onResult: (PageTranslationResult) -> Unit = {}) {
        val port = ports[tabId] ?: return onResult(PageTranslationResult.Unsupported)
        port.restoreOriginal { result ->
            update(tabId) { current ->
                when (result) {
                    PageTranslationResult.Done -> current.copy(pair = null, inProgress = false, error = null)
                    is PageTranslationResult.Failed -> current.copy(error = result.error)
                }
            }
            onResult(result)
        }
    }

    private fun update(tabId: String, change: (OnDeviceTranslationUiState) -> OnDeviceTranslationUiState) {
        if (tabId !in ports) return
        states[tabId] = change(stateOf(tabId))
    }
}

/**
 * One tab's translation as the v5 screens show it: the engine's [page] state, the [pair] the user
 * asked for, whether a request is [inProgress] (a model may be downloading) and the last [error].
 */
internal data class OnDeviceTranslationUiState(
    val page: PageTranslationState = PageTranslationState(),
    val pair: TranslationPair? = null,
    val inProgress: Boolean = false,
    val error: PageTranslationError? = null,
) {
    val isTranslated: Boolean get() = page.isTranslated
}
