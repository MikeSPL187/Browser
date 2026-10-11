package dev.sk2andy.materialbrowser.ui.theme.v5

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/*
 * The ui.v5 flag (plan-v5.md, section 9): the new interface replaces the current one screen by
 * screen while this is on. Off by default and only reachable from developer options until v5 is
 * the default (V28). It lives in its own preferences file so the screens of v5 can read it without
 * the browser session store.
 */
object UiV5Flag {
    const val KEY = "ui.v5"
    private const val PREFERENCES = "vola_ui_v5"

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(KEY, enabled) }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}

/** The flag as Compose state; writing the state stores it. */
@Composable
fun rememberUiV5Flag(): MutableState<Boolean> {
    val context = LocalContext.current
    return remember(context) {
        object : MutableState<Boolean> {
            private val state = mutableStateOf(UiV5Flag.isEnabled(context))

            override var value: Boolean
                get() = state.value
                set(enabled) {
                    state.value = enabled
                    UiV5Flag.setEnabled(context, enabled)
                }

            override fun component1(): Boolean = value

            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}
