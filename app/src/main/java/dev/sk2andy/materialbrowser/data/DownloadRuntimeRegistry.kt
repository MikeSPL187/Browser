package dev.sk2andy.materialbrowser.data

import java.util.concurrent.atomic.AtomicInteger

/** Process-only progress for Gecko rows while MediaStore keeps them hidden as pending. */
internal object DownloadRuntimeRegistry {
    private val nextId = AtomicInteger(1)
    private val entries = linkedMapOf<Int, DownloadEntry>()
    private data class Controls(
        val cancel: () -> Unit,
        val pause: (() -> Boolean)?,
        val resume: (() -> Boolean)?,
    )
    private val controls = linkedMapOf<Int, Controls>()

    /** How failed or cancelled rows start again, oldest first; see [offerRetry]. */
    private val retries = linkedMapOf<Int, () -> Boolean>()

    fun nextTransferId(): Int = nextId.getAndUpdate { value ->
        if (value == Int.MAX_VALUE) 1 else value + 1
    }

    @Synchronized
    fun started(
        id: Int,
        name: String,
        source: String,
        mime: String,
        total: Long,
        startedAt: Long,
        mediaStoreId: Long? = null,
        cancel: (() -> Unit)? = null,
        pause: (() -> Boolean)? = null,
        resume: (() -> Boolean)? = null,
    ) {
        entries[id] = DownloadEntry(
            id = mediaStoreId?.let(DownloadEntryIds::encodeMediaStoreId) ?: encodeRuntimeId(id),
            name = name,
            source = source,
            status = DownloadStatus.Running,
            bytes = 0L,
            total = total,
            lastModified = startedAt,
            mime = mime,
            supportsPause = pause != null && resume != null,
            supportsCancel = cancel != null,
        )
        if (cancel != null) controls[id] = Controls(cancel, pause, resume)
    }

    @Synchronized
    fun progress(id: Int, bytes: Long, total: Long, updatedAt: Long) {
        val current = entries[id] ?: return
        entries[id] = current.copy(
            bytes = bytes.coerceAtLeast(0L),
            total = total,
            lastModified = updatedAt,
        )
    }

    @Synchronized
    fun completed(id: Int) {
        entries.remove(id)
        controls.remove(id)
        retries.remove(id)
    }

    @Synchronized
    fun failed(id: Int, cancelled: Boolean, updatedAt: Long) {
        val current = entries[id] ?: return
        entries[id] = current.copy(
            status = if (cancelled) DownloadStatus.Cancelled else DownloadStatus.Failed,
            lastModified = updatedAt,
        )
        controls.remove(id)
    }

    /**
     * Lets the failed or cancelled row of transfer [id] start again through [start], which runs off
     * the lock once [retry] has turned the row pending. [start] answers whether the attempt began;
     * whatever comes of it, it ends in [completed] (a new row took over) or [retryFailed]. Only the
     * latest [MAX_RETRIES] rows keep theirs: each holds an address in memory. Answers whether the
     * row took it; one that never started writing has no row.
     */
    @Synchronized
    fun offerRetry(id: Int, start: () -> Boolean): Boolean {
        val current = entries[id]?.takeIf { entry -> entry.status.isRetryable } ?: return false
        entries[id] = current.copy(retry = DownloadRetry.Available)
        retries.remove(id)
        retries[id] = start
        while (retries.size > MAX_RETRIES) {
            val oldest = retries.keys.first()
            withholdRetry(oldest, NOT_REMEMBERED)
        }
        return true
    }

    /** The transfers whose rows may still start again. */
    @Synchronized
    fun retryableTransferIds(): Set<Int> = retries.keys.toSet()

    @Synchronized
    fun withholdRetry(id: Int, retry: DownloadRetry.Unavailable) {
        retries.remove(id)
        val current = entries[id] ?: return
        entries[id] = current.copy(retry = retry)
    }

    /** Starts the row [entryId] again; it waits as pending, without controls, until the attempt ends. */
    fun retry(entryId: Long): Boolean {
        val start = synchronized(this) {
            val (id, entry) = entries.entries.firstOrNull { (_, entry) ->
                entry.id == entryId && entry.status.isRetryable
            } ?: return false
            val start = retries[id] ?: return false
            entries[id] = entry.copy(
                status = DownloadStatus.Pending,
                supportsPause = false,
                supportsCancel = false,
            )
            start
        }
        return start()
    }

