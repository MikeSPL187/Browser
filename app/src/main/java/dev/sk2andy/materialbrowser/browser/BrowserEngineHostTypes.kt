package dev.sk2andy.materialbrowser.browser

import android.graphics.Rect
import android.net.Uri
import android.view.View
import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.gecko.BrowserEnginePreviewCapture
import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineCommand

/*
 * Bookkeeping types of BrowserController's engine hosts, kept apart so the controller stays
 * within its size budget (docs/vola/tech-plan.md, section 1).
 */

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

internal data class FirefoxExtensionOptionsTabChrome(
    val title: String,
    val scheme: String,
    val host: String,
    val port: Int,
) {
    fun owns(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() == scheme &&
            uri.host?.lowercase() == host &&
            uri.port == port
    }

    companion object {
        fun create(title: String, url: String): FirefoxExtensionOptionsTabChrome? {
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host?.lowercase()?.takeIf(String::isNotBlank) ?: return null
            if (scheme != "moz-extension") return null
            return FirefoxExtensionOptionsTabChrome(
                title = title,
                scheme = scheme,
                host = host,
                port = uri.port,
            )
        }
    }
}
