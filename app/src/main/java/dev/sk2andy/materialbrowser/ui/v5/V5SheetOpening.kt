package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A key that changes each time a sheet opens, for the drafts its fields keep with rememberSaveable:
 * the draft starts afresh on opening and keeps what it showed while the sheet slides away, whatever
 * the values it started from change to once «Save» applied them.
 */
@Composable
internal fun rememberSheetOpening(visible: Boolean): Int {
    val opening = remember { SheetOpening() }
    if (visible && !opening.shown) opening.count++
    opening.shown = visible
    return opening.count
}

/** Not state: the key is read in the same composition that changes it. */
private class SheetOpening {
    var count = 0
    var shown = false
}
