package dev.sk2andy.materialbrowser.browser

import android.view.View
import dev.sk2andy.materialbrowser.browser.engine.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.integration.ExternalPreviewDownloadGrant

internal sealed interface ExternalLinkPreviewEngineBinding {
    val view: View

    data class Gecko(
        val session: AndroidBrowserEngineSessionPort,
        override val view: View,
    ) : ExternalLinkPreviewEngineBinding
}

internal data class ExternalLinkPreviewRuntime(
    val sessionId: Long,
    val generation: Int,
    val policyTab: BrowserTab,
    val binding: ExternalLinkPreviewEngineBinding,
    var hasStarted: Boolean = false,
    var downloadGrant: ExternalPreviewDownloadGrant? = null,
    var pendingInternalNavigationUrl: String? = null,
    var downloadNavigationRevision: Int = 0,
    var backdropBlurRegion: BrowserBackdropBlurRegion? = null,
) {
    val geckoBinding: ExternalLinkPreviewEngineBinding.Gecko
        get() = binding as ExternalLinkPreviewEngineBinding.Gecko
}
