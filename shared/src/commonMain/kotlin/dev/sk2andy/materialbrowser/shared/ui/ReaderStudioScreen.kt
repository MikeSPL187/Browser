package dev.sk2andy.materialbrowser.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.reader.ReaderBlock
import dev.sk2andy.materialbrowser.reader.ReaderBlockKind
import dev.sk2andy.materialbrowser.reader.ReaderDocument
import dev.sk2andy.materialbrowser.reader.ReaderExtractionFailure
import dev.sk2andy.materialbrowser.reader.ReaderExtractionResult
import dev.sk2andy.materialbrowser.reader.ReaderLibraryDataSource
import dev.sk2andy.materialbrowser.reader.ReaderLibraryRules
import dev.sk2andy.materialbrowser.reader.ReaderLibraryState
import dev.sk2andy.materialbrowser.reader.ReaderSettings
import dev.sk2andy.materialbrowser.reader.ReaderSnapshot
import dev.sk2andy.materialbrowser.reader.ReaderSpeech
import dev.sk2andy.materialbrowser.reader.ReaderSpeechRules
import dev.sk2andy.materialbrowser.reader.ReaderSpeechState
import dev.sk2andy.materialbrowser.reader.ReaderSpeechStatus
import dev.sk2andy.materialbrowser.reader.ReaderTextAlignment
import dev.sk2andy.materialbrowser.reader.ReaderTheme
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import kotlinx.coroutines.delay

object ReaderStudioTestTags {
    const val Screen = "reader_studio_screen"
    const val Loading = "reader_studio_loading"
    const val Error = "reader_studio_error"
    const val Article = "reader_studio_article"
    const val Progress = "reader_studio_progress"
    const val Save = "reader_studio_save"
    const val Library = "reader_studio_library"
    const val PrivateNotice = "reader_studio_private_notice"
    const val SpeechPlay = "reader_studio_speech_play"
    const val SpeechPause = "reader_studio_speech_pause"
    const val SpeechStop = "reader_studio_speech_stop"
    const val SpeechTransport = "reader_studio_speech_transport"
    const val ThemeSegmented = "reader_studio_theme_segmented"
    const val FontSegmented = "reader_studio_font_segmented"
    const val FontFamily = "reader_studio_font_family"
    const val WideMargins = "reader_studio_wide_margins"
    const val SettingsButton = "reader_studio_settings_button"
    const val SettingsPanel = "reader_studio_settings_panel"
    const val Original = "reader_studio_original"
}

enum class ReaderStudioLabel {
    Close,
    Title,
    Article,
    Original,
    OfflineCount,
    Extracting,
    ExtractionFailedTitle,
    ExtractionUnsupported,
    ExtractionEmpty,
    ExtractionInvalid,
    Retry,
    SaveOffline,
    ThemeLight,
    ThemePaper,
    ThemeDark,
    ReadingView,
    ReadingTime,
    ReadingSettings,
    TextSize,
    FontSerif,
    FontSans,
    WideMargins,
    Listen,
    SpeechStart,
    SpeechResume,
    SpeechPause,
    SpeechStop,
    PrivateNotice,
    OfflineLibrary,
    OfflineEmpty,
    ProgressPercent,
    DeleteSnapshot,
}

enum class ReaderStudioIcon {
    Download,
    Pause,
    Stop,
}

interface ReaderStudioResources {
    @Composable
    fun text(label: ReaderStudioLabel, value: Int? = null): String

    @Composable
    fun icon(
        icon: ReaderStudioIcon,
        modifier: Modifier,
        contentDescription: String?,
    )
}

