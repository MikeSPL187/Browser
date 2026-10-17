package dev.sk2andy.materialbrowser.ui.theme.v5

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/**
 * «Translate into» (V23, Settings → Translation): the language tag the translate sheet offers
 * first. Empty until the user picks one; then the app's language is offered. One shared state, as
 * the v5 switches are, so a choice in settings reaches an open sheet at once.
 */
object V5TranslateIntoSetting {
    private const val KEY = "ui.v5.translate_into"

    @Volatile
    private var state: MutableState<String>? = null

    fun set(context: Context, languageTag: String) {
        state(context).value = languageTag
        preferences(context).edit { putString(KEY, languageTag) }
    }

    internal fun state(context: Context): MutableState<String> =
        state ?: synchronized(this) {
            state ?: mutableStateOf(preferences(context).getString(KEY, null).orEmpty()).also { state = it }
        }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(V5Preference.PREFERENCES, Context.MODE_PRIVATE)
}

/** The chosen tag as Compose state, empty when none was chosen; read-only, [V5TranslateIntoSetting.set] writes. */
@Composable
fun rememberV5TranslateInto(): String {
    val context = LocalContext.current
    return remember(context) { V5TranslateIntoSetting.state(context) }.value
}
