package dev.sk2andy.materialbrowser.ui.passwords

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.credentials.vault.AndroidCredentialVault
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorMode
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorOptions
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorRules
import dev.sk2andy.materialbrowser.shared.credentials.PasswordStrength
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

