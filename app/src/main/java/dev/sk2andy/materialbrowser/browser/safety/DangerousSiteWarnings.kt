package dev.sk2andy.materialbrowser.browser.safety

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.BrowserSessionStore

/**
 * «Warn about dangerous sites» in Protection and data: on by default. Off, no address is checked
 * and the threat list is not even loaded.
 */
class DangerousSiteWarnings private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val store = BrowserSessionStore(appContext)

    var enabled by mutableStateOf(store.loadDangerousSiteWarningsEnabled())
        private set

    init {
        if (enabled) ThreatHostList.get(appContext)
    }

    fun updateEnabled(value: Boolean) {
        if (value == enabled) return
        enabled = value
        store.saveDangerousSiteWarningsEnabled(value)
        if (value) ThreatHostList.get(appContext)
    }

    /** True when warnings are on and [host] is on the bundled threat list. */
    fun isListed(host: String): Boolean = enabled && ThreatHostList.get(appContext).contains(host)

    companion object {
        @Volatile
        private var instance: DangerousSiteWarnings? = null

        fun get(context: Context): DangerousSiteWarnings = instance ?: synchronized(this) {
            instance ?: DangerousSiteWarnings(context).also { instance = it }
        }
    }
}