internal object DefaultReaderStudioResources : ReaderStudioResources {
    @Composable
    override fun text(label: ReaderStudioLabel, value: Int?): String = when (label) {
        ReaderStudioLabel.Close -> "Close Reader Studio"
        ReaderStudioLabel.Title -> "Reader Studio"
        ReaderStudioLabel.Article -> "Article"
        ReaderStudioLabel.Original -> "Original"
        ReaderStudioLabel.OfflineCount -> "Offline (${requireNotNull(value)})"
        ReaderStudioLabel.Extracting -> "Preparing article on this device…"
        ReaderStudioLabel.ExtractionFailedTitle -> "Article could not be prepared"
        ReaderStudioLabel.ExtractionUnsupported ->
            "Reader Studio works with HTTP and HTTPS pages."
        ReaderStudioLabel.ExtractionEmpty ->
            "No sufficiently readable article text was found. The original page is unchanged."
        ReaderStudioLabel.ExtractionInvalid ->
            "The page changed or returned invalid content. Try again or return to the original."
        ReaderStudioLabel.Retry -> "Try again"
        ReaderStudioLabel.SaveOffline -> "Save offline"
        ReaderStudioLabel.ThemeLight -> "Light"
        ReaderStudioLabel.ThemePaper -> "Paper"
        ReaderStudioLabel.ThemeDark -> "Dark"
        ReaderStudioLabel.ReadingView -> "Reading view"
        ReaderStudioLabel.ReadingTime -> "${requireNotNull(value)} min"
        ReaderStudioLabel.ReadingSettings -> "Reading view settings"
        ReaderStudioLabel.TextSize -> "Text size"
        ReaderStudioLabel.FontSerif -> "Serif"
        ReaderStudioLabel.FontSans -> "Sans serif"
        ReaderStudioLabel.WideMargins -> "Wide margins"
        ReaderStudioLabel.Listen -> "Listen"
        ReaderStudioLabel.SpeechStart -> "Read aloud"
        ReaderStudioLabel.SpeechResume -> "Resume"
        ReaderStudioLabel.SpeechPause -> "Pause"
        ReaderStudioLabel.SpeechStop -> "Stop"
        ReaderStudioLabel.PrivateNotice ->
            "Private reading stays in memory. Progress, settings, and offline copies are not saved."
        ReaderStudioLabel.OfflineLibrary -> "Offline articles"
        ReaderStudioLabel.OfflineEmpty -> "No offline articles saved yet."
        ReaderStudioLabel.ProgressPercent -> "${requireNotNull(value)}% read"
        ReaderStudioLabel.DeleteSnapshot -> "Delete offline article"
    }

