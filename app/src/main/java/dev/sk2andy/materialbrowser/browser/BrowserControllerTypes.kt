package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.engine.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.userscript.UserScriptDependencyFailureReason
import dev.sk2andy.materialbrowser.browser.userscript.UserScriptRejectionReason

/* Small state types of [BrowserController], kept out of its file so the controller doesn't grow. */

internal data class FullscreenVideoState(
    val tabId: String,
    val minimizedByUser: Boolean,
    val sourceRevision: Int,
    val source: FullscreenVideoSource,
    val host: FullscreenVideoHost,
)

internal enum class FullscreenVideoSource {
    GeckoView,
}

internal enum class FullscreenVideoHost {
    BrowserViewport,
    Overlay,
}

internal sealed interface UserScriptSaveOutcome {
    data object Saved : UserScriptSaveOutcome
    data object LimitReached : UserScriptSaveOutcome
    data object Missing : UserScriptSaveOutcome
    data object PersistenceFailed : UserScriptSaveOutcome
    data class Rejected(val reason: UserScriptRejectionReason) : UserScriptSaveOutcome
    data class DependencyFailed(
        val reason: UserScriptDependencyFailureReason,
    ) : UserScriptSaveOutcome
}

internal data class BrowserActivityResultIdentity(
    val tabId: String,
    val session: AndroidBrowserEngineSessionPort,
    val navigationGeneration: Int,
)

internal data class ExternalNavigationRollback(
    val tabId: String,
    val session: AndroidBrowserEngineSessionPort,
    val navigationGeneration: Int,
    val sourceUrl: String,
    var restoringSource: Boolean = false,
)

internal data class ExternalAppNavigationRecovery(
    val session: AndroidBrowserEngineSessionPort,
    val targetUrl: String,
    val sourceTab: BrowserTab,
    val navigationGeneration: Int,
    var restored: Boolean = false,
)

internal data class AutoDeAmpReplacementGuard(
    val publisherUrl: String,
    val expiresAtElapsedRealtime: Long,
)

enum class BrowserGestureHapticFeedback {
    RubberbandStart,
    RubberbandStop,
    Confirm,
}
