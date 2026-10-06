package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import dev.sk2andy.materialbrowser.R

/**
 * The address editor's close button. The full-screen dismiss scrim behind the field carries the
 * same TalkBack label, so a match by label alone finds two nodes.
 */
internal fun closeAddressInputButton(context: Context): SemanticsMatcher =
    hasContentDescription(context.getString(R.string.cd_close_address_input)) and
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

/**
 * Waits for the address editor to hold focus. It takes focus once its expand motion has started
 * and the window has focus, a few frames after it appears; on a loaded emulator that is not the
 * first frame. A timeout ends in the plain assertion, whose message shows the editor's semantics.
 */
internal fun ComposeTestRule.awaitAddressEditorFocused(timeoutMillis: Long = 10_000L) {
    try {
        waitUntil(timeoutMillis) {
            runCatching { onNodeWithTag(AddressBarTestTags.Editor).assertIsFocused() }.isSuccess
        }
    } catch (_: ComposeTimeoutException) {
        onNodeWithTag(AddressBarTestTags.Editor).assertIsFocused()
    }
}