    @Composable
    override fun icon(
        icon: ReaderStudioIcon,
        modifier: Modifier,
        contentDescription: String?,
    ) {
        Icon(
            imageVector = when (icon) {
                ReaderStudioIcon.Download -> VolaIcons.Download
                ReaderStudioIcon.Pause -> VolaIcons.PauseFilled
                ReaderStudioIcon.Stop -> VolaIcons.StopFilled
            },
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

internal class SessionReaderLibraryDataSource : ReaderLibraryDataSource {
    private var state = ReaderLibraryState()
    private var nextSnapshotId = 1L

    override fun load(isPrivate: Boolean, onLoaded: (ReaderLibraryState) -> Unit) {
        onLoaded(ReaderLibraryRules.visibleState(state, isPrivate))
    }

    override fun updateSettings(
        settings: ReaderSettings,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    ) {
        state = ReaderLibraryRules.updateSettings(state, settings, isPrivate)
        onUpdated(ReaderLibraryRules.visibleState(state, isPrivate))
    }

    override fun updateProgress(sourceUrl: String, progress: Float, isPrivate: Boolean) {
        state = ReaderLibraryRules.updateProgress(state, sourceUrl, progress, isPrivate)
    }

    override fun saveSnapshot(
        document: ReaderDocument,
        progress: Float,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    ) {
        val snapshotId = nextSnapshotId++
        state = ReaderLibraryRules.saveSnapshot(
            state = state,
            snapshot = ReaderSnapshot(
                id = "reader-$snapshotId",
                document = document,
                progress = progress.coerceIn(0f, 1f),
                savedAtMillis = snapshotId,
            ),
            isPrivate = isPrivate,
        )
        onUpdated(ReaderLibraryRules.visibleState(state, isPrivate))
    }

    override fun deleteSnapshot(
        snapshotId: String,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    ) {
        state = ReaderLibraryRules.deleteSnapshot(state, snapshotId, isPrivate)
        onUpdated(ReaderLibraryRules.visibleState(state, isPrivate))
    }
}

internal class UnavailableReaderSpeech : ReaderSpeech {
    override var state = ReaderSpeechState(status = ReaderSpeechStatus.Unavailable)
        private set

    override fun play(content: String) = Unit

    override fun pause() = Unit

    override fun stop() = Unit

    override fun close() {
        state = ReaderSpeechState(status = ReaderSpeechStatus.Closed)
    }
}

@Composable
fun ReaderStudioScreen(
    result: ReaderExtractionResult?,
    sourceUrl: String,
    isPrivate: Boolean,
    repository: ReaderLibraryDataSource,
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    speechFactory: () -> ReaderSpeech,
    /** Registers a system back handler; shared code has none of its own. */
    backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenOriginal: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    /** Opens with «Reading view» shown, for previews. */
    initialSettingsVisible: Boolean = false,
) {
    var library by remember(isPrivate) { mutableStateOf(ReaderLibraryState()) }
    var activeDocument by remember(result) {
        mutableStateOf((result as? ReaderExtractionResult.Success)?.document)
    }
    var activeSnapshot by remember { mutableStateOf<ReaderSnapshot?>(null) }
    var libraryVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(initialSettingsVisible) }
    var libraryLoaded by remember(isPrivate) { mutableStateOf(isPrivate) }
    var sessionSettings by remember(library.settings, isPrivate) {
        mutableStateOf(library.settings)
    }
    val browserDark = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE
    val colors = style.palette(sessionSettings.theme, browserDark)
    LaunchedEffect(result, isPrivate) {
        libraryLoaded = false
        repository.load(isPrivate) { loaded ->
            library = loaded
            libraryLoaded = true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ReaderStudioTestTags.Screen),
        color = colors.background,
        contentColor = colors.content,
    ) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            ReaderStudioHeader(
                resources = resources,
                style = style,
                colors = colors,
                document = activeDocument,
                settingsAvailable = activeDocument != null && !libraryVisible,
                libraryVisible = libraryVisible,
                onSettings = { settingsVisible = !settingsVisible },
                onShowArticle = { libraryVisible = false },
                onDismiss = onDismiss,
            )
            if (libraryVisible && !isPrivate) {
                ReaderLibraryContent(
                    resources = resources,
                    snapshots = library.snapshots,
                    progressByUrl = library.progressByUrl,
                    colors = colors,
                    onOpen = { snapshot ->
                        activeSnapshot = snapshot
                        activeDocument = snapshot.document
                        libraryVisible = false
                    },
                    onDelete = { snapshot ->
                        if (activeSnapshot?.id == snapshot.id) activeSnapshot = null
                        repository.deleteSnapshot(snapshot.id, isPrivate = false) { updated ->
                            library = updated
                        }
                    },
                )
            } else {
                when {
                    result == null && activeDocument == null -> ReaderLoading(resources, colors)
                    result is ReaderExtractionResult.Failure && activeDocument == null ->
                        ReaderError(
                            resources,
                            result.reason,
                            colors,
                            onRetry,
                            onOpenOriginal = { onOpenOriginal(sourceUrl) },
                        )
                    activeDocument != null -> ReaderArticle(
                        document = checkNotNull(activeDocument),
                        snapshotProgress = activeSnapshot?.progress,
                        isPrivate = isPrivate,
                        settings = sessionSettings,
                        library = library,
                        libraryLoaded = libraryLoaded,
                        repository = repository,
                        resources = resources,
                        style = style,
                        browserDark = browserDark,
                        speechFactory = speechFactory,
                        colors = colors,
                        settingsVisible = settingsVisible,
                        onSettingsDismiss = { settingsVisible = false },
                        onSettingsChanged = { settings ->
                            sessionSettings = settings
                            if (!isPrivate) {
                                repository.updateSettings(
                                    settings,
                                    isPrivate = false,
                                ) { updated ->
                                    library = updated
                                }
                            }
                        },
                        onLibraryChanged = { updatedLibrary ->
                            library = updatedLibrary
                            activeSnapshot = library.snapshots.firstOrNull {
                                it.document.sourceUrl == activeDocument?.sourceUrl
                            }
                        },
                        onShowLibrary = {
                            settingsVisible = false
                            libraryVisible = true
                        },
                        onOpenOriginal = {
                            onOpenOriginal(activeDocument?.sourceUrl ?: sourceUrl)
                        },
                        onOpenLink = onOpenLink,
                    )
                }
            }
        }
    }
    backHandler(settingsVisible || libraryVisible) {
        if (settingsVisible) settingsVisible = false else libraryVisible = false
    }
}

