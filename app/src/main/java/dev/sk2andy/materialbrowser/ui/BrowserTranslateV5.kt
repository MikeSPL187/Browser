package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.translation.OnDeviceTranslationUiState
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationResult
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguage
import dev.sk2andy.materialbrowser.browser.translation.TranslationLanguages
import dev.sk2andy.materialbrowser.browser.translation.TranslationPair
import dev.sk2andy.materialbrowser.ui.theme.v5.rememberV5TranslateInto
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateLanguage
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateRules
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateSheet
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateSheetActions
import dev.sk2andy.materialbrowser.ui.v5.V5TranslateSheetState
import dev.sk2andy.materialbrowser.ui.v5.V5TranslatedAccessory
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
    LaunchedEffect(visible) {
        if (visible && languages == null) translation.languages { languages = it }
    }
    val preferredTarget = rememberV5TranslateInto().ifEmpty { null }
    val initial = BrowserTranslateRules.initialPair(ui, translation.suggestedPair(tab.id), preferredTarget, locale.language)
    var fromCode by rememberSaveable(visible, tab.id) { mutableStateOf(initial.from) }
    var toCode by rememberSaveable(visible, tab.id) { mutableStateOf(initial.to) }
    var downloadBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(visible, fromCode, toCode) {
        downloadBytes = null
        val from = fromCode
        val to = toCode
        if (visible && from != null && to != null && from != to) {
            // An answer for a pair the user has since changed is dropped.
            translation.downloadSize(TranslationPair(from, to)) { bytes ->
                if (fromCode == from && toCode == to) downloadBytes = bytes
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
        // The engine port keeps no «always translate this language» choice yet: the row stays hidden.
        alwaysTranslate = null,
        downloadBytes = downloadBytes,
        inProgress = ui.inProgress,
        error = V5TranslateRules.error(ui.error),
    )
    V5TranslateSheet(
        visible = visible,
        state = state,
        actions = V5TranslateSheetActions(
            onFromChange = { fromCode = it },
            onToChange = { toCode = it },
            onAlwaysTranslateChange = {},
            onTranslate = {
                val from = fromCode
                val to = toCode
                if (from != null && to != null) {
                    // The note named the pack's size, so «Translate» is the consent to download it.
                    translation.translate(tab.id, TranslationPair(from, to), allowModelDownload = true) { result ->
                        if (result == PageTranslationResult.Done) onDismiss()
                    }
                }
            },
        ),
        onDismiss = onDismiss,
    )
}

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

/** Pure decisions of the translate binding, for unit tests. */
internal object BrowserTranslateRules {
    data class Codes(val from: String?, val to: String?)

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
