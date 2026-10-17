package dev.sk2andy.materialbrowser.browser

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration

/**
 * Lets go of background tabs when the system runs short of memory, on the main thread: at a
 * warning it asks [trim] for a smaller number of live sessions
 * ([BrowserSessionResidencyRules.pressureLimit]) than [limit], the person's setting, which itself
 * stays. A low-memory call counts as the most severe warning. [close] stops listening; the
 * application context holds no reference after it.
 */
internal class BrowserMemoryPressure(
    context: Context,
    private val limit: () -> Int,
    private val trim: (limit: Int) -> Unit,
) : ComponentCallbacks2, AutoCloseable {
    private val application = context.applicationContext
    private var listening = true

    init {
        application.registerComponentCallbacks(this)
    }

    override fun onTrimMemory(level: Int) {
        if (!listening) return
        BrowserSessionResidencyRules.pressureLimit(level, limit())?.let(trim)
    }

    @Deprecated("Deprecated in Android; kept for the systems that still call it.")
    override fun onLowMemory() = onTrimMemory(COMPLETE)

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun close() {
        if (!listening) return
        listening = false
        application.unregisterComponentCallbacks(this)
    }

    private companion object {
        /** `ComponentCallbacks2.TRIM_MEMORY_COMPLETE`, deprecated on Android 14 but the most severe level. */
        const val COMPLETE = 80
    }
}
