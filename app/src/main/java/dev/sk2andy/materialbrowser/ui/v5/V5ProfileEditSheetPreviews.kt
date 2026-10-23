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

@Preview(name = "New profile · dark", heightDp = 900)
@Composable
private fun V5ProfileNewDarkPreview() = EditPreview(dark = true, creating = true)

@Preview(name = "Edit profile · light", heightDp = 900)
@Composable
private fun V5ProfileEditLightPreview() = EditPreview(dark = false, creating = false)

@Preview(name = "New profile · largest font", heightDp = 1400, fontScale = 2f)
@Composable
private fun V5ProfileNewLargeFontPreview() = EditPreview(dark = false, creating = true)

/** «Delete profile»: where the tabs go and everything that goes with the profile (PF-04). */
@Composable
private fun DeletePreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5PromptSheet(
            visible = true,
            title = "Удалить «Работа»?",
            message = "Вкладки перейдут в профиль «Дом», без групп. Вместе с профилем удалятся его группы, " +
                "закреплённые сайты, избранное, скрытое на сайтах, настройки сайтов, история, cookie и входы.",
            actions = listOf(V5PromptAction("Удалить", {}, V5PromptRole.Destructive)),
            onDismiss = {},
        )
    }
}

@Preview(name = "Delete profile · light", heightDp = 520)
@Composable
private fun V5ProfileDeleteLightPreview() = DeletePreview(dark = false)

@Preview(name = "Delete profile · dark", heightDp = 520)
@Composable
private fun V5ProfileDeleteDarkPreview() = DeletePreview(dark = true)
