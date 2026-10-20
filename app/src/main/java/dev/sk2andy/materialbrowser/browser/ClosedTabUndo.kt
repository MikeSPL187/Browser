package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.ClosedTabUndoRules
import dev.sk2andy.materialbrowser.data.ClosedTabUndoToken

/**
 * The one «Undo» offered after closing a tab (#500): the token the snackbar shows, what the tab
 * needs to come back exactly as it was, and the timer after which the offer lapses. Closing and
 * restoring the tab itself stays with the controller. [B] is an image ([android.graphics.Bitmap]).
 */
internal class ClosedTabUndo<B : Any>(
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    /** The offer lapsed: the controller lets go of the tab's stored state. */
    onExpired: () -> Unit,
) {
    /** What the closed tab had besides itself. */
    class Snapshot<B>(
        val trail: CandyTrail?,
        val preview: B?,
        val favicon: B?,
        val stack: TabStack?,
        val stackAfterClose: TabStack?,
    )

    var offer by mutableStateOf<ClosedTabUndoToken?>(null)
        private set
    private var snapshot: Snapshot<B>? = null
    private val expiry = Runnable { onExpired() }

    fun keep(token: ClosedTabUndoToken, snapshot: Snapshot<B>) {
        offer = token
        this.snapshot = snapshot
        postDelayed(expiry, ClosedTabUndoRules.DURATION_MILLIS)
    }

    /** What [token]'s tab had, while [token] is the offer shown. */
    fun snapshotFor(token: ClosedTabUndoToken): Snapshot<B>? = snapshot.takeIf { offer == token }

    fun clear() {
        removeCallbacks(expiry)
        offer = null
        snapshot = null
    }
}
