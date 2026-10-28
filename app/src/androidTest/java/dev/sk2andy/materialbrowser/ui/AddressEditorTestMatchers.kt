package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import dev.sk2andy.materialbrowser.ui.v5.V5EditorTestTags

/** The address editor's «Cancel», which closes it. */
internal fun closeAddressInputButton(): SemanticsMatcher = hasTestTag(V5EditorTestTags.Cancel)

/**
 * Waits for the address field to hold focus. It takes focus once the editor has opened and the
 * window has focus, a few frames after it appears; on a loaded emulator that is not the first
 * frame. A timeout ends in the plain assertion, whose message shows the field's semantics.
 */
internal fun ComposeTestRule.awaitAddressEditorFocused(timeoutMillis: Long = 10_000L) {
    try {
        waitUntil(timeoutMillis) {
            runCatching { onNodeWithTag(V5EditorTestTags.Field).assertIsFocused() }.isSuccess
        }
    } catch (_: ComposeTimeoutException) {
        onNodeWithTag(V5EditorTestTags.Field).assertIsFocused()
    }
}
