package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/**
 * The v5 theme for a prompt raised from the current shell (V28, B9): light or dark as the shell
 * around it is.
 */
@Composable
internal fun V5PromptTheme(content: @Composable () -> Unit) {
    VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < PROMPT_DARK_LUMINANCE, content = content)
}

private const val PROMPT_DARK_LUMINANCE = 0.5f
