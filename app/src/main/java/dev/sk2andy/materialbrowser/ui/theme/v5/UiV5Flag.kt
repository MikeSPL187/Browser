package dev.sk2andy.materialbrowser.ui.theme.v5

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/*
 * Switches of the v5 interface. They live in their own preferences file so the screens of v5 can
 * read them without the browser session store, and each is one shared state, so turning it on in
 * settings changes the browser screen underneath at once.
 */
open class V5Preference internal constructor(
    private val key: String,
    private val default: Boolean,
) {
    @Volatile
    private var state: MutableState<Boolean>? = null

    fun isEnabled(context: Context): Boolean = state(context).value

    fun setEnabled(context: Context, enabled: Boolean) {
        state(context).value = enabled
        preferences(context).edit { putBoolean(key, enabled) }
    }

    internal fun state(context: Context): MutableState<Boolean> =
        state ?: synchronized(this) {
            state ?: mutableStateOf(preferences(context).getBoolean(key, default)).also { state = it }
        }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private companion object {
        const val PREFERENCES = "vola_ui_v5"
    }
}

/**
 * The ui.v5 flag (plan-v5.md, section 9): the new interface on a phone. On by default since V28,
 * step 2; a tablet stays on the current interface whatever it says (`rememberDrawsV5`). Developer
 * options can still turn it off until the current interface leaves the phone (V28, step 4); a
 * choice made there is kept.
 */
object UiV5Flag : V5Preference(key = "ui.v5", default = true)

/**
 * «Tint the bar with the site's color» (system-v5.md, section 1.3): on by default; off keeps the
 * neutral bar on every site, as Safari allows after complaints about unreadable bars.
 */
object V5SiteTintSetting : V5Preference(key = "ui.v5.site_tint", default = true)

/** The flag as Compose state; writing the state stores it. */
@Composable
fun rememberUiV5Flag(): MutableState<Boolean> = rememberV5Preference(UiV5Flag)

/** [preference] as Compose state; writing the state stores it. */
@Composable
fun rememberV5Preference(preference: V5Preference): MutableState<Boolean> {
    val context = LocalContext.current
    return remember(context, preference) {
        val shared = preference.state(context)
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = shared.value
                set(enabled) = preference.setEnabled(context, enabled)

            override fun component1(): Boolean = value

            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}
