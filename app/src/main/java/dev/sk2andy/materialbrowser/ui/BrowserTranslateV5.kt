package dev.sk2andy.materialbrowser.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.translation.OnDeviceTranslationRules
import dev.sk2andy.materialbrowser.browser.translation.OnDeviceTranslationUiState
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationError
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationResult
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguageSetting
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguages
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import dev.sk2andy.materialbrowser.ui.theme.v5.V5TranslateOfferSetting
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5Preference
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5TranslateInto
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateLanguage
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateLanguagesStatus
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateRules
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateSheet
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateSheetActions
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateSheetState
import dev.sk2andy.materialbrowser.ui.v5.V5TranslatedAccessory
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateOffer
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateOfferState
import dev.sk2andy.materialbrowser.ui.v5.V5TranslatedRowState
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain
import java.util.Locale

/*
 * The v5 translate screens (V23) bound to the selected tab: the sheet the page menu opens and the
 * row above the bar while the page is translated. Only an engine that translates on the phone
 * (capabilities.onDeviceTranslation) gets them; System WebView keeps the provider translation.
 */

/** The translate sheet for [tab]'s page: languages from the engine, the pack's size before the first use. */
@Composable
internal fun BrowserTranslateSheetV5(
    controller: BrowserController,
    tab: BrowserTab,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val translation = controller.onDeviceTranslation
    val ui = translation.stateOf(tab.id)
    val locale = LocalConfiguration.current.locales[0]
    var languages by remember { mutableStateOf<TranslationLanguages?>(null) }
    var languagesFailed by remember { mutableStateOf(false) }
    var languagesAsked by remember { mutableIntStateOf(0) }
    // Asked each time the sheet opens, as a pack downloaded or the app's language changed since may
    // change the lists (ПВ-5); the last answer shows meanwhile.
    LaunchedEffect(visible, languagesAsked) {
        if (visible) {
            languagesFailed = false
            translation.languages { answer ->
                if (answer != null) languages = answer
                languagesFailed = answer == null && languages == null
            }
        }
    }
    val context = LocalContext.current
    val sheetOpen by rememberUpdatedState(visible)
    val preferredTarget = rememberV5TranslateInto().ifEmpty { null }
    val initial = BrowserTranslateRules.initialPair(ui, translation.suggestedPair(tab.id), preferredTarget, locale.language)
    var fromCode by rememberSaveable(visible, tab.id) { mutableStateOf(initial.from) }
    var toCode by rememberSaveable(visible, tab.id) { mutableStateOf(initial.to) }
    // The engine may name the page's language after the sheet opened: an empty side takes it then.
    LaunchedEffect(visible, initial.from) { if (visible && fromCode == null) fromCode = initial.from }
    LaunchedEffect(visible, initial.to) { if (visible && toCode == null) toCode = initial.to }
    var downloadBytes by remember { mutableStateOf<Long?>(null) }
    var downloadSizeUnknown by remember { mutableStateOf(false) }
    var languageSettings by remember { mutableStateOf<Map<String, TranslationLanguageSetting>?>(null) }
    var settingsRead by remember { mutableIntStateOf(0) }
    LaunchedEffect(visible, settingsRead) {
        if (visible) translation.models.languageSettings { languageSettings = it }
    }
    LaunchedEffect(visible, fromCode, toCode) {
        downloadBytes = null
        downloadSizeUnknown = false
        val from = fromCode
        val to = toCode
        if (visible && from != null && to != null && from != to) {
            // An answer for a pair the user has since changed is dropped.
            translation.downloadSize(TranslationPair(from, to)) { bytes ->
                if (fromCode == from && toCode == to) {
                    downloadBytes = bytes
                    downloadSizeUnknown = bytes == null
                }
            }
        }
    }
    val name = { code: String -> BrowserTranslateRules.languageName(code, locale) }
    val fromOptions = languages?.from.orEmpty().map { BrowserTranslateRules.option(it, name) }
    val toOptions = languages?.to.orEmpty().map { BrowserTranslateRules.option(it, name) }
    val state = V5TranslateSheetState(
        host = v5BarDomain(tab.url),
        from = fromCode?.let { BrowserTranslateRules.pick(it, fromOptions, name) },
        to = toCode?.let { BrowserTranslateRules.pick(it, toOptions, name) },
        fromOptions = fromOptions,
        toOptions = toOptions,
        alwaysTranslate = BrowserTranslateRules.always(fromCode, languageSettings, downloadBytes),
        downloadBytes = downloadBytes,
        inProgress = ui.inProgress,
        // An error belongs to the pair it was asked for; another pair chosen since starts clean.
        error = V5TranslateRules.error(ui.error).takeIf { BrowserTranslateRules.sameRequest(ui.pair, fromCode, toCode) },
        languagesStatus = BrowserTranslateRules.languagesStatus(languages, languagesFailed),
        downloadSizeUnknown = downloadSizeUnknown,
        translated = ui.isTranslated,
        alwaysLater = fromCode != null && languageSettings != null &&
            BrowserTranslateRules.always(fromCode, languageSettings, downloadBytes) == null,
        metered = remember(visible) { context.isOnMeteredNetwork() },
    )
    V5TranslateSheet(
        visible = visible,
        state = state,
        backdrop = rememberPageBackdrop(controller, tab, visible),
        actions = V5TranslateSheetActions(
            onFromChange = { fromCode = it },
            onToChange = { toCode = it },
            onAlwaysTranslateChange = { on ->
                fromCode?.let { code ->
                    val setting = if (on) TranslationLanguageSetting.Always else TranslationLanguageSetting.Offer
                    // A refusal (packs missing) leaves the switch as the engine reads it back.
                    translation.models.setLanguageSetting(code, setting) { settingsRead++ }
                }
            },
            onTranslate = {
                val from = fromCode
                val to = toCode
                if (from != null && to != null) {
                    // The note named the pack's size, or said a pack may download, so «Translate» is the
                    // consent to download it.
                    translation.translate(tab.id, TranslationPair(from, to), allowModelDownload = true) { result ->
                        if (result == PageTranslationResult.Done) {
                            onDismiss()
                        } else if (!sheetOpen) {
                            // The sheet was closed while the pack downloaded: the failure is said where
                            // the user is, not kept for the next opening (ПВ-1).
                            V5TranslateRules.error(translation.stateOf(tab.id).error)?.let { error ->
                                V5Feedback.show(context, V5TranslateRules.errorText(error))
                            }
                        }
                    }
                }
            },
            onRetryLanguages = { languagesAsked++ },
            onShowOriginal = {
                translation.showOriginal(tab.id)
                onDismiss()
            },
        ),
        onDismiss = onDismiss,
    )
}

