package dev.sk2andy.materialbrowser.ui

import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.LinkPeekPreviewStatus
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.V5LinkGlance
import dev.sk2andy.materialbrowser.ui.v5.V5LinkGlanceActions
import dev.sk2andy.materialbrowser.ui.v5.V5LinkGlanceMessage
import dev.sk2andy.materialbrowser.ui.v5.V5LinkGlanceState

/**
 * The v5 link preview (V21) bound to the browser: the same ephemeral engine page as the current
 * preview (no tab, nothing in history), shown in the v5 card. «In background» and «New tab» move
 * that page into a new tab next to this one without loading it again (V21c); «Open» still loads
 * the link in this tab, so Back returns to this page. The other actions are the link's usual ones.
 */
@Composable
internal fun BrowserLinkGlanceV5(controller: BrowserController) {
    val target = controller.contentActions.target ?: return
    val url = target.linkUrl ?: return
    // A new link is a new card: its own engine page, flight and state.
    key(url) { LinkGlance(controller, url, target.canDownloadImage) }
}

@Composable
private fun LinkGlance(controller: BrowserController, url: String, canDownloadImage: Boolean) {
    val view = LocalView.current
    val tab = controller.selectedTab
    var progress by remember { mutableIntStateOf(0) }
    var committedUrl by remember { mutableStateOf(url) }
    var status by remember { mutableStateOf<LinkPeekPreviewStatus>(LinkPeekPreviewStatus.Loading) }
    var title by remember { mutableStateOf<String?>(null) }
    // Retry builds a fresh engine page; a released one must not report into it.
    var attempt by remember { mutableIntStateOf(0) }
    // The live page, handed to the new tab by «In background» and «New tab» instead of a reload.
    var page by remember { mutableStateOf<View?>(null) }
    val origin = remember { PageTouchPoint.lastDownInWindow }
    val host = BrowserUriPolicy.displayHttpHost(committedUrl)
    val message = linkPeekMessageText(status, host)?.let {
        V5LinkGlanceMessage(title = it.title, body = it.body, danger = it.danger, retryable = it.retryable)
    }
    val openedInBackground = stringResource(R.string.v5_glance_opened_background)
    val state = V5LinkGlanceState(
        url = committedUrl,
        progress = (progress / PERCENT).takeIf { status == LinkPeekPreviewStatus.Loading && progress < FULL_PERCENT },
        privateTab = tab.isIncognito,
        message = message,
        // From a private tab «New tab» already opens a private one.
        canOpenInPrivate = controller.canOpenLinkInPrivate && !tab.isIncognito,
        canDownloadImage = canDownloadImage,
    )
    val dismiss = controller.contentActions::dismiss
    val actions = V5LinkGlanceActions(
        onOpen = {
            val sourceIsCurrent = controller.contextLinkSourceTabId != null
            dismiss()
            if (sourceIsCurrent) controller.openUrl(committedUrl)
        },
        onOpenInBackground = {
            if (controller.openLinkPreviewInNewTab(page, committedUrl, foreground = false)) {
                view.performV5Haptic(V5Haptic.Confirm)
                Toast.makeText(view.context, openedInBackground, Toast.LENGTH_SHORT).show()
            }
        },
        onOpenInNewTab = { controller.openLinkPreviewInNewTab(page, committedUrl, foreground = true) },
        onOpenInPrivate = { controller.openLinkInPrivate(committedUrl) },
        onCopy = {
            dismiss()
            controller.copyLink(committedUrl)
        },
        onShare = {
            dismiss()
            controller.shareLink(committedUrl)
        },
        // Both download what was long-pressed, so they run before the target is let go.
        onDownloadLink = {
            controller.downloadContextLink()
            dismiss()
        },
        onDownloadImage = {
            controller.downloadContextImage()
            dismiss()
        },
        onRetry = {
            status = LinkPeekPreviewStatus.Loading
            progress = 0
            attempt++
        },
        onDismiss = dismiss,
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
    VolaV5Theme(dark = dark, privateBrowsing = tab.isIncognito) {
        V5LinkGlance(
            state = state,
            actions = actions,
            origin = origin,
            onPageScale = { LinkGlancePage.scale = it },
        ) { modifier ->
            key(attempt) {
                AndroidView(
                    factory = {
                        val current = attempt
                        val reports = { current == attempt }
                        controller.createLinkPeekPreviewView(
                            url = url,
                            onProgressChanged = { if (reports()) progress = it },
                            onCommittedUrlChanged = { if (reports()) committedUrl = it },
                            onStatusChanged = { if (reports()) status = it },
                            onTitleChanged = { if (reports()) title = it },
                        ).also { created -> page = created }
                    },
                    onRelease = controller::releaseLinkPeekPreviewView,
                    // The engine's own tree stays hidden (the preview takes no input); TalkBack reads
                    // which page it shows, and nothing while a message covers it.
                    modifier = modifier
                        .clearSemanticsWhen(status.coversPage)
                        .semantics { contentDescription = title ?: host },
                )
            }
        }
    }
}

/**
 * The page's scale under the link preview: the browser's content applies it with
 * [linkGlancePageScale], the preview sets it. One per process, as there is one browser screen.
 */
internal object LinkGlancePage {
    var scale by mutableFloatStateOf(1f)
}

/** Shrinks the page and its bar while a link preview is over them (read at draw, no recomposition). */
internal fun Modifier.linkGlancePageScale(): Modifier = graphicsLayer {
    val scale = LinkGlancePage.scale
    scaleX = scale
    scaleY = scale
}

/** The last finger down on the page, where a link preview starts; recorded by the page's host view. */
internal object PageTouchPoint {
    var lastDownInWindow: Offset? = null
        private set

    fun record(host: View, event: MotionEvent) {
        val location = IntArray(2)
        host.getLocationInWindow(location)
        lastDownInWindow = Offset(location[0] + event.x, location[1] + event.y)
    }
}

private const val PERCENT = 100f
private const val FULL_PERCENT = 100

/** Below this background luminance the current theme is dark. */
private const val DARK_BACKGROUND_LUMINANCE = 0.5f
