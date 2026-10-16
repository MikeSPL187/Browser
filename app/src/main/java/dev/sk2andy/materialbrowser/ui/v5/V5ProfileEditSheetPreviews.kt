package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* V11b: «New profile» with its own storage, and «Edit profile» with the biometric lock and «Delete». */

private val previewIcons = listOf("🏔", "💼", "🏠", "⭐", "📚", "🎮", "✈️", "🎵", "🧪", "📰", "❤️", "🔥").map { key ->
    V5ProfileIconChoice(key, WorkspaceIcons.vector(key), key)
}

@Composable
private fun EditPreview(dark: Boolean, creating: Boolean) {
    VolaV5Theme(dark = dark) {
        V5ProfileEditSheet(
            visible = true,
            creating = creating,
            initial = if (creating) {
                V5ProfileDraft(name = "", iconKey = "🏔", accent = V5Accent.Blue)
            } else {
                V5ProfileDraft(name = "Работа", iconKey = "💼", accent = V5Accent.Orange)
            },
            icons = previewIcons,
            privacy = V5ProfilePrivacy(ownStorage = true, biometricLock = if (creating) null else false),
            onSave = {},
            onOwnStorageChange = {},
            onBiometricLockChange = {},
            onDismiss = {},
            onDelete = if (creating) null else ({}),
        )
    }
}

@Preview(name = "New profile · light", heightDp = 900)
@Composable
private fun V5ProfileNewLightPreview() = EditPreview(dark = false, creating = true)

@Preview(name = "Edit profile · dark", heightDp = 900)
@Composable
private fun V5ProfileEditDarkPreview() = EditPreview(dark = true, creating = false)
