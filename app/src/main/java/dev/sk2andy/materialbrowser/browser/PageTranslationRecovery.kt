package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineEventType

/**
 * Translating a page through a web provider (#500): the provider page opens in the tab, its
 * navigation and, for providers that show an empty shell on failure, its text are watched, and a
 * failure on the selected tab offers one recovery (another provider or the original page).
 * [S] is the engine session the page text is read from.
 */
internal class PageTranslationRecovery<S : Any>(private val host: Host<S>) {
    interface Host<S> {
        val isDestroyed: Boolean
        val selectedTabId: String

        /** The page's address as the tab shows it, or null when the tab is gone. */
        fun tabUrl(tabId: String): String?

        fun navigationGeneration(tabId: String): Int

        /** The language pages are translated into: the device's. */
        fun targetLanguage(): String

        /** Opens [url] in the selected tab. */
        fun navigate(url: String)

        fun session(tabId: String): S?

        /** The page's readable text as JSON, or null; see [PageTranslationRecoveryRules.contentOutcome]. */
        fun extractPage(session: S, onComplete: (String?) -> Unit)

        fun postDelayed(task: () -> Unit, delayMillis: Long)
    }

    private val attempts = mutableMapOf<String, PageTranslationAttempt>()

    /** The one recovery the snackbar offers, for the selected tab. */
    var offer by mutableStateOf<PageTranslationRecoveryOffer?>(null)
        private set
    private var offerSequence = 0L

    /** Translates [tabId]'s page at [sourceUrl] with [provider]; the tab must be the selected one. */
    fun translate(tabId: String, sourceUrl: String, provider: PageTranslationProvider) {
        val translationUrl = translationUrl(provider, sourceUrl) ?: return
        attempts[tabId] = PageTranslationAttempt(tabId = tabId, sourceUrl = sourceUrl, provider = provider)
        if (offer?.tabId == tabId) offer = null
        host.navigate(translationUrl)
    }

    fun recover(token: Long) {
        val offer = offer?.takeIf { it.token == token } ?: return
        this.offer = null
        if (!isCurrent(offer)) return
        val targetUrl = when (offer.action) {
            PageTranslationRecoveryAction.TryYandex -> {
                val provider = PageTranslationProvider.Yandex
                val translationUrl = translationUrl(provider, offer.sourceUrl) ?: return
                attempts[offer.tabId] = PageTranslationAttempt(
                    tabId = offer.tabId,
                    sourceUrl = offer.sourceUrl,
                    provider = provider,
                )
                translationUrl
            }
            PageTranslationRecoveryAction.OpenOriginal -> offer.sourceUrl
        }
        host.navigate(targetUrl)
    }

    fun dismiss(token: Long) {
        if (offer?.token == token) offer = null
    }

    /** [tabId] closed or was replaced: its translation is no longer watched. */
    fun forgetAttempt(tabId: String) {
        attempts.remove(tabId)
    }

    /** [tabId] left the screen: its offer goes with it. */
    fun dismissOfferFor(tabId: String) {
        if (offer?.tabId == tabId) offer = null
    }

    fun clear() {
        offer = null
        attempts.clear()
    }

    private fun translationUrl(provider: PageTranslationProvider, sourceUrl: String): String? =
        PageTranslationRules.buildTranslationUrl(
            provider = provider,
            sourceUrl = sourceUrl,
            targetLanguage = PageTranslationRules.targetLanguage(host.targetLanguage()),
        )

    fun onEngineEvent(tabId: String, type: BrowserEngineEventType, address: String?, httpStatusCode: Int?) {
        val attempt = attempts[tabId] ?: return
        if (type == BrowserEngineEventType.Closed) {
            attempts.remove(tabId)
            return
        }
        val tabUrl = host.tabUrl(tabId) ?: run {
            attempts.remove(tabId)
            return
        }
        val eventUrl = address ?: tabUrl
        if (!PageTranslationRules.isProviderPage(attempt.provider, eventUrl)) {
            if (type == BrowserEngineEventType.NavigationStarted) attempts.remove(tabId)
            return
        }
        if (
            PageTranslationRecoveryRules.isProviderFailure(
                provider = attempt.provider,
                url = eventUrl,
                navigationFailed = type == BrowserEngineEventType.NavigationFailed ||
                    type == BrowserEngineEventType.Crashed,
                httpStatusCode = httpStatusCode,
            ) || PageTranslationRecoveryRules.isRejectedEntryPage(
                provider = attempt.provider,
                url = eventUrl,
                navigationCommitted = type == BrowserEngineEventType.NavigationCommitted,
            )
        ) {
            reportFailure(attempt, eventUrl, host.navigationGeneration(tabId))
            return
        }
        if (type != BrowserEngineEventType.NavigationCommitted ||
            !PageTranslationRules.isProviderResultPage(attempt.provider, eventUrl)
        ) return
        if (!PageTranslationRecoveryRules.shouldCheckResultContent(attempt.provider, eventUrl)) {
            attempts.remove(tabId)
            return
        }
        scheduleContentCheck(attempt, eventUrl, host.navigationGeneration(tabId))
    }

