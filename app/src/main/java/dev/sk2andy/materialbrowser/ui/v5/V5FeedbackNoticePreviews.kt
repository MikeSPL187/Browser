package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 notice line (canvas «ц1», «ц2»): undo, an offer, an error. Preview text is not localized. */

private class PreviewData(override val visuals: SnackbarVisuals) : SnackbarData {
    override fun performAction() = Unit

    override fun dismiss() = Unit
}

@Composable
private fun NoticesPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Column(
            Modifier.background(V5Theme.colors.background).padding(vertical = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            V5FeedbackNotice(PreviewData(V5FeedbackVisuals("Вкладка закрыта", actionLabel = "Отменить")))
            V5FeedbackNotice(PreviewData(V5FeedbackVisuals("Сайт предлагает открыть приложение", actionLabel = "Открыть")))
            V5FeedbackNotice(
                PreviewData(V5FeedbackVisuals("Не удалось удалить профиль. Попробуйте ещё раз", withDismissAction = true, error = true)),
            )
        }
    }
}

@Preview(name = "V5FeedbackNotice · light")
@Composable
private fun V5FeedbackNoticeLightPreview() = NoticesPreview(dark = false)

@Preview(name = "V5FeedbackNotice · dark")
@Composable
private fun V5FeedbackNoticeDarkPreview() = NoticesPreview(dark = true)

@Preview(name = "V5FeedbackNotice · light, largest font", fontScale = 2f)
@Composable
private fun V5FeedbackNoticeLightPreviewLargeFont() = NoticesPreview(dark = false)
