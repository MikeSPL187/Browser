package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.EssentialCandidate
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

private val previewEntries = listOf(
    EssentialEntry("https://mail.example.com/", "Почта"),
    EssentialEntry("https://calendar.example.com/", "Календарь"),
    EssentialEntry("https://tasks.example.com/", "Задачи"),
    EssentialEntry("https://docs.example.com/", "Документы"),
    EssentialEntry("https://github.com/", ""),
    EssentialEntry("https://chat.example.com/", "Chat"),
)

private object PreviewEditor : NewTabEssentialsEditor {
    override val candidates = listOf(EssentialCandidate("tab", "https://metrics.example.com/", "Metrics"))
    override val candidateIcons: Map<String, Bitmap> = emptyMap()
    override val isFull = false

    override fun remove(entry: EssentialEntry) = Unit
    override fun move(entry: EssentialEntry, toIndex: Int) = Unit
    override fun add(candidate: EssentialCandidate) = Unit
}

@Composable
private fun EssentialsPreviewFrame(content: @Composable () -> Unit) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Box(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x4),
        ) {
            content()
        }
    }
}

@VolaPreviews
@Composable
private fun NewTabEssentialsPreview() {
    EssentialsPreviewFrame {
        NewTabEssentialsSection(
            entries = previewEntries,
            icons = emptyMap(),
            editing = false,
            onEditingChange = {},
            enabled = true,
            onOpen = {},
            editor = PreviewEditor,
        )
    }
}

@VolaPreviews
@Composable
private fun NewTabEssentialsEditingPreview() {
    EssentialsPreviewFrame {
        NewTabEssentialsSection(
            entries = previewEntries,
            icons = emptyMap(),
            editing = true,
            onEditingChange = {},
            enabled = true,
            onOpen = {},
            editor = PreviewEditor,
        )
    }
}

@VolaPreviews
@Composable
private fun NewTabEssentialsEmptyPreview() {
    EssentialsPreviewFrame {
        NewTabEssentialsSection(
            entries = emptyList(),
            icons = emptyMap(),
            editing = false,
            onEditingChange = {},
            enabled = true,
            onOpen = {},
            editor = PreviewEditor,
        )
    }
}
