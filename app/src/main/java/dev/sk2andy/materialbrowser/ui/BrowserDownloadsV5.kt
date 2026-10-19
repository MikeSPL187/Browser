package dev.sk2andy.materialbrowser.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.DownloadsController
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRepository
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Haptic
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.performV5Haptic
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadNotice
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadNoticeAccessory
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadRing
import dev.sk2andy.materialbrowser.ui.v5.V5DownloadRingState
import dev.sk2andy.materialbrowser.ui.v5.v5DownloadRingColor
import dev.sk2andy.materialbrowser.ui.v5.v5DownloadsSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the downloads ring shows (V22, system-v5.md section 10): the progress of everything that is
 * downloading, summed in bytes; a running arc while some size is unknown; nothing when idle.
 */
internal object DownloadRingRules {
    fun ring(entries: List<DownloadEntry>): V5DownloadRingState? {
        val active = entries.filter { entry -> entry.status.isActive }
        if (active.isEmpty()) return null
        if (active.any { entry -> entry.total <= 0L }) return V5DownloadRingState.Indeterminate(active.size)
        val total = active.sumOf { entry -> entry.total }
        val bytes = active.sumOf { entry -> entry.bytes.coerceIn(0L, entry.total) }
        return V5DownloadRingState.Progress(active.size, bytes.toFloat() / total)
    }

    /**
     * Downloads that were running in [previous] and have now finished or failed (V22b). A transfer
     * the engine held in memory comes back under a new id once saved, so its name is the second
     * match. A cancelled one is not news.
     */
    fun ended(previous: List<DownloadEntry>, current: List<DownloadEntry>): List<DownloadEntry> =
        previous.filter { entry -> entry.status.isActive }.mapNotNull { before ->
            val now = current.firstOrNull { entry -> entry.id == before.id }
                ?: current.firstOrNull { entry -> entry.name == before.name && entry.status.isTerminal }
            now?.takeIf { entry -> entry.status == DownloadStatus.Successful || entry.status == DownloadStatus.Failed }
        }

    /** The line for what has just ended: a failure first, it asks for attention. */
    fun notice(ended: List<DownloadEntry>): V5DownloadNotice? {
        ended.lastOrNull { entry -> entry.status == DownloadStatus.Failed }
            ?.let { entry ->
                return V5DownloadNotice.Failed(
                    id = entry.id,
                    name = entry.name,
                    canRetry = entry.retry == DownloadRetry.Available,
                    pageUrl = page(entry),
                )
            }
        return ended.lastOrNull { entry -> entry.status == DownloadStatus.Successful }
            ?.let { entry -> V5DownloadNotice.Done(entry.id, entry.name) }
    }

    /**
     * The ring: what is still downloading; once nothing is, a failure in its line keeps a danger
     * ring and a finish closes it for a moment.
     */
    fun shown(progress: V5DownloadRingState?, notice: V5DownloadNotice?, justDone: Boolean): V5DownloadRingState? =
        progress ?: when {
            notice is V5DownloadNotice.Failed -> V5DownloadRingState.Failed
            justDone -> V5DownloadRingState.Done
            else -> null
        }

    /**
     * The page to open instead when a failed download cannot start again (V22): a `blob:` file, or
     * one the site now answers with another file. Only a web page; a private tab's is never named.
     */
    fun page(entry: DownloadEntry): String? = (entry.retry as? DownloadRetry.Unavailable)
        ?.pageUrl
        ?.takeIf { url ->
            url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
        }

    /** Polls a little longer after a download is asked for: its row can take a moment to appear. */
    const val START_POLLS = 3

    /** How long the line stays above the bar; a failure a little longer. */
    const val DONE_NOTICE_MILLIS = 6_000L
    const val FAILED_NOTICE_MILLIS = 10_000L

    /** How long the closed ring stays before it fades. */
    const val DONE_RING_MILLIS = 1_200L
}

