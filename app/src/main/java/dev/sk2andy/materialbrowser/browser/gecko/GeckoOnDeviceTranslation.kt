package dev.sk2andy.materialbrowser.browser.gecko

import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationError
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationListener
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationPort
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationResult
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationState
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguages
import dev.sk2andy.materialbrowser.browser.translation.TranslationModel
import dev.sk2andy.materialbrowser.browser.translation.TranslationModelsPort
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.TranslationsController.Language
import org.mozilla.geckoview.TranslationsController.RuntimeTranslation
import org.mozilla.geckoview.TranslationsController.SessionTranslation
import org.mozilla.geckoview.TranslationsController.TranslationsException

/**
 * [PageTranslationPort] for one tab on Gecko's `TranslationsController`: Bergamot models translate
 * the page inside Gecko. Gecko downloads a missing model from Mozilla only when [translate] allows
 * it; the page text stays on the phone. Every call and callback is on the main thread.
 */
internal class GeckoPageTranslation(private val session: GeckoSession) : PageTranslationPort {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var closed = false

    override val isAvailable: Boolean = true

    @UiThread
    override fun setListener(listener: PageTranslationListener?) {
        if (closed) return
        session.translationsSessionDelegate = listener?.let(::Delegate)
    }

    @UiThread
    override fun translate(
        pair: TranslationPair,
        allowModelDownload: Boolean,
        onResult: (PageTranslationResult) -> Unit,
    ) {
        val translation = sessionTranslation() ?: return onResult(PageTranslationResult.Unsupported)
        val options = SessionTranslation.TranslationOptions.Builder()
            .downloadModel(allowModelDownload)
            .build()
        deliver(onResult) { translation.translate(pair.from, pair.to, options) }
    }

    @UiThread
    override fun restoreOriginal(onResult: (PageTranslationResult) -> Unit) {
        val translation = sessionTranslation() ?: return onResult(PageTranslationResult.Unsupported)
        deliver(onResult) { translation.restoreOriginalPage() }
    }

    @UiThread
    override fun neverTranslateSite(onResult: (Boolean?) -> Unit) {
        val translation = sessionTranslation() ?: return onResult(null)
        runCatching { translation.neverTranslateSiteSetting }
            .getOrNull()
            ?.withHandler(mainHandler)
            ?.accept({ never -> onResult(never) }, { onResult(null) })
            ?: onResult(null)
    }

    @UiThread
    override fun setNeverTranslateSite(never: Boolean, onResult: (PageTranslationResult) -> Unit) {
        val translation = sessionTranslation() ?: return onResult(PageTranslationResult.Unsupported)
        deliver(onResult) { translation.setNeverTranslateSiteSetting(never) }
    }

    /** Stops the offers and state changes; the session is about to close. */
    @UiThread
    fun close() {
        if (closed) return
        closed = true
        session.translationsSessionDelegate = null
    }

    private fun sessionTranslation(): SessionTranslation? =
        if (closed) null else session.sessionTranslation

    private fun deliver(onResult: (PageTranslationResult) -> Unit, request: () -> GeckoResult<Void>) =
        GeckoTranslationResults.deliver(mainHandler, onResult, request)

    private class Delegate(private val listener: PageTranslationListener) : SessionTranslation.Delegate {
        override fun onOfferTranslate(session: GeckoSession) = listener.onOffer()

        override fun onTranslationStateChange(
            session: GeckoSession,
            translationState: SessionTranslation.TranslationState?,
        ) {
            translationState?.let { listener.onStateChanged(GeckoTranslationMapping.state(it)) }
        }
    }
}

/**
 * [TranslationModelsPort] on Gecko's runtime-wide translation calls. Gecko must be running, which
 * the engine factory that hands this out guarantees. Every call and callback is on the main thread.
 */
internal class GeckoTranslationModels : TranslationModelsPort {
    private val mainHandler = Handler(Looper.getMainLooper())

    override val isAvailable: Boolean = true

    @UiThread
    override fun isSupportedOnDevice(onResult: (Boolean) -> Unit) =
        query(RuntimeTranslation::isTranslationsEngineSupported, { it == true }, false, onResult)

    @UiThread
    override fun languages(onResult: (TranslationLanguages?) -> Unit) =
        query(RuntimeTranslation::listSupportedLanguages, GeckoTranslationMapping::languages, null, onResult)

    @UiThread
    override fun models(onResult: (List<TranslationModel>?) -> Unit) =
        query(RuntimeTranslation::listModelDownloadStates, GeckoTranslationMapping::models, null, onResult)

    @UiThread
    override fun downloadSize(pair: TranslationPair, onResult: (Long?) -> Unit) =
        query({ RuntimeTranslation.checkPairDownloadSize(pair.from, pair.to) }, { it }, null, onResult)

    @UiThread
    override fun download(languageCode: String, onResult: (PageTranslationResult) -> Unit) =
        manage(languageCode, RuntimeTranslation.DOWNLOAD, onResult)

