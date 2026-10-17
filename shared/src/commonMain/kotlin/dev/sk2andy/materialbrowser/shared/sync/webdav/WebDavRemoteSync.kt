package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark

/** Where the adapter keeps the account and the memory between syncs; called off the main thread. */
interface WebDavSyncStore {
    fun loadAccount(): WebDavStoredAccount?

    fun saveAccount(stored: WebDavStoredAccount): Boolean

    fun loadMemory(): WebDavSyncMemory?

    fun saveMemory(memory: WebDavSyncMemory): Boolean

    /** Forgets the account and the memory. */
    fun clear()
}

/** This phone's tabs and bookmarks for one library [scope] (a profile); called on the main thread. */
interface WebDavSyncLibrary {
    /** The scope a new connection syncs: the active profile. */
    fun currentScope(): String

    /** What [scope] holds now; null when it is gone (the profile was deleted). */
    fun local(scope: String): WebDavLocalState?

    /** Applies [changes] to [scope]'s bookmarks and returns the bookmarks it holds afterwards. */
    fun apply(scope: String, changes: WebDavBookmarkChanges): List<LocalTransferBookmark>
}

/** Runs the sync's work: [background] one block at a time in order, [main] on the main thread. */
interface WebDavSyncRunner {
    fun background(block: () -> Unit)

    fun main(block: () -> Unit)
}

/**
 * [RemoteSyncPort] over [WebDavSyncEngine] (V27): the network and the keystore off the main
 * thread, the library and the state on it. One sync at a time; a request during a sync runs one
 * more sync after it. The adapter calls [onAppOpened] and [onAppLeft]; there is no background job.
 */
