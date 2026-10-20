package dev.sk2andy.materialbrowser.browser.engine

import android.app.PendingIntent
import android.content.Context
import dev.sk2andy.materialbrowser.browser.gecko.GeckoActivityIntegration
import dev.sk2andy.materialbrowser.browser.gecko.GeckoBrowserEngineSessionFactory
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionManagerCoordinator
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPerformanceDiagnostics
import dev.sk2andy.materialbrowser.browser.gecko.currentGeckoIdentity
import dev.sk2andy.materialbrowser.browser.gecko.webpush.GeckoWebPushCoordinator
import kotlinx.coroutines.CoroutineScope

/** The `full` build: GeckoView, with System WebView as the other engine one can choose. */
internal val engineFlavor: EngineFlavor = object : EngineFlavor {
    override fun createGeckoSessionFactory(context: Context): AndroidBrowserEngineFactory =
        GeckoBrowserEngineSessionFactory(context)

    override fun createActivityIntegration(
        context: Context,
        launch: (PendingIntent) -> Unit,
        onPendingChanged: (Boolean) -> Unit,
        isPendingRequestCurrent: () -> Boolean,
    ): EngineActivityIntegration = GeckoActivityIntegration(
        context = context,
        launch = launch,
        onPendingChanged = onPendingChanged,
        isPendingRequestCurrent = isPendingRequestCurrent,
    )

    override fun createExtensionManager(
        context: Context,
        scope: CoroutineScope,
        onPageRuntimeChanged: () -> Unit,
        onOpenOptionsPage: (GeckoExtensionOptionsTarget) -> Boolean,
    ): GeckoExtensionManager = GeckoExtensionManagerCoordinator.create(
        context = context,
        scope = scope,
        onPageRuntimeChanged = onPageRuntimeChanged,
        onOpenOptionsPage = onOpenOptionsPage,
    )

    override val webPush: EngineWebPush = GeckoWebPushCoordinator

    override val isPerformanceRecording: Boolean
        get() = GeckoPerformanceDiagnostics.isRecording

    override fun runtimeIdentity(): String = currentGeckoIdentity()
}
