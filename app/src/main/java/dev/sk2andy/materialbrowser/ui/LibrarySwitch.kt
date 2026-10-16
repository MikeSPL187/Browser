package dev.sk2andy.materialbrowser.ui

import android.content.Intent
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment

/**
 * How one screen of the v5 library hands over to another (V28, B2): favorites, history and
 * downloads are still three activities, each started for its result by the browser. A segment
 * tapped in one returns this request and the browser starts the other with its own launcher, so
 * each result (a page to open, history to clear) still reaches the code that reads it.
 */
internal object LibrarySwitch {
    private const val EXTRA_SEGMENT = "v5_library_switch_to"

    /** [result] (or a new intent) asking the browser to open [segment] next. */
    fun request(segment: V5LibrarySegment, result: Intent = Intent()): Intent =
        result.putExtra(EXTRA_SEGMENT, segment.name)

    fun from(intent: Intent?): V5LibrarySegment? =
        intent?.getStringExtra(EXTRA_SEGMENT)?.let { name ->
            V5LibrarySegment.entries.firstOrNull { segment -> segment.name == name }
        }
}
