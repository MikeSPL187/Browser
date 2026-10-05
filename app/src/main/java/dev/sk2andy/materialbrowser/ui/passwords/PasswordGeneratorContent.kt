package dev.sk2andy.materialbrowser.ui.passwords

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.credentials.vault.AndroidCredentialVault
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorMode
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorOptions
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorRules
import dev.sk2andy.materialbrowser.shared.credentials.PasswordStrength
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.AppearanceChoiceRow
import dev.sk2andy.materialbrowser.ui.SettingsCardSlider
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPasswords
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import java.security.SecureRandom

internal object PasswordGeneratorTestTags {
    const val Value = "password_generator_value"
    const val Refresh = "password_generator_refresh"
    const val Use = "password_generator_use"
    const val DigitsAndSymbols = "password_generator_digits_symbols"
    const val AvoidAmbiguous = "password_generator_avoid_ambiguous"
    fun mode(mode: PasswordGeneratorMode) = "password_generator_mode:${mode.name}"
}

/**
 * The generator's state: the options and the value they made. Values come from the system CSPRNG;
 * phrases use the EFF short word list the recovery phrase already ships with. The last options are
 * remembered for the rest of the process, never written down.
 */
@Stable
internal class PasswordGeneratorState(
    private val wordlist: List<String>?,
    private val random: SecureRandom = SecureRandom(),
) {
    var options by mutableStateOf(lastOptions.takeIf { wordlist != null || it.mode == PasswordGeneratorMode.Password } ?: PasswordGeneratorOptions())
        private set
    var value by mutableStateOf(generate(options))
        private set

    val phraseAvailable: Boolean get() = wordlist != null

    val strength: PasswordStrength
        get() = PasswordGeneratorRules.strength(PasswordGeneratorRules.entropyBits(options, wordlist?.size ?: 0))

    fun update(options: PasswordGeneratorOptions) {
        val normalized = options.normalized()
        if (normalized == this.options) return
        this.options = normalized
        lastOptions = normalized
        regenerate()
    }

    fun regenerate() {
        value = generate(options)
    }

    private fun generate(options: PasswordGeneratorOptions): String {
        val nextInt: (Int) -> Int = random::nextInt
        return if (options.mode == PasswordGeneratorMode.Phrase && wordlist != null) {
            PasswordGeneratorRules.phrase(wordlist, options.words, nextInt)
        } else {
            PasswordGeneratorRules.password(options, nextInt)
        }
    }

    private companion object {
        var lastOptions = PasswordGeneratorOptions()
    }
}

@Composable
internal fun rememberPasswordGeneratorState(): PasswordGeneratorState {
    val context = LocalContext.current
    return remember { PasswordGeneratorState(generatorWordlist(context)) }
}

private fun generatorWordlist(context: Context): List<String>? = AndroidCredentialVault.wordlist(context)?.words

/**
 * «Strong password» (board W-Generator): password or phrase, the value with «new» and «copy», how
 * strong it is, length or words, the two switches, and one button that puts it to use.
 */
