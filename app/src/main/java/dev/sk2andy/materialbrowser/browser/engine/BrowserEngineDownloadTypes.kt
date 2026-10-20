package dev.sk2andy.materialbrowser.browser.engine

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.DownloadDirectoryRules
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import java.io.Closeable
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

internal data class GeckoDownloadTransferStart(
    val id: Int,
    val fileName: String,
    val mimeType: String,
    val sourceUrl: String,
    val referrer: String?,
    val startedAtMillis: Long,
    val totalBytes: Long,
)

/** The final name and type of a transfer, known from its response before anything is written. */
internal data class GeckoDownloadTransferMetadata(
    val fileName: String,
    val mimeType: String,
    val sourceUrl: String,
)

/** The one answer to [GeckoDownloadTransferListener.onMetadata]; later answers are ignored. */
internal interface GeckoDownloadMetadataDecision {
    /** Writes the file. */
    fun proceed()

    /** Closes the response; nothing is written and no failure is reported. */
    fun abort()
}

internal interface GeckoDownloadTransferListener {
    /**
     * The final name and type are known and nothing is written yet. The transfer waits, holding
     * the response open, until [decision] proceeds or aborts; by default it proceeds at once.
     */
    fun onMetadata(metadata: GeckoDownloadTransferMetadata, decision: GeckoDownloadMetadataDecision) =
        decision.proceed()

    fun onStarted(start: GeckoDownloadTransferStart)

    fun onProgress(bytesReceived: Long, totalBytes: Long) = Unit

    fun onComplete(bytesReceived: Long) = Unit

    fun onFailed(reason: GeckoDownloadFailure) = Unit
}

internal enum class GeckoDownloadFailure {
    InvalidRequest,
    Network,
    Http,
    Storage,
    Cancelled,
}

internal object GeckoDownloadResponseRules {
    fun shouldRejectStatus(
        uri: String,
        statusCode: Int,
        allowHttpErrors: Boolean,
    ): Boolean {
        if (allowHttpErrors || statusCode in 200..299) return false
        return statusCode != 0 || !uri.startsWith("blob:", ignoreCase = true)
    }
}

/** What a failed transfer may do again (V22): a retry fetches the file from the start by GET. */
internal object GeckoDownloadRetryRules {
    /** A `blob:` address lives only as long as its page, which [pageUrl] names. */
    fun offer(url: String, pageUrl: String?): DownloadRetry = when {
        url.startsWith("blob:", ignoreCase = true) ->
            DownloadRetry.Unavailable(DownloadRetryUnavailable.Blob, pageUrl)
        url.startsWith("https:", ignoreCase = true) || url.startsWith("http:", ignoreCase = true) ->
            DownloadRetry.Available
        else -> DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered)
    }

    /**
     * A retry saves only the file the first attempt was writing. A page's response may have come
     * from a POST, which the retry cannot repeat: answered by GET, the server may send an error page
     * or another file, and saving that under the download's name would be a lie.
     */
    fun isSameFile(expected: GeckoDownloadTransferMetadata, actual: GeckoDownloadTransferMetadata): Boolean =
        expected.fileName == actual.fileName && expected.mimeType == actual.mimeType

    /** A private tab's download is not retried once the tab is closed; its rows never name its page. */
    val privateClosed = DownloadRetry.Unavailable(DownloadRetryUnavailable.PrivateClosed)
}

internal object GeckoDownloadStreamRules {
    /**
     * Opens the output of a row that was just inserted. When opening fails, by returning null or by
     * throwing, the row is rolled back so no pending file is left behind, and the failure goes on.
     */
    fun <T : Any> openOrRollback(open: () -> T?, rollback: () -> Unit): T {
        val output = try {
            open()
        } catch (failure: Throwable) {
            runCatching(rollback)
            throw failure
        }
        if (output == null) {
            runCatching(rollback)
            error("MediaStore could not open download")
        }
        return output
    }
}

internal fun interface GeckoDownloadCancellation {
    fun cancel()
}

internal interface GeckoDownloadStreamSink {
    fun open(fileName: String, mimeType: String): GeckoDownloadStreamEntry
}

internal interface GeckoDownloadStreamEntry : Closeable {
    val uri: Uri
    val output: OutputStream

    fun commit()

    fun abort()
}

/** Scoped-storage writer. Incomplete and cancelled transfers never become visible downloads. */
internal class MediaStoreDownloadStreamSink(context: Context) : GeckoDownloadStreamSink {
    private val resolver = context.applicationContext.contentResolver
    private val settingsStore = BrowserSessionStore(context.applicationContext)

    override fun open(fileName: String, mimeType: String): GeckoDownloadStreamEntry {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                DownloadDirectoryRules.mediaStoreRelativePath(
                    settingsStore.loadDownloadSettings().downloadSubdirectory,
                ),
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
        ) { "MediaStore rejected download" }
        val output = GeckoDownloadStreamRules.openOrRollback(
            open = { resolver.openOutputStream(uri, "w") },
            rollback = { resolver.delete(uri, null, null) },
        )
        return MediaStoreDownloadStreamEntry(resolver, uri, output)
    }
}

private class MediaStoreDownloadStreamEntry(
    private val resolver: ContentResolver,
    override val uri: Uri,
    override val output: OutputStream,
) : GeckoDownloadStreamEntry {
    private val finished = AtomicBoolean(false)

    @Synchronized
    override fun commit() {
        if (finished.get()) return
        output.close()
        val updated = resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null,
        )
        check(updated == 1) { "MediaStore could not publish download" }
        finished.set(true)
    }

    @Synchronized
    override fun abort() {
        if (!finished.compareAndSet(false, true)) return
        runCatching(output::close)
        resolver.delete(uri, null, null)
    }

    override fun close() = abort()
}
