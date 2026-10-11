package dev.sk2andy.materialbrowser.browser.translation

/**
 * Page translation on the phone (v5 V23): the engine translates the page in place with language
 * models it keeps locally, so the page text never goes to a translation service. Unlike the
 * provider translation in `PageTranslation.kt`, which sends the page address to Google, Yandex or
 * Kagi, nothing about the page leaves the phone; only a missing model is downloaded, once per
 * language, and only when a translation asks for it ([PageTranslationPort.translate]).
 *
 * GeckoView implements it (`TranslationsController`, Bergamot models). System WebView has no
 * on-device translation: its adapter returns [PageTranslationPort.Unavailable] and
 * [TranslationModelsPort.Unavailable], and the capability flag hides the feature in the UI.
 */
internal interface PageTranslationPort {
    /** Whether this engine can translate pages at all; false means hide the feature. */
    val isAvailable: Boolean

    /** Receives offers and state changes of this tab's page; null stops them. */
    fun setListener(listener: PageTranslationListener?)

    /**
     * Translates the current page. A model that is not on the phone yet is downloaded only when
     * [allowModelDownload] is true; otherwise the result is [PageTranslationError.ModelDownloadRequired],
     * so the UI can ask first (size from [TranslationModelsPort.downloadSize]).
     */
    fun translate(
        pair: TranslationPair,
        allowModelDownload: Boolean,
        onResult: (PageTranslationResult) -> Unit,
    )

    /** Shows the page as it was before translation. */
    fun restoreOriginal(onResult: (PageTranslationResult) -> Unit)

    /** Whether the user asked never to offer translation on this site; null when unknown. */
    fun neverTranslateSite(onResult: (Boolean?) -> Unit)

    fun setNeverTranslateSite(never: Boolean, onResult: (PageTranslationResult) -> Unit)

    /** The engine without on-device translation (System WebView). */
    object Unavailable : PageTranslationPort {
        override val isAvailable: Boolean = false

        override fun setListener(listener: PageTranslationListener?) = Unit

        override fun translate(
            pair: TranslationPair,
            allowModelDownload: Boolean,
            onResult: (PageTranslationResult) -> Unit,
        ) = onResult(PageTranslationResult.Unsupported)

        override fun restoreOriginal(onResult: (PageTranslationResult) -> Unit) =
            onResult(PageTranslationResult.Unsupported)

        override fun neverTranslateSite(onResult: (Boolean?) -> Unit) = onResult(null)

        override fun setNeverTranslateSite(never: Boolean, onResult: (PageTranslationResult) -> Unit) =
            onResult(PageTranslationResult.Unsupported)
    }
}

/** Language models, shared by every tab: what can be translated and what is on the phone. */
internal interface TranslationModelsPort {
    val isAvailable: Boolean

    /** Whether this device can run the models (the engine checks the processor); false hides the feature. */
    fun isSupportedOnDevice(onResult: (Boolean) -> Unit)

    /** Languages pages can be translated from and to; null when the list could not be loaded. */
    fun languages(onResult: (TranslationLanguages?) -> Unit)

    /** Every model with its size and whether it is downloaded; null when unknown. */
    fun models(onResult: (List<TranslationModel>?) -> Unit)

    /** Bytes still to download before [pair] can be translated; 0 when nothing is missing. */
    fun downloadSize(pair: TranslationPair, onResult: (Long?) -> Unit)

    /** Downloads the models of one language ahead of time, for example on Wi-Fi. */
    fun download(languageCode: String, onResult: (PageTranslationResult) -> Unit)

    /** Deletes the models of one language to free space. */
    fun delete(languageCode: String, onResult: (PageTranslationResult) -> Unit)

    /** The engine without on-device translation (System WebView). */
    object Unavailable : TranslationModelsPort {
        override val isAvailable: Boolean = false

        override fun isSupportedOnDevice(onResult: (Boolean) -> Unit) = onResult(false)

        override fun languages(onResult: (TranslationLanguages?) -> Unit) = onResult(null)

        override fun models(onResult: (List<TranslationModel>?) -> Unit) = onResult(null)

        override fun downloadSize(pair: TranslationPair, onResult: (Long?) -> Unit) = onResult(null)

        override fun download(languageCode: String, onResult: (PageTranslationResult) -> Unit) =
            onResult(PageTranslationResult.Unsupported)

        override fun delete(languageCode: String, onResult: (PageTranslationResult) -> Unit) =
            onResult(PageTranslationResult.Unsupported)
    }
}

/** What the page's tab hears about translation. Both run on the main thread. */
internal interface PageTranslationListener {
    /**
     * The page is in a language the user does not read, can be translated, and the site has not
     * been offered this session nor set to «never»: the moment for an offer.
     */
    fun onOffer() = Unit

    fun onStateChanged(state: PageTranslationState) = Unit
}

/** A BCP 47 language tag pair, such as `de` → `ru`. */
internal data class TranslationPair(val from: String, val to: String)

/** A language by BCP 47 tag; [displayName] is in the app language when the engine knows it. */
internal data class TranslationLanguage(val code: String, val displayName: String?)

internal data class TranslationLanguages(
    val from: List<TranslationLanguage>,
    val to: List<TranslationLanguage>,
)

internal data class TranslationModel(
    val language: TranslationLanguage,
    val isDownloaded: Boolean,
    val sizeBytes: Long,
)

/**
 * What the engine knows about the page's translation. [pageLanguage] is the detected language,
 * [userLanguage] the one the user reads; [requested] is set once a translation was asked for and
 * [isTranslated] once the text on screen visibly changed.
 */
internal data class PageTranslationState(
    val pageLanguage: String? = null,
    val pageLanguageSupported: Boolean = false,
    val userLanguage: String? = null,
    val requested: TranslationPair? = null,
    val isEngineReady: Boolean = false,
    val isTranslated: Boolean = false,
    val hasError: Boolean = false,
)

internal enum class PageTranslationError {
    /** The engine or this device cannot translate. */
    Unsupported,

    /** The engine switched translation off for this session (too many failures, low memory). */
    EngineDeactivated,
    LanguageNotSupported,

    /** The pair needs a model that is not on the phone and the download was not allowed. */
    ModelDownloadRequired,
    ModelDownloadFailed,
    ModelDeleteFailed,
    CouldNotTranslate,
    CouldNotRestore,
    CouldNotLoadLanguages,
    Unknown,
}

internal sealed interface PageTranslationResult {
    data object Done : PageTranslationResult

    data class Failed(val error: PageTranslationError) : PageTranslationResult

    companion object {
        val Unsupported: PageTranslationResult = Failed(PageTranslationError.Unsupported)
    }
}
