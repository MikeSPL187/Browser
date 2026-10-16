package dev.sk2andy.materialbrowser.browser

import android.view.View
import android.view.ViewGroup

/**
 * Takes an engine view off its parent before it moves to another host, without handing the focus
 * it held to the rest of the window.
 *
 * Removing a focused view makes Android ask the root view for focus. Engine views move while
 * Compose attaches an AndroidView, which can happen inside a measure pass (a host in
 * BoxWithConstraints), and there that request reaches Compose's focus search: it lays out a lazy
 * list beyond its bounds and throws "performMeasureAndLayout called during measure layout". With
 * the root's descendants blocked for the removal, the window keeps no focus until the next touch
 * or key, which is what a moved page needs anyway.
 */
internal fun View.removeFromParentWithoutFocusHandoff() {
    val parent = parent as? ViewGroup ?: return
    val root = rootView as? ViewGroup
    if (!hasFocus() || root == null || root === this) {
        parent.removeView(this)
        return
    }
    val descendantFocusability = root.descendantFocusability
    root.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    try {
        parent.removeView(this)
    } finally {
        root.descendantFocusability = descendantFocusability
    }
}