/** The downloads the v5 bar shows: the ring around «•••» and the line when one ends. */
@Stable
internal class DownloadRing {
    internal var progress by mutableStateOf<V5DownloadRingState?>(null)
    internal var justDone by mutableStateOf(false)

    var notice by mutableStateOf<V5DownloadNotice?>(null)
        internal set

    /** Bumped when a download finishes: «•••» springs once. */
    var donePulse by mutableIntStateOf(0)
        internal set

    /** Bumped when a failed download starts again: the store is watched until it ends. */
    internal var retries by mutableIntStateOf(0)

    /** Starts the failed download [id] again (V22); set by [rememberDownloadRing]. */
    internal var retry: (id: Long) -> Unit = {}

    val ring: V5DownloadRingState?
        get() = DownloadRingRules.shown(progress, notice, justDone)
}

/**
 * The downloads the v5 bar watches: read from the same store as the Downloads screen, only while
 * the browser is on screen and something downloads or was just asked for, so an idle browser does
 * not poll.
 */
@Composable
internal fun rememberDownloadRing(controller: BrowserController): DownloadRing {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val downloads = remember(context) { downloadsController(context.applicationContext, scope) }
    val state = remember { DownloadRing() }
    SideEffect {
        state.retry = { id ->
            downloads.downloads.firstOrNull { entry -> entry.id == id }?.let { entry ->
                downloads.retry(entry) { started ->
                    if (started) {
                        state.retries++
                    } else {
                        V5Feedback.show(context, R.string.downloads_control_failed)
                    }
                }
            }
        }
    }
    // Only while the browser is on screen, and seldom while what is left is paused or waits for a
    // network (DL-04); coming back reads again at once.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val onScreen = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(downloads, controller.contentActions.downloadRevision, state.retries, onScreen) {
        if (!onScreen) return@LaunchedEffect
        var polls = 0
        do {
            downloads.refresh()
            delay(DownloadsController.nextPollMillis(downloads.downloads) ?: DownloadsController.ACTIVE_POLL_MILLIS)
            polls++
        } while (polls < DownloadRingRules.START_POLLS || DownloadsController.nextPollMillis(downloads.downloads) != null)
    }
    val previous = remember { LastDownloads() }
    val current = downloads.downloads
    LaunchedEffect(current) {
        val notice = DownloadRingRules.notice(DownloadRingRules.ended(previous.entries, current))
        previous.entries = current
        state.progress = DownloadRingRules.ring(current)
        when (notice) {
            is V5DownloadNotice.Failed -> view.performV5Haptic(V5Haptic.Reject)
            is V5DownloadNotice.Done -> {
                view.performV5Haptic(V5Haptic.Confirm)
                state.donePulse++
                state.justDone = true
            }
            null -> Unit
        }
        if (notice != null) state.notice = notice
    }
    LaunchedEffect(state.donePulse) {
        delay(DownloadRingRules.DONE_RING_MILLIS)
        state.justDone = false
    }
    val notice = state.notice
    LaunchedEffect(notice) {
        delay(
            when (notice) {
                is V5DownloadNotice.Done -> DownloadRingRules.DONE_NOTICE_MILLIS
                is V5DownloadNotice.Failed -> DownloadRingRules.FAILED_NOTICE_MILLIS
                null -> return@LaunchedEffect
            },
        )
        state.notice = null
    }
    return state
}

/**
 * «•••» with the downloads ring around it (V22): the ring fades out when it is done, and the
 * button springs once when a download finishes. TalkBack hears what is downloading.
 */
