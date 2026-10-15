package dev.sk2andy.materialbrowser.browser.gecko

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dev.sk2andy.materialbrowser.browser.downloads.CandyDownloadNotifier
import dev.sk2andy.materialbrowser.data.BrowserDownloadRequestFactory
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.DownloadDirectoryRules
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadRetryUnavailable
import dev.sk2andy.materialbrowser.data.DownloadRuntimeRegistry
import dev.sk2andy.materialbrowser.data.SafeDownloadValues
import java.io.Closeable
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import org.mozilla.geckoview.WebResponse

internal data class GeckoDownloadOwner(
    val profileId: String,
    val isPrivate: Boolean,
    val sessionKey: Any,
    /** The owner tab's own cookie jar ([GeckoProfileStorageRules.contextId]); null for the shared one. */
    val contextId: String? = null,
)

internal object GeckoDownloadFetchRules {
    /**
     * Gecko's fetch always runs in the shared cookie jar (or the shared private one) and cannot
     * reach a tab's own: an isolated profile's or a private tab's. Rather than send another jar's
     * cookies and keep the ones the site sets in it, such a fetch goes without cookies (V29).
     */
    fun flags(requested: Int, owner: GeckoDownloadOwner): Int {
        var flags = requested
        if (owner.isPrivate) flags = flags or GeckoWebExecutor.FETCH_FLAGS_PRIVATE
        if (owner.contextId != null) flags = flags or GeckoWebExecutor.FETCH_FLAGS_ANONYMOUS
        return flags
    }
}

internal data class GeckoDownloadTransferRequest(
    val owner: GeckoDownloadOwner,
    val request: WebRequest,
    val fetchFlags: Int,
    val suggestedFileName: String? = null,
    val allowHttpErrors: Boolean = false,
    /**
     * Whether a failed or cancelled transfer may start again by the same GET ([GeckoDownloadRetryRules]).
     * An extension's own download is not: the extension follows it and would not see a retry.
     */
    val retryable: Boolean = false,
)

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

/**
 * Fetches downloads inside Gecko, then streams into scoped storage. No cookie material crosses the
 * engine boundary; a tab with its own cookie jar fetches without cookies ([GeckoDownloadFetchRules]).
 */