class WebDavRemoteSync(
    private val engine: WebDavSyncEngine,
    private val store: WebDavSyncStore,
    private val runner: WebDavSyncRunner,
    private val newDeviceId: () -> String,
    private val nowMillis: () -> Long,
) : RemoteSyncPort {
    private val listeners = LinkedHashSet<(RemoteSyncState) -> Unit>()
    private var library: WebDavSyncLibrary? = null
    private var stored: WebDavStoredAccount? = null
    private var memory = WebDavSyncMemory()
    private var syncAgain = false
    private var busy = false
    private var retries = 0

    /** The typed values of a connect that answered [RemoteSyncFailure.PhraseMismatch], for [startOver]. */
    private var mismatched: PendingConnect? = null

    override var state = RemoteSyncState()
        private set

    init {
        runner.background {
            val account = store.loadAccount()
            val remembered = store.loadMemory() ?: WebDavSyncMemory()
            runner.main {
                if (stored == null && !busy) {
                    stored = account
                    memory = remembered
                    if (account != null) update(state.copy(server = account.account.host, devices = summaries(remembered)))
                }
            }
        }
    }

    /** Gives the sync this phone's library; until then nothing syncs. */
    fun attach(library: WebDavSyncLibrary) {
        this.library = library
    }

    fun onAppOpened() = syncNow()

    fun onAppLeft() = syncNow()

    override fun addListener(listener: (RemoteSyncState) -> Unit): RemoteSyncHandle {
        listeners += listener
        return RemoteSyncHandle { listeners -= listener }
    }

    override fun connect(
        address: String,
        login: String,
        appPassword: String,
        phrase: String,
        onResult: (RemoteSyncConnectResult) -> Unit,
    ) {
        mismatched = null
        val scope = library?.currentScope() ?: return onResult(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable))
        val pending = PendingConnect(address, login, appPassword, phrase)
        exclusive(onResult) {
            val bytes = phrase.encodeToByteArray()
            val outcome = try {
                engine.connect(address, login, appPassword, bytes, newDeviceId())
            } finally {
                bytes.fill(0)
            }
            finishConnect(outcome, scope, onResult) { reason ->
                if (reason == RemoteSyncFailure.PhraseMismatch) mismatched = pending
            }
        }
    }

    override fun syncNow() {
        val account = stored ?: return
        val library = library ?: return
        if (busy) {
            syncAgain = true
            return
        }
        val local = library.local(account.scope) ?: return
        busy = true
        syncAgain = false
        update(state.copy(syncing = true))
        val before = memory
        runner.background {
            val pulled = engine.pull(account.account, local, before, nowMillis())
            runner.main {
                if (stored !== account) return@main done()
                when (pulled) {
                    is WebDavPullOutcome.Failed -> finishSync(pulled.reason, devices = state.devices)
                    is WebDavPullOutcome.Pulled -> {
                        val applied = library.apply(account.scope, pulled.plan.changes)
                        runner.background {
                            val pushed = engine.push(account.account, pulled.plan, applied, nowMillis())
                            runner.main {
                                if (stored !== account) return@main done()
                                memory = pushed.memory
                                // Saved from the main thread's side, so a disconnect queued meanwhile clears it after.
                                runner.background { store.saveMemory(pushed.memory) }
                                retries = if (pushed.again) retries + 1 else 0
                                if (pushed.again && retries <= MAX_RETRIES) syncAgain = true
                                finishSync(pushed.failure, devices = summaries(pushed.memory))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun deviceTabs(deviceId: String) = memory.devices[deviceId]?.record?.tabs
        ?.map { RemoteSyncTab(it.url, it.title) }
        .orEmpty()

    override fun disconnect(deleteServerData: Boolean, onDone: (Boolean) -> Unit) {
        mismatched = null
        val account = stored
        stored = null
        memory = WebDavSyncMemory()
        syncAgain = false
        update(RemoteSyncState())
        runner.background {
            val removed = account == null || engine.remove(account.account, deleteServerData)
            store.clear()
            runner.main { onDone(removed) }
        }
    }

    override fun startOver(onResult: (RemoteSyncConnectResult) -> Unit) {
        val pending = mismatched
        val account = stored
        val scope = account?.scope ?: library?.currentScope()
        if ((pending == null && account == null) || scope == null) {
            return onResult(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable))
        }
        mismatched = null
        exclusive(onResult) {
            val outcome = if (pending != null) {
                val bytes = pending.phrase.encodeToByteArray()
                try {
                    engine.startOver(pending.address, pending.login, pending.appPassword, bytes, newDeviceId())
                } finally {
                    bytes.fill(0)
                }
            } else {
                engine.startOver(requireNotNull(account).account)
            }
            finishConnect(outcome, scope, onResult) {}
        }
    }

    /** Runs [work] off the main thread with no sync in flight; a busy port answers at once. */
    private fun exclusive(onResult: (RemoteSyncConnectResult) -> Unit, work: () -> Unit) {
        if (busy) return onResult(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable))
        busy = true
        update(state.copy(syncing = true))
        runner.background(work)
    }

    /** Called off the main thread with a connect's [outcome]: stores the account, then syncs. */
    private fun finishConnect(
        outcome: WebDavConnectOutcome,
        scope: String,
        onResult: (RemoteSyncConnectResult) -> Unit,
        onFailure: (RemoteSyncFailure) -> Unit,
    ) {
        when (outcome) {
            is WebDavConnectOutcome.Connected -> {
                val account = WebDavStoredAccount(outcome.account, scope)
                val saved = store.saveAccount(account) && store.saveMemory(WebDavSyncMemory())
                runner.main {
                    busy = false
                    if (!saved) {
                        update(state.copy(syncing = false))
                        return@main onResult(RemoteSyncConnectResult.Failed(RemoteSyncFailure.Unreachable))
                    }
                    stored = account
                    memory = WebDavSyncMemory()
                    update(RemoteSyncState(server = outcome.account.host))
                    onResult(RemoteSyncConnectResult.Connected)
                    syncNow()
                }
            }
            is WebDavConnectOutcome.Failed -> runner.main {
                busy = false
                onFailure(outcome.reason)
                update(state.copy(syncing = false))
                onResult(RemoteSyncConnectResult.Failed(outcome.reason))
            }
        }
    }

    private fun finishSync(failure: RemoteSyncFailure?, devices: List<RemoteSyncDevice>) {
        busy = false
        update(
            state.copy(
                syncing = false,
                lastError = failure,
                lastSyncedAtMillis = if (failure == null) nowMillis() else state.lastSyncedAtMillis,
                devices = devices,
            ),
        )
        if (syncAgain && failure == null) syncNow()
    }

    private fun done() {
        busy = false
        if (state.syncing) update(state.copy(syncing = false))
    }

    private fun update(next: RemoteSyncState) {
        if (next == state) return
        state = next
        listeners.toList().forEach { it(next) }
    }

    private fun summaries(memory: WebDavSyncMemory): List<RemoteSyncDevice> = memory.devices.values
        .map { RemoteSyncDevice(it.record.deviceId, it.record.name, it.record.tabs.size, it.record.updatedAtMillis) }
        .sortedByDescending { it.updatedAtMillis }

    private companion object {
        /** Syncs in a row after this device's file changed under the upload. */
        const val MAX_RETRIES = 2
    }

    private class PendingConnect(val address: String, val login: String, val appPassword: String, val phrase: String) {
        override fun toString() = "PendingConnect"
    }
}