/** Whether the phone's network is metered: mobile data, or a hotspot marked so. */
private fun Context.isOnMeteredNetwork(): Boolean =
    getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == true

/** «Translated: English → Russian» above the bar while [tab]'s page is translated. */
@Composable
internal fun BrowserTranslatedAccessory(controller: BrowserController, tab: BrowserTab) {
    if (!controller.browserEngineCapabilities.onDeviceTranslation) return
    val translation = controller.onDeviceTranslation
    val ui = translation.stateOf(tab.id)
    val locale = LocalConfiguration.current.locales[0]
    var hidden by remember(tab.id) { mutableStateOf<TranslationPair?>(null) }
    // Close hides the row for this translation only; the next one shows it again.
    LaunchedEffect(ui.isTranslated) { if (!ui.isTranslated) hidden = null }
    val pair = BrowserTranslateRules.rowPair(ui, hidden)
    V5TranslatedAccessory(
        state = pair?.let {
            V5TranslatedRowState(
                from = BrowserTranslateRules.languageName(it.from, locale),
                to = BrowserTranslateRules.languageName(it.to, locale),
            )
        },
        onOriginal = { translation.showOriginal(tab.id) },
        onClose = { hidden = pair },
    )
}

/**
 * Vola's offer to translate [tab]'s page (canvas «Translate»), when the engine offers it and
 * Settings → Translation leaves «Offer translation» on. «Translate» translates at once; when the
 * language pack is not on the phone yet the sheet opens to say its size first. «Not for this site»
 * tells the engine; close puts the offer away for this page.
 */
@Composable
internal fun BrowserTranslateOfferV5(controller: BrowserController, tab: BrowserTab, modifier: Modifier = Modifier) {
    if (!controller.browserEngineCapabilities.onDeviceTranslation) return
    val offerOn by rememberV5Preference(V5TranslateOfferSetting)
    val translation = controller.onDeviceTranslation
    val ui = translation.stateOf(tab.id)
    val pair = translation.suggestedPair(tab.id)
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    var sheet by remember(tab.id) { mutableStateOf(false) }
    V5TranslateOffer(
        state = pair?.takeIf { offerOn && BrowserTranslateRules.showsOffer(ui, it) }?.let {
            V5TranslateOfferState(
                to = BrowserTranslateRules.languageName(it.to, locale),
                from = BrowserTranslateRules.languageName(it.from, locale),
            )
        },
        onTranslate = {
            if (pair != null) {
                translation.translate(tab.id, pair, allowModelDownload = false) { result ->
                    val error = (result as? PageTranslationResult.Failed)?.error
                    if (error == PageTranslationError.ModelDownloadRequired) {
                        sheet = true
                    } else {
                        V5TranslateRules.error(error)?.let { V5Feedback.show(context, V5TranslateRules.errorText(it)) }
                    }
                }
            }
        },
        onNeverOnSite = { translation.neverOfferOnSite(tab.id) },
        onClose = { translation.dismissOffer(tab.id) },
        modifier = modifier,
    )
    BrowserTranslateSheetV5(controller = controller, tab = tab, visible = sheet, onDismiss = { sheet = false })
}

