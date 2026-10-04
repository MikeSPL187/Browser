package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Under a modal overlay, such as Glance, the browser beneath leaves the accessibility tree, as it
 * would under a dialog. Being covered by the overlay's scrim is not enough: while the overlay
 * animates, parts of the bars beneath show through and TalkBack could still reach them.
 */
internal fun Modifier.hiddenUnderModal(modalVisible: Boolean): Modifier =
    if (modalVisible) clearAndSetSemantics { } else this
