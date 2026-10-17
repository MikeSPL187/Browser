package dev.sk2andy.materialbrowser.ui.v5

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import java.util.Locale

object V5TranslationSettingsTestTags {
    const val Page = "v5_translation_settings"
    const val Offer = "v5_translation_offer"
    const val TranslateInto = "v5_translation_into"
    const val NeverTranslate = "v5_translation_never"
    const val IntoPage = "v5_translation_into_page"
    const val NeverPage = "v5_translation_never_page"

    fun pack(code: String) = "v5_translation_pack_$code"

    fun into(code: String) = "v5_translation_into_$code"

    fun never(code: String) = "v5_translation_never_$code"
}

/** One language pack: the language's models on the phone, or ready to download. */
@Immutable
data class V5LanguagePack(
    val code: String,
    val name: String,
    val sizeBytes: Long,
    val downloaded: Boolean,
    val downloading: Boolean = false,
)

/**
 * Settings → Translation (V23, canvas «т3»): whether the menu offers translation, the language
 * pages are translated into (by name, [translateIntoCode] checked among [intoOptions]), the
 * languages never to translate (null where the engine keeps no such list: the row is hidden), and
 * the language packs. Null [packs] while the engine has not listed them.
 */
@Immutable
data class V5TranslationSettingsState(
    val offerInMenu: Boolean,
    val translateInto: String,
    val neverTranslate: List<String>?,
    val packs: List<V5LanguagePack>?,
    val translateIntoCode: String? = null,
    val intoOptions: List<V5TranslateLanguage> = emptyList(),
    val neverOptions: List<V5TranslateLanguage> = emptyList(),
    val neverCodes: Set<String> = emptySet(),
)

class V5TranslationSettingsActions(
    val onBack: () -> Unit,
    val onOfferChange: (Boolean) -> Unit,
    val onTranslateInto: (String) -> Unit,
    val onNeverTranslate: (code: String, never: Boolean) -> Unit,
    val onDownload: (String) -> Unit,
    val onDelete: (String) -> Unit,
)

/**
 * The translation settings page. One layer: three rows, then the packs with their sizes; a
 * downloaded pack is deleted by a swipe to the left (or TalkBack's «Delete»), as a library row is.
 * The footer says plainly that translation stays on the phone and what private tabs do.
 */
@Composable
fun V5TranslationSettings(
    state: V5TranslationSettingsState,
    actions: V5TranslationSettingsActions,
    modifier: Modifier = Modifier,
) {
    // «Translate into» and «Never translate» are pages of their own, as a language list is in the
    // translate sheet.
    var page by rememberSaveable { mutableStateOf(V5TranslationSettingsRules.Page.Main) }
    when (page) {
        V5TranslationSettingsRules.Page.Main -> Unit
        V5TranslationSettingsRules.Page.Into -> {
            BackHandler { page = V5TranslationSettingsRules.Page.Main }
            V5TranslationIntoPage(state, onBack = { page = V5TranslationSettingsRules.Page.Main }) { code ->
                actions.onTranslateInto(code)
                page = V5TranslationSettingsRules.Page.Main
            }
            return
        }
        V5TranslationSettingsRules.Page.Never -> {
            BackHandler { page = V5TranslationSettingsRules.Page.Main }
            V5TranslationNeverPage(state, onBack = { page = V5TranslationSettingsRules.Page.Main }, onChange = actions.onNeverTranslate)
            return
        }
    }
    val colors = V5Theme.colors
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5TranslationSettingsTestTags.Page),
    ) {
        V5PageBar(title = stringResource(R.string.v5_translation_settings_title), onBack = actions.onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                V5ListRow(
                    title = stringResource(R.string.v5_translation_offer),
                    subtitle = stringResource(R.string.v5_translation_offer_hint),
                    trailing = V5RowTrailing.Toggle(state.offerInMenu, actions.onOfferChange),
                    modifier = Modifier.testTag(V5TranslationSettingsTestTags.Offer),
                )
                V5ListRow(
                    title = stringResource(R.string.v5_translation_into),
                    value = V5TranslateRules.displayName(state.translateInto, locale),
                    trailing = V5RowTrailing.Chevron,
                    enabled = state.intoOptions.isNotEmpty(),
                    onClick = { page = V5TranslationSettingsRules.Page.Into },
                    modifier = Modifier.testTag(V5TranslationSettingsTestTags.TranslateInto),
                )
                state.neverTranslate?.let { never ->
                    V5ListRow(
                        title = stringResource(R.string.v5_translation_never),
                        value = V5TranslationSettingsRules.neverSummary(never, locale)
                            ?: stringResource(R.string.v5_translation_never_none),
                        trailing = V5RowTrailing.Chevron,
                        enabled = state.neverOptions.isNotEmpty(),
                        onClick = { page = V5TranslationSettingsRules.Page.Never },
                        modifier = Modifier.testTag(V5TranslationSettingsTestTags.NeverTranslate),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                Text(
                    stringResource(
                        R.string.v5_translation_packs,
                        Formatter.formatShortFileSize(context, V5TranslationSettingsRules.downloadedBytes(state.packs)),
                    ),
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                )
                Column {
                    V5TranslationSettingsRules.ordered(state.packs).forEach { pack ->
                        PackRow(pack, locale, actions)
                    }
                }
                Text(
                    stringResource(R.string.v5_translation_footer),
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                )
            }
        }
    }
}