    /** The attempt to start transfer [id] again ended without a new row; [retry] says whether it may run again. */
    @Synchronized
    fun retryFailed(id: Int, updatedAt: Long, retry: DownloadRetry = DownloadRetry.Available) {
        val current = entries[id]?.takeIf { entry -> entry.status == DownloadStatus.Pending } ?: return
        entries[id] = current.copy(status = DownloadStatus.Failed, lastModified = updatedAt)
        when {
            retry is DownloadRetry.Unavailable -> withholdRetry(id, retry)
            id !in retries -> withholdRetry(id, NOT_REMEMBERED)
        }
    }

    @Synchronized
    fun paused(id: Int, paused: Boolean, updatedAt: Long) {
        val current = entries[id]?.takeIf { it.status.isActive } ?: return
        entries[id] = current.copy(
            status = if (paused) DownloadStatus.Paused else DownloadStatus.Running,
            lastModified = updatedAt,
        )
    }

    fun cancel(entryId: Long): Boolean {
        val control = synchronized(this) {
            entries.entries.firstOrNull { (_, entry) -> entry.id == entryId && entry.status.isActive }
                ?.key?.let(controls::get)
        } ?: return false
        control.cancel()
        return true
    }

    fun cancelTransfer(transferId: Int): Boolean {
        val control = synchronized(this) {
            entries[transferId]
                ?.takeIf { entry -> entry.status.isActive }
                ?.let { controls[transferId] }
        } ?: return false
        control.cancel()
        return true
    }

    fun togglePause(entryId: Long): Boolean {
        val action = synchronized(this) {
            val (id, entry) = entries.entries.firstOrNull { (_, entry) ->
                entry.id == entryId && entry.status.isActive
            } ?: return false
            val control = controls[id] ?: return false
            if (entry.status == DownloadStatus.Paused) control.resume else control.pause
        } ?: return false
        return action()
    }

    fun togglePauseTransfer(transferId: Int): Boolean {
        val action = synchronized(this) {
            val entry = entries[transferId]?.takeIf { it.status.isActive } ?: return false
            val control = controls[transferId] ?: return false
            if (entry.status == DownloadStatus.Paused) control.resume else control.pause
        } ?: return false
        return action()
    }

    @Synchronized
    fun entryForTransfer(transferId: Int): DownloadEntry? = entries[transferId]

    @Synchronized
    fun activeTransferIds(): Set<Int> = entries.asSequence()
        .filter { (_, entry) -> entry.status.isActive }
        .map { (id, _) -> id }
        .toSet()

    @Synchronized
    fun snapshot(): List<DownloadEntry> = entries.values.toList()

    @Synchronized
    fun clear(ids: Collection<Long>) {
        entries.entries.filter { (_, entry) -> entry.id in ids }.forEach { (id, _) ->
            controls.remove(id)
            retries.remove(id)
        }
        entries.entries.removeAll { (_, entry) -> entry.id in ids }
    }

    @Synchronized
    fun containsEntryId(id: Long): Boolean = entries.values.any { entry -> entry.id == id }

    fun isRuntimeId(id: Long): Boolean = id <= RUNTIME_ID_MAX

    private val DownloadStatus.isRetryable: Boolean
        get() = this == DownloadStatus.Failed || this == DownloadStatus.Cancelled

    private fun encodeRuntimeId(id: Int): Long = Long.MIN_VALUE + id.toLong()
    private const val RUNTIME_ID_MAX = Long.MIN_VALUE + Int.MAX_VALUE.toLong()
    private val NOT_REMEMBERED = DownloadRetry.Unavailable(DownloadRetryUnavailable.NotRemembered)

    const val MAX_RETRIES = 20
}

internal object DownloadEntryIds {
    fun encodeMediaStoreId(id: Long): Long = -id - 1L

    fun decodeMediaStoreId(encodedId: Long): Long = -encodedId - 1L
}