/** Pure decisions of the translate binding, for unit tests. */
internal object BrowserTranslateRules {
    /** The offer card shows while the engine's offer waits and the page is neither translated nor being translated. */
    fun showsOffer(ui: OnDeviceTranslationUiState, pair: TranslationPair): Boolean =
        ui.offered && !ui.inProgress && OnDeviceTranslationRules.shouldOffer(ui.page, pair, neverTranslateSite = false)

    data class Codes(val from: String?, val to: String?)

    /** Whether the last request's [pair] is the one the sheet now shows. */
    fun sameRequest(pair: TranslationPair?, fromCode: String?, toCode: String?): Boolean =
        pair != null && pair.from == fromCode && pair.to == toCode

    /** The languages lists: answered, still asked for, or the engine could not say ([failed]). */
    fun languagesStatus(languages: TranslationLanguages?, failed: Boolean): V5TranslateLanguagesStatus = when {
        languages != null -> V5TranslateLanguagesStatus.Ready
        failed -> V5TranslateLanguagesStatus.Failed
        else -> V5TranslateLanguagesStatus.Loading
    }

    /** Whether «Translate» opens the on-device sheet: the engine translates on the phone and a web page is open. */
    fun usesOnDevice(onDeviceTranslation: Boolean, url: String): Boolean =
        onDeviceTranslation && (url.startsWith("https://") || url.startsWith("http://"))

    /**
     * The languages the sheet opens with: the pair asked for last, else the page's language as the
     * engine suggests or detected it into [preferredTarget] (Settings → Translation), else into the
     * language the engine suggests, the user reads, or the app's, at worst.
     */
    fun initialPair(
        ui: OnDeviceTranslationUiState,
        suggested: TranslationPair?,
        preferredTarget: String?,
        appLanguage: String,
    ): Codes {
        val last = ui.pair ?: ui.page.requested
        return Codes(
            from = last?.from ?: suggested?.from ?: ui.page.pageLanguage,
            to = last?.to ?: preferredTarget ?: suggested?.to ?: ui.page.userLanguage ?: appLanguage,
        )
    }

    /** Whether the menu offers translating a page in [pageLanguage]: not when the user set it to «never». */
    fun offers(pageLanguage: String?, settings: Map<String, TranslationLanguageSetting>?): Boolean =
        pageLanguage == null || settings?.get(pageLanguage) != TranslationLanguageSetting.Never

    /**
     * «Always translate this language» in the sheet: on when it is set; offered to turn on only when
     * the pair's packs are on the phone ([downloadBytes] is 0), since the engine would otherwise
     * download them by itself, without asking. Null hides the row.
     */
    fun always(fromCode: String?, settings: Map<String, TranslationLanguageSetting>?, downloadBytes: Long?): Boolean? {
        if (fromCode == null || settings == null) return null
        return when {
            settings[fromCode] == TranslationLanguageSetting.Always -> true
            downloadBytes == 0L -> false
            else -> null
        }
    }

    /** The pair the row names: only while the page is translated, and not the one the user closed. */
    fun rowPair(ui: OnDeviceTranslationUiState, hidden: TranslationPair?): TranslationPair? =
        (ui.pair ?: ui.page.requested)?.takeIf { ui.isTranslated && it != hidden }

    /** A language as the sheet lists it: the engine's name, or the system's for its tag. */
    fun option(language: TranslationLanguage, name: (String) -> String): V5TranslateLanguage =
        V5TranslateLanguage(language.code, language.displayName?.takeIf(String::isNotBlank) ?: name(language.code))

    /** [code] among [options]; a language the lists lack still shows by its system name. */
    fun pick(code: String, options: List<V5TranslateLanguage>, name: (String) -> String): V5TranslateLanguage =
        options.firstOrNull { it.code == code } ?: V5TranslateLanguage(code, name(code))

    /** The language's name in the app language, as inside a sentence («английский»); the tag when unknown. */
    fun languageName(code: String, locale: Locale): String =
        Locale.forLanguageTag(code).getDisplayLanguage(locale).ifBlank { code }
}