internal class GeckoDownloadTransferManager(
    context: Context,
    private val executor: GeckoWebExecutor,
    private val sink: GeckoDownloadStreamSink = MediaStoreDownloadStreamSink(context),
    private val notifier: CandyDownloadNotifier = CandyDownloadNotifier(context),
) : AutoCloseable {
    private class Operation(
        val ownerKey: Any,
        /** How to fetch it again, or null when it may not be retried. */
        val retrySource: RetrySource?,
        val cancelled: AtomicBoolean = AtomicBoolean(false),
        val terminal: AtomicBoolean = AtomicBoolean(false),
    ) {
        @Volatile var response: WebResponse? = null
        @Volatile var entry: GeckoDownloadStreamEntry? = null
        @Volatile var metadata: GeckoDownloadTransferMetadata? = null
        val pauseLock = ReentrantLock()
        val pauseChanged = pauseLock.newCondition()
        var paused = false
    }

    private val io = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "candy-gecko-download").apply { isDaemon = true }
    }
    private val operations = ConcurrentHashMap<Int, Operation>()

    private class RetrySource(
        val owner: GeckoDownloadOwner,
        val url: String,
        val referrer: String?,
        val fetchFlags: Int,
    ) {
        /** The page a row may offer to open instead; a private tab's is not named outside it. */
        val pageUrl: String? get() = referrer.takeUnless { owner.isPrivate }
    }

    /**
     * A failed row's way back, in memory only. The tab is held weakly: a closed tab's download is
     * still retried (by the profile's jar), a closed private tab's is not.
     */
    private class RetryTicket(
        val profileId: String,
        val isPrivate: Boolean,
        val contextId: String?,
        val ownerKey: WeakReference<Any>,
        val url: String,
        val referrer: String?,
        val fetchFlags: Int,
        val expected: GeckoDownloadTransferMetadata,
    )
    private val retryTickets = ConcurrentHashMap<Int, RetryTicket>()
    private val closed = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("WrongConstant") // Gecko combines these @IntDef bit flags at runtime.
    fun start(
        transfer: GeckoDownloadTransferRequest,
        listener: GeckoDownloadTransferListener,
    ): GeckoDownloadCancellation? {
        if (closed.get() || !isValid(transfer)) {
            dispatch { listener.onFailed(GeckoDownloadFailure.InvalidRequest) }
            return null
        }
        val id = DownloadRuntimeRegistry.nextTransferId()
        val retrySource = if (transfer.retryable) {
            RetrySource(transfer.owner, transfer.request.uri, transfer.request.referrer, transfer.fetchFlags)
        } else {
            null
        }
        val operation = Operation(transfer.owner.sessionKey, retrySource)
        operations[id] = operation
        val flags = GeckoDownloadFetchRules.flags(transfer.fetchFlags, transfer.owner)
        executor.fetch(transfer.request, flags).accept(
            { response ->
                if (response == null) {
                    fail(id, operation, listener, GeckoDownloadFailure.Network)
                } else {
                    receiveResponse(
                        id = id,
                        operation = operation,
                        response = response,
                        suggestedFileName = transfer.suggestedFileName,
                        referrer = transfer.request.referrer,
                        allowHttpErrors = transfer.allowHttpErrors,
                        listener = listener,
                    )
                }
            },
            { fail(id, operation, listener, GeckoDownloadFailure.Network) },
        )
        return GeckoDownloadCancellation {
            cancel(id, operation, listener)
        }
    }

    /** Consumes Gecko's original response body without a second, context-losing network request. */
    fun startResponse(
        owner: GeckoDownloadOwner,
        response: WebResponse,
        referrer: String?,
        listener: GeckoDownloadTransferListener,
    ): GeckoDownloadCancellation? {
        if (closed.get() || owner.profileId.isBlank() || response.body == null) {
            runCatching { response.body?.close() }
            dispatch { listener.onFailed(GeckoDownloadFailure.InvalidRequest) }
            return null
        }
        val id = DownloadRuntimeRegistry.nextTransferId()
        // Gecko does not say how the page asked for it; a retry asks by GET (GeckoDownloadRetryRules).
        val retrySource = RetrySource(owner, response.uri, referrer, GeckoWebExecutor.FETCH_FLAGS_NONE)
        val operation = Operation(owner.sessionKey, retrySource)
        operations[id] = operation
        receiveResponse(
            id = id,
            operation = operation,
            response = response,
            suggestedFileName = null,
            referrer = referrer,
            allowHttpErrors = false,
            listener = listener,
        )
        return GeckoDownloadCancellation { cancel(id, operation, listener) }
    }

    fun cancelOwner(ownerKey: Any) {
        operations.entries
            .filter { (_, operation) -> operation.ownerKey === ownerKey }
            .forEach { (id, operation) -> cancel(id, operation, null) }
        retryTickets.entries
            .filter { (_, ticket) -> ticket.isPrivate && ticket.ownerKey.get() === ownerKey }
            .forEach { (id, _) -> forgetRetry(id, GeckoDownloadRetryRules.privateClosed) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        operations.entries.toList().forEach { (id, operation) -> cancel(id, operation, null) }
        retryTickets.keys.toList().forEach { id ->
            forgetRetry(id, DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered))
        }
        io.shutdownNow()
    }

    /** Lets the failed row of [id] start again, if its transfer had begun writing and may be retried. */
    private fun offerRetry(id: Int, operation: Operation) {
        val source = operation.retrySource ?: return
        val metadata = operation.metadata ?: return
        when (val offer = GeckoDownloadRetryRules.offer(source.url, source.pageUrl)) {
            is DownloadRetry.Unavailable -> DownloadRuntimeRegistry.withholdRetry(id, offer)
            DownloadRetry.Available -> {
                // Rows the registry stopped remembering, or the user cleared, drop their tickets.
                retryTickets.keys.retainAll(DownloadRuntimeRegistry.retryableTransferIds())
                retryTickets[id] = RetryTicket(
                    profileId = source.owner.profileId,
                    isPrivate = source.owner.isPrivate,
                    contextId = source.owner.contextId,
                    ownerKey = WeakReference(source.owner.sessionKey),
                    url = source.url,
                    referrer = source.referrer,
                    fetchFlags = source.fetchFlags,
                    expected = metadata,
                )
                if (!DownloadRuntimeRegistry.offerRetry(id) { retry(id) }) retryTickets.remove(id)
            }
        }
    }

    private fun forgetRetry(id: Int, reason: DownloadRetry.Unavailable) {
        retryTickets.remove(id)
        DownloadRuntimeRegistry.withholdRetry(id, reason)
    }

    /** Fetches the failed row [failedId] again; its row stays until a new one starts writing. */
    private fun retry(failedId: Int): Boolean {
        val ticket = retryTickets[failedId]
        val ownerKey = ticket?.ownerKey?.get() ?: if (ticket?.isPrivate == false) Any() else null
        if (closed.get() || ticket == null || ownerKey == null) {
            retryTickets.remove(failedId)
            val reason = if (ticket?.isPrivate == true) {
                GeckoDownloadRetryRules.privateClosed
            } else {
                DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered)
            }
            DownloadRuntimeRegistry.retryFailed(failedId, System.currentTimeMillis(), reason)
            return false
        }
        // Gecko answers a fetch on the looper that asked for it.
        mainHandler.post { startRetry(failedId, ticket, ownerKey) }
        return true
    }

    private fun startRetry(failedId: Int, ticket: RetryTicket, ownerKey: Any) {
        val request = runCatching {
            WebRequest.Builder(ticket.url)
                .method("GET")
                .apply { ticket.referrer?.let(::referrer) }
                .build()
        }.getOrNull()
        if (request == null) {
            forgetRetry(failedId, DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered))
            DownloadRuntimeRegistry.retryFailed(failedId, System.currentTimeMillis())
            return
        }
        start(
            transfer = GeckoDownloadTransferRequest(
                owner = GeckoDownloadOwner(ticket.profileId, ticket.isPrivate, ownerKey, ticket.contextId),
                request = request,
                fetchFlags = ticket.fetchFlags,
                suggestedFileName = ticket.expected.fileName,
                retryable = true,
            ),
            listener = object : GeckoDownloadTransferListener {
                override fun onMetadata(
                    metadata: GeckoDownloadTransferMetadata,
                    decision: GeckoDownloadMetadataDecision,
                ) {
                    if (GeckoDownloadRetryRules.isSameFile(ticket.expected, metadata)) {
                        decision.proceed()
                    } else {
                        decision.abort()
                        retryTickets.remove(failedId)
                        DownloadRuntimeRegistry.retryFailed(
                            id = failedId,
                            updatedAt = System.currentTimeMillis(),
                            retry = DownloadRetry.Unavailable(
                                DownloadRetryUnavailable.ResponseChanged,
                                pageUrl = ticket.referrer.takeUnless { ticket.isPrivate },
                            ),
                        )
                    }
                }

                override fun onStarted(start: GeckoDownloadTransferStart) {
                    retryTickets.remove(failedId)
                    DownloadRuntimeRegistry.completed(failedId)
                }

                // Before a new row: the old one may try again. After: the new row has its own ticket.
                override fun onFailed(reason: GeckoDownloadFailure) =
                    DownloadRuntimeRegistry.retryFailed(failedId, System.currentTimeMillis())
            },
        )
    }

    private fun receiveResponse(
        id: Int,
        operation: Operation,
        response: WebResponse,
        suggestedFileName: String?,
        referrer: String?,
        allowHttpErrors: Boolean,
        listener: GeckoDownloadTransferListener,
    ) {
        operation.response = response
        val body = response.body
        if (body == null) {
            fail(id, operation, listener, GeckoDownloadFailure.Network)
            return
        }
        if (operation.cancelled.get()) {
            runCatching(body::close)
            operations.remove(id, operation)
            return
        }
        if (
            GeckoDownloadResponseRules.shouldRejectStatus(
                uri = response.uri,
                statusCode = response.statusCode,
                allowHttpErrors = allowHttpErrors,
            )
        ) {
            runCatching(body::close)
            fail(id, operation, listener, GeckoDownloadFailure.Http)
            return
        }
        val contentType = response.header("content-type")
        val contentDisposition = response.header("content-disposition")
            ?: suggestedFileName?.let { name -> "attachment; filename=\"$name\"" }
        val candidateMimeType = SafeDownloadValues.mimeType(contentType, response.uri)
        val safeFileName = SafeDownloadValues.fileName(
            response.uri,
            contentDisposition,
            candidateMimeType,
        )
        val safeMimeType = SafeDownloadValues.finalMimeType(safeFileName, candidateMimeType)
        val totalBytes = response.header("content-length")?.toLongOrNull()?.coerceAtLeast(-1L) ?: -1L
        val metadata = GeckoDownloadTransferMetadata(safeFileName, safeMimeType, response.uri)
        val answered = AtomicBoolean(false)
        val decision = object : GeckoDownloadMetadataDecision {
            override fun proceed() {
                if (answered.compareAndSet(false, true)) {
                    open(id, operation, body, metadata, referrer, totalBytes, listener)
                }
            }

            override fun abort() {
                if (answered.compareAndSet(false, true)) discard(id, operation, body)
            }
        }
        dispatch { listener.onMetadata(metadata, decision) }
    }

    /** Starts writing once the final [metadata] may be saved. */
    private fun open(
        id: Int,
        operation: Operation,
        body: java.io.InputStream,
        metadata: GeckoDownloadTransferMetadata,
        referrer: String?,
        totalBytes: Long,
        listener: GeckoDownloadTransferListener,
    ) {
        if (operation.cancelled.get() || operation.terminal.get()) {
            discard(id, operation, body)
            return
        }
        val safeFileName = metadata.fileName
        val safeMimeType = metadata.mimeType
        operation.metadata = metadata
        val entry = runCatching { sink.open(safeFileName, safeMimeType) }.getOrElse {
            runCatching(body::close)
            fail(id, operation, listener, GeckoDownloadFailure.Storage)
            return
        }
        val started = GeckoDownloadTransferStart(
            id = id,
            fileName = safeFileName,
            mimeType = safeMimeType,
            sourceUrl = metadata.sourceUrl,
            referrer = referrer,
            startedAtMillis = System.currentTimeMillis(),
            totalBytes = totalBytes,
        )
        synchronized(operation) {
            if (operation.cancelled.get() || operation.terminal.get()) {
                runCatching(body::close)
                runCatching(entry::abort)
                operations.remove(id, operation)
                return
            }
            operation.entry = entry
            DownloadRuntimeRegistry.started(
                id = started.id,
                name = started.fileName,
                source = started.sourceUrl,
                mime = started.mimeType,
                total = started.totalBytes,
                startedAt = started.startedAtMillis,
                mediaStoreId = runCatching { ContentUris.parseId(entry.uri) }.getOrNull(),
                cancel = { cancel(id, operation, listener) },
                pause = { setPaused(id, operation, started, paused = true) },
                resume = { setPaused(id, operation, started, paused = false) },
            )
            dispatch { listener.onStarted(started) }
            notifier.started(started.id)
            io.execute { copy(id, operation, body, started, listener) }
        }
    }

    /** Drops a response that will not be written, without reporting a failure. */
    private fun discard(id: Int, operation: Operation, body: java.io.InputStream) {
        synchronized(operation) {
            operation.terminal.set(true)
            runCatching(body::close)
            operations.remove(id, operation)
        }
    }

    private fun copy(
        id: Int,
        operation: Operation,
        body: java.io.InputStream,
        started: GeckoDownloadTransferStart,
        listener: GeckoDownloadTransferListener,
    ) {
        var received = 0L
        val result = runCatching {
            body.use { input ->
                val entry = checkNotNull(operation.entry)
                entry.output.use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        operation.pauseLock.withLock {
                            while (operation.paused && !operation.cancelled.get()) {
                                operation.pauseChanged.await()
                            }
                        }
                        if (operation.cancelled.get()) throw DownloadCancelledException()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        received += count
                        val progress = received
                        synchronized(operation) {
                            if (operation.cancelled.get() || operation.terminal.get()) {
                                throw DownloadCancelledException()
                            }
                            DownloadRuntimeRegistry.progress(
                                id = id,
                                bytes = progress,
                                total = started.totalBytes,
                                updatedAt = System.currentTimeMillis(),
                            )
                            dispatch { listener.onProgress(progress, started.totalBytes) }
                            notifier.progress(started.id)
                        }
                    }
                }
            }
        }
        if (operation.cancelled.get() || result.exceptionOrNull() is DownloadCancelledException) {
            finishFailure(id, operation, listener, GeckoDownloadFailure.Cancelled)
        } else if (result.isFailure) {
            finishFailure(id, operation, listener, GeckoDownloadFailure.Network)
        } else {
            synchronized(operation) {
                if (operation.terminal.get()) return
                runCatching { operation.entry?.commit() }.fold(
                    onSuccess = {
                        if (!operation.terminal.compareAndSet(false, true)) return@fold
                        operations.remove(id, operation)
                        DownloadRuntimeRegistry.completed(id)
                        dispatch { listener.onComplete(received) }
                        operation.entry?.uri?.let { uri ->
                            notifier.complete(
                                transferId = started.id,
                                fileName = started.fileName,
                                mimeType = started.mimeType,
                                uri = uri,
                            )
                        }
                    },
                    onFailure = {
                        finishFailure(id, operation, listener, GeckoDownloadFailure.Storage)
                    },
                )
            }
        }
    }

    private fun cancel(
        id: Int,
        operation: Operation,
        listener: GeckoDownloadTransferListener?,
    ) {
        synchronized(operation) {
            if (!operation.terminal.compareAndSet(false, true)) return
            operation.cancelled.set(true)
            operation.pauseLock.withLock { operation.pauseChanged.signalAll() }
            runCatching { operation.response?.body?.close() }
            runCatching { operation.entry?.abort() }
            operations.remove(id, operation)
            DownloadRuntimeRegistry.failed(
                id = id,
                cancelled = true,
                updatedAt = System.currentTimeMillis(),
            )
            offerRetry(id, operation)
            notifier.cancel(id)
            listener?.let { target -> dispatch { target.onFailed(GeckoDownloadFailure.Cancelled) } }
        }
    }

    private fun setPaused(
        id: Int,
        operation: Operation,
        started: GeckoDownloadTransferStart,
        paused: Boolean,
    ): Boolean =
        synchronized(operation) {
            if (operation.terminal.get()) return false
            operation.pauseLock.withLock {
                operation.paused = paused
                operation.pauseChanged.signalAll()
            }
            DownloadRuntimeRegistry.paused(id, paused, System.currentTimeMillis())
            notifier.paused(started.id)
            true
        }

    private fun fail(
        id: Int,
        operation: Operation,
        listener: GeckoDownloadTransferListener,
        failure: GeckoDownloadFailure,
    ) {
        synchronized(operation) {
            if (!operation.terminal.compareAndSet(false, true)) return
            operations.remove(id, operation)
            DownloadRuntimeRegistry.failed(
                id = id,
                cancelled = failure == GeckoDownloadFailure.Cancelled,
                updatedAt = System.currentTimeMillis(),
            )
            offerRetry(id, operation)
            dispatch { listener.onFailed(failure) }
            notifier.cancel(id)
        }
    }

    private fun finishFailure(
        id: Int,
        operation: Operation,
        listener: GeckoDownloadTransferListener,
        failure: GeckoDownloadFailure,
    ) {
        synchronized(operation) {
            if (!operation.terminal.compareAndSet(false, true)) return
            operation.cancelled.set(true)
            runCatching { operation.entry?.abort() }
            operations.remove(id, operation)
            DownloadRuntimeRegistry.failed(
                id = id,
                cancelled = failure == GeckoDownloadFailure.Cancelled,
                updatedAt = System.currentTimeMillis(),
            )
            offerRetry(id, operation)
            dispatch { listener.onFailed(failure) }
            notifier.cancel(id)
        }
    }

    private fun isValid(transfer: GeckoDownloadTransferRequest): Boolean =
        transfer.owner.profileId.isNotBlank() &&
            BrowserDownloadRequestFactory.create(url = transfer.request.uri) != null

    private fun dispatch(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }

    private fun WebResponse.header(name: String): String? = headers.entries.firstOrNull {
        (headerName, _) -> headerName.equals(name, ignoreCase = true)
    }?.value

    private class DownloadCancelledException : Exception()

    private companion object {
        const val COPY_BUFFER_BYTES = 32 * 1024
    }
}
