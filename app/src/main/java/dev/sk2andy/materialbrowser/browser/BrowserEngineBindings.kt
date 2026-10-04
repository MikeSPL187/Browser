package dev.sk2andy.materialbrowser.browser

import android.graphics.Rect
import android.view.View
import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.browser.gecko.GeckoInlineVideoIdentity
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommand
import java.util.concurrent.atomic.AtomicBoolean

// The engine's bookkeeping records for BrowserController: views bound to tabs, pending captures,
// navigations and media presentations. Moved out of BrowserController.kt unchanged.

internal class PendingGeckoPreviewCapture(
    val tabId: String,
    val session: AndroidBrowserEngineSessionPort,
    val view: View,
    val pageUrl: String,
    val navigationGeneration: Int,
    val previewEpoch: Int,
    val sourceRect: Rect,
    onComplete: () -> Unit,
    var acceptAfterDeparture: Boolean,
) {
    val completionCallbacks = mutableListOf(onComplete)
    var capture: BrowserEnginePreviewCapture? = null
    var timeout: Runnable? = null
    var uiCompleted = false
    var expired = false
}

internal class GeckoMediaPresentation(
    val tabId: String,
    val session: AndroidBrowserEngineSessionPort,
    val view: View,
    val inlineVideoIdentity: GeckoInlineVideoIdentity?,
    var minimizedByUser: Boolean,
)

internal data class InlineVideoGestureHapticOwner(
    val tabId: String,
    val session: AndroidBrowserEngineSessionPort,
    val navigationGeneration: Int,
    val identity: GeckoInlineVideoIdentity,
)

internal class PendingMediaLayoutRestoration(
    val request: MediaLayoutRestorationGate.Request,
    val view: View,
    val restore: (acceptCompletion: () -> Boolean) -> Unit,
) {
    var started = false
}

internal data class GeckoViewBinding(
    val tabId: String,
    val session: AndroidBrowserEngineSessionPort,
    val view: View,
)

internal class PendingInitialBrowserEngineNavigation(
    val session: AndroidBrowserEngineSessionPort,
    val command: BrowserEngineCommand?,
) {
    private var observedView: View? = null
    private var layoutListener: View.OnLayoutChangeListener? = null

    fun observeLayout(view: View, onLayout: (View) -> Unit) {
        stopObservingLayout()
        val listener = View.OnLayoutChangeListener { changedView, _, _, _, _, _, _, _, _ ->
            onLayout(changedView)
        }
        observedView = view
        layoutListener = listener
        view.addOnLayoutChangeListener(listener)
        if (view.isLaidOut && !view.isLayoutRequested) onLayout(view)
    }

    fun stopObservingLayout() {
        val view = observedView
        val listener = layoutListener
        if (view != null && listener != null) view.removeOnLayoutChangeListener(listener)
        observedView = null
        layoutListener = null
    }
}

internal data class TabFaviconFetchAttempt(
    val session: AndroidBrowserEngineSessionPort,
    val pageUrl: String,
    val navigationGeneration: Int,
    val faviconEpoch: Int,
) {
    val cancelled = AtomicBoolean(false)
}

internal data class PendingGeckoViewAttach(
    val token: Any,
    val onContentPresented: ((String) -> Unit)?,
    val backdropCaptureEnabled: Boolean,
)

internal data class GeckoLinkPeekBinding(
    val sourceTabId: String,
    val contentRevision: Long,
    val session: AndroidBrowserEngineSessionPort,
    val view: View,
    var committedUrl: String,
    var title: String? = null,
    var progress: Int = 0,
    var isLoading: Boolean = true,
)
