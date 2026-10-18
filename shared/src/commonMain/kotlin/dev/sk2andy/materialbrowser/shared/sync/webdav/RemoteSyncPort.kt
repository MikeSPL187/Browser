package dev.sk2andy.materialbrowser.shared.sync.webdav

/**
 * Sync through the person's own WebDAV or Nextcloud server (V27): tabs and bookmarks, end-to-end
 * encrypted with a phrase only the person knows. The platform adapter runs the network, keeps the
 * secrets in the system keystore and syncs when Vola opens and when it is left; the screens see
 * only this port. Callbacks and [addListener] updates arrive on the main thread.
 */
interface RemoteSyncPort {
    /** The current state; [addListener] hears every later one. */
    val state: RemoteSyncState

    /** [listener] hears each new [RemoteSyncState] until the handle is cancelled. */
    fun addListener(listener: (RemoteSyncState) -> Unit): RemoteSyncHandle

    /**
     * Connects to the server at the HTTPS [address] with [login] and an app password, then syncs
     * once. On a server that already holds Vola's data, [phrase] must open it: otherwise the
     * result is [RemoteSyncFailure.PhraseMismatch] and nothing on the server is touched. The
     * keystore holds the app password and the key derived from the phrase, never the phrase
     * itself; after a [RemoteSyncFailure.PhraseMismatch] the port holds the typed values in memory
     * for [startOver] until the next [connect], [startOver] or [disconnect].
     */
    fun connect(
        address: String,
        login: String,
        appPassword: String,
        phrase: String,
        onResult: (RemoteSyncConnectResult) -> Unit,
    )

    /** Syncs now; does nothing when not connected or already syncing. */
    fun syncNow()

    /** The tabs another device last published, in its order; empty for an unknown device. */
    fun deviceTabs(deviceId: String): List<RemoteSyncTab>

    /**
     * Stops sync on this phone and forgets the server and its secrets. This phone's entry leaves
     * the server too, so the other devices stop listing it; with [deleteServerData] all of Vola's
     * data on the server goes, for every device. [onDone] hears false when the server could not
     * be reached for that. Without [deleteServerData] the phone is disconnected anyway; with it,
     * the phone stays connected, so the deletion can be asked again: forgetting the app password
     * would leave the data on the server with no way to delete it from here.
     */
    fun disconnect(deleteServerData: Boolean, onDone: (Boolean) -> Unit)

    /**
     * Deletes Vola's data on the server for every device and starts again from this phone's tabs
     * and bookmarks. Right after a [RemoteSyncFailure.PhraseMismatch] answer to [connect], it uses
     * that connection's server and phrase; when connected, it keeps this phone's phrase. The
     * other devices then hear [RemoteSyncFailure.PhraseMismatch] until they connect again.
     */
    fun startOver(onResult: (RemoteSyncConnectResult) -> Unit)

    /** A build without the sync (System WebView flavor): not connected, and every call fails at once. */
    object Unavailable : RemoteSyncPort {
        override val state = RemoteSyncState()

        override fun addListener(listener: (RemoteSyncState) -> Unit) = RemoteSyncHandle.Done

        override fun connect(
            address: String,
            login: String,
            appPassword: String,
            phrase: String,
            onResult: (RemoteSyncConnectResult) -> Unit,
        ) = onResult(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable))

        override fun syncNow() = Unit

        override fun deviceTabs(deviceId: String): List<RemoteSyncTab> = emptyList()

        override fun disconnect(deleteServerData: Boolean, onDone: (Boolean) -> Unit) = onDone(true)

        override fun startOver(onResult: (RemoteSyncConnectResult) -> Unit) =
            onResult(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable))
    }
}

/** A running listener; [cancel] stops it. */
fun interface RemoteSyncHandle {
    fun cancel()

    companion object {
        val Done = RemoteSyncHandle {}
    }
}

/** What Settings → Sync shows about the own server. */
data class RemoteSyncState(
    /** The server's host, as the person typed it; null when not connected. */
    val server: String? = null,
    /** When the last sync finished; null before the first one. */
    val lastSyncedAtMillis: Long? = null,
    val syncing: Boolean = false,
    /** Why the last sync failed; null when it worked. */
    val lastError: RemoteSyncFailure? = null,
    /** The other devices on the server, most recently updated first. */
    val devices: List<RemoteSyncDevice> = emptyList(),
) {
    val connected: Boolean get() = server != null
}

/** Another device that syncs through the same server. */
data class RemoteSyncDevice(
    val id: String,
    val name: String,
    val tabCount: Int,
    val updatedAtMillis: Long,
)

/** One open tab of another device. */
data class RemoteSyncTab(val url: String, val title: String)

sealed interface RemoteSyncConnectResult {
    data object Connected : RemoteSyncConnectResult

    data class Failed(val reason: RemoteSyncFailure) : RemoteSyncConnectResult
}

/** What went wrong, as the person can act on it. */
enum class RemoteSyncFailure {
    /** No answer: a typo in the address, the server down, no network or a certificate the system does not trust. */
    Unreachable,

    /** The server refused the login or the app password. */
    LoginRejected,

    /** An `http://` address: the password and the files would travel in the clear. */
    NotHttps,

    /** Something answers there, but not as WebDAV. */
    NotWebDav,

    /**
     * The server holds Vola's data that this phrase does not open: another phrase, data started
     * over from another device, or files that are damaged. Nothing was overwritten.
     */
    PhraseMismatch,

    /** The server holds Vola's data in a newer format than this Vola reads: update Vola. */
    NewerFormat,

    /** The address's folder is not on the server (404): a typo in the path, or it was removed. */
    FolderNotFound,

    /** The server has no room left (507) or refuses a file this size (413). */
    ServerFull,

    /** The server asks to wait: a locked file (423), too many requests (429) or maintenance (503). */
    ServerBusy,

    /** The server failed on its side (another 5xx). */
    ServerError,
}
