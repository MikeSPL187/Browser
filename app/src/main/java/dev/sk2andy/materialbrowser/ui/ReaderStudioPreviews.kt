package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.reader.ReaderBlock
import dev.sk2andy.materialbrowser.reader.ReaderBlockKind
import dev.sk2andy.materialbrowser.reader.ReaderDocument
import dev.sk2andy.materialbrowser.reader.ReaderExtractionResult
import dev.sk2andy.materialbrowser.reader.ReaderLibraryDataSource
import dev.sk2andy.materialbrowser.reader.ReaderLibraryState
import dev.sk2andy.materialbrowser.reader.ReaderSettings
import dev.sk2andy.materialbrowser.reader.ReaderSpeech
import dev.sk2andy.materialbrowser.reader.ReaderSpeechState
import dev.sk2andy.materialbrowser.reader.ReaderSpeechStatus
import dev.sk2andy.materialbrowser.reader.ReaderTheme
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioScreen as SharedReaderStudioScreen

private val previewArticle = ReaderDocument(
    title = "Lake Baikal in winter: choosing a route on the ice",
    sourceUrl = "https://north-guide.example/baikal-ice",
    siteName = "north-guide.example",
    blocks = listOf(
        ReaderBlock(
            ReaderBlockKind.Paragraph,
            "The ice on Baikal grows strong by mid-February. Before that, keep to walks along " +
                "the shore and routes with a guide.",
        ),
        ReaderBlock(ReaderBlockKind.Heading, "Three routes", level = 2),
        ReaderBlock(
            ReaderBlockKind.Paragraph,
            "Below are three routes of different length: from a short walk to the grotto to " +
                "a two-day crossing along the shore.",
        ),
        ReaderBlock(ReaderBlockKind.Quote, "Always check the ice with a local guide."),
    ),
)

/** Keeps the library in memory with the given settings. */
private class PreviewReaderLibrary(settings: ReaderSettings) : ReaderLibraryDataSource {
    private val state = ReaderLibraryState(settings = settings)

    override fun load(isPrivate: Boolean, onLoaded: (ReaderLibraryState) -> Unit) = onLoaded(state)

    override fun updateSettings(
        settings: ReaderSettings,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    ) = onUpdated(state.copy(settings = settings))

    override fun updateProgress(sourceUrl: String, progress: Float, isPrivate: Boolean) = Unit

    override fun saveSnapshot(
        document: ReaderDocument,
        progress: Float,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    ) = onUpdated(state)

    override fun deleteSnapshot(
        snapshotId: String,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    ) = onUpdated(state)
}

private object PreviewReaderSpeech : ReaderSpeech {
    override val state = ReaderSpeechState(status = ReaderSpeechStatus.Ready)

    override fun play(content: String) = Unit

    override fun pause() = Unit

    override fun stop() = Unit

    override fun close() = Unit
}

@Composable
private fun ReaderPreview(settings: ReaderSettings, settingsVisible: Boolean) {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        SharedReaderStudioScreen(
            result = ReaderExtractionResult.Success(previewArticle),
            sourceUrl = previewArticle.sourceUrl,
            isPrivate = false,
            repository = PreviewReaderLibrary(settings),
            resources = AndroidReaderStudioResources,
            style = rememberReaderStudioStyle(),
            speechFactory = { PreviewReaderSpeech },
            backHandler = { _, _ -> },
            onRetry = {},
            onDismiss = {},
            onOpenOriginal = {},
            onOpenLink = {},
            initialSettingsVisible = settingsVisible,
        )
    }
}

/** The article in the theme that follows the browser: white by day, pure black at night. */
@VolaPreviews
@Composable
private fun ReaderArticlePreview() {
    ReaderPreview(ReaderSettings(), settingsVisible = false)
}

/** «Reading view» over a paper page in Literata (board W-Reader). */
@VolaPreviews
@Composable
private fun ReaderSettingsPaperPreview() {
    ReaderPreview(ReaderSettings(theme = ReaderTheme.Paper), settingsVisible = true)
}
