package dev.sk2andy.materialbrowser.ui

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadHistoryRules
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.data.DownloadTimeFilter
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsScreen(
    downloads: List<DownloadEntry>,
    isClearing: Boolean = false,
    onClearFinished: (List<DownloadEntry>) -> Unit,
    onOpenDownload: (DownloadEntry) -> Unit,
    onBack: () -> Unit,
    onCancelDownload: (DownloadEntry) -> Unit = {},
    onTogglePauseDownload: (DownloadEntry) -> Unit = {},
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    var query by rememberSaveable { mutableStateOf("") }
    var timeFilterName by rememberSaveable { mutableStateOf(DownloadTimeFilter.All.name) }
    var kindFilterName by rememberSaveable { mutableStateOf(DownloadKindFilter.All.name) }
    var clearConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val timeFilter = DownloadTimeFilter.valueOf(timeFilterName)
    val kindFilter = DownloadKindFilter.valueOf(kindFilterName)
    val visibleDownloads = remember(downloads, query, timeFilter, kindFilter, zoneId) {
        LibraryFileRules.filter(
            DownloadHistoryRules.visibleEntries(
                entries = downloads,
                query = query,
                timeFilter = timeFilter,
                nowMillis = System.currentTimeMillis(),
                zoneId = zoneId,
            ),
            kindFilter,
        )
    }
    val active = remember(visibleDownloads) { visibleDownloads.filter { it.status.isActive } }
    val sections = remember(visibleDownloads, zoneId) {
        LibraryFileRules.daySections(visibleDownloads, zoneId)
    }
    val clearableDownloads = remember(downloads) {
        downloads.filter { entry -> entry.status.isTerminal }
    }
    val today = remember { LocalDate.now(zoneId) }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            VolaIcons.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { clearConfirmationVisible = true },
                        enabled = clearableDownloads.isNotEmpty() && !isClearing,
                        modifier = Modifier.testTag(DownloadsScreenTestTags.Clear),
                    ) {
                        Icon(
                            VolaIcons.DeleteSweep,
                            contentDescription = stringResource(R.string.downloads_clear),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.testTag(DownloadsScreenTestTags.More),
                        ) {
                            Icon(
                                VolaIcons.MoreVert,
                                contentDescription = stringResource(R.string.downloads_more),
                            )
                        }
                        // The period: rarer than the kind, so it waits in the menu.
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DownloadTimeFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(downloadTimeFilterLabel(filter)) },
                                    trailingIcon = if (timeFilter == filter) {
                                        { Icon(VolaIcons.Check, contentDescription = null) }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        timeFilterName = filter.name
                                        menuOpen = false
                                    },
                                    modifier = Modifier
                                        .semantics { selected = timeFilter == filter }
                                        .testTag(DownloadsScreenTestTags.timeFilter(filter)),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .navigationBarsPadding()
                .testTag(DownloadsScreenTestTags.List),
        ) {
            item(key = "search") {
                LibrarySearchBar(
                    query = query,
                    placeholder = stringResource(R.string.downloads_search),
                    clearContentDescription = stringResource(R.string.downloads_clear_search),
                    testTag = DownloadsScreenTestTags.SearchField,
                    onQueryChange = {
                        query = it.take(DownloadHistoryRules.MAX_QUERY_CHARS)
                    },
                )
            }

            item(key = "kinds") {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = VolaLibrary.sidePadding)
                        .padding(top = VolaLibrary.sectionGap),
                    horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
                ) {
                    DownloadKindFilter.entries.forEach { filter ->
                        val selected = kindFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { kindFilterName = filter.name },
                            label = { Text(downloadKindFilterLabel(filter)) },
                            leadingIcon = if (selected) {
                                { Icon(VolaIcons.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            modifier = Modifier.testTag(DownloadsScreenTestTags.kindFilter(filter)),
                        )
                    }
                }
            }

            if (visibleDownloads.isEmpty()) {
                item(key = "empty") {
                    DownloadsEmptyState(
                        searching = query.isNotBlank() ||
                            timeFilter != DownloadTimeFilter.All ||
                            kindFilter != DownloadKindFilter.All,
                        modifier = Modifier
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap),
                    )
                }
            }
            // Downloads still running sit on top, each in its own card (board W-Downloads).
            itemsIndexed(active, key = { _, entry -> entry.id }) { _, entry ->
                LibraryCardSlice(
                    position = LibraryRowPosition.Single,
                    modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                ) {
                    DownloadActiveRow(
                        entry = entry,
                        progressLabel = downloadProgressLabel(context, entry),
                        onCancel = { onCancelDownload(entry) },
                        onTogglePause = { onTogglePauseDownload(entry) },
                    )
                }
            }
            sections.forEach { section ->
                item(key = "day:${section.date}") {
                    LibrarySectionLabel(
                        text = when (section.date) {
                            today -> stringResource(R.string.downloads_time_today)
                            today.minusDays(1) -> stringResource(R.string.downloads_yesterday)
                            else -> dateFormatter.format(section.date)
                        },
                        modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                    )
                }
                itemsIndexed(section.entries, key = { _, entry -> entry.id }) { index, entry ->
                    LibraryCardSlice(position = LibraryRules.position(index, section.entries.size)) {
                        DownloadRow(
                            entry = entry,
                            size = Formatter.formatShortFileSize(context, maxOf(entry.bytes, entry.total)),
                            time = timeFormatter.format(
                                Instant.ofEpochMilli(entry.lastModified).atZone(zoneId),
                            ),
                            onOpen = { onOpenDownload(entry) },
                        )
                    }
                }
            }
        }
    }

    if (clearConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { clearConfirmationVisible = false },
            modifier = Modifier.testTag(DownloadsScreenTestTags.ClearDialog),
            title = { Text(stringResource(R.string.downloads_clear_title)) },
            text = { Text(stringResource(R.string.downloads_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearConfirmationVisible = false
                        onClearFinished(clearableDownloads)
                    },
                    enabled = !isClearing,
                    modifier = Modifier.testTag(DownloadsScreenTestTags.ClearConfirm),
                ) {
                    Text(stringResource(R.string.downloads_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmationVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** «18.6 of 30 MB» while it downloads; just the size so far when the total is unknown. */
private fun downloadProgressLabel(context: android.content.Context, entry: DownloadEntry): String {
    val bytes = Formatter.formatShortFileSize(context, entry.bytes)
    return if (entry.total > 0L) {
        context.getString(
            R.string.downloads_progress_of,
            bytes,
            Formatter.formatShortFileSize(context, entry.total),
        )
    } else {
        bytes
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadActiveRow(
    entry: DownloadEntry,
    progressLabel: String,
    onCancel: () -> Unit,
    onTogglePause: () -> Unit,
) {
    val progress = DownloadHistoryRules.progress(entry)
    val paused = entry.status == DownloadStatus.Paused
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DownloadsScreenTestTags.download(entry.id))
            .padding(bottom = VolaLibrary.sectionGap),
    ) {
        LibraryRow(
            title = entry.name,
            detail = stringResource(downloadStatusLabel(entry.status), progressLabel),
            onClick = {},
            enabled = false,
            leading = { DownloadTile(kind = LibraryFileRules.kind(entry.name, entry.mime)) },
            trailing = {
                if (entry.supportsPause) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag(DownloadsScreenTestTags.pause(entry.id)),
                    ) {
                        Icon(
                            if (paused) VolaIcons.PlayArrow else VolaIcons.Pause,
                            contentDescription = stringResource(
                                if (paused) R.string.downloads_resume else R.string.downloads_pause,
                            ),
                        )
                    }
                }
                if (entry.supportsCancel) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag(DownloadsScreenTestTags.cancel(entry.id)),
                    ) {
                        Icon(VolaIcons.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            },
        )
        val indicatorModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaLibrary.sidePadding)
            .testTag(DownloadsScreenTestTags.progress(entry.id))
        if (progress == null) {
            LinearWavyProgressIndicator(modifier = indicatorModifier)
        } else {
            LinearWavyProgressIndicator(progress = { progress }, modifier = indicatorModifier)
        }
    }
}

@Composable
private fun DownloadRow(
    entry: DownloadEntry,
    size: String,
    time: String,
    onOpen: () -> Unit,
) {
    val kind = LibraryFileRules.kind(entry.name, entry.mime)
    val failed = entry.status == DownloadStatus.Failed || entry.status == DownloadStatus.Cancelled
    val detail = if (failed) {
        stringResource(downloadStatusLabel(entry.status), time)
    } else {
        listOfNotNull(downloadKindLabel(kind), size, entry.sourceHost() ?: time).joinToString(" · ")
    }
    LibraryRow(
        title = entry.name,
        detail = detail,
        detailColor = if (failed) MaterialTheme.colorScheme.error else null,
        enabled = entry.status == DownloadStatus.Successful,
        onClick = onOpen,
        modifier = Modifier.testTag(DownloadsScreenTestTags.download(entry.id)),
        leading = { DownloadTile(kind = kind, failed = failed) },
    )
}

/** A file's tile: its kind's symbol on a soft color; a failed one turns to the error tint. */
@Composable
private fun DownloadTile(kind: DownloadKind, failed: Boolean = false) {
    val tile = VolaLibrary.tile(kind.ordinal)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(VolaLibrary.tileSize)
            .clip(VolaLibrary.tileShape)
            .background(if (failed) colors.errorContainer else tile.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (kind) {
                DownloadKind.Pdf -> VolaIcons.PictureAsPdf
                DownloadKind.Document -> VolaIcons.Description
                DownloadKind.Spreadsheet -> VolaIcons.TableChart
                DownloadKind.Image -> VolaIcons.Image
                DownloadKind.Video -> VolaIcons.Movie
                DownloadKind.Audio -> VolaIcons.Headphones
                DownloadKind.Archive -> VolaIcons.Inventory2
                DownloadKind.App -> VolaIcons.Apps
                DownloadKind.Other -> VolaIcons.Download
            },
            contentDescription = null,
            modifier = Modifier.size(VolaLibrary.tileIconSize),
            tint = if (failed) colors.onErrorContainer else tile.content,
        )
    }
}

/** Board W-States: no downloads yet, or a search or filter that found nothing. */
@Composable
private fun DownloadsEmptyState(searching: Boolean, modifier: Modifier = Modifier) {
    VolaStateMessage(
        icon = rememberVectorPainter(VolaIcons.Download),
        title = stringResource(if (searching) R.string.downloads_no_results else R.string.downloads_empty),
        message = stringResource(
            if (searching) R.string.downloads_no_results_message else R.string.downloads_empty_message,
        ),
        modifier = modifier,
        tone = if (searching) VolaStateTone.Neutral else VolaStateTone.Empty,
    )
}

private fun DownloadEntry.sourceHost(): String? = source.takeIf(String::isNotBlank)
    ?.let(Uri::parse)
    ?.host
    ?.takeIf(String::isNotBlank)

@Composable
private fun downloadTimeFilterLabel(filter: DownloadTimeFilter): String = stringResource(
    when (filter) {
        DownloadTimeFilter.All -> R.string.downloads_time_all
        DownloadTimeFilter.Today -> R.string.downloads_time_today
        DownloadTimeFilter.Last7Days -> R.string.downloads_time_seven_days
        DownloadTimeFilter.Last30Days -> R.string.downloads_time_thirty_days
    },
)

@Composable
private fun downloadKindFilterLabel(filter: DownloadKindFilter): String = stringResource(
    when (filter) {
        DownloadKindFilter.All -> R.string.downloads_kind_all
        DownloadKindFilter.Documents -> R.string.downloads_kind_documents
        DownloadKindFilter.Images -> R.string.downloads_kind_images
        DownloadKindFilter.Videos -> R.string.downloads_kind_videos
    },
)

@Composable
private fun downloadKindLabel(kind: DownloadKind): String? = when (kind) {
    DownloadKind.Pdf -> "PDF"
    DownloadKind.Document -> stringResource(R.string.downloads_kind_document)
    DownloadKind.Spreadsheet -> stringResource(R.string.downloads_kind_spreadsheet)
    DownloadKind.Image -> stringResource(R.string.downloads_kind_image)
    DownloadKind.Video -> stringResource(R.string.downloads_kind_video)
    DownloadKind.Audio -> stringResource(R.string.downloads_kind_audio)
    DownloadKind.Archive -> stringResource(R.string.downloads_kind_archive)
    DownloadKind.App -> stringResource(R.string.downloads_kind_app)
    DownloadKind.Other -> null
}

private fun downloadStatusLabel(status: DownloadStatus): Int = when (status) {
    DownloadStatus.Pending -> R.string.downloads_status_pending
    DownloadStatus.Running -> R.string.downloads_status_running
    DownloadStatus.Paused -> R.string.downloads_status_paused
    DownloadStatus.Successful -> R.string.downloads_status_successful
    DownloadStatus.Failed -> R.string.downloads_status_failed
    DownloadStatus.Cancelled -> R.string.downloads_status_cancelled
}

internal object DownloadsScreenTestTags {
    const val List = "downloads_list"
    const val SearchField = "downloads_search_field"
    const val Clear = "downloads_clear"
    const val More = "downloads_more"
    const val ClearDialog = "downloads_clear_dialog"
    const val ClearConfirm = "downloads_clear_confirm"

    fun timeFilter(filter: DownloadTimeFilter): String = "downloads_time_filter:${filter.name}"

    fun kindFilter(filter: DownloadKindFilter): String = "downloads_kind_filter:${filter.name}"

    fun download(id: Long): String = "downloads_entry:$id"

    fun progress(id: Long): String = "downloads_progress:$id"

    fun cancel(id: Long): String = "downloads_cancel:$id"

    fun pause(id: Long): String = "downloads_pause:$id"
}
