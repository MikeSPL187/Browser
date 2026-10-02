package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The address editor's input, owned by [AddressBarController]: whether it is open, the typed text,
 * the highlighted suggestion and the clip the clipboard chip already used. Kept out of Compose so
 * it survives recomposition and is testable on the JVM.
 */
class AddressEditorState {
    var isVisible by mutableStateOf(false)
    var value by mutableStateOf(TextFieldValue())
    var highlightedIndex by mutableIntStateOf(NO_HIGHLIGHT)

    /** Bumped to move the focus (and the keyboard) back into the field. */
    var focusNonce by mutableIntStateOf(0)

    /** Bumped when the editor opens or closes; a pending open checks it is still current. */
    var openGeneration by mutableIntStateOf(0)

    /** When the clip the chip already used was copied; that clip is not offered again. */
    var usedClipCopiedAtElapsedMillis by mutableStateOf<Long?>(null)
        private set

    /** Opens the editor on [text], selected so typing replaces it. */
    fun open(text: String) {
        value = TextFieldValue(text = text, selection = TextRange(text.length, 0))
        isVisible = true
        highlightedIndex = NO_HIGHLIGHT
        focusNonce++
    }

    /**
     * Opens the editor on [text] when the launcher asked for it ([requested]), before the first
     * frame; returns whether it opened.
     */
    fun openOnLaunch(requested: Boolean, text: String): Boolean {
        if (requested) open(text)
        return requested
    }

    /** Puts [text] into the field with the cursor at its end, keeping the editor open. */
    fun fill(text: String) {
        value = TextFieldValue(text = text, selection = TextRange(text.length))
        highlightedIndex = NO_HIGHLIGHT
        focusNonce++
    }

    fun close() {
        openGeneration++
        isVisible = false
        highlightedIndex = NO_HIGHLIGHT
    }

    fun moveHighlight(delta: Int, suggestionCount: Int) {
        highlightedIndex =
            AddressEditorRules.movedHighlight(highlightedIndex, delta, suggestionCount)
    }

    fun markClipUsed(copiedAtElapsedMillis: Long) {
        usedClipCopiedAtElapsedMillis = copiedAtElapsedMillis
    }

    companion object {
        const val NO_HIGHLIGHT = -1
    }
}

object AddressEditorRules {
    /**
     * The highlight after an arrow key. From the field, down enters the list at the top and up at
     * the bottom, next to the field; moving past the top returns to the field.
     */
    fun movedHighlight(current: Int, delta: Int, suggestionCount: Int): Int {
        if (suggestionCount <= 0) return AddressEditorState.NO_HIGHLIGHT
        val last = suggestionCount - 1
        return when {
            current !in 0..last && delta > 0 -> 0
            current !in 0..last -> last
            else -> (current + delta).coerceIn(AddressEditorState.NO_HIGHLIGHT, last)
        }
    }
}
