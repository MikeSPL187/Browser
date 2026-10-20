package dev.sk2andy.materialbrowser.data

import android.content.Context
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal enum class HistoryRecordingMode(val storedId: String) {
    Enabled("enabled"),
    ClearOnExit("clear_on_exit"),
    Disabled("disabled"),
    ;

    internal companion object {
        fun fromStoredValue(value: String?): HistoryRecordingMode = when (value) {
            null, Enabled.storedId, Enabled.name -> Enabled
            ClearOnExit.storedId, ClearOnExit.name -> ClearOnExit
            Disabled.storedId, Disabled.name -> Disabled
            else -> Disabled
        }
    }
}

internal data class HistoryMutationResult(
    val history: List<HistoryEntry>,
    val committed: Boolean,
)

internal data class HistoryRecordResult(
    val history: List<HistoryEntry>,
    val recorded: Boolean,
)

internal class BrowsingHistoryRepository private constructor(
    context: Context,
    /** Saves a visit after it is shown: every visit wrote the whole history on the main thread (V-07). */
    private val background: Executor = Executors.newSingleThreadExecutor { task -> Thread(task, "Vola-history") },
) {
    private val appContext = context.applicationContext
    private val store = BrowserSessionStore(appContext)
    private val recallRepository = RecallRepository.get(appContext)

    /**
     * The history, read once and kept while the file stays as this process left it; a file
     * replaced from outside (a data transfer) is read again.
     */
    private var cache: List<HistoryEntry>? = null
    private var cacheStamp: String? = null

    /** A visit waits to be saved: the next background write saves the history as it is then. */
    private var saveScheduled = false

    private fun current(): List<HistoryEntry> {
        cache?.takeIf { store.historyStamp() == cacheStamp }?.let { return it }
        // Changed from outside: what is saved wins over a visit still waiting to be saved.
        saveScheduled = false
        return store.loadHistory().also { loaded ->
            cache = loaded
            cacheStamp = store.historyStamp()
        }
    }

    /** [updated] once the store has it; the cache follows only a write that succeeded. */
    private fun committed(updated: List<HistoryEntry>, saved: Boolean): Boolean {
        if (saved) {
            cache = updated
            cacheStamp = store.historyStamp()
        }
        return saved
    }

    @Synchronized
    fun snapshot(): List<HistoryEntry> = current()

    /**
     * Records a visit. It shows at once and is saved in the background: a visit lost to a crash in
     * that moment costs less than a stalled frame on every page. The Recall text of a visit that
     * falls off the end is deleted first, as before: it must not outlive its visit.
     */
    @Synchronized
    fun record(entry: HistoryEntry): HistoryRecordResult {
        val current = current()
        if (recordingMode() == HistoryRecordingMode.Disabled) {
            return HistoryRecordResult(history = current, recorded = false)
        }
        val updated = BrowsingLibraryRules.addHistory(current, entry)
        if (updated == current) return HistoryRecordResult(history = current, recorded = false)
        val retainedKeys = updated.mapNotNullTo(hashSetOf(), ::documentKey)
        val evicted = current.filter { existing -> documentKey(existing) !in retainedKeys }
        if (evicted.isNotEmpty() && !recallRepository.deleteEntries(evicted)) {
            return HistoryRecordResult(history = current, recorded = false)
        }
        cache = updated
        scheduleSave()
        return HistoryRecordResult(history = updated, recorded = true)
    }

    /** Renames one saved visit to a page title that arrived after it was recorded. */
    @Synchronized
    fun updateTitle(visitId: String, title: String): HistoryRecordResult {
        val current = current()
        val updated = BrowsingLibraryRules.renameHistoryVisit(current, visitId, title)
        if (updated == current) return HistoryRecordResult(history = current, recorded = false)
        cache = updated
        scheduleSave()
        return HistoryRecordResult(history = updated, recorded = true)
    }

    private fun scheduleSave() {
        if (saveScheduled) return
        saveScheduled = true
        background.execute(::saveNow)
    }

    /**
     * Writes the history as it is at this moment, under the same lock as every change, so a clear
     * that came first is never undone by an older copy.
     */
    @Synchronized
    private fun saveNow() {
        if (!saveScheduled) return
        saveScheduled = false
        val history = cache ?: return
        committed(history, store.commitHistory(history))
    }

    @Synchronized
    fun remove(entries: Collection<HistoryEntry>): HistoryMutationResult {
        val current = current()
        if (!recallRepository.deleteEntries(entries)) {
            return HistoryMutationResult(history = current, committed = false)
        }
        return mutateResult(current) { history ->
            BrowsingHistoryRules.removeEntries(history, entries)
        }
    }

    @Synchronized
    fun clearProfiles(
        profileIds: Set<String>,
        trailTabIds: Set<String> = emptySet(),
        recallAlreadyDeleted: Boolean = false,
    ): HistoryMutationResult {
        if (!recallAlreadyDeleted && !recallRepository.deleteProfiles(profileIds)) {
            return HistoryMutationResult(history = current(), committed = false)
        }
        return mutateWithTrailRedaction(trailTabIds) { current ->
            BrowsingHistoryRules.removeProfiles(current, profileIds)
        }
    }

    @Synchronized
    fun clearRange(
        request: HistoryClearRequest,
        trailTabIds: Set<String> = emptySet(),
    ): HistoryMutationResult {
        if (!recallRepository.deleteRange(request)) {
            return HistoryMutationResult(history = current(), committed = false)
        }
        return mutateWithTrailRedaction(
            trailTabIds = trailTabIds,
            sinceInclusiveMillis = request.sinceInclusiveMillis,
            untilExclusiveMillis = request.untilExclusiveMillis,
        ) { current ->
            BrowsingHistoryRules.removeRange(current, request)
        }
    }

    @Synchronized
    fun clear(): HistoryMutationResult {
        val current = current()
        if (!recallRepository.clear()) {
            return HistoryMutationResult(history = current, committed = false)
        }
        return mutateResult(current) { emptyList() }
    }

    @Synchronized
    fun recordingMode(): HistoryRecordingMode = store.loadHistoryRecordingMode()

    @Synchronized
    fun setRecordingMode(mode: HistoryRecordingMode): Boolean =
        store.saveHistoryRecordingMode(
            mode = mode,
            sessionActive = mode == HistoryRecordingMode.ClearOnExit,
        )

    @Synchronized
    fun beginSession() {
        val clearsOnExit = recordingMode() == HistoryRecordingMode.ClearOnExit
        if (clearsOnExit && store.loadHistorySessionActive()) {
            saveClearedHistoryWithTrailRedaction(
                sessionActive = true,
            )
        } else {
            store.saveHistorySessionActive(clearsOnExit)
        }
    }

    @Synchronized
    fun markForegroundSessionActive() {
        store.saveHistorySessionActive(recordingMode() == HistoryRecordingMode.ClearOnExit)
    }

    fun clearOnExit(): Boolean = clearOnExit(
        isSessionCurrent = { true },
        untilExclusiveMillis = Long.MAX_VALUE,
    )

    fun clearOnExit(
        isSessionCurrent: () -> Boolean,
        untilExclusiveMillis: Long,
    ): Boolean {
        val shouldClear = synchronized(this) {
            recordingMode() == HistoryRecordingMode.ClearOnExit && isSessionCurrent()
        }
        if (!shouldClear) return false
        val recallCleared = if (untilExclusiveMillis == Long.MAX_VALUE) {
            recallRepository.clear()
        } else {
            recallRepository.deleteRange(
                HistoryClearRequest(
                    profileIds = regularProfileIds(),
                    sinceInclusiveMillis = Long.MIN_VALUE,
                    untilExclusiveMillis = untilExclusiveMillis,
                ),
            )
        }
        if (!recallCleared) return false
        return synchronized(this) {
            val sessionStillCurrent = isSessionCurrent()
            val retainedHistory = if (sessionStillCurrent) {
                emptyList()
            } else {
                current().filter { entry ->
                    entry.lastVisitedAt >= untilExclusiveMillis
                }
            }
            saveClearedHistoryWithTrailRedaction(
                sessionActive = !sessionStillCurrent &&
                    recordingMode() == HistoryRecordingMode.ClearOnExit,
                clearRecall = false,
                retainedHistory = retainedHistory,
                untilExclusiveMillis = untilExclusiveMillis,
            )
        }
    }

    private fun mutateResult(
        current: List<HistoryEntry> = current(),
        transform: (List<HistoryEntry>) -> List<HistoryEntry>,
    ): HistoryMutationResult {
        val updated = transform(current)
        val committed = updated == current || committed(updated, store.commitHistory(updated))
        return HistoryMutationResult(
            history = if (committed) updated else current,
            committed = committed,
        )
    }

    private fun mutateWithTrailRedaction(
        trailTabIds: Set<String>,
        sinceInclusiveMillis: Long = Long.MIN_VALUE,
        untilExclusiveMillis: Long = Long.MAX_VALUE,
        transform: (List<HistoryEntry>) -> List<HistoryEntry>,
    ): HistoryMutationResult {
        val current = current()
        val updated = transform(current)
        val redaction = trailRedaction(
            tabIds = trailTabIds,
            sinceInclusiveMillis = sinceInclusiveMillis,
            untilExclusiveMillis = untilExclusiveMillis,
        )
        if (updated == current && redaction == null) {
            return HistoryMutationResult(history = updated, committed = true)
        }
        val saved = committed(
            updated,
            if (redaction == null) {
                store.commitHistory(updated)
            } else {
                store.saveHistoryAndTrailRedaction(updated, redaction)
            },
        )
        return HistoryMutationResult(
            history = if (saved) updated else current,
            committed = saved,
        )
    }

    private fun saveClearedHistoryWithTrailRedaction(
        sessionActive: Boolean,
        clearRecall: Boolean = true,
        retainedHistory: List<HistoryEntry> = emptyList(),
        untilExclusiveMillis: Long = Long.MAX_VALUE,
    ): Boolean {
        if (clearRecall && !recallRepository.clear()) return false
        val redaction = trailRedaction(
            tabIds = persistentRegularTabIds(),
            sinceInclusiveMillis = Long.MIN_VALUE,
            untilExclusiveMillis = untilExclusiveMillis,
        )
        return committed(
            retainedHistory,
            if (redaction == null) {
                store.saveHistoryAndSessionState(retainedHistory, sessionActive)
            } else {
                store.saveHistoryAndTrailRedaction(
                    history = retainedHistory,
                    redaction = redaction,
                    sessionActive = sessionActive,
                )
            },
        )
    }

    private fun persistentRegularTabIds(): Set<String> = buildSet {
        store.loadTabs().first.asSequence()
            .filterNot { tab -> tab.isIncognito }
            .mapTo(this, BrowserTab::id)
        SnoozedTabStore(appContext).load().asSequence()
            .map { snoozed -> snoozed.tab.id }
            .forEach(::add)
    }

    private fun regularProfileIds(): Set<String> = buildSet {
        store.loadProfiles().first.mapTo(this, BrowserProfile::id)
        current().mapTo(this, HistoryEntry::profileId)
    }

    private fun documentKey(entry: HistoryEntry): String? =
        CanonicalWebUrl.key(entry.url)?.let { canonical -> "${entry.profileId}\u0000$canonical" }

    private fun trailRedaction(
        tabIds: Set<String>,
        sinceInclusiveMillis: Long,
        untilExclusiveMillis: Long,
    ): PendingCandyTrailRedaction? {
        if (tabIds.isEmpty() || sinceInclusiveMillis >= untilExclusiveMillis) return null
        return PendingCandyTrailRedaction(
            id = UUID.randomUUID().toString(),
            tabIds = tabIds,
            sinceInclusiveMillis = sinceInclusiveMillis,
            untilExclusiveMillis = untilExclusiveMillis,
        )
    }

    companion object {
        @Volatile
        private var instance: BrowsingHistoryRepository? = null

        fun get(context: Context): BrowsingHistoryRepository = instance ?: synchronized(this) {
            instance ?: BrowsingHistoryRepository(context.applicationContext).also { instance = it }
        }
    }
}