@Composable
internal fun PasswordGeneratorContent(
    subtitle: String?,
    useLabel: String,
    useNeedsFingerprint: Boolean,
    onUse: (String) -> Unit,
    state: PasswordGeneratorState = rememberPasswordGeneratorState(),
) {
    val context = LocalContext.current
    val options = state.options
    VaultSheetColumn {
        VaultSheetHeader(title = stringResource(R.string.passwords_generator_title)) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.phraseAvailable) {
            AppearanceChoiceRow(
                options = PasswordGeneratorMode.entries,
                selected = options.mode,
                label = { mode ->
                    stringResource(
                        when (mode) {
                            PasswordGeneratorMode.Password -> R.string.passwords_generator_password
                            PasswordGeneratorMode.Phrase -> R.string.passwords_generator_phrase
                        },
                    )
                },
                icon = null,
                testTag = PasswordGeneratorTestTags::mode,
                onSelect = { mode -> state.update(options.copy(mode = mode)) },
            )
        }
        VaultSheetCard {
            Row(
                modifier = Modifier.padding(VolaPasswords.generatorValuePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.value,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(PasswordGeneratorTestTags.Value),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
                IconButton(
                    onClick = state::regenerate,
                    modifier = Modifier.testTag(PasswordGeneratorTestTags.Refresh),
                ) {
                    Icon(VolaIcons.Refresh, contentDescription = stringResource(R.string.passwords_generator_new))
                }
                IconButton(onClick = { SensitiveClipboard.copy(context, state.value) }) {
                    Icon(VolaIcons.ContentCopy, contentDescription = stringResource(R.string.passwords_copy_password))
                }
            }
        }
        Text(
            text = stringResource(state.strength.label()),
            modifier = Modifier.padding(VolaPasswords.sheetHeaderPadding),
            style = MaterialTheme.typography.labelLarge,
            color = state.strength.color(),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = VolaPasswords.sheetCardShape,
            color = VolaTheme.extendedColors.card,
        ) {
            Column(modifier = Modifier.padding(VolaPasswords.generatorOptionsPadding)) {
                val phrase = options.mode == PasswordGeneratorMode.Phrase
                val count = if (phrase) options.words else options.length
                // The slider follows the finger here; the value is made again when it lifts.
                var shown by remember(phrase, count) { mutableIntStateOf(count) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(if (phrase) R.string.passwords_generator_words else R.string.passwords_generator_length),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = shown.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SettingsCardSlider(
                    value = count,
                    range = if (phrase) {
                        PasswordGeneratorRules.MIN_WORDS..PasswordGeneratorRules.MAX_WORDS
                    } else {
                        PasswordGeneratorRules.MIN_LENGTH..PasswordGeneratorRules.MAX_LENGTH
                    },
                    label = stringResource(if (phrase) R.string.passwords_generator_words else R.string.passwords_generator_length),
                    onValueChange = { value -> shown = value },
                    onValueChangeFinished = { value ->
                        state.update(if (phrase) options.copy(words = value) else options.copy(length = value))
                    },
                )
                if (!phrase) {
                    GeneratorSwitch(
                        label = stringResource(R.string.passwords_generator_digits_symbols),
                        checked = options.digitsAndSymbols,
                        testTag = PasswordGeneratorTestTags.DigitsAndSymbols,
                        onCheckedChange = { state.update(options.copy(digitsAndSymbols = it)) },
                    )
                    GeneratorSwitch(
                        label = stringResource(R.string.passwords_generator_avoid_ambiguous),
                        checked = options.avoidAmbiguous,
                        testTag = PasswordGeneratorTestTags.AvoidAmbiguous,
                        onCheckedChange = { state.update(options.copy(avoidAmbiguous = it)) },
                    )
                }
            }
        }
        Button(
            onClick = { onUse(state.value) },
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaPasswords.sheetButtonHeight)
                .testTag(PasswordGeneratorTestTags.Use),
            contentPadding = if (useNeedsFingerprint) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding,
        ) {
            if (useNeedsFingerprint) VaultButtonIcon(VolaIcons.Fingerprint)
            Text(useLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GeneratorSwitch(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaPasswords.generatorSwitchHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun PasswordStrength.label(): Int = when (this) {
    PasswordStrength.Weak -> R.string.passwords_strength_weak
    PasswordStrength.Fair -> R.string.passwords_strength_fair
    PasswordStrength.Strong -> R.string.passwords_strength_strong
    PasswordStrength.VeryStrong -> R.string.passwords_strength_very_strong
}

@Composable
private fun PasswordStrength.color(): Color = when (this) {
    PasswordStrength.Weak -> MaterialTheme.colorScheme.error
    PasswordStrength.Fair -> VolaTheme.extendedColors.warn
    PasswordStrength.Strong, PasswordStrength.VeryStrong -> VolaTheme.extendedColors.ok
}

/** The generator sheet over a sign-up page, with the vault set up. */
@VolaPreviews
@Composable
private fun PasswordGeneratorPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            PasswordGeneratorContent(
                subtitle = "cloud.example.com",
                useLabel = stringResource(R.string.passwords_generator_use_and_save),
                useNeedsFingerprint = true,
                onUse = {},
                state = remember { PasswordGeneratorState(wordlist = listOf("acid", "acorn", "acre", "acts")) },
            )
        }
    }
}
