package dev.sk2andy.materialbrowser.browser.gecko

import androidx.annotation.UiThread
import org.mozilla.geckoview.WebExtensionController

/**
 * Keeps the extensions process coming back. Gecko stops starting it after it crashed or was killed
 * too often in a short time; until it starts again, candy_privacy blocks nothing. The guard asks for
 * it again [MAX_RESTARTS] times per app process, so a process that keeps crashing cannot loop
 * forever; after that extensions stay off until Vola restarts.
 */
internal class GeckoExtensionProcessGuard(
    private val restart: () -> Unit,
) : WebExtensionController.ExtensionProcessDelegate {
    var restarts = 0
        private set

    @UiThread
    override fun onDisabledProcessSpawning() {
        if (restarts >= MAX_RESTARTS) return
        restarts++
        restart()
    }

    companion object {
        const val MAX_RESTARTS = 3
    }
}
