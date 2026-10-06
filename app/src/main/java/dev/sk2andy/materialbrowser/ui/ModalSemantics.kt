package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Under a modal overlay, such as Glance, the browser beneath leaves the accessibility tree, as it
 * would under a dialog. Being covered by the overlay's scrim is not enough: while the overlay
 * animates, parts of the bars beneath show through and TalkBack could still reach them.
 */
internal fun Modifier.hiddenUnderModal(modalVisible: Boolean): Modifier =
    if (modalVisible) clearAndSetSemantics { } else this

/**
 * Clears the subtree's semantics while [hidden], keeping one stable node in the chain. Adding and
 * removing a `clearAndSetSemantics` node as the flag flips left the address bar out of the
 * accessibility tree after the tab overview morph; a node that stays and invalidates itself does not.
 */
internal fun Modifier.clearSemanticsWhen(hidden: Boolean): Modifier =
    this then ClearSemanticsWhenElement(hidden)

private data class ClearSemanticsWhenElement(
    val hidden: Boolean,
) : ModifierNodeElement<ClearSemanticsWhenNode>() {
    override fun create() = ClearSemanticsWhenNode(hidden)

    override fun update(node: ClearSemanticsWhenNode) {
        if (node.hidden != hidden) {
            node.hidden = hidden
            node.invalidateSemantics()
        }
    }
}

private class ClearSemanticsWhenNode(var hidden: Boolean) : Modifier.Node(), SemanticsModifierNode {
    override val shouldClearDescendantSemantics: Boolean
        get() = hidden

    override fun SemanticsPropertyReceiver.applySemantics() = Unit
}
