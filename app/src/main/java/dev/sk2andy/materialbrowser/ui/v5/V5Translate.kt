package dev.sk2andy.materialbrowser.ui.v5

import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.translation.PageTranslationError
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.util.Locale

object V5TranslateTestTags {
    const val Sheet = "v5_translate_sheet"
    const val From = "v5_translate_from"
    const val To = "v5_translate_to"
    const val Always = "v5_translate_always"
    const val Translate = "v5_translate_action"
    const val Note = "v5_translate_note"
    const val Error = "v5_translate_error"
    const val Status = "v5_translate_status"
    const val RetryLanguages = "v5_translate_retry_languages"
    const val Back = "v5_translate_back"
    const val Row = "v5_translated_row"
    const val Original = "v5_translated_original"
    const val RowClose = "v5_translated_close"
    const val ShowOriginal = "v5_translate_show_original"
    const val Metered = "v5_translate_metered"
    const val LanguageSearch = "v5_translate_language_search"

    fun language(code: String) = "v5_translate_language_$code"
}

/** A language the sheet offers, by BCP 47 [code], with the [name] the engine gave it. */
@Immutable
data class V5TranslateLanguage(val code: String, val name: String)

/** Where the lists of languages are: asked for, answered, or the engine could not say. */
enum class V5TranslateLanguagesStatus { Loading, Ready, Failed }

/** What the sheet says went wrong; the engine's finer reasons fold into these three. */
enum class V5TranslateError { CouldNotTranslate, DownloadFailed, LanguageUnsupported }

/**
 * The translate sheet (V23, canvas «т1»): the page's [from] language and the [to] one with what
 * else each can be, «Always translate this language» (null where the engine keeps no such choice),
 * the bytes still to download before the first translation of the pair ([downloadBytes], null when
 * unknown), whether a translation is under way and what went wrong last.
 */
@Immutable
data class V5TranslateSheetState(
    val host: String?,
    val from: V5TranslateLanguage?,
    val to: V5TranslateLanguage?,
    val fromOptions: List<V5TranslateLanguage>,
    val toOptions: List<V5TranslateLanguage>,
    val alwaysTranslate: Boolean?,
    val downloadBytes: Long?,
    val inProgress: Boolean = false,
    val error: V5TranslateError? = null,
    val languagesStatus: V5TranslateLanguagesStatus = V5TranslateLanguagesStatus.Ready,
    /** The engine did not say how much the pair's pack weighs, so a download may come unannounced by size. */
    val downloadSizeUnknown: Boolean = false,
    /** The page is translated now: the sheet offers the original back (ПВ-2). */
    val translated: Boolean = false,
    /** «Always translate» is kept back until the pair's packs are on the phone: the sheet says so (ПВ-4). */
    val alwaysLater: Boolean = false,
    /** The phone is on a metered network: a pack to download is said to come over it (ПВ-3). */
    val metered: Boolean = false,
)

class V5TranslateSheetActions(
    val onFromChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onAlwaysTranslateChange: (Boolean) -> Unit,
    val onTranslate: () -> Unit,
    /** Asks the engine for the languages again after it could not say. */
    val onRetryLanguages: () -> Unit = {},
    /** Brings the page's own text back, also after the row above the bar was closed (ПВ-2). */
    val onShowOriginal: () -> Unit = {},
)

/**
 * Translating a page on the phone (V23, canvas «т1»), opened from the page menu. One layer: the
 * two languages, «always», one «Translate» and a line that says the text stays on the phone and,
 * before the first translation of a pair, how much the language pack weighs. Choosing a language
 * is a page of the same sheet, as «Details» is in site protection.
 */
