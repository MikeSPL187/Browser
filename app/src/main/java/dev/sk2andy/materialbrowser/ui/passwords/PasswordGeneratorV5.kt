package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorMode
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorRules
import dev.sk2andy.materialbrowser.shared.credentials.PasswordStrength
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5Segmented
import dev.sk2andy.materialbrowser.ui.v5.V5Slider

/**
 * «Strong password» under v5 (canvas «ц4»): the prompt sheet with the value, how strong it is, the
 * v5 slider for length or words, the two switches, and «Use» as the sheet's one action. The same
 * [PasswordGeneratorState] as before, so values, options and the test tags do not change.
 */
@Composable
internal fun PasswordGeneratorSheetV5(
    visible: Boolean,
    subtitle: String?,
    useLabel: String,
    useNeedsFingerprint: Boolean,
    onUse: (String) -> Unit,
    onDismiss: () -> Unit,
    state: PasswordGeneratorState = rememberPasswordGeneratorState(),
) {
    V5PromptSheet(
        visible = visible,
        title = stringResource(R.string.passwords_generator_title),
        message = subtitle,
        actions = listOf(
            V5PromptAction(
                label = useLabel,
                onClick = { onUse(state.value) },
                role = V5PromptRole.Primary,
                testTag = PasswordGeneratorTestTags.Use,
                icon = if (useNeedsFingerprint) VolaIcons.Fingerprint else null,
            ),
        ),
        onDismiss = onDismiss,
    ) {
        PasswordGeneratorBodyV5(state)
    }
}

@Composable
private fun PasswordGeneratorBodyV5(state: PasswordGeneratorState) {
    val context = LocalContext.current
    val colors = V5Theme.colors
    val options = state.options
    val phrase = options.mode == PasswordGeneratorMode.Phrase
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space3)) {
        if (state.phraseAvailable) {
            val modes = PasswordGeneratorMode.entries
            V5Segmented(
                options = modes.map { mode ->
                    stringResource(
                        when (mode) {
                            PasswordGeneratorMode.Password -> R.string.passwords_generator_password
                            PasswordGeneratorMode.Phrase -> R.string.passwords_generator_phrase
                        },
                    )
                },
                selectedIndex = modes.indexOf(options.mode),
                onSelect = { index -> state.update(options.copy(mode = modes[index])) },
                modifier = Modifier.padding(horizontal = V5Spacing.marginPhone),
            )
        }
        Row(
            Modifier
                .padding(horizontal = V5Spacing.marginPhone)
                .fillMaxWidth()
                .clip(V5Theme.shapes.m)
                .background(colors.surfaceElevated)
                .padding(start = V5Spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.value,
                style = V5Theme.type.headline,
                fontFamily = FontFamily.Monospace,
                color = colors.label,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = V5Spacing.space3)
                    .testTag(PasswordGeneratorTestTags.Value),
            )
            IconButton(onClick = state::regenerate, modifier = Modifier.testTag(PasswordGeneratorTestTags.Refresh)) {
                Icon(VolaIcons.Refresh, contentDescription = stringResource(R.string.passwords_generator_new), tint = colors.accent)
            }
            IconButton(onClick = { SensitiveClipboard.copy(context, state.value) }) {
                Icon(VolaIcons.ContentCopy, contentDescription = stringResource(R.string.passwords_copy_password), tint = colors.accent)
            }
        }
        Text(
            stringResource(state.strength.labelRes()),
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = state.strength.colorV5(),
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
        )
        V5ListGroup {
            val count = if (phrase) options.words else options.length
            val range = if (phrase) {
                PasswordGeneratorRules.MIN_WORDS..PasswordGeneratorRules.MAX_WORDS
            } else {
                PasswordGeneratorRules.MIN_LENGTH..PasswordGeneratorRules.MAX_LENGTH
            }
            // The slider follows the finger; the value is made again when it lifts.
            var shown by remember(phrase, count) { mutableIntStateOf(count) }
            V5ListRow(
                title = stringResource(if (phrase) R.string.passwords_generator_words else R.string.passwords_generator_length),
                value = shown.toString(),
            )
            V5Slider(
                value = shown.toFloat(),
                onValueChange = { shown = it.toInt() },
                onValueChangeFinished = {
                    state.update(if (phrase) options.copy(words = shown) else options.copy(length = shown))
                },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
                modifier = Modifier.padding(horizontal = V5Spacing.space4),
            )
            if (!phrase) {
                V5ListRow(
                    title = stringResource(R.string.passwords_generator_digits_symbols),
                    trailing = V5RowTrailing.Toggle(options.digitsAndSymbols, { state.update(options.copy(digitsAndSymbols = it)) }),
                    modifier = Modifier.testTag(PasswordGeneratorTestTags.DigitsAndSymbols),
                )
                V5ListRow(
                    title = stringResource(R.string.passwords_generator_avoid_ambiguous),
                    trailing = V5RowTrailing.Toggle(options.avoidAmbiguous, { state.update(options.copy(avoidAmbiguous = it)) }),
                    modifier = Modifier.testTag(PasswordGeneratorTestTags.AvoidAmbiguous),
                )
            }
        }
    }
}

private fun PasswordStrength.labelRes(): Int = when (this) {
    PasswordStrength.Weak -> R.string.passwords_strength_weak
    PasswordStrength.Fair -> R.string.passwords_strength_fair
    PasswordStrength.Strong -> R.string.passwords_strength_strong
    PasswordStrength.VeryStrong -> R.string.passwords_strength_very_strong
}

@Composable
private fun PasswordStrength.colorV5(): Color = when (this) {
    PasswordStrength.Weak -> V5Theme.colors.danger
    PasswordStrength.Fair -> V5Theme.colors.warning
    PasswordStrength.Strong, PasswordStrength.VeryStrong -> V5Theme.colors.success
}
