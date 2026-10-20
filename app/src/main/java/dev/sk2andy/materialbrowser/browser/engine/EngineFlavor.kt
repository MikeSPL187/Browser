package dev.sk2andy.materialbrowser.browser.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * What only one build's engine provides (#500). Each flavor's source set declares its own
 * [engineFlavor]: `full` with GeckoView, `systemwebview` without it, where every factory answers
 * null and every clean-up has nothing to do. Code in `main` reaches Gecko only through here.
 */
internal interface EngineFlavor {
    /** GeckoView's session factory; null in a build without GeckoView. */
    fun createGeckoSessionFactory(context: Context): AndroidBrowserEngineFactory?

    /** Gecko's activity-result wiring (passkeys); null without GeckoView. */
    fun createActivityIntegration(
        context: Context,
        launch: (PendingIntent) -> Unit,
        onPendingChanged: (Boolean) -> Unit,
        isPendingRequestCurrent: () -> Boolean,
    ): EngineActivityIntegration?

    /** The Firefox extension manager; null without GeckoView. */
    fun createExtensionManager(
        context: Context,
        scope: CoroutineScope,
        onPageRuntimeChanged: () -> Unit,
        onOpenOptionsPage: (GeckoExtensionOptionsTarget) -> Boolean,
    ): GeckoExtensionManager?

    /** Web Push subscriptions, which only GeckoView keeps. */
    val webPush: EngineWebPush

    /** Whether a Gecko profiler capture is under way. */
    val isPerformanceRecording: Boolean

    /** The engine build an archive records, so a restore can reject another engine's state. */
    fun runtimeIdentity(): String?
}

/** The activity's side of the engine's activity results. */
internal interface EngineActivityIntegration : AutoCloseable {
    fun onActivityResult(resultCode: Int, data: Intent?)

    fun onHostPaused()

    fun onHostResumed()
}

/** Web Push clean-up; each call answers whether it finished, which a build without it always has. */
internal interface EngineWebPush {
    fun onBrowserEngineChanged(context: Context, kind: AndroidBrowserEngineKind)

    suspend fun clearAllData(context: Context): Boolean

    suspend fun removeProfileSubscriptions(context: Context, profileId: String): Boolean

    object None : EngineWebPush {
        override fun onBrowserEngineChanged(context: Context, kind: AndroidBrowserEngineKind) = Unit

        override suspend fun clearAllData(context: Context): Boolean = true

        override suspend fun removeProfileSubscriptions(context: Context, profileId: String): Boolean = true
    }
}

/** The Firefox extension manager the extensions screen drives. */
internal interface GeckoExtensionManager {
    val state: GeckoExtensionManagerState

    fun open(context: GeckoExtensionManagementContext): Job?

    fun openOptionsPage(extensionId: String, onOpened: () -> Unit = {}): Job?

    fun install(rawUri: String): Job?

    fun setEnabled(extension: GeckoExtension, enabled: Boolean): Job?

    fun setAllowedInPrivateBrowsing(extension: GeckoExtension, allowed: Boolean): Job?

    fun update(extension: GeckoExtension): Job?

    fun uninstall(extension: GeckoExtension): Job?

    fun completePermissionRequest(granted: Boolean, allowPrivate: Boolean)

    fun dismiss()

    fun close()
}