@Composable
fun V5TranslateSheet(
    visible: Boolean,
    state: V5TranslateSheetState,
    actions: V5TranslateSheetActions,
    onDismiss: () -> Unit,
    /** A snapshot of the page under the sheet, for its glass ([V5Sheet]). */
    backdrop: Bitmap? = null,
) {
    var page by rememberSaveable(visible) { mutableStateOf(V5TranslateRules.Page.Main) }
    val title = stringResource(R.string.v5_translate_title)
    V5WindowSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = title,
        modifier = Modifier.testTag(V5TranslateTestTags.Sheet),
        backdrop = backdrop,
    ) {
        // Back steps out of a language list before it closes the sheet.
        BackHandler(enabled = page != V5TranslateRules.Page.Main) { page = V5TranslateRules.Page.Main }
        // Without system animations the pages only cross-fade.
        val animated = V5Motion.animationsEnabled
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (animated) {
                    fadeIn(V5Motion.effects.spec()) togetherWith fadeOut(V5Motion.effects.spec())
                } else {
                    fadeIn(V5Motion.reducedFade()) togetherWith fadeOut(V5Motion.reducedFade())
                }
            },
            label = "translate page",
        ) { shown ->
            Column {
                when (shown) {
                    V5TranslateRules.Page.Main -> V5SheetHeader(
                        title = title,
                        subtitle = state.host,
                        onClose = onDismiss,
                        leading = { TranslateBadge() },
                    )
                    V5TranslateRules.Page.From, V5TranslateRules.Page.To -> V5SheetHeader(
                        title = stringResource(
                            if (shown == V5TranslateRules.Page.From) R.string.v5_translate_from else R.string.v5_translate_to,
                        ),
                        onClose = onDismiss,
                        leading = { BackButton { page = V5TranslateRules.Page.Main } },
                    )
                }
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
                ) {
                    when (shown) {
                        V5TranslateRules.Page.Main -> MainPage(state, actions, onOpen = { page = it })
                        V5TranslateRules.Page.From -> LanguagePage(state.fromOptions, state.from) { code ->
                            actions.onFromChange(code)
                            page = V5TranslateRules.Page.Main
                        }
                        V5TranslateRules.Page.To -> LanguagePage(state.toOptions, state.to) { code ->
                            actions.onToChange(code)
                            page = V5TranslateRules.Page.Main
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPage(
    state: V5TranslateSheetState,
    actions: V5TranslateSheetActions,
    onOpen: (V5TranslateRules.Page) -> Unit,
) {
    val colors = V5Theme.colors
    val locale = LocalConfiguration.current.locales[0]
    V5ListGroup(
        footer = stringResource(R.string.v5_translate_always_later).takeIf { state.alwaysLater },
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(
            title = stringResource(R.string.v5_translate_from),
            value = state.from?.let { V5TranslateRules.displayName(it.name, locale) }
                ?: stringResource(R.string.v5_translate_language_unknown),
            trailing = V5RowTrailing.Chevron,
            enabled = state.fromOptions.isNotEmpty() && !state.inProgress,
            onClick = { onOpen(V5TranslateRules.Page.From) },
            modifier = Modifier.testTag(V5TranslateTestTags.From),
        )
        V5ListRow(
            title = stringResource(R.string.v5_translate_to),
            value = state.to?.let { V5TranslateRules.displayName(it.name, locale) }
                ?: stringResource(R.string.v5_translate_language_unknown),
            trailing = V5RowTrailing.Chevron,
            enabled = state.toOptions.isNotEmpty() && !state.inProgress,
            onClick = { onOpen(V5TranslateRules.Page.To) },
            modifier = Modifier.testTag(V5TranslateTestTags.To),
        )
        state.alwaysTranslate?.let { always ->
            V5ListRow(
                title = stringResource(R.string.v5_translate_always),
                trailing = V5RowTrailing.Toggle(always, actions.onAlwaysTranslateChange),
                modifier = Modifier.testTag(V5TranslateTestTags.Always),
            )
        }
    }
    Column(
        Modifier.padding(horizontal = V5Spacing.marginPhone),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        // Why «Translate» waits or cannot work, said before the button rather than left to guess.
        when (state.languagesStatus) {
            V5TranslateLanguagesStatus.Loading -> SheetLine(stringResource(R.string.v5_translate_languages_loading))
            V5TranslateLanguagesStatus.Failed -> {
                SheetLine(stringResource(R.string.v5_translate_languages_failed), colors.danger)
                TextButton(
                    onClick = actions.onRetryLanguages,
                    modifier = Modifier.fillMaxWidth().testTag(V5TranslateTestTags.RetryLanguages),
                ) {
                    Text(stringResource(R.string.action_retry), style = V5Theme.type.headline, color = colors.accent)
                }
            }
            V5TranslateLanguagesStatus.Ready -> if (V5TranslateRules.sameLanguage(state)) {
                SheetLine(stringResource(R.string.v5_translate_same_language))
            }
        }
        V5StatePrimaryAction(
            V5StateAction(
                label = stringResource(
                    if (state.inProgress) R.string.v5_translate_in_progress else R.string.v5_translate_action,
                ),
                onClick = actions.onTranslate,
                enabled = V5TranslateRules.canTranslate(state),
                testTag = V5TranslateTestTags.Translate,
            ),
        )
        if (state.translated) {
            V5StateQuietAction(
                V5StateAction(
                    label = stringResource(R.string.v5_translate_show_original),
                    onClick = actions.onShowOriginal,
                    testTag = V5TranslateTestTags.ShowOriginal,
                ),
            )
        }
        state.error?.let { error ->
            Text(
                stringResource(V5TranslateRules.errorText(error)),
                style = V5Theme.type.footnote,
                color = colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(V5TranslateTestTags.Error),
            )
        }
        val context = LocalContext.current
        Text(
            when {
                V5TranslateRules.needsDownload(state.downloadBytes) -> stringResource(
                    R.string.v5_translate_note_download,
                    Formatter.formatShortFileSize(context, requireNotNull(state.downloadBytes)),
                )
                // «Translate» may still download a pack; the note does not promise otherwise.
                state.downloadSizeUnknown -> stringResource(R.string.v5_translate_note_download_unknown)
                else -> stringResource(R.string.v5_translate_note)
            },
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag(V5TranslateTestTags.Note),
        )
        if (state.metered && (V5TranslateRules.needsDownload(state.downloadBytes) || state.downloadSizeUnknown)) {
            Text(
                stringResource(R.string.v5_translate_note_metered),
                style = V5Theme.type.footnote,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag(V5TranslateTestTags.Metered),
            )
        }
    }
}

/** A short centered line above «Translate»: what it waits for, or why it cannot work. */
@Composable
private fun SheetLine(text: String, color: Color = V5Theme.colors.labelSecondary) {
    Text(
        text,
        style = V5Theme.type.footnote,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(V5TranslateTestTags.Status),
    )
}

/** The languages one side can be, the chosen one checked; a touch picks it and goes back. */
@Composable
private fun LanguagePage(
    options: List<V5TranslateLanguage>,
    chosen: V5TranslateLanguage?,
    onChoose: (String) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    // A long list gets a search, so a language is typed rather than scrolled for (ПВ-6).
    var query by rememberSaveable { mutableStateOf("") }
    if (options.size > V5TranslateRules.SEARCH_FROM) {
        V5SearchField(
            query = query,
            onQueryChange = { query = it },
            hint = stringResource(R.string.v5_translate_language_search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = V5Spacing.marginPhone),
            fieldModifier = Modifier.testTag(V5TranslateTestTags.LanguageSearch),
        )
    }
    val shown = V5TranslateRules.matching(options, query) { V5TranslateRules.displayName(it.name, locale) }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        shown.forEach { language ->
            V5ListRow(
                title = V5TranslateRules.displayName(language.name, locale),
                trailing = if (language.code == chosen?.code) V5RowTrailing.Check else V5RowTrailing.None,
                onClick = { onChoose(language.code) },
                modifier = Modifier.testTag(V5TranslateTestTags.language(language.code)),
            )
        }
    }
}

/** The sheet's glyph: the menu's translate glyph on the blue settings tile. */
@Composable
private fun TranslateBadge() {
    Box(
        Modifier
            .size(V5Sizes.rowTile + V5Spacing.space2)
            .clip(V5Theme.shapes.s)
            .background(V5Tile.Blue.color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            VolaIcons.Language,
            contentDescription = null,
            tint = V5Tile.Blue.glyph,
            modifier = Modifier.size(V5Sizes.iconSmall),
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.heightIn(min = V5Sizes.touchTarget).testTag(V5TranslateTestTags.Back),
    ) {
        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = V5Theme.colors.label)
    }
}

/** The translated page's row (canvas «т2»): which languages, and the way back to the original. */
@Immutable
data class V5TranslatedRowState(val from: String, val to: String)

/**
 * The row above the bar while the page is translated (V23, canvas «т2»), in the bar's colors like
 * «Listen»: «Translated: English → Russian», «Original» and close. Close only hides the row; the
 * page stays translated until «Original» or a new page.
 */
@Composable
fun V5TranslatedRow(
    state: V5TranslatedRowState,
    onOriginal: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paneTitle = stringResource(R.string.v5_translate_title)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .padding(start = V5Spacing.space4, end = V5Spacing.space1)
            .semantics { this.paneTitle = paneTitle }
            .testTag(V5TranslateTestTags.Row),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(VolaIcons.Language, contentDescription = null)
        Text(
            // Names inside a sentence as the engine writes them in the app language: «английский».
            text = stringResource(R.string.v5_translated_from_to, state.from, state.to),
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onOriginal,
            modifier = Modifier.heightIn(min = V5Sizes.touchTarget).testTag(V5TranslateTestTags.Original),
        ) {
            Text(
                stringResource(R.string.v5_translate_original),
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag(V5TranslateTestTags.RowClose)) {
            Icon(VolaIcons.Close, contentDescription = stringResource(R.string.v5_translated_hide))
        }
    }
}

/** [V5TranslatedRow] that unfolds above the bar on the sheet spring; without animations it only fades. */
@Composable
fun V5TranslatedAccessory(
    state: V5TranslatedRowState?,
    onOriginal: () -> Unit,
    onClose: () -> Unit,
) {
    // The last state stays on screen while the row folds away.
    val last = remember { LastTranslatedRow() }
    if (state != null) last.state = state
    val animated = V5Motion.animationsEnabled
    AnimatedVisibility(
        visible = state != null,
        enter = if (animated) {
            expandVertically(V5Motion.sheet.spec()) + fadeIn(V5Motion.effects.spec())
        } else {
            fadeIn(V5Motion.reducedFade())
        },
        exit = if (animated) {
            shrinkVertically(V5Motion.sheet.spec()) + fadeOut(V5Motion.effects.spec())
        } else {
            fadeOut(V5Motion.reducedFade())
        },
    ) {
        last.state?.let { V5TranslatedRow(state = it, onOriginal = onOriginal, onClose = onClose) }
    }
}

private class LastTranslatedRow {
    var state: V5TranslatedRowState? = null
}

/** Pure decisions of the translate screens, for unit tests. */
internal object V5TranslateRules {
    enum class Page { Main, From, To }

    /** From this many languages on, a list gets its search field. */
    const val SEARCH_FROM = 12

    /** The languages whose name or code holds [query], ignoring case; all for a blank query. */
    fun <T : Any> matching(options: List<T>, query: String, name: (T) -> String): List<T> {
        val needle = query.trim()
        if (needle.isEmpty()) return options
        return options.filter { option -> name(option).contains(needle, ignoreCase = true) }
    }

    /** «Translate» works once both languages are known, differ, and nothing is under way. */
    fun canTranslate(state: V5TranslateSheetState): Boolean =
        !state.inProgress && state.from != null && state.to != null && state.from.code != state.to.code

    /** Both languages are the same: nothing to translate, and the sheet says so. */
    fun sameLanguage(state: V5TranslateSheetState): Boolean =
        state.from != null && state.to != null && state.from.code == state.to.code

    /** Whether the note names a download: the pair's pack is not on the phone yet. */
    fun needsDownload(downloadBytes: Long?): Boolean = downloadBytes != null && downloadBytes > 0

    /** The sheet's message for an engine [error]; null for errors the sheet does not show. */
    fun error(error: PageTranslationError?): V5TranslateError? = when (error) {
        null, PageTranslationError.ModelDownloadRequired -> null
        PageTranslationError.ModelDownloadFailed -> V5TranslateError.DownloadFailed
        PageTranslationError.LanguageNotSupported, PageTranslationError.Unsupported -> V5TranslateError.LanguageUnsupported
        else -> V5TranslateError.CouldNotTranslate
    }

    fun errorText(error: V5TranslateError): Int = when (error) {
        V5TranslateError.CouldNotTranslate -> R.string.v5_translate_error
        V5TranslateError.DownloadFailed -> R.string.v5_translate_error_download
        V5TranslateError.LanguageUnsupported -> R.string.v5_translate_error_language
    }

    /** A language name standing alone, as in a list row: first letter up («английский» → «Английский»). */
    fun displayName(name: String, locale: Locale): String = name.replaceFirstChar { it.titlecase(locale) }
}
