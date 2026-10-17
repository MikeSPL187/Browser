package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguageSetting
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguages
import dev.sk2andy.materialbrowser.browser.translation.TranslationModel
import dev.sk2andy.materialbrowser.ui.theme.v5.V5TranslateIntoSetting
import dev.sk2andy.materialbrowser.ui.theme.v5.V5TranslateOfferSetting
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5Preference
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5TranslateInto
import dev.sk2andy.materialbrowser.ui.v5.V5LanguagePack
import dev.sk2andy.materialbrowser.ui.v5.V5TranslationSettings
import dev.sk2andy.materialbrowser.ui.v5.V5TranslationSettingsActions
import dev.sk2andy.materialbrowser.ui.v5.V5TranslationSettingsState

/**
 * Settings → Translation (V23, canvas «т3») bound to the engine's language models: the packs with
 * their sizes, downloaded and deleted here; the language the translate sheet offers first; and
 * whether the page menu offers translation, and the languages never to translate (the engine's
 * per-language setting; «Always» lives in the translate sheet).
 */
@Composable
internal fun BrowserTranslationSettingsV5(controller: BrowserController, onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val models = controller.onDeviceTranslation.models
    var offer by rememberV5Preference(V5TranslateOfferSetting)
    val intoCode = rememberV5TranslateInto().ifEmpty { locale.language }
    var languages by remember { mutableStateOf<TranslationLanguages?>(null) }
    var modelList by remember { mutableStateOf<List<TranslationModel>?>(null) }
    var downloading by remember { mutableStateOf(emptySet<String>()) }
    // Each finished download or delete asks the engine again what is on the phone.
    var listed by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf<Map<String, TranslationLanguageSetting>?>(null) }
    var settingsRead by remember { mutableIntStateOf(0) }
    LaunchedEffect(settingsRead) { models.languageSettings { settings = it } }
    LaunchedEffect(Unit) { models.languages { languages = it } }
    LaunchedEffect(listed) { models.models { modelList = it } }
    val name = { code: String -> BrowserTranslateRules.languageName(code, locale) }
    val intoOptions = languages?.to.orEmpty().map { BrowserTranslateRules.option(it, name) }
    val neverOptions = languages?.from.orEmpty().map { BrowserTranslateRules.option(it, name) }
    val neverCodes = BrowserTranslationSettingsRules.never(settings)
    V5TranslationSettings(
        state = V5TranslationSettingsState(
            offerInMenu = offer,
            translateInto = BrowserTranslateRules.pick(intoCode, intoOptions, name).name,
            neverTranslate = settings?.let { neverCodes.map { code -> BrowserTranslateRules.pick(code, neverOptions, name).name } },
            packs = modelList?.let { BrowserTranslationSettingsRules.packs(it, downloading, name) },
            translateIntoCode = intoCode,
            intoOptions = intoOptions,
            neverOptions = neverOptions,
            neverCodes = neverCodes,
        ),
        actions = V5TranslationSettingsActions(
            onBack = onBack,
            onOfferChange = { offer = it },
            onTranslateInto = { code -> V5TranslateIntoSetting.set(context, code) },
            onNeverTranslate = { code, never ->
                val setting = if (never) TranslationLanguageSetting.Never else TranslationLanguageSetting.Offer
                models.setLanguageSetting(code, setting) { settingsRead++ }
            },
            onDownload = { code ->
                downloading = downloading + code
                models.download(code) {
                    downloading = downloading - code
                    listed++
                }
            },
            onDelete = { code -> models.delete(code) { listed++ } },
        ),
    )
}

/** Pure decisions of the translation settings binding, for unit tests. */
internal object BrowserTranslationSettingsRules {
    /** The languages set to «never translate», in a stable order; none while the engine has not said. */
    fun never(settings: Map<String, TranslationLanguageSetting>?): Set<String> =
        settings.orEmpty().filterValues { it == TranslationLanguageSetting.Never }.keys.sorted().toSet()

    /** The engine's models as packs, one per language, with the ones being downloaded marked. */
    fun packs(models: List<TranslationModel>, downloading: Set<String>, name: (String) -> String): List<V5LanguagePack> =
        models.distinctBy { it.language.code }.map { model ->
            val code = model.language.code
            V5LanguagePack(
                code = code,
                name = model.language.displayName?.takeIf(String::isNotBlank) ?: name(code),
                sizeBytes = model.sizeBytes,
                downloaded = model.isDownloaded,
                downloading = code in downloading && !model.isDownloaded,
            )
        }
}
