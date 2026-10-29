package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

/** Which workspace sheet or dialog the tab overview shows, if any. */
@Stable
internal class WorkspaceSheetsState {
    /** The workspace whose settings sheet is open (long press on its gem). */
    var actionsProfileId by mutableStateOf<String?>(null)

    /** A storage isolation change waiting for confirmation: workspace and the new value. */
    var isolationChange by mutableStateOf<Pair<String, Boolean>?>(null)
    var protectionTargetId by mutableStateOf<String?>(null)

    /** «New workspace» is open. */
    var creating by mutableStateOf(false)

    val isOpen: Boolean
        get() = actionsProfileId != null ||
            isolationChange != null ||
            protectionTargetId != null ||
            creating

    fun startCreating() {
        creating = true
    }

    companion object {
        /**
         * Only «New workspace» outlives recreation: the sheets of an existing workspace close, so
         * a workspace that is locked again after process death never shows its settings.
         */
        val Saver: Saver<WorkspaceSheetsState, Boolean> = Saver(
            save = { state -> state.creating },
            restore = { creating -> WorkspaceSheetsState().apply { this.creating = creating } },
        )
    }

    /** A workspace that just locked takes its open sheets with it. */
    fun forgetLocked(lockedProfileIds: Set<String>) {
        if (actionsProfileId in lockedProfileIds) actionsProfileId = null
        if (isolationChange?.first in lockedProfileIds) isolationChange = null
        if (protectionTargetId in lockedProfileIds) protectionTargetId = null
    }
}