private const val DARK_LUMINANCE = 0.5f

/**
 * The site and how long the article takes to read on the left, «Aa» for the reading view and
 * close on the right (board W-Reader).
 */
@Composable
private fun ReaderStudioHeader(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    colors: ReaderPalette,
    document: ReaderDocument?,
    settingsAvailable: Boolean,
    libraryVisible: Boolean,
    onSettings: () -> Unit,
    onShowArticle: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(style.headerPadding),
        horizontalArrangement = Arrangement.spacedBy(style.headerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (document != null) {
            Box(
                modifier = Modifier
                    .size(style.siteGemSize)
                    .background(colors.accent, style.siteGemShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = document.siteName.trim().take(1).uppercase(),
                    color = colors.onAccent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = document.siteName + " · " + resources.text(
                    ReaderStudioLabel.ReadingTime,
                    remember(document) {
                        ReaderLibraryRules.readingMinutes(document.speechText)
                    },
                ),
                modifier = Modifier.weight(1f),
                color = colors.muted,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = resources.text(ReaderStudioLabel.Title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (libraryVisible && document != null) {
            TextButton(onClick = onShowArticle) {
                Text(resources.text(ReaderStudioLabel.Article), color = colors.content)
            }
        }
        if (settingsAvailable) {
            val settingsDescription = resources.text(ReaderStudioLabel.ReadingSettings)
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .testTag(ReaderStudioTestTags.SettingsButton)
                    .semantics { contentDescription = settingsDescription },
                colors = IconButtonDefaults.iconButtonColors(containerColor = colors.card),
            ) {
                Text(
                    text = "Aa",
                    modifier = Modifier.clearAndSetSemantics { },
                    fontFamily = style.serifFontFamily,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            colors = IconButtonDefaults.iconButtonColors(containerColor = colors.card),
        ) {
            Icon(
                VolaIcons.Close,
                contentDescription = resources.text(ReaderStudioLabel.Close),
            )
        }
    }
}

@Composable
private fun ReaderLoading(
    resources: ReaderStudioResources,
    colors: ReaderPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ReaderStudioTestTags.Loading),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = colors.accent)
            Spacer(Modifier.height(16.dp))
            Text(resources.text(ReaderStudioLabel.Extracting))
        }
    }
}

@Composable
private fun ReaderError(
    resources: ReaderStudioResources,
    failure: ReaderExtractionFailure,
    colors: ReaderPalette,
    onRetry: () -> Unit,
    onOpenOriginal: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(ReaderStudioTestTags.Error),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(32.dp), color = colors.card) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    resources.text(ReaderStudioLabel.ExtractionFailedTitle),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    resources.text(
                        when (failure) {
                            ReaderExtractionFailure.UnsupportedPage ->
                                ReaderStudioLabel.ExtractionUnsupported
                            ReaderExtractionFailure.EmptyArticle -> ReaderStudioLabel.ExtractionEmpty
                            ReaderExtractionFailure.InvalidResponse ->
                                ReaderStudioLabel.ExtractionInvalid
                        },
                    ),
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenOriginal) {
                        Text(resources.text(ReaderStudioLabel.Original))
                    }
                    Button(onClick = onRetry) {
                        Text(resources.text(ReaderStudioLabel.Retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderArticle(
    document: ReaderDocument,
    snapshotProgress: Float?,
    isPrivate: Boolean,
    settings: ReaderSettings,
    library: ReaderLibraryState,
    libraryLoaded: Boolean,
    repository: ReaderLibraryDataSource,
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    browserDark: Boolean,
    speechFactory: () -> ReaderSpeech,
    colors: ReaderPalette,
    settingsVisible: Boolean,
    onSettingsDismiss: () -> Unit,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onLibraryChanged: (ReaderLibraryState) -> Unit,
    onShowLibrary: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val speech = remember(document.sourceUrl) { speechFactory() }
    var restored by remember(document.sourceUrl) { mutableStateOf(false) }
    val progress = ReaderLibraryRules.progress(scrollState.value, scrollState.maxValue)
    val fontFamily = if (settings.serif) style.serifFontFamily else style.sansFontFamily

    DisposableEffect(speech) { onDispose(speech::close) }
    LaunchedEffect(document.sourceUrl, scrollState.maxValue, libraryLoaded) {
        if (restored || !libraryLoaded || scrollState.maxValue <= 0) return@LaunchedEffect
        val savedProgress = ReaderLibraryRules.resumeProgress(
            state = library,
            snapshotProgress = snapshotProgress,
            sourceUrl = document.sourceUrl,
        )
        scrollState.scrollTo((scrollState.maxValue * savedProgress).toInt())
        restored = true
    }
    LaunchedEffect(scrollState.isScrollInProgress, restored) {
        if (restored && !scrollState.isScrollInProgress) {
            delay(350)
            repository.updateProgress(document.sourceUrl, progress, isPrivate)
        }
    }
    DisposableEffect(document.sourceUrl, isPrivate) {
        onDispose {
            if (isPrivate) return@onDispose
            val finalProgress = ReaderLibraryRules.progress(scrollState.value, scrollState.maxValue)
            repository.updateProgress(document.sourceUrl, finalProgress, isPrivate)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(style.progressHeight)
                    .testTag(ReaderStudioTestTags.Progress),
                color = colors.accent,
                trackColor = colors.card,
                drawStopIndicator = {},
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = if (settings.wideMargins) {
                            style.wideArticlePadding
                        } else {
                            style.articlePadding
                        },
                    )
                    .testTag(ReaderStudioTestTags.Article),
            ) {
                Spacer(Modifier.height(style.articleGap))
                Text(
                    document.title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = style.titleFontSize * settings.fontScale,
                        lineHeight = style.titleLineHeight * settings.fontScale,
                    ),
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(style.articleGap))
                document.blocks.forEach { block ->
                    ReaderBlockContent(
                        block = block,
                        fontScale = settings.fontScale,
                        textAlignment = settings.textAlignment,
                        fontFamily = fontFamily,
                        colors = colors,
                        onOpenLink = onOpenLink,
                    )
                }
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(style.articleGap),
                )
            }
        }
        if (settingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { onSettingsDismiss() } },
            )
        }
        AnimatedVisibility(
            visible = settingsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ReaderSettingsPanel(
                resources = resources,
                style = style,
                settings = settings,
                browserDark = browserDark,
                isPrivate = isPrivate,
                snapshotCount = library.snapshots.size,
                speechStatus = speech.state.status,
                speechExcerpt = ReaderSpeechRules.currentExcerpt(
                    document.speechText,
                    speech.state.characterOffset,
                ),
                onSettingsChanged = onSettingsChanged,
                onPlay = { speech.play(document.speechText) },
                onPause = speech::pause,
                onStop = speech::stop,
                onSave = {
                    repository.saveSnapshot(document, progress, isPrivate, onLibraryChanged)
                },
                onShowLibrary = onShowLibrary,
                onOpenOriginal = onOpenOriginal,
            )
        }
    }
}

