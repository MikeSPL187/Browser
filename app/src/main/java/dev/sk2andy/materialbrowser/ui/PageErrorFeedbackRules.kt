package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import java.net.URI

internal sealed interface PageErrorFeedbackState {
    data object Hidden : PageErrorFeedbackState

    data object NotFound : PageErrorFeedbackState

    /** The engine found no server for the address: most likely a typo. */
    data object UnknownHost : PageErrorFeedbackState

    data class Error(val message: String) : PageErrorFeedbackState

    data object Retrying : PageErrorFeedbackState

    /**
     * The page failed without a connection. [isOnlineReady]: the connection is back, but the
     * page failed once more after reloading by itself, so the next try is the user's.
     */
    data class Offline(
        val isOnlineReady: Boolean = false,
    ) : PageErrorFeedbackState
}

internal data class PageErrorObservation(
    val state: PageErrorFeedbackState,
    val shouldReload: Boolean = false,
)

internal data class PageErrorRetryTransition(
    val state: PageErrorFeedbackState,
    val shouldReload: Boolean,
    val emitConfirmHaptic: Boolean,
)

internal object PageErrorFeedbackRules {
    fun observe(
        current: PageErrorFeedbackState,
        error: String?,
        httpStatusCode: Int?,
        isLoading: Boolean,
        isOnline: Boolean,
        failureKind: BrowserEngineFailureKind? = null,
        isWebPage: Boolean = true,
    ): PageErrorObservation = when {
        !isWebPage -> PageErrorObservation(PageErrorFeedbackState.Hidden)
        !isOnline && current is PageErrorFeedbackState.Offline -> PageErrorObservation(
            state = current.copy(isOnlineReady = false),
        )
        !isOnline && error != null -> PageErrorObservation(
            state = PageErrorFeedbackState.Offline(isOnlineReady = false),
        )
        // The connection came back while the page waited for it: reload once by itself, the way
        // the page promises. If that fails too, the state below waits for the Retry button.
        isOnline && current is PageErrorFeedbackState.Offline && !current.isOnlineReady ->
            PageErrorObservation(state = PageErrorFeedbackState.Retrying, shouldReload = true)
        isOnline && current is PageErrorFeedbackState.Offline -> PageErrorObservation(current)
        failureKind == BrowserEngineFailureKind.Offline && error != null -> PageErrorObservation(
            state = PageErrorFeedbackState.Offline(isOnlineReady = isOnline),
        )
        failureKind == BrowserEngineFailureKind.UnknownHost && error != null ->
            PageErrorObservation(PageErrorFeedbackState.UnknownHost)
        httpStatusCode == HTTP_NOT_FOUND_STATUS && !isLoading ->
            PageErrorObservation(PageErrorFeedbackState.NotFound)
        error != null -> PageErrorObservation(PageErrorFeedbackState.Error(error))
        else -> PageErrorObservation(PageErrorFeedbackState.Hidden)
    }

    /**
     * Retry always reloads, even while offline: the system may be wrong about the connection,
     * and a button that does nothing feels broken. Offline, the page fails fast and comes back.
     */
    fun requestRetry(current: PageErrorFeedbackState): PageErrorRetryTransition = when (current) {
        PageErrorFeedbackState.NotFound,
        PageErrorFeedbackState.UnknownHost,
        is PageErrorFeedbackState.Error,
        is PageErrorFeedbackState.Offline,
        -> PageErrorRetryTransition(
            state = PageErrorFeedbackState.Retrying,
            shouldReload = true,
            emitConfirmHaptic = true,
        )
        PageErrorFeedbackState.Hidden,
        PageErrorFeedbackState.Retrying,
        -> PageErrorRetryTransition(
            state = current,
            shouldReload = false,
            emitConfirmHaptic = false,
        )
    }

    /** The site's name for the page's sentence: its host, or the address itself without one. */
    fun displayHost(url: String): String =
        runCatching { URI(url).host }.getOrNull()
            ?.removePrefix("www.")
            ?.takeIf(String::isNotBlank)
            ?: url

    private const val HTTP_NOT_FOUND_STATUS = 404
}
