package dev.sk2andy.materialbrowser.ui

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import dev.sk2andy.materialbrowser.R

/**
 * The address editor's close button. The full-screen dismiss scrim behind the field carries the
 * same TalkBack label, so a match by label alone finds two nodes.
 */
internal fun closeAddressInputButton(context: Context): SemanticsMatcher =
    hasContentDescription(context.getString(R.string.cd_close_address_input)) and
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
