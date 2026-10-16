package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* V11a: the profiles sheet — the active profile checked, a locked one, one from another device. */

private val previewProfiles = listOf(
    V5ProfileItem("personal", WorkspaceIcons.vector("🏔"), "Личный", V5Accent.Blue, tabCount = 7, active = true),
    V5ProfileItem("work", WorkspaceIcons.vector("💼"), "Работа", V5Accent.Orange, tabCount = 12, active = false),
    V5ProfileItem("bank", WorkspaceIcons.vector("🏦"), "Банк", V5Accent.Green, tabCount = 1, active = false, locked = true),
    V5ProfileItem(
        "laptop",
        WorkspaceIcons.vector("💻"),
        "Ноутбук",
        V5Accent.Purple,
        tabCount = 4,
        active = false,
        device = "Pixel Tablet",
    ),
)

@Composable
private fun SheetPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5ProfileSheet(visible = true, profiles = previewProfiles, onSelect = {}, onDismiss = {})
    }
}

@Preview(name = "Profiles · light", heightDp = 640)
@Composable
private fun V5ProfileSheetLightPreview() = SheetPreview(dark = false)

@Preview(name = "Profiles · dark", heightDp = 640)
@Composable
private fun V5ProfileSheetDarkPreview() = SheetPreview(dark = true)

/** V28 B11: «Move to profile» — the same sheet, only the profiles the tab can go to, no counts. */
@Preview(name = "Move to profile · dark", heightDp = 480)
@Composable
private fun V5MoveToProfileDarkPreview() {
    VolaV5Theme(dark = true) {
        V5ProfileSheet(
            visible = true,
            profiles = previewProfiles.drop(1).take(2).map { it.copy(tabCount = null) },
            onSelect = {},
            onDismiss = {},
            title = "Переместить в профиль",
        )
    }
}
