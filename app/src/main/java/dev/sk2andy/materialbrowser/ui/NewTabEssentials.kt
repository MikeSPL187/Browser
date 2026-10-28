@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.EssentialsController
import dev.sk2andy.materialbrowser.data.EssentialCandidate
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.EssentialsRules
/** Changes the new tab makes to its workspace's Essentials; `null` where the page is a preview. */
@Stable
internal interface NewTabEssentialsEditor {
    /** Open tabs of the workspace that are not pinned yet, most recent first. */
    val candidates: List<EssentialCandidate>

    /** Tab icons by tab id, for the add sheet. */
    val candidateIcons: Map<String, Bitmap>

    /** Favorites and history pages to pin besides open tabs (Н-1); the v4 sheet offers tabs only. */
    val suggestions: Pair<List<EssentialCandidate>, List<EssentialCandidate>>
        get() = emptyList<EssentialCandidate>() to emptyList()
    val isFull: Boolean

    fun remove(entry: EssentialEntry)
    fun move(entry: EssentialEntry, toIndex: Int)
    fun add(candidate: EssentialCandidate)
}

@Composable
internal fun rememberNewTabEssentialsEditor(
    controller: BrowserController,
    profileId: String,
): NewTabEssentialsEditor = remember(controller, profileId) {
    val essentials = controller.essentials
    object : NewTabEssentialsEditor {
        override val candidates: List<EssentialCandidate>
            get() = EssentialsRules.candidates(
                entries = essentials.entriesFor(profileId),
                tabs = controller.activeTabs
                    .filter { tab -> tab.profileId == profileId && !tab.isIncognito }
                    .sortedByDescending(BrowserTab::lastAccessedAt)
                    .map { tab -> EssentialCandidate(tab.id, tab.url, tab.title) },
            )
        override val candidateIcons: Map<String, Bitmap>
            get() = controller.favicons
        override val suggestions: Pair<List<EssentialCandidate>, List<EssentialCandidate>>
            get() = EssentialsRules.suggestions(
                entries = essentials.entriesFor(profileId),
                offered = candidates,
                favorites = if (controller.activeProfileId == profileId) controller.favorites else emptyList(),
                history = controller.history.filter { it.profileId == profileId },
            )
        override val isFull: Boolean
            get() = essentials.entriesFor(profileId).size >= EssentialsRules.MAX_ENTRIES

        override fun remove(entry: EssentialEntry) {
            essentials.remove(profileId, entry.id)
        }

        override fun move(entry: EssentialEntry, toIndex: Int) {
            essentials.move(profileId, entry.id, toIndex)
        }

        override fun add(candidate: EssentialCandidate) {
            essentials.add(profileId, candidate.url, candidate.title, controller.favicons[candidate.tabId])
        }
    }
}

/** «Removed from Essentials · Undo» on the browser's snackbar, like a closed tab. */
@Composable
internal fun EssentialRemovalSnackbarEffect(
    essentials: EssentialsController,
    hostState: SnackbarHostState,
) {
    val removal = essentials.removal
    val message = stringResource(R.string.essentials_removed)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(removal) {
        val token = removal ?: return@LaunchedEffect
        hostState.currentSnackbarData?.dismiss()
        try {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) essentials.undoRemoval(token)
        } finally {
            essentials.dismissRemoval(token)
        }
    }
}
