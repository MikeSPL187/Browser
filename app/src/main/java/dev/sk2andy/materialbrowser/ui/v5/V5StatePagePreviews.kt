package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The one v5 lock screen (V28, B13): private tabs and a locked profile. Preview text is not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

@Preview(name = "Locked private tabs", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivateLockPreview() {
    VolaV5Theme(dark = true, privateBrowsing = true) {
        V5StatePage(
            icon = VolaIcons.Fingerprint,
            title = "Частные вкладки закрыты",
            message = "3 частные вкладки ждут, пока вы подтвердите, что это вы.",
            primary = V5StateAction("Разблокировать", {}, icon = VolaIcons.Fingerprint),
            secondary = listOf(
                V5StateAction("Обычные вкладки", {}),
                V5StateAction("Закрыть 3 частные вкладки", {}, destructive = true),
            ),
        )
    }
}

@Preview(name = "Locked profile · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5ProfileLockPreview() {
    VolaV5Theme(dark = false) {
        V5StatePage(
            icon = VolaIcons.Lock,
            title = "Профиль «Работа» закрыт",
            message = "Подтвердите, что это вы, чтобы открыть его вкладки.",
            primary = V5StateAction("Разблокировать", {}, icon = VolaIcons.Fingerprint),
            secondary = listOf(V5StateAction("Другой профиль", {})),
        )
    }
}

@Preview(name = "Locked profile · no biometrics, dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5ProfileLockUnavailablePreview() {
    VolaV5Theme(dark = true) {
        V5StatePage(
            icon = VolaIcons.Lock,
            title = "Профиль закрыт",
            message = "На этом устройстве нет блокировки экрана — включите её в настройках Android.",
            primary = V5StateAction("Разблокировать", {}, icon = VolaIcons.Fingerprint, enabled = false),
        )
    }
}
