package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.reader.ReaderExtractionFailure
import dev.sk2andy.materialbrowser.reader.ReaderExtractionResult
import dev.sk2andy.materialbrowser.reader.ReaderLibraryRepository
import dev.sk2andy.materialbrowser.reader.ReaderSpeechController
import dev.sk2andy.materialbrowser.shared.ui.ReaderPalette
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioIcon
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioLabel
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioResources
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioStyle
import dev.sk2andy.materialbrowser.ui.theme.LiterataFontFamily
import dev.sk2andy.materialbrowser.ui.theme.ManropeFontFamily
import dev.sk2andy.materialbrowser.ui.theme.VolaReader
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioScreen as SharedReaderStudioScreen
import dev.sk2andy.materialbrowser.shared.ui.ReaderStudioTestTags as SharedReaderStudioTestTags

internal typealias ReaderStudioTestTags = SharedReaderStudioTestTags

@Composable
internal fun ReaderStudioScreen(
    result: ReaderExtractionResult?,
    sourceUrl: String,
    isPrivate: Boolean,
    repository: ReaderLibraryRepository,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenOriginal: (String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val context = LocalContext.current
    SharedReaderStudioScreen(
        result = result,
        sourceUrl = sourceUrl,
        isPrivate = isPrivate,
        repository = repository,
        resources = AndroidReaderStudioResources,
        style = rememberReaderStudioStyle(),
        speechFactory = { ReaderSpeechController(context) },
        backHandler = { enabled, onBack -> BackHandler(enabled = enabled, onBack = onBack) },
        onRetry = onRetry,
        onDismiss = onDismiss,
        onOpenOriginal = onOpenOriginal,
        onOpenLink = onOpenLink,
    )
}

internal object AndroidReaderStudioResources : ReaderStudioResources {
    @Composable
    override fun text(label: ReaderStudioLabel, value: Int?): String = when (label) {
        ReaderStudioLabel.Close -> stringResource(R.string.reader_close)
        ReaderStudioLabel.Title -> stringResource(R.string.reader_studio_title)
        ReaderStudioLabel.Article -> stringResource(R.string.reader_article)
        ReaderStudioLabel.Original -> stringResource(R.string.reader_original)
        ReaderStudioLabel.OfflineCount ->
            stringResource(R.string.reader_offline_count, requireNotNull(value))
        ReaderStudioLabel.Extracting -> stringResource(R.string.reader_extracting)
        ReaderStudioLabel.ExtractionFailedTitle ->
            stringResource(R.string.reader_extraction_failed_title)
        ReaderStudioLabel.ExtractionUnsupported ->
            stringResource(R.string.reader_extraction_unsupported)
        ReaderStudioLabel.ExtractionEmpty -> stringResource(R.string.reader_extraction_empty)
        ReaderStudioLabel.ExtractionInvalid -> stringResource(R.string.reader_extraction_invalid)
        ReaderStudioLabel.Retry -> stringResource(R.string.reader_retry)
        ReaderStudioLabel.SaveOffline -> stringResource(R.string.reader_save_offline)
        ReaderStudioLabel.ThemeLight -> stringResource(R.string.reader_theme_light)
        ReaderStudioLabel.ThemePaper -> stringResource(R.string.reader_theme_paper)
        ReaderStudioLabel.ThemeDark -> stringResource(R.string.reader_theme_dark)
        ReaderStudioLabel.ReadingView -> stringResource(R.string.reader_reading_view)
        ReaderStudioLabel.ReadingTime -> requireNotNull(value).let { minutes ->
            pluralStringResource(R.plurals.reader_reading_minutes, minutes, minutes)
        }
        ReaderStudioLabel.ReadingSettings -> stringResource(R.string.reader_reading_settings)
        ReaderStudioLabel.TextSize -> stringResource(R.string.reader_text_size)
        ReaderStudioLabel.FontSerif -> stringResource(R.string.reader_font_serif)
        ReaderStudioLabel.FontSans -> stringResource(R.string.reader_font_sans)
        ReaderStudioLabel.WideMargins -> stringResource(R.string.reader_wide_margins)
        ReaderStudioLabel.Listen -> stringResource(R.string.reader_listen)
        ReaderStudioLabel.SpeechStart -> stringResource(R.string.reader_speech_start)
        ReaderStudioLabel.SpeechResume -> stringResource(R.string.reader_speech_resume)
        ReaderStudioLabel.SpeechPause -> stringResource(R.string.reader_speech_pause)
        ReaderStudioLabel.SpeechStop -> stringResource(R.string.reader_speech_stop)
        ReaderStudioLabel.PrivateNotice -> stringResource(R.string.reader_private_notice)
        ReaderStudioLabel.OfflineLibrary -> stringResource(R.string.reader_offline_library)
        ReaderStudioLabel.OfflineEmpty -> stringResource(R.string.reader_offline_empty)
        ReaderStudioLabel.ProgressPercent ->
            stringResource(R.string.reader_progress_percent, requireNotNull(value))
        ReaderStudioLabel.DeleteSnapshot -> stringResource(R.string.reader_delete_snapshot)
    }

    @Composable
    override fun icon(
        icon: ReaderStudioIcon,
        modifier: Modifier,
        contentDescription: String?,
    ) {
        Icon(
            painter = painterResource(
                when (icon) {
                    ReaderStudioIcon.Download -> R.drawable.ic_reader_download
                    ReaderStudioIcon.Library -> R.drawable.ic_widget_workspace_books
                    ReaderStudioIcon.Pause -> R.drawable.ic_reader_pause
                    ReaderStudioIcon.Stop -> R.drawable.ic_reader_stop
                },
            ),
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

/** The reader's look from the design tokens (board W-Reader), with the browser's accent. */
@Composable
internal fun rememberReaderStudioStyle(): ReaderStudioStyle {
    val scheme = MaterialTheme.colorScheme
    val browserDark = scheme.background.luminance() < 0.5f
    // On a page of the other brightness than the browser, the inverse accent keeps its contrast.
    val pageAccent = if (browserDark) scheme.inversePrimary else scheme.primary
    val onPageAccent = if (browserDark) scheme.onPrimaryContainer else scheme.onPrimary
    val darkAccent = if (browserDark) scheme.primary else scheme.inversePrimary
    val onDarkAccent = if (browserDark) scheme.onPrimary else scheme.onPrimaryContainer
    return remember(scheme) {
        ReaderStudioStyle(
            light = ReaderPalette(
                background = VolaReader.lightBackground,
                content = VolaReader.lightContent,
                muted = VolaReader.lightMuted,
                card = VolaReader.lightCard,
                accent = pageAccent,
                onAccent = onPageAccent,
            ),
            paper = ReaderPalette(
                background = VolaReader.paperBackground,
                content = VolaReader.paperContent,
                muted = VolaReader.paperMuted,
                card = VolaReader.paperCard,
                accent = pageAccent,
                onAccent = onPageAccent,
            ),
            dark = ReaderPalette(
                background = VolaReader.darkBackground,
                content = VolaReader.darkContent,
                muted = VolaReader.darkMuted,
                card = VolaReader.darkCard,
                accent = darkAccent,
                onAccent = onDarkAccent,
            ),
            serifFontFamily = LiterataFontFamily,
            sansFontFamily = ManropeFontFamily,
            titleFontSize = VolaReader.titleFontSize,
            titleLineHeight = VolaReader.titleLineHeight,
            progressHeight = VolaReader.progressHeight,
            articlePadding = VolaReader.articlePadding,
            wideArticlePadding = VolaReader.wideArticlePadding,
            articleGap = VolaReader.articleGap,
            headerPadding = VolaReader.headerPadding,
            headerGap = VolaReader.headerGap,
            siteGemSize = VolaReader.siteGemSize,
            siteGemShape = VolaReader.siteGemShape,
            panelMargin = VolaReader.panelMargin,
            panelShape = VolaReader.panelShape,
            panelElevation = VolaReader.panelElevation,
            panelPadding = VolaReader.panelPadding,
            panelGap = VolaReader.panelGap,
            swatchHeight = VolaReader.swatchHeight,
            swatchShape = VolaReader.swatchShape,
            swatchSelectedBorder = VolaReader.swatchSelectedBorder,
            cardShape = VolaReader.cardShape,
            cardPadding = PaddingValues(
                horizontal = VolaReader.cardPaddingHorizontal,
                vertical = VolaReader.cardPaddingVertical,
            ),
            buttonHeight = VolaReader.buttonHeight,
            iconSize = VolaReader.iconSize,
            handleWidth = VolaReader.handleWidth,
            handleHeight = VolaReader.handleHeight,
        )
    }
}

/** The snackbar line after saving a page for offline reading. */
internal fun ReaderExtractionResult.readerActionMessageRes(): Int = when (this) {
    is ReaderExtractionResult.Success -> R.string.reader_saved_offline_confirmation
    is ReaderExtractionResult.Failure -> when (reason) {
        ReaderExtractionFailure.UnsupportedPage -> R.string.reader_extraction_unsupported
        ReaderExtractionFailure.EmptyArticle -> R.string.reader_extraction_empty
        ReaderExtractionFailure.InvalidResponse -> R.string.reader_extraction_invalid
    }
}