@Composable
internal fun DownloadsMenuButton(downloads: DownloadRing, ink: Color, onClick: () -> Unit) {
    val ring = downloads.ring
    val pulse = remember { Animatable(1f) }
    val animated = V5Motion.animationsEnabled
    LaunchedEffect(downloads.donePulse) {
        if (downloads.donePulse == 0 || !animated) return@LaunchedEffect
        pulse.animateTo(V5Motion.DOWNLOAD_DONE_PULSE_SCALE, V5Motion.press.spec())
        pulse.animateTo(1f, V5Motion.glance.spec())
    }
    val last = remember { LastRing() }
    if (ring != null) last.state = ring
    Box(
        Modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        },
        contentAlignment = Alignment.Center,
    ) {
        val description = ring?.let { v5DownloadsSummary(it) }
        IconButton(
            onClick = onClick,
            modifier = Modifier.semantics { description?.let { stateDescription = it } },
        ) {
            Icon(VolaIcons.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
        }
        AnimatedVisibility(
            visible = ring != null,
            enter = fadeIn(if (animated) V5Motion.effects.spec() else V5Motion.reducedFade()),
            exit = fadeOut(if (animated) V5Motion.effects.spec() else V5Motion.reducedFade()),
            modifier = Modifier.matchParentSize(),
        ) {
            last.state?.let { shown ->
                V5DownloadRing(shown, color = v5DownloadRingColor(shown, ink), modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** The line above the bar when a download ends: open the file, retry it, or the Downloads screen. */
@Composable
internal fun BrowserDownloadNoticeAccessory(
    downloads: DownloadRing,
    selectedTabPrivate: Boolean,
    onDownloads: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    V5DownloadNoticeAccessory(
        notice = downloads.notice,
        onOpen = { notice ->
            downloads.notice = null
            scope.launch { openDownload(context, notice.id) }
        },
        onDownloads = {
            downloads.notice = null
            onDownloads()
        },
        privateTab = selectedTabPrivate,
        onRetry = { notice ->
            downloads.notice = null
            downloads.retry(notice.id)
        },
        onOpenPage = { url ->
            downloads.notice = null
            openDownloadPage(context, url)
        },
    )
}

/** Opens a download's page in a new tab of Vola, as a link from another app would. */
internal fun openDownloadPage(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(context.packageName))
    }.onFailure { V5Feedback.show(context, R.string.downloads_control_failed) }
}

/** Opens a finished download as the Downloads screen does; the store is read off the main thread. */
private suspend fun openDownload(context: Context, id: Long) {
    val target = withContext(Dispatchers.IO) {
        val repository = DownloadRepository(context.applicationContext)
        val entry = runCatching { repository.snapshot() }.getOrNull()?.firstOrNull { it.id == id }
            ?: return@withContext null
        val uri = runCatching { repository.contentUri(entry) }.getOrNull() ?: return@withContext null
        uri to (entry.mime.takeIf(String::isNotBlank) ?: context.contentResolver.getType(uri))
    }
    if (target == null) {
        V5Feedback.show(context, R.string.downloads_open_failed)
        return
    }
    val (uri, mime) = target
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime ?: "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }.onFailure { V5Feedback.show(context, R.string.downloads_open_failed) }
}

private fun downloadsController(context: Context, scope: CoroutineScope): DownloadsController {
    val repository = DownloadRepository(context)
    return DownloadsController(
        store = object : DownloadsController.DownloadStore {
            override fun snapshot() = repository.snapshot()
            override fun clear(entries: Collection<DownloadEntry>) = repository.clear(entries)
            override fun cancel(entry: DownloadEntry) = repository.cancel(entry)
            override fun togglePause(entry: DownloadEntry) = repository.togglePause(entry)
            override fun retry(entry: DownloadEntry) = repository.retry(entry)
        },
        worker = object : DownloadsController.Worker {
            override fun <T> run(work: () -> T, onResult: (T) -> Unit) {
                scope.launch { onResult(withContext(Dispatchers.IO) { work() }) }
            }
        },
    )
}

// Kept out of snapshot state: the effects read and write them without recomposing.
private class LastDownloads {
    var entries: List<DownloadEntry> = emptyList()
}

private class LastRing {
    var state: V5DownloadRingState? = null
}
