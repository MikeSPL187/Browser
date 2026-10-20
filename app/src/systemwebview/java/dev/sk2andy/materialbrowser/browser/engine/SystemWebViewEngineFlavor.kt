package dev.sk2andy.materialbrowser.browser.engine

import android.app.PendingIntent
import android.content.Context
import kotlinx.coroutines.CoroutineScope

/** The `systemwebview` build: no GeckoView, so nothing Gecko-only exists to create or clean up. */
internal val engineFlavor: EngineFlavor = object : EngineFlavor {
    override fun createGeckoSessionFactory(context: Context): AndroidBrowserEngineFactory? = null

    override fun createActivityIntegration(
        context: Context,
        launch: (PendingIntent) -> Unit,
        onPendingChanged: (Boolean) -> Unit,
        isPendingRequestCurrent: () -> Boolean,
    ): EngineActivityIntegration? = null

    override fun createExtensionManager(
        context: Context,
        scope: CoroutineScope,
        onPageRuntimeChanged: () -> Unit,
        onOpenOptionsPage: (GeckoExtensionOptionsTarget) -> Boolean,
    ): GeckoExtensionManager? = null

    override val webPush: EngineWebPush = EngineWebPush.None

    override val isPerformanceRecording: Boolean = false

    override fun runtimeIdentity(): String? = null
}
