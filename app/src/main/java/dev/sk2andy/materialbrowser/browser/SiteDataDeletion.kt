package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One site's data waiting out its undo window. */
internal data class PendingSiteDataDeletion(val baseDomain: String, val id: Long)

internal object SiteDataDeletionRules {
    /** As long as «Undo» stays on screen before the data really goes. */
    const val UNDO_WINDOW_MILLIS = 5_000L

    /**
     * Site info offers «Delete» for a site on the web when the engine can delete one site's data.
     * Not from a private tab: its data dies with it, and the deletion would reach regular tabs.
     */
    fun offers(supported: Boolean, baseDomain: String?, isPrivate: Boolean): Boolean =
        supported && baseDomain != null && !isPrivate

    /** After the deletion the selected page reloads, but only if it still shows that site. */
    fun reloadsSelected(baseDomain: String, selectedBaseDomain: String?): Boolean =
        selectedBaseDomain == baseDomain
}

/**
 * «Delete site data» from Site info (Q10b). Deleting cannot be taken back once the engine has done
 * it, so it waits [SiteDataDeletionRules.UNDO_WINDOW_MILLIS] behind «Undo» first; leaving the app
 * carries it out at once, so a request is never silently lost.
 */
class SiteDataDeletion internal constructor(
    private val clearSiteData: (baseDomain: String, onComplete: (Boolean) -> Unit) -> Unit,
    private val selectedBaseDomain: () -> String?,
    private val reloadSelected: () -> Unit,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
) {
    internal var pending by mutableStateOf<PendingSiteDataDeletion?>(null)
        private set

    private var nextId = 0L
    private val commitWhenDue = Runnable { commit() }

    /**
     * Starts the undo window for [baseDomain]; a deletion already waiting is carried out first.
     * [undoWindowMillis] may be longer than the default when accessibility services need more time.
     */
    fun request(
        baseDomain: String,
        undoWindowMillis: Long = SiteDataDeletionRules.UNDO_WINDOW_MILLIS,
    ) {
        commit()
        pending = PendingSiteDataDeletion(baseDomain, ++nextId)
        postDelayed(commitWhenDue, undoWindowMillis)
    }

    internal fun undo(deletion: PendingSiteDataDeletion) {
        if (pending?.id != deletion.id) return
        removeCallbacks(commitWhenDue)
        pending = null
    }

    /** Carries out the waiting deletion now: its window ran out, or the app left the screen. */
    fun commit() {
        val deletion = pending ?: return
        removeCallbacks(commitWhenDue)
        pending = null
        clearSiteData(deletion.baseDomain) { cleared ->
            val reloads = SiteDataDeletionRules.reloadsSelected(
                baseDomain = deletion.baseDomain,
                selectedBaseDomain = selectedBaseDomain(),
            )
            if (cleared && reloads) reloadSelected()
        }
    }
}