/**
 * «Reading view» (board W-Reader): listen, the reading theme, text size, the typeface, wide
 * margins, and the offline copy. It sits on the browser's own surface, not the reading theme,
 * so the swatches show each theme as it is.
 */
@Composable
private fun ReaderSettingsPanel(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    settings: ReaderSettings,
    browserDark: Boolean,
    isPrivate: Boolean,
    snapshotCount: Int,
    speechStatus: ReaderSpeechStatus,
    speechExcerpt: String,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    onShowLibrary: () -> Unit,
    onOpenOriginal: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(style.panelMargin)
            .testTag(ReaderStudioTestTags.SettingsPanel),
        shape = style.panelShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = style.panelElevation,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(style.panelPadding),
            verticalArrangement = Arrangement.spacedBy(style.panelGap),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(style.handleWidth, style.handleHeight)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    resources.text(ReaderStudioLabel.ReadingView),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ReaderListenControl(
                    resources = resources,
                    style = style,
                    status = speechStatus,
                    onPlay = onPlay,
                    onPause = onPause,
                    onStop = onStop,
                )
            }
            if (
                speechStatus == ReaderSpeechStatus.Speaking ||
                speechStatus == ReaderSpeechStatus.Paused
            ) {
                Text(
                    speechExcerpt,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ReaderThemeSwatches(
                resources = resources,
                style = style,
                settings = settings,
                browserDark = browserDark,
                onSettingsChanged = onSettingsChanged,
            )
            ReaderTextSize(
                resources = resources,
                style = style,
                settings = settings,
                onSettingsChanged = onSettingsChanged,
            )
            ReaderFontFamilyChoice(
                resources = resources,
                style = style,
                settings = settings,
                onSettingsChanged = onSettingsChanged,
            )
            ReaderWideMargins(
                resources = resources,
                style = style,
                settings = settings,
                onSettingsChanged = onSettingsChanged,
            )
            if (!isPrivate) {
                Row(horizontalArrangement = Arrangement.spacedBy(style.panelGap)) {
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = style.buttonHeight)
                            .testTag(ReaderStudioTestTags.Save),
                    ) {
                        resources.icon(
                            icon = ReaderStudioIcon.Download,
                            modifier = Modifier.size(style.iconSize),
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            resources.text(ReaderStudioLabel.SaveOffline),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = onShowLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = style.buttonHeight)
                            .testTag(ReaderStudioTestTags.Library),
                    ) {
                        Text(
                            resources.text(ReaderStudioLabel.OfflineCount, snapshotCount),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            TextButton(
                onClick = onOpenOriginal,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(ReaderStudioTestTags.Original),
            ) {
                Text(resources.text(ReaderStudioLabel.Original))
            }
            if (isPrivate) {
                Text(
                    resources.text(ReaderStudioLabel.PrivateNotice),
                    modifier = Modifier.testTag(ReaderStudioTestTags.PrivateNotice),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** «Listen» until speech starts, then pause or resume and stop. */
@Composable
private fun ReaderListenControl(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    status: ReaderSpeechStatus,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val isSpeaking = status == ReaderSpeechStatus.Speaking
    val isPaused = status == ReaderSpeechStatus.Paused
    Row(
        modifier = Modifier.testTag(ReaderStudioTestTags.SpeechTransport),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSpeaking || isPaused) {
            FilledIconButton(
                onClick = if (isSpeaking) onPause else onPlay,
                modifier = Modifier.testTag(
                    if (isSpeaking) {
                        ReaderStudioTestTags.SpeechPause
                    } else {
                        ReaderStudioTestTags.SpeechPlay
                    },
                ),
            ) {
                val description = resources.text(
                    if (isSpeaking) {
                        ReaderStudioLabel.SpeechPause
                    } else {
                        ReaderStudioLabel.SpeechResume
                    },
                )
                if (isSpeaking) {
                    resources.icon(
                        icon = ReaderStudioIcon.Pause,
                        modifier = Modifier.size(style.iconSize),
                        contentDescription = description,
                    )
                } else {
                    Icon(VolaIcons.PlayArrowFilled, contentDescription = description)
                }
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier.testTag(ReaderStudioTestTags.SpeechStop),
            ) {
                resources.icon(
                    icon = ReaderStudioIcon.Stop,
                    modifier = Modifier.size(style.iconSize),
                    contentDescription = resources.text(ReaderStudioLabel.SpeechStop),
                )
            }
        } else {
            FilledTonalButton(
                onClick = onPlay,
                enabled = status == ReaderSpeechStatus.Ready,
                modifier = Modifier.testTag(ReaderStudioTestTags.SpeechPlay),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    VolaIcons.Headphones,
                    contentDescription = null,
                    modifier = Modifier.size(style.iconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(resources.text(ReaderStudioLabel.Listen))
            }
        }
    }
}

/** Light, Paper and Dark, each drawn in its own colors; the one in use is outlined. */
@Composable
private fun ReaderThemeSwatches(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    settings: ReaderSettings,
    browserDark: Boolean,
    onSettingsChanged: (ReaderSettings) -> Unit,
) {
    val current = settings.theme.resolved(browserDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .testTag(ReaderStudioTestTags.ThemeSegmented),
        horizontalArrangement = Arrangement.spacedBy(style.panelGap),
    ) {
        READER_SWATCH_THEMES.forEach { theme ->
            val palette = style.palette(theme, browserDark)
            val selected = current == theme
            Surface(
                selected = selected,
                onClick = { onSettingsChanged(settings.copy(theme = theme)) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = style.swatchHeight)
                    .semantics { role = Role.RadioButton },
                shape = style.swatchShape,
                color = palette.background,
                contentColor = palette.content,
                border = if (selected) {
                    BorderStroke(style.swatchSelectedBorder, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant)
                },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Aa",
                        modifier = Modifier.clearAndSetSemantics { },
                        fontFamily = style.serifFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        resources.text(
                            when (theme) {
                                ReaderTheme.Paper -> ReaderStudioLabel.ThemePaper
                                ReaderTheme.Dark -> ReaderStudioLabel.ThemeDark
                                ReaderTheme.System,
                                ReaderTheme.Light,
                                -> ReaderStudioLabel.ThemeLight
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val READER_SWATCH_THEMES = listOf(ReaderTheme.Light, ReaderTheme.Paper, ReaderTheme.Dark)

@Composable
private fun ReaderTextSize(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
) {
    val description = resources.text(ReaderStudioLabel.TextSize)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ReaderStudioTestTags.FontSegmented),
        horizontalArrangement = Arrangement.spacedBy(style.panelGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "A",
            modifier = Modifier.clearAndSetSemantics { },
            fontFamily = style.serifFontFamily,
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = settings.fontScale,
            onValueChange = { scale ->
                if (scale != settings.fontScale) onSettingsChanged(settings.copy(fontScale = scale))
            },
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = description },
            valueRange = ReaderLibraryRules.MIN_FONT_SCALE..ReaderLibraryRules.MAX_FONT_SCALE,
            steps = ReaderLibraryRules.FONT_SCALE_STEPS,
        )
        Text(
            "A",
            modifier = Modifier.clearAndSetSemantics { },
            fontFamily = style.serifFontFamily,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun ReaderFontFamilyChoice(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ReaderStudioTestTags.FontFamily),
    ) {
        listOf(true, false).forEachIndexed { index, serif ->
            SegmentedButton(
                selected = settings.serif == serif,
                onClick = { onSettingsChanged(settings.copy(serif = serif)) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                label = {
                    Text(
                        resources.text(
                            if (serif) ReaderStudioLabel.FontSerif else ReaderStudioLabel.FontSans,
                        ),
                        fontFamily = if (serif) style.serifFontFamily else style.sansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun ReaderWideMargins(
    resources: ReaderStudioResources,
    style: ReaderStudioStyle,
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
) {
    Surface(
        shape = style.cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = style.buttonHeight)
                .toggleable(
                    value = settings.wideMargins,
                    role = Role.Switch,
                    onValueChange = { onSettingsChanged(settings.copy(wideMargins = it)) },
                )
                .padding(style.cardPadding)
                .testTag(ReaderStudioTestTags.WideMargins),
            horizontalArrangement = Arrangement.spacedBy(style.panelGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                VolaIcons.FormatSize,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                resources.text(ReaderStudioLabel.WideMargins),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(checked = settings.wideMargins, onCheckedChange = null)
        }
    }
}

@Composable
private fun ReaderBlockContent(
    block: ReaderBlock,
    fontScale: Float,
    textAlignment: ReaderTextAlignment,
    fontFamily: FontFamily,
    colors: ReaderPalette,
    onOpenLink: (String) -> Unit,
) {
    val justify = ReaderLibraryRules.shouldJustify(block.kind, textAlignment)
    val style = when (block.kind) {
        ReaderBlockKind.Heading -> MaterialTheme.typography.headlineSmall.copy(
            fontSize = (24 - block.level.coerceAtLeast(1)).sp * fontScale,
            lineHeight = 29.sp * fontScale,
            fontWeight = FontWeight.Bold,
        )
        ReaderBlockKind.Quote -> MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp * fontScale,
            lineHeight = 29.sp * fontScale,
            fontWeight = FontWeight.Medium,
        )
        ReaderBlockKind.ListItem,
        ReaderBlockKind.Paragraph,
        -> MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp * fontScale,
            lineHeight = 29.sp * fontScale,
        )
    }
    val prefix = if (block.kind == ReaderBlockKind.ListItem) "•  " else ""
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (block.kind == ReaderBlockKind.Quote) colors.card else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (block.kind == ReaderBlockKind.Quote) 16.dp else 0.dp,
                    vertical = 10.dp,
                ),
        ) {
            Text(
                prefix + block.text,
                modifier = if (block.kind == ReaderBlockKind.Heading) {
                    Modifier.semantics { heading() }
                } else {
                    Modifier
                },
                style = style.copy(
                    fontFamily = fontFamily,
                    textAlign = if (justify) TextAlign.Justify else TextAlign.Start,
                    hyphens = if (justify) Hyphens.Auto else Hyphens.None,
                ),
            )
            block.links.forEach { link ->
                Surface(
                    onClick = { onOpenLink(link.url) },
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.accent.copy(alpha = 0.12f),
                    contentColor = colors.accent,
                    shape = CircleShape,
                ) {
                    Text(
                        "↗ ${link.label}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderLibraryContent(
    resources: ReaderStudioResources,
    snapshots: List<ReaderSnapshot>,
    progressByUrl: Map<String, Float>,
    colors: ReaderPalette,
    onOpen: (ReaderSnapshot) -> Unit,
    onDelete: (ReaderSnapshot) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        Text(
            resources.text(ReaderStudioLabel.OfflineLibrary),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        if (snapshots.isEmpty()) {
            Text(resources.text(ReaderStudioLabel.OfflineEmpty), color = colors.muted)
        }
        snapshots.forEach { snapshot ->
            Surface(
                onClick = { onOpen(snapshot) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                color = colors.card,
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(snapshot.document.title, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(snapshot.document.siteName, color = colors.muted)
                        Text(
                            resources.text(
                                ReaderStudioLabel.ProgressPercent,
                                (
                                    progressByUrl[snapshot.document.sourceUrl]
                                        ?: snapshot.progress
                                    ).times(100).toInt(),
                            ),
                            color = colors.muted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IconButton(onClick = { onDelete(snapshot) }) {
                        Icon(
                            VolaIcons.Delete,
                            contentDescription = resources.text(ReaderStudioLabel.DeleteSnapshot),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
