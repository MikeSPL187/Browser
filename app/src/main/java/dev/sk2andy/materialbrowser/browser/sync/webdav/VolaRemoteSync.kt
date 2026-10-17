package dev.sk2andy.materialbrowser.browser.sync.webdav

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.transfer.LocalTransferLibraryRules
import dev.sk2andy.materialbrowser.browser.transfer.localTransferDeviceName
import dev.sk2andy.materialbrowser.data.sync.webdav.AndroidWebDavSyncStore
import dev.sk2andy.materialbrowser.shared.sync.webdav.RemoteSyncPort
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavBookmarkChanges
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavLocalState
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavRemoteSync
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncEngine
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncFormat
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncLibrary
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncRunner
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.Executors

/**
 * The app's one WebDAV sync (V27): network and keystore work on one background thread, the
 * library on the main thread, a sync when Vola comes to the foreground and when it leaves it
 * (the process lifecycle, so switching between Vola's own screens does not count). GeckoView
 * builds only; the screens take [port], and a System WebView build shows
 * [RemoteSyncPort.Unavailable].
 */
internal object VolaRemoteSync {
    @Volatile
    private var instance: WebDavRemoteSync? = null
    private var observing = false

    /** The sync, created on first use; call on the main thread. */
    fun port(context: Context): WebDavRemoteSync = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    /**
     * Lets the sync read and change [controller]'s tabs and favorites, then follows the app's
     * lifecycle. A newer controller (the activity was recreated) replaces the older one.
     */
    fun attach(context: Context, controller: BrowserController) {
        val port = port(context)
        port.attach(ControllerLibrary(controller, context.applicationContext))
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = port.onAppOpened()

                override fun onStop(owner: LifecycleOwner) = port.onAppLeft()
            },
        )
    }

    private fun create(context: Context): WebDavRemoteSync {
        val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "vola-webdav-sync").apply { isDaemon = true } }
        val mainHandler = Handler(Looper.getMainLooper())
        val runner = object : WebDavSyncRunner {
            override fun background(block: () -> Unit) = worker.execute(block)

            override fun main(block: () -> Unit) {
                mainHandler.post(block)
            }
        }
        return WebDavRemoteSync(
            engine = WebDavSyncEngine(OkHttpWebDavTransport(), AndroidWebDavSyncCrypto),
            store = AndroidWebDavSyncStore(context),
            runner = runner,
            newDeviceId = { WebDavSyncFormat.newDeviceId(AndroidWebDavSyncCrypto) },
            nowMillis = System::currentTimeMillis,
        )
    }

    /**
     * The active profile's open web tabs and favorites. The sync belongs to the profile that was
     * active when it connected; while another profile is active it waits, so that profile's
     * favorites are never taken for deletions of the synced ones.
     */
    private class ControllerLibrary(controller: BrowserController, private val context: Context) : WebDavSyncLibrary {
        /** Weak: the sync lives as long as the process, the controller only as long as its activity. */
        private val controllerReference = WeakReference(controller)

        /** The bookmarks the last [local] handed out, to notice an edit made while the sync ran. */
        private var snapshot: List<LocalTransferBookmark>? = null

        override fun currentScope(): String = controllerReference.get()?.activeProfileId.orEmpty()

        override fun local(scope: String): WebDavLocalState? {
            val controller = controllerReference.get() ?: return null
            if (scope != controller.activeProfileId) return null
            val bookmarks = LocalTransferLibraryRules.bookmarks(controller.store.loadFavoriteLibrary(scope))
            snapshot = bookmarks
            return WebDavLocalState(localTransferDeviceName(context), LocalTransferLibraryRules.tabs(controller.activeTabs), bookmarks)
        }

        /**
         * Applies [changes] when the favorites are still as [local] read them. After an edit made
         * meanwhile it applies nothing and answers with that earlier state, so the next sync sees
         * the edit as an edit: a bookmark deleted meanwhile becomes a deletion, an added one an addition.
         */
        override fun apply(scope: String, changes: WebDavBookmarkChanges): List<LocalTransferBookmark> {
            val controller = controllerReference.get() ?: return snapshot.orEmpty()
            val store = controller.store
            val current = store.loadFavoriteLibrary(scope)
            val before = LocalTransferLibraryRules.bookmarks(current)
            val read = snapshot ?: before
            if (before != read) return read
            if (changes.isEmpty) return before
            val next = runCatching {
                WebDavFavoriteRules.apply(current, changes, System.currentTimeMillis()) { UUID.randomUUID().toString() }
            }.getOrNull() ?: return before
            if (!store.saveFavoriteLibraryCommitted(next, current, scope)) return before
            if (scope == controller.activeProfileId) controller.reloadFavorites()
            return LocalTransferLibraryRules.bookmarks(next)
        }
    }
}
