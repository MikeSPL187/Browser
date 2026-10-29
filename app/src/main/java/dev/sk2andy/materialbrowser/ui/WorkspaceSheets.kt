@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent

/** A gem for a workspace that may not exist yet: the draft icon in the draft color. */
@Composable
private fun WorkspaceGemPreview(
    emoji: String,
    accent: WorkspaceAccent,
    size: androidx.compose.ui.unit.Dp,
) {
    WorkspaceGem(
        workspace = BrowserProfile(id = "", emoji = emoji, accent = accent),
        size = size,
    )
}
