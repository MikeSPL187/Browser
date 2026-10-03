package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

private val previewIcons = listOf(
    "💼", "🏠", "🎬", "📚", "⭐", "🎨",
    "🌍", "⚡", "📷", "👤", "🔖", "🔍",
)

@Composable
private fun WorkspaceSheetPreviewFrame(content: @Composable () -> Unit) {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
        content = content,
    )
}

@VolaPreviews
@Composable
private fun NewWorkspaceSheetPreview() {
    WorkspaceSheetPreviewFrame {
        NewWorkspaceSheet(
            visible = true,
            isolationSupported = true,
            profileProtectionSupported = true,
            icons = previewIcons,
            onCreate = { _, _, _ -> },
            onDismiss = {},
        )
    }
}

@VolaPreviews
@Composable
private fun WorkspaceSettingsSheetPreview() {
    WorkspaceSheetPreviewFrame {
        WorkspaceSettingsSheet(
            profile = BrowserProfile(
                id = "work",
                emoji = "💼",
                name = "Work",
                accent = WorkspaceAccent.Teal,
                isolationEnabled = true,
            ),
            tabCount = 6,
            essentialsCount = 5,
            icons = previewIcons,
            canDelete = true,
            isolationSupported = true,
            profileProtectionSupported = true,
            onRename = {},
            onAccentChange = {},
            onIconChange = {},
            onCustomizeWallpaper = {},
            onIsolationChange = {},
            onEnableProtection = {},
            onDisableProtection = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
