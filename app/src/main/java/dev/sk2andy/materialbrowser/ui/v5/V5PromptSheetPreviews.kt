package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
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

/** V28 B9a: a site's question with three answers, and a choice without a message. */
@Preview(name = "Prompt · site sign-in, light", heightDp = 560)
@Composable
private fun V5PromptSiteLightPreview() {
    VolaV5Theme(dark = false) {
        V5PromptSheet(
            visible = true,
            title = "Войти через Google?",
            message = "Сайт shop.example хочет открыть вход через Google. Для этого ему нужны cookie Google.",
            actions = listOf(
                V5PromptAction("Разрешить в этой вкладке", {}, V5PromptRole.Primary),
                V5PromptAction("Разрешить в профиле", {}),
                V5PromptAction("Не разрешать", {}),
            ),
            onDismiss = {},
        )
    }
}

@Preview(name = "Prompt · download manager, dark", heightDp = 420)
@Composable
private fun V5PromptChooserDarkPreview() {
    VolaV5Theme(dark = true) {
        V5PromptSheet(
            visible = true,
            title = "Чем скачать",
            message = null,
            actions = listOf(V5PromptAction("Vola", {}), V5PromptAction("1DM", {}), V5PromptAction("ADM", {})),
            onDismiss = {},
        )
    }
}

@Preview(name = "Choice · site permission, light", heightDp = 420)
@Composable
private fun V5ChoiceLightPreview() = ChoicePreview(dark = false)

@Preview(name = "Choice · site permission, dark", heightDp = 420)
@Composable
private fun V5ChoiceDarkPreview() = ChoicePreview(dark = true)

@Composable
private fun ChoicePreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        V5ChoiceSheet(
            visible = true,
            title = "Местоположение",
            options = listOf("Спрашивать", "Разрешать", "Блокировать"),
            selected = 0,
            onSelect = {},
            onDismiss = {},
        )
    }
}

/** A menu as a prompt sheet (the phone all v5): a link's actions, each with its glyph. */
@Preview(name = "Prompt · link menu, dark", heightDp = 560)
@Composable
private fun V5PromptLinkMenuDarkPreview() {
    VolaV5Theme(dark = true) {
        V5PromptSheet(
            visible = true,
            title = "north-guide.example/baikal",
            message = null,
            actions = listOf(
                V5PromptAction("Открыть в новой вкладке", {}, icon = VolaIcons.OpenInNew),
                V5PromptAction("Копировать ссылку", {}, icon = VolaIcons.ContentCopy),
                V5PromptAction("Поделиться", {}, icon = VolaIcons.Share),
                V5PromptAction("Скачать по ссылке", {}, icon = VolaIcons.Download),
            ),
            onDismiss = {},
        )
    }
}
