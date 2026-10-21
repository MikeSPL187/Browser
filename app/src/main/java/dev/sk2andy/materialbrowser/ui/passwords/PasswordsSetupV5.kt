package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.V5PromptTheme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5NameField
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StateGlyph
import dev.sk2andy.materialbrowser.ui.v5.V5StatePrimaryAction
import dev.sk2andy.materialbrowser.ui.v5.v5FieldLabel

/*
 * Setting the vault up and opening it with the phrase under v5 (canvas «ц4», approved 17.10): the
 * first visit, the 12 words, three of them typed back, the phrase typed in. The same callbacks,
 * limits and test tags as the current screens; the tablet keeps those.
 */

/** First visit: what the vault is, before anything is created. */
@Composable
internal fun PasswordsIntroV5(busy: Boolean, message: String?, onStart: () -> Unit, onBack: () -> Unit) {
    SetupPageV5(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_intro_title),
        body = null,
        error = message,
        primary = V5StateAction(
            stringResource(R.string.passwords_intro_action),
            onStart,
            enabled = !busy,
            testTag = PasswordsTestTags.Start,
        ),
        onBack = onBack,
    ) {
        V5ListGroup {
            V5ListRow(stringResource(R.string.passwords_intro_encrypted), icon = V5RowIcon(VolaIcons.Lock, V5Tile.Blue))
            V5ListRow(stringResource(R.string.passwords_intro_unlock), icon = V5RowIcon(VolaIcons.Fingerprint, V5Tile.Green))
            V5ListRow(stringResource(R.string.passwords_intro_phrase), icon = V5RowIcon(VolaIcons.Key, V5Tile.Gray))
        }
    }
}

/** The 12 words, numbered down the first column and then the second, as a written list reads. */
@Composable
internal fun RecoveryPhraseV5(words: List<String>, onDone: () -> Unit, onBack: () -> Unit) {
    val colors = V5Theme.colors
    SetupPageV5(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_phrase_title),
        body = stringResource(R.string.passwords_phrase_body),
        error = null,
        primary = V5StateAction(stringResource(R.string.passwords_phrase_done), onDone, testTag = PasswordsTestTags.PhraseDone),
        onBack = onBack,
    ) {
        val rows = (words.size + PHRASE_COLUMNS - 1) / PHRASE_COLUMNS
        Column(
            Modifier
                .padding(horizontal = V5Spacing.marginPhone)
                .fillMaxWidth()
                .clip(V5Theme.shapes.m)
                .background(colors.surface)
                .padding(V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            repeat(rows) { row ->
                Row {
                    repeat(PHRASE_COLUMNS) { column ->
                        val index = column * rows + row
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            if (index < words.size) {
                                Text(
                                    "${index + 1}",
                                    style = V5Theme.type.footnote,
                                    color = colors.labelSecondary,
                                    modifier = Modifier.width(V5Spacing.space6),
                                )
                                Text(words[index], style = V5Theme.type.headline, fontFamily = FontFamily.Monospace, color = colors.label)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Three words typed back from the note: the phrase is really written down before it is needed. */
@Composable
internal fun RecoveryConfirmV5(
    positions: List<Int>,
    wrong: Boolean,
    busy: Boolean,
    onCheck: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val answers = remember(positions) { mutableStateListOf(*Array(positions.size) { "" }) }
    val check = { if (!busy) onCheck(answers.toList()) }
    SetupPageV5(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_confirm_title),
        body = stringResource(R.string.passwords_confirm_body),
        error = if (wrong) stringResource(R.string.passwords_confirm_wrong) else null,
        primary = V5StateAction(
            stringResource(R.string.passwords_confirm_action),
            check,
            enabled = !busy,
            testTag = PasswordsTestTags.ConfirmCheck,
        ),
        onBack = onBack,
    ) {
        positions.forEachIndexed { index, position ->
            V5NameField(
                value = answers[index],
                placeholder = stringResource(R.string.passwords_confirm_word, position + 1),
                onValueChange = { answers[index] = it },
                onDone = check,
                maxLength = MAX_WORD_LENGTH,
                capitalize = false,
                // The password keyboard: no suggestions and nothing learned from a secret word.
                keyboardType = KeyboardType.Password,
                modifier = Modifier.testTag(PasswordsTestTags.confirmWord(position)),
            )
        }
    }
}

/** The device key is gone: the vault opens with the 12 words typed in. */
@Composable
internal fun RecoveryEntryV5(wrong: Boolean, busy: Boolean, onOpen: (String) -> Unit, onBack: () -> Unit) {
    var phrase by remember { mutableStateOf("") }
    val colors = V5Theme.colors
    val hint = stringResource(R.string.passwords_recover_hint)
    SetupPageV5(
        icon = VolaIcons.Key,
        title = stringResource(R.string.passwords_recover_title),
        body = null,
        error = if (wrong) stringResource(R.string.passwords_recover_wrong) else null,
        primary = V5StateAction(
            stringResource(R.string.passwords_recover_action),
            { onOpen(phrase) },
            enabled = !busy,
            testTag = PasswordsTestTags.RecoverOpen,
        ),
        onBack = onBack,
    ) {
        Box(
            Modifier
                .padding(horizontal = V5Spacing.marginPhone)
                .fillMaxWidth()
                .heightIn(min = V5Sizes.rowThreeLine)
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .padding(V5Spacing.space4),
        ) {
            // The field carries the name for TalkBack; this is only what the eye reads (СС-1).
            if (phrase.isEmpty()) Text(hint, style = V5Theme.type.body, color = colors.labelTertiary, modifier = Modifier.clearAndSetSemantics {})
            BasicTextField(
                value = phrase,
                onValueChange = { phrase = it.take(MAX_PHRASE_LENGTH) },
                textStyle = V5Theme.type.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .v5FieldLabel(hint, empty = phrase.isEmpty())
                    .testTag(PasswordsTestTags.RecoverField),
            )
        }
    }
}

/** The frame of these pages: back in the bar, the glyph, the title, a few lines, what to fill, one action. */
@Composable
private fun SetupPageV5(
    icon: ImageVector,
    title: String,
    body: String?,
    error: String?,
    primary: V5StateAction,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    V5PromptTheme {
        val colors = V5Theme.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            V5PageBar(title = "", onBack = onBack)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .widthIn(max = V5Sizes.stateContentMax)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = V5Spacing.space4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
                ) {
                    V5StateGlyph(icon, Modifier.padding(bottom = V5Spacing.space3))
                    // Groups and fields keep the side margin themselves, so only the text gets it here.
                    val side = Modifier.padding(horizontal = V5Spacing.marginPhone)
                    Text(
                        title,
                        style = V5Theme.type.title,
                        color = colors.label,
                        textAlign = TextAlign.Center,
                        modifier = side.semantics { heading() },
                    )
                    if (body != null) {
                        Text(body, style = V5Theme.type.callout, color = colors.labelSecondary, textAlign = TextAlign.Center, modifier = side)
                    }
                    content()
                    if (error != null) {
                        Text(error, style = V5Theme.type.footnote, color = colors.danger, textAlign = TextAlign.Center, modifier = side)
                    }
                    V5StatePrimaryAction(primary, side.padding(top = V5Spacing.space3))
                }
            }
        }
    }
}

private const val PHRASE_COLUMNS = 2
private const val MAX_WORD_LENGTH = 16
private const val MAX_PHRASE_LENGTH = 256