/** The languages pages can be translated into, the chosen one checked; a touch picks it and goes back. */
@Composable
internal fun V5TranslationIntoPage(state: V5TranslationSettingsState, onBack: () -> Unit, onChoose: (String) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier
            .fillMaxSize()
            .background(V5Theme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5TranslationSettingsTestTags.IntoPage),
    ) {
        V5PageBar(title = stringResource(R.string.v5_translation_into), onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
        ) {
            V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                state.intoOptions.forEach { language ->
                    V5ListRow(
                        title = V5TranslateRules.displayName(language.name, locale),
                        trailing = if (language.code == state.translateIntoCode) V5RowTrailing.Check else V5RowTrailing.None,
                        onClick = { onChoose(language.code) },
                        modifier = Modifier.testTag(V5TranslationSettingsTestTags.into(language.code)),
                    )
                }
            }
        }
    }
}

/**
 * The languages whose pages are never offered a translation, each with its own switch: unlike
 * «Translate into», several can be on at once.
 */
@Composable
internal fun V5TranslationNeverPage(
    state: V5TranslationSettingsState,
    onBack: () -> Unit,
    onChange: (code: String, never: Boolean) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier
            .fillMaxSize()
            .background(V5Theme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5TranslationSettingsTestTags.NeverPage),
    ) {
        V5PageBar(title = stringResource(R.string.v5_translation_never), onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                state.neverOptions.forEach { language ->
                    V5ListRow(
                        title = V5TranslateRules.displayName(language.name, locale),
                        trailing = V5RowTrailing.Toggle(language.code in state.neverCodes) { on -> onChange(language.code, on) },
                        modifier = Modifier.testTag(V5TranslationSettingsTestTags.never(language.code)),
                    )
                }
            }
            Text(
                stringResource(R.string.v5_translation_never_footer),
                style = V5Theme.type.footnote,
                color = V5Theme.colors.labelSecondary,
                modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
            )
        }
    }
}

/** A pack as a card of its own, as the library's rows are, so a swipe can take it away. */
@Composable
private fun PackRow(pack: V5LanguagePack, locale: Locale, actions: V5TranslationSettingsActions) {
    val colors = V5Theme.colors
    val context = LocalContext.current
    val size = Formatter.formatShortFileSize(context, pack.sizeBytes)
    val deleteLabel = stringResource(R.string.v5_translation_pack_delete)
    val downloadLabel = stringResource(R.string.v5_translation_pack_download)
    val content: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = V5Spacing.marginPhone)
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .heightIn(min = V5Sizes.rowTwoLine)
                .then(
                    if (!pack.downloaded && !pack.downloading) {
                        Modifier.clickable(role = Role.Button, onClickLabel = downloadLabel) { actions.onDownload(pack.code) }
                    } else {
                        Modifier
                    },
                )
                .semantics(mergeDescendants = true) {
                    if (pack.downloaded) {
                        customActions = listOf(CustomAccessibilityAction(deleteLabel) { actions.onDelete(pack.code); true })
                    }
                }
                .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2)
                .testTag(V5TranslationSettingsTestTags.pack(pack.code)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    V5TranslateRules.displayName(pack.name, locale),
                    style = V5Theme.type.body,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        pack.downloading -> stringResource(R.string.v5_translation_pack_downloading, size)
                        pack.downloaded -> stringResource(R.string.v5_translation_pack_downloaded, size)
                        else -> size
                    },
                    style = V5Theme.type.footnote,
                    color = colors.labelSecondary,
                )
            }
            when {
                pack.downloaded -> Icon(
                    VolaIcons.CheckCircle,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.padding(start = V5Spacing.space1),
                )
                !pack.downloading -> Text(
                    downloadLabel,
                    style = V5Theme.type.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                )
            }
        }
    }
    Box(Modifier.padding(vertical = V5Spacing.hairline)) {
        if (pack.downloaded) {
            V5SwipeToDelete(key = pack.code, onDelete = { actions.onDelete(pack.code) }, content = content)
        } else {
            content()
        }
    }
}

/** Pure decisions of the translation settings, for unit tests. */
internal object V5TranslationSettingsRules {
    enum class Page { Main, Into, Never }

    /** Downloaded packs first, then by name, so what takes space is on top. */
    fun ordered(packs: List<V5LanguagePack>?): List<V5LanguagePack> =
        packs.orEmpty().sortedWith(compareByDescending<V5LanguagePack> { it.downloaded }.thenBy { it.name })

    /** The space the downloaded packs take, for the section header. */
    fun downloadedBytes(packs: List<V5LanguagePack>?): Long = packs.orEmpty().filter { it.downloaded }.sumOf { it.sizeBytes }

    /** The «Never translate» value: one language by name, more as «first +N»; null for none. */
    fun neverSummary(languages: List<String>, locale: Locale): String? = when (languages.size) {
        0 -> null
        1 -> V5TranslateRules.displayName(languages.first(), locale)
        else -> "${V5TranslateRules.displayName(languages.first(), locale)} +${languages.size - 1}"
    }
}