    private fun scheduleContentCheck(
        attempt: PageTranslationAttempt,
        resultUrl: String,
        navigationGeneration: Int,
        emptyChecksRemaining: Int = EMPTY_CHECKS_REQUIRED,
        delayMillis: Long = CONTENT_CHECK_DELAY_MILLIS,
    ) {
        val session = host.session(attempt.tabId) ?: return
        val isCurrent = { isCurrent(attempt, resultUrl, navigationGeneration, session) }
        host.postDelayed(
            {
                if (isCurrent()) {
                    host.extractPage(session) { rawResult ->
                        if (!isCurrent()) return@extractPage
                        when (
                            PageTranslationRecoveryRules.contentAction(
                                outcome = PageTranslationRecoveryRules.contentOutcome(rawResult),
                                emptyChecksRemaining = emptyChecksRemaining,
                            )
                        ) {
                            PageTranslationContentAction.ReportFailure ->
                                reportFailure(attempt, resultUrl, navigationGeneration)
                            PageTranslationContentAction.Retry -> scheduleContentCheck(
                                attempt = attempt,
                                resultUrl = resultUrl,
                                navigationGeneration = navigationGeneration,
                                emptyChecksRemaining = emptyChecksRemaining - 1,
                                delayMillis = CONTENT_RETRY_DELAY_MILLIS,
                            )
                            PageTranslationContentAction.Complete -> attempts.remove(attempt.tabId)
                        }
                    }
                }
            },
            delayMillis,
        )
    }

    private fun isCurrent(
        attempt: PageTranslationAttempt,
        resultUrl: String,
        navigationGeneration: Int,
        session: S,
    ): Boolean {
        val tabUrl = host.tabUrl(attempt.tabId) ?: return false
        return !host.isDestroyed &&
            attempts[attempt.tabId] == attempt &&
            host.session(attempt.tabId) === session &&
            host.navigationGeneration(attempt.tabId) == navigationGeneration &&
            BrowserUriPolicy.normalizeHttpUrl(tabUrl) == BrowserUriPolicy.normalizeHttpUrl(resultUrl)
    }

    private fun reportFailure(attempt: PageTranslationAttempt, failedUrl: String, navigationGeneration: Int) {
        if (attempts.remove(attempt.tabId) != attempt) return
        // The one recovery Snackbar belongs to the selected tab; a late background failure must
        // not replace it. That tab keeps the provider page, and translating again starts afresh.
        if (host.selectedTabId != attempt.tabId) return
        offerSequence++
        offer = PageTranslationRecoveryOffer(
            token = offerSequence,
            tabId = attempt.tabId,
            sourceUrl = attempt.sourceUrl,
            failedUrl = failedUrl,
            provider = attempt.provider,
            navigationGeneration = navigationGeneration,
        )
    }

    private fun isCurrent(offer: PageTranslationRecoveryOffer): Boolean {
        val tabUrl = host.tabUrl(offer.tabId) ?: return false
        return !host.isDestroyed &&
            host.selectedTabId == offer.tabId &&
            host.navigationGeneration(offer.tabId) == offer.navigationGeneration &&
            BrowserUriPolicy.normalizeHttpUrl(tabUrl) == BrowserUriPolicy.normalizeHttpUrl(offer.failedUrl)
    }

    private companion object {
        const val CONTENT_CHECK_DELAY_MILLIS = 2_500L
        const val CONTENT_RETRY_DELAY_MILLIS = 2_500L
        const val EMPTY_CHECKS_REQUIRED = 2
    }
}