    @UiThread
    override fun delete(languageCode: String, onResult: (PageTranslationResult) -> Unit) =
        manage(languageCode, RuntimeTranslation.DELETE, onResult)

    private fun manage(languageCode: String, operation: String, onResult: (PageTranslationResult) -> Unit) {
        if (languageCode.isBlank()) return onResult(PageTranslationResult.Failed(PageTranslationError.LanguageNotSupported))
        val options = RuntimeTranslation.ModelManagementOptions.Builder()
            .languageToManage(languageCode)
            .operation(operation)
            .operationLevel(RuntimeTranslation.LANGUAGE)
            .build()
        GeckoTranslationResults.deliver(mainHandler, onResult) { RuntimeTranslation.manageLanguageModel(options) }
    }

    /** Runs a Gecko query and hands [onResult] its mapped value, or [fallback] when it fails. */
    private fun <T, R> query(
        request: () -> GeckoResult<T>,
        map: (T?) -> R,
        fallback: R,
        onResult: (R) -> Unit,
    ) {
        runCatching(request).getOrNull()
            ?.withHandler(mainHandler)
            ?.accept({ value -> onResult(map(value)) }, { onResult(fallback) })
            ?: onResult(fallback)
    }
}

private object GeckoTranslationResults {
    /** Runs [request] and reports done or the mapped error; a call that throws is an unknown error. */
    fun deliver(
        handler: Handler,
        onResult: (PageTranslationResult) -> Unit,
        request: () -> GeckoResult<Void>,
    ) {
        val result = runCatching(request).getOrElse {
            onResult(PageTranslationResult.Failed(PageTranslationError.Unknown))
            return
        }
        result.withHandler(handler).accept(
            { onResult(PageTranslationResult.Done) },
            { error -> onResult(PageTranslationResult.Failed(GeckoTranslationMapping.error(error))) },
        )
    }
}

/** Gecko's translation types to the engine-neutral ones of `browser/translation`. */
internal object GeckoTranslationMapping {
    fun state(state: SessionTranslation.TranslationState): PageTranslationState {
        val detected = state.detectedLanguages
        val requested = state.requestedTranslationPair
        val from = requested?.fromLanguage
        val to = requested?.toLanguage
        return PageTranslationState(
            pageLanguage = detected?.docLangTag,
            pageLanguageSupported = detected?.isDocLangTagSupported == true,
            userLanguage = detected?.userLangTag,
            requested = if (from != null && to != null) TranslationPair(from, to) else null,
            isEngineReady = state.isEngineReady == true,
            isTranslated = state.hasVisibleChange == true,
            hasError = !state.error.isNullOrBlank(),
        )
    }

    fun languages(support: RuntimeTranslation.TranslationSupport?): TranslationLanguages? {
        support ?: return null
        return TranslationLanguages(
            from = support.fromLanguages.orEmpty().map(::language),
            to = support.toLanguages.orEmpty().map(::language),
        )
    }

    fun models(models: List<RuntimeTranslation.LanguageModel>?): List<TranslationModel>? =
        models?.mapNotNull { model ->
            val language = model.language ?: return@mapNotNull null
            TranslationModel(language(language), isDownloaded = model.isDownloaded == true, sizeBytes = model.size)
        }

    fun error(error: Throwable?): PageTranslationError = when ((error as? TranslationsException)?.code) {
        TranslationsException.ERROR_ENGINE_NOT_SUPPORTED -> PageTranslationError.Unsupported
        TranslationsException.ERROR_ENGINE_DEACTIVATED -> PageTranslationError.EngineDeactivated
        TranslationsException.ERROR_LANGUAGE_NOT_SUPPORTED,
        TranslationsException.ERROR_MODEL_LANGUAGE_REQUIRED,
        -> PageTranslationError.LanguageNotSupported
        TranslationsException.ERROR_MODEL_DOWNLOAD_REQUIRED -> PageTranslationError.ModelDownloadRequired
        TranslationsException.ERROR_MODEL_COULD_NOT_DOWNLOAD,
        TranslationsException.ERROR_MODEL_COULD_NOT_RETRIEVE,
        -> PageTranslationError.ModelDownloadFailed
        TranslationsException.ERROR_MODEL_COULD_NOT_DELETE -> PageTranslationError.ModelDeleteFailed
        TranslationsException.ERROR_COULD_NOT_TRANSLATE -> PageTranslationError.CouldNotTranslate
        TranslationsException.ERROR_COULD_NOT_RESTORE -> PageTranslationError.CouldNotRestore
        TranslationsException.ERROR_COULD_NOT_LOAD_LANGUAGES -> PageTranslationError.CouldNotLoadLanguages
        else -> PageTranslationError.Unknown
    }

    private fun language(language: Language) = TranslationLanguage(language.code, language.localizedDisplayName)
}
