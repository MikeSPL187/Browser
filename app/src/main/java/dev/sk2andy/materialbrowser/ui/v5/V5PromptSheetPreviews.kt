package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The one v5 prompt sheet (V28, B14a): an update offer and the crash report. Preview text is not localized. */

@Composable
private fun UpdatePreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5PromptSheet(
            visible = true,
            title = "Доступно обновление",
            message = "Доступна Vola 5.3.0. Скачать подписанный APK с GitHub? Когда загрузка закончится, " +
                "откройте файл — Android проверит и установит обновление.",
            actions = listOf(
                V5PromptAction("Скачать обновление", {}, V5PromptRole.Primary),
                V5PromptAction("Что нового", {}),
            ),
            onDismiss = {},
        )
    }
}

@Preview(name = "Prompt · update, light", heightDp = 520)
@Composable
private fun V5PromptUpdateLightPreview() = UpdatePreview(dark = false)

@Preview(name = "Prompt · update, dark", heightDp = 520)
@Composable
private fun V5PromptUpdateDarkPreview() = UpdatePreview(dark = true)

@Preview(name = "Prompt · crash report, dark", heightDp = 520)
@Composable
private fun V5PromptCrashDarkPreview() {
    VolaV5Theme(dark = true) {
        V5PromptSheet(
            visible = true,
            title = "Vola закрылась с ошибкой",
            message = "Отчёт сохранён на устройстве: ошибка и модель устройства, без страниц и адресов. " +
                "Он покинет устройство, только если вы его скопируете или отправите.",
            actions = listOf(
                V5PromptAction("Поделиться", {}, V5PromptRole.Primary),
                V5PromptAction("Скопировать отчёт", {}),
            ),
            onDismiss = {},
        )
    }
}
