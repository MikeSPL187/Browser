package dev.sk2andy.materialbrowser.browser.systemwebview

import android.content.ContentUris
import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.sk2andy.materialbrowser.browser.downloads.CandyDownloadNotifier
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadCancellation
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadFailure
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadStreamEntry
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadStreamSink
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadTransferListener
import dev.sk2andy.materialbrowser.browser.gecko.GeckoDownloadTransferStart
import dev.sk2andy.materialbrowser.browser.gecko.MediaStoreDownloadStreamSink
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import dev.sk2andy.materialbrowser.data.DownloadRuntimeRegistry
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** What a private tab's download sends: the cookies are read again for every hop. */
internal data class SystemWebViewPrivateDownloadRequest(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val pageUrl: String?,
    val userAgent: String,
    val cookiesFor: (String) -> String?,
)

/**
 * Streams a private tab's download straight into Downloads, as blob downloads already are (V29).
 * Android's DownloadManager keeps every download's address and request headers, the private
 * profile's cookies included, in its own database long after the private session has ended; this
 * transfer leaves only the file. A failed or cancelled row may start again while the tab lives
 * (V22): its request stays in memory only, and the retry sends what the first attempt did.
 */
internal class SystemWebViewPrivateDownloadTransfer(
    context: Context,
    private val sink: GeckoDownloadStreamSink = MediaStoreDownloadStreamSink(context),
    private val notifier: CandyDownloadNotifier = CandyDownloadNotifier(context),
) : AutoCloseable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "candy-system-private-download").apply { isDaemon = true }
    }

    /** Set when the tab goes away: its downloads finish, as DownloadManager's did, but quietly. */
    @Volatile
    private var detached = false

    /** The requests of this tab's rows that may start again, by transfer id. */
    private val retryRequests = ConcurrentHashMap<Int, SystemWebViewPrivateDownloadRequest>()

    fun start(
        request: SystemWebViewPrivateDownloadRequest,
        listener: GeckoDownloadTransferListener,
    ): GeckoDownloadCancellation {
        val id = DownloadRuntimeRegistry.nextTransferId()
        val cancelled = AtomicBoolean(false)
        io.execute { transfer(id, request, listener, cancelled) }
        return GeckoDownloadCancellation { cancelled.set(true) }
    }

    /** The tab is gone: its downloads finish quietly, and its failed rows no longer start again. */
    override fun close() {
        detached = true
        retryRequests.keys.toList().forEach { id -> forgetRetry(id) }
    }

    /** Lets the failed or cancelled row of [id] start again with the same [request]. */
    private fun offerRetry(id: Int, request: SystemWebViewPrivateDownloadRequest) {
        // Rows the registry stopped remembering, or the user cleared, drop their requests.
        retryRequests.keys.retainAll(DownloadRuntimeRegistry.retryableTransferIds())
        retryRequests[id] = request
        if (!DownloadRuntimeRegistry.offerRetry(id) { retry(id) }) retryRequests.remove(id)
        // The tab may have closed meanwhile; then close() has not seen this row.
        if (detached) forgetRetry(id)
    }

    private fun forgetRetry(id: Int) {
        retryRequests.remove(id)
        DownloadRuntimeRegistry.withholdRetry(id, PRIVATE_CLOSED)
    }

    /** Fetches the failed row [failedId] again; its row stays until the new one starts writing. */
    private fun retry(failedId: Int): Boolean {
        val request = retryRequests[failedId]
        if (detached || request == null) {
            retryRequests.remove(failedId)
            val reason = if (detached) PRIVATE_CLOSED else NOT_REMEMBERED
            DownloadRuntimeRegistry.retryFailed(failedId, System.currentTimeMillis(), reason)
            return false
        }
        val id = DownloadRuntimeRegistry.nextTransferId()
        io.execute { transfer(id, request, RetryListener, AtomicBoolean(false), replaces = failedId) }
        return true
    }

    /**
     * Streams [request] as transfer [id]. A retry [replaces] the failed row: the new row takes its
     * place once it starts writing, and until then a failure leaves the old row to try again.
     */
    private fun transfer(
        id: Int,
        request: SystemWebViewPrivateDownloadRequest,
        listener: GeckoDownloadTransferListener,
        cancelled: AtomicBoolean,
        replaces: Int? = null,
    ) {
        var entry: GeckoDownloadStreamEntry? = null
        var started = false
        val failure = try {
            val connection = connect(request, cancelled)
            try {
                val total = connection.contentLengthLong
                val opened = try {
                    sink.open(request.fileName, request.mimeType)
                } catch (cause: Exception) {
                    throw StorageFailure(cause)
                }
                entry = opened
                val start = GeckoDownloadTransferStart(
                    id = id,
                    fileName = request.fileName,
                    mimeType = request.mimeType,
                    sourceUrl = request.pageUrl ?: request.url,
                    referrer = null,
                    startedAtMillis = System.currentTimeMillis(),
                    totalBytes = total,
                )
                DownloadRuntimeRegistry.started(
                    id = id,
                    name = start.fileName,
                    source = start.sourceUrl,
                    mime = start.mimeType,
                    total = total,
                    startedAt = start.startedAtMillis,
                    mediaStoreId = runCatching { ContentUris.parseId(opened.uri) }.getOrNull(),
                    cancel = { cancelled.set(true) },
                )
                started = true
                if (replaces != null) {
                    retryRequests.remove(replaces)
                    DownloadRuntimeRegistry.completed(replaces)
                }
                dispatch { listener.onStarted(start) }
                notifier.started(id)
                val received = copy(id, connection, opened, total, listener, cancelled)
                if (total >= 0 && received != total) throw IOException("Download ended early")
                try {
                    opened.commit()
                } catch (cause: Exception) {
                    throw StorageFailure(cause)
                }
                DownloadRuntimeRegistry.completed(id)
                dispatch { listener.onComplete(received) }
                notifier.complete(id, start.fileName, start.mimeType, opened.uri)
                null
            } finally {
                connection.disconnect()
            }
        } catch (error: StorageFailure) {
            GeckoDownloadFailure.Storage
        } catch (error: HttpFailure) {
            GeckoDownloadFailure.Http
        } catch (error: Exception) {
            if (cancelled.get()) GeckoDownloadFailure.Cancelled else GeckoDownloadFailure.Network
        }
        if (failure != null) {
            runCatching { entry?.abort() }
            if (started) {
                DownloadRuntimeRegistry.failed(
                    id = id,
                    cancelled = failure == GeckoDownloadFailure.Cancelled,
                    updatedAt = System.currentTimeMillis(),
                )
                notifier.cancel(id)
                offerRetry(id, request)
            } else if (replaces != null) {
                DownloadRuntimeRegistry.retryFailed(
                    id = replaces,
                    updatedAt = System.currentTimeMillis(),
                    retry = if (detached) PRIVATE_CLOSED else DownloadRetry.Available,
                )
            }
            dispatch { listener.onFailed(failure) }
        }
    }

    /** Follows redirects itself, so each hop gets only its own site's cookies. */
    private fun connect(
        request: SystemWebViewPrivateDownloadRequest,
        cancelled: AtomicBoolean,
    ): HttpURLConnection {
        var url = request.url
        repeat(SystemWebViewPrivateDownloadRules.MAX_REDIRECTS + 1) {
            if (cancelled.get()) throw IOException("Cancelled")
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("User-Agent", request.userAgent)
                request.cookiesFor(url)?.takeIf(String::isNotBlank)?.let { setRequestProperty("Cookie", it) }
            }
            val status = connection.responseCode
            if (status in 200..299) return connection
            val next = if (status in 300..399) {
                SystemWebViewPrivateDownloadRules.nextHop(url, connection.getHeaderField("Location"))
            } else {
                null
            }
            connection.disconnect()
            url = next ?: throw HttpFailure()
        }
        throw HttpFailure()
    }

    private fun copy(
        id: Int,
        connection: HttpURLConnection,
        entry: GeckoDownloadStreamEntry,
        total: Long,
        listener: GeckoDownloadTransferListener,
        cancelled: AtomicBoolean,
    ): Long {
        var received = 0L
        var reportedAt = 0L
        val buffer = ByteArray(BUFFER_BYTES)
        connection.inputStream.use { input ->
            while (true) {
                if (cancelled.get()) throw IOException("Cancelled")
                val count = input.read(buffer)
                if (count < 0) break
                try {
                    entry.output.write(buffer, 0, count)
                } catch (cause: IOException) {
                    throw StorageFailure(cause)
                }
                received += count
                val now = System.currentTimeMillis()
                if (now - reportedAt >= PROGRESS_INTERVAL_MILLIS) {
                    reportedAt = now
                    val bytes = received
                    DownloadRuntimeRegistry.progress(id, bytes, total, now)
                    dispatch { listener.onProgress(bytes, total) }
                    notifier.progress(id)
                }
            }
        }
        return received
    }

    private fun dispatch(action: () -> Unit) {
        mainHandler.post { if (!detached) action() }
    }

    private class StorageFailure(cause: Throwable) : IOException(cause)

    private class HttpFailure : IOException()

    /** A retry reports through its row and notification; the tab has nothing to show for it. */
    private object RetryListener : GeckoDownloadTransferListener {
        override fun onStarted(start: GeckoDownloadTransferStart) = Unit
    }

    private companion object {
        /** A private tab's row never names its page. */
        val PRIVATE_CLOSED = DownloadRetry.Unavailable(DownloadRetryUnavailable.PrivateClosed)
        val NOT_REMEMBERED = DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered)

        const val TIMEOUT_MILLIS = 30_000
        const val BUFFER_BYTES = 64 * 1_024
        const val PROGRESS_INTERVAL_MILLIS = 250L
    }
}

internal object SystemWebViewPrivateDownloadRules {
    const val MAX_REDIRECTS = 5

    /**
     * Where a redirect may lead: an http(s) address without credentials in it, never from HTTPS
     * down to plain HTTP. Anything else ends the download.
     */
    fun nextHop(current: String, location: String?): String? = runCatching {
        if (location.isNullOrBlank()) return null
        val from = URI(current)
        val next = from.resolve(location.trim())
        val scheme = next.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (from.scheme.equals("https", ignoreCase = true) && scheme != "https") return null
        if (next.rawUserInfo != null || next.host.isNullOrBlank()) return null
        next.toString()
    }.getOrNull()
}
