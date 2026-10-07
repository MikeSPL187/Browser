package dev.sk2andy.materialbrowser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.sk2andy.materialbrowser.browser.integration.HistoryActivityContract
import dev.sk2andy.materialbrowser.browser.integration.LauncherShortcutRules
import dev.sk2andy.materialbrowser.browser.ProfileProtectionSession
import dev.sk2andy.materialbrowser.data.AppDataTransferLock
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.BrowsingHistoryRepository
import dev.sk2andy.materialbrowser.data.CandyTrailRepository
import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.data.HistoryEntry
import dev.sk2andy.materialbrowser.data.RecallRepository
import dev.sk2andy.materialbrowser.data.SnoozedTabStore
import dev.sk2andy.materialbrowser.ui.HistoryScreen
import dev.sk2andy.materialbrowser.ui.theme.CandyTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent
import dev.sk2andy.materialbrowser.recall.RecallMatch
import dev.sk2andy.materialbrowser.recall.RecallRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class HistoryActivity : ComponentActivity() {
    private val historyRepository by lazy { BrowsingHistoryRepository.get(this) }
    private val candyTrailRepository by lazy { CandyTrailRepository.get(this) }
    private val recallRepository by lazy { RecallRepository.get(this) }
    private var history by mutableStateOf<List<HistoryEntry>>(emptyList())
    private var recallMatches by mutableStateOf<List<RecallMatch>>(emptyList())
    private var recallRequestId = 0
    private val clearRequests = mutableListOf<HistoryClearRequest>()
    // Edits run in the process scope, so one confirmed as the screen closes is still saved.
    private val mutations = HistoryMutationQueue(
        scope = ProcessLifecycleOwner.get().lifecycleScope,
        mutex = mutationMutex,
        onIdle = ::runAfterMutations,
    )
    private var afterMutations: (() -> Unit)? = null
    private var hasHistoryMutations = false
    private var isFullImmersiveModeEnabled = false
    private var accessibleProfileIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        BrowsingHistoryLifecycle.install(application)
        super.onCreate(savedInstanceState)
        if (AppDataTransferLock.isActive(this)) {
            finish()
            return
        }
        enableEdgeToEdge()
        val store = BrowserSessionStore(this)
        isFullImmersiveModeEnabled = store.loadFullImmersiveModeEnabled()
        applyFullImmersiveMode(isFullImmersiveModeEnabled)
        val (storedProfiles, storedActiveProfileId) = store.loadProfiles()
        val configuredProfiles = if (store.loadProfilesEnabled()) {
            storedProfiles
        } else {
            storedProfiles.take(1)
        }
        val profiles = configuredProfiles.filter { profile ->
            profile.protection == null || ProfileProtectionSession.isUnlocked(profile.id)
        }
        if (profiles.isEmpty()) {
            finish()
            return
        }
        setRecentsPreviewEnabled(profiles.none { profile -> profile.protection != null })
        accessibleProfileIds = profiles.mapTo(hashSetOf()) { profile -> profile.id }
        val activeProfileId = storedActiveProfileId.takeIf { candidate ->
            profiles.any { profile -> profile.id == candidate }
        } ?: profiles.first().id
        clearRequests += HistoryActivityContract.clearRequestsFrom(savedInstanceState)
        history = accessibleHistory()
        val appearanceSettings = store.loadAppearanceSettings()
        val workspaceAccent = store.loadActiveWorkspaceAccent()
        val recallEnabled = store.loadRecallEnabled()

        setCandyContent(animationsEnabled = appearanceSettings.animationsEnabled) {
            val appearanceDark = appearanceSettings.usesDarkColors(isSystemInDarkTheme())
            SideEffect { applyAppearanceSystemBars(appearanceDark) }
            CandyTheme(
                settings = appearanceSettings,
                workspaceAccent = workspaceAccent,
            ) {
                HistoryScreen(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    history = history,
                    recallMatches = recallMatches,
                    onRecallCriteriaChanged = { query, profileIds ->
                        val requestId = ++recallRequestId
                        val recallQuery = RecallRules.historyQuery(query)
                        if (!recallEnabled || recallQuery == null) {
                            recallMatches = emptyList()
                        } else {
                            recallMatches = emptyList()
                            recallRepository.search(
                                profileIds = profileIds,
                                query = recallQuery,
                                limit = RecallRules.MAX_HISTORY_RESULTS,
                            ) { matches ->
                                if (requestId == recallRequestId && !isFinishing) {
                                    recallMatches = matches
                                }
                            }
                        }
                    },
                    onDeleteEntries = { entries -> mutations.enqueue { deleteEntries(entries) } },
                    onClearHistory = { request -> mutations.enqueue { clearHistory(request) } },
                    onOpenEntry = { entry ->
                        whenMutationsSaved {
                            setResult(
                                Activity.RESULT_OK,
                                HistoryActivityContract.resultIntent(entry, clearRequests),
                            )
                            finish()
                        }
                    },
                    onBack = { whenMutationsSaved(::finishWithResult) },
                    onOpenNewTab = ::openNewTab,
                    isChangingConfigurations = { isChangingConfigurations },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullImmersiveMode(isFullImmersiveModeEnabled)
    }

    override fun onResume() {
        super.onResume()
        val currentAccessibleProfileIds = BrowserSessionStore(this).let { store ->
            val (storedProfiles) = store.loadProfiles()
            val configuredProfiles = if (store.loadProfilesEnabled()) {
                storedProfiles
            } else {
                storedProfiles.take(1)
            }
            configuredProfiles.asSequence()
                .filter { profile ->
                    profile.protection == null || ProfileProtectionSession.isUnlocked(profile.id)
                }
                .mapTo(hashSetOf()) { profile -> profile.id }
        }
        if (currentAccessibleProfileIds != accessibleProfileIds) {
            recreate()
            return
        }
        history = accessibleHistory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        HistoryActivityContract.saveClearRequests(outState, clearRequests)
        super.onSaveInstanceState(outState)
    }

    private suspend fun deleteEntries(entries: List<HistoryEntry>) {
        val previous = history
        val mutation = withContext(Dispatchers.IO) { historyRepository.remove(entries) }
        history = mutation.history.filterAccessibleProfiles()
        if (!mutation.committed) {
            showMutationFailed()
            return
        }
        hasHistoryMutations = hasHistoryMutations || history != previous
        val deletedKeys = entries.mapNotNullTo(hashSetOf()) { entry ->
            RecallRules.canonicalUrl(entry.url)?.let { url -> "${entry.profileId}\u0000$url" }
        }
        recallMatches = recallMatches.filterNot { match ->
            "${match.profileId}\u0000${match.url}" in deletedKeys
        }
    }

    private suspend fun clearHistory(request: HistoryClearRequest) {
        val previous = history
        val trailTabIds = persistentTrailTabs().asSequence()
            .filterNot { tab -> tab.isIncognito }
            .filter { tab -> tab.profileId in request.profileIds }
            .mapTo(linkedSetOf()) { tab -> tab.id }
        val mutation = withContext(Dispatchers.IO) {
            historyRepository.clearRange(request, trailTabIds)
        }
        history = mutation.history.filterAccessibleProfiles()
        if (!mutation.committed) {
            showMutationFailed()
            return
        }
        if (history != previous) {
            hasHistoryMutations = true
            clearRequests += request
            candyTrailRepository.processPendingRedactions()
            setResult(
                Activity.RESULT_OK,
                HistoryActivityContract.resultIntent(clearRequests = clearRequests),
            )
        }
        recallMatches = recallMatches.filterNot { match ->
            match.profileId in request.profileIds &&
                match.visitedAt >= request.sinceInclusiveMillis &&
                match.visitedAt < request.untilExclusiveMillis
        }
    }

    private fun showMutationFailed() {
        Toast.makeText(applicationContext, R.string.history_clear_failed, Toast.LENGTH_SHORT).show()
    }

    /** Leaves only once every confirmed edit is saved, so the result reports all of them. */
    private fun whenMutationsSaved(action: () -> Unit) {
        if (mutations.isBusy) afterMutations = action else action()
    }

    private fun runAfterMutations() {
        val action = afterMutations ?: return
        afterMutations = null
        if (!isDestroyed) action()
    }

    private fun persistentTrailTabs() = buildList {
        addAll(BrowserSessionStore(this@HistoryActivity).loadTabs().first)
        addAll(SnoozedTabStore(this@HistoryActivity).load().map { snoozed -> snoozed.tab })
    }.distinctBy { tab -> tab.id }

    private fun accessibleHistory(): List<HistoryEntry> =
        historyRepository.snapshot().filterAccessibleProfiles()

    private fun List<HistoryEntry>.filterAccessibleProfiles(): List<HistoryEntry> =
        filter { entry -> entry.profileId in accessibleProfileIds }

    /** The empty history's way back to browsing: a fresh tab, as the launcher's shortcut opens it. */
    private fun openNewTab() = whenMutationsSaved {
        finishWithResult()
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(LauncherShortcutRules.ACTION_NEW_TAB),
        )
    }

    private fun finishWithResult() {
        if (hasHistoryMutations || clearRequests.isNotEmpty()) {
            setResult(
                Activity.RESULT_OK,
                HistoryActivityContract.resultIntent(clearRequests = clearRequests),
            )
        }
        finish()
    }

    private companion object {
        /** One History screen's edits never interleave with a recreated screen's. */
        val mutationMutex = Mutex()
    }
}
