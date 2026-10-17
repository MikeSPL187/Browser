package dev.sk2andy.materialbrowser.browser.translation

import android.content.Context

/**
 * The browser's [OnDeviceTranslationController]: the languages the user reads are the app's
 * locales, first one first, read at each question so a language change applies at once. [models]
 * is asked for only when a screen first needs the models, so creating the controller does not
 * start the engine.
 */
internal fun androidOnDeviceTranslationController(
    context: Context,
    models: () -> TranslationModelsPort,
): OnDeviceTranslationController = OnDeviceTranslationController(LazyTranslationModels(models)) {
    OnDeviceTranslationLanguages.fromLanguageTags(context.resources.configuration.locales.toLanguageTags())
}

internal object OnDeviceTranslationLanguages {
    /** The tags of a `LocaleList.toLanguageTags()` string (`ru-RU,en-US`), blank and repeated ones left out. */
    fun fromLanguageTags(languageTags: String): List<String> =
        languageTags.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
}

/** A [TranslationModelsPort] that resolves the engine's port on its first call. */
internal class LazyTranslationModels(resolve: () -> TranslationModelsPort) : TranslationModelsPort {
    private val port by lazy(LazyThreadSafetyMode.NONE, resolve)

    override fun isSupportedOnDevice(onResult: (Boolean) -> Unit) = port.isSupportedOnDevice(onResult)

    override fun languages(onResult: (TranslationLanguages?) -> Unit) = port.languages(onResult)

    override fun models(onResult: (List<TranslationModel>?) -> Unit) = port.models(onResult)

    override fun downloadSize(pair: TranslationPair, onResult: (Long?) -> Unit) = port.downloadSize(pair, onResult)

    override fun download(languageCode: String, onResult: (PageTranslationResult) -> Unit) =
        port.download(languageCode, onResult)

    override fun delete(languageCode: String, onResult: (PageTranslationResult) -> Unit) =
        port.delete(languageCode, onResult)
}
