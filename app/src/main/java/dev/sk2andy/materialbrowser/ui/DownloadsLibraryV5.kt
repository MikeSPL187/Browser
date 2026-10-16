package dev.sk2andy.materialbrowser.ui

import android.text.format.Formatter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadHistoryRules
import dev.sk2andy.materialbrowser.data.DownloadRetry
import dev.sk2andy.materialbrowser.data.DownloadStatus
import dev.sk2andy.materialbrowser.data.DownloadTimeFilter
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5Library
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryActions
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryLeading
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryRow
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySection
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryState
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The downloads segment of the v5 library (V28, B2d; canvas screen «р3»): downloads under way on
 * top with their progress (a tap pauses or resumes, a swipe cancels), then finished files by day.
 * A tap opens a file; a failed one starts again, or opens the page it came from. «Clear…» deletes
 * the finished files only after a confirmation: they are the files themselves, not a list.
 */
@Composable
internal fun DownloadsLibraryV5(
    downloads: List<DownloadEntry>,
    isClearing: Boolean,
    onOpen: (DownloadEntry) -> Unit,
    onTogglePause: (DownloadEntry) -> Unit,
    onCancel: (DownloadEntry) -> Unit,
    onRetry: (DownloadEntry) -> Unit,
    onOpenPage: (String) -> Unit,
    onClearFinished: (List<DownloadEntry>) -> Unit,
    onSwitch: (V5LibrarySegment) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    var clearing by rememberSaveable { mutableStateOf(false) }
    val today = rememberLocalToday()
    val zoneId = remember(today) { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val visible = remember(downloads, query, zoneId) {
        DownloadHistoryRules.visibleEntries(downloads, query, DownloadTimeFilter.All, System.currentTimeMillis(), zoneId)
    }
    val byKey = remember(visible) { visible.associateBy { entry -> entry.id.toString() } }
    val clearable = remember(downloads) { downloads.filter { entry -> entry.status.isTerminal } }

    @Composable
    fun row(entry: DownloadEntry): V5LibraryRow {
        val kind = LibraryFileRules.kind(entry.name, entry.mime)
        val failed = DownloadsLibraryV5Rules.failed(entry)
        val time = timeFormatter.format(Instant.ofEpochMilli(entry.lastModified).atZone(zoneId))
        val size = Formatter.formatShortFileSize(context, maxOf(entry.bytes, entry.total))
        return V5LibraryRow(
            key = entry.id.toString(),
            title = entry.name,
            subtitle = when {
                entry.status.isActive -> stringResource(downloadStatusLabel(entry.status), downloadProgressLabel(context, entry))
                failed -> stringResource(downloadStatusLabel(entry.status), time)
                else -> listOfNotNull(size, entry.sourceHost()).joinToString(" · ")
            },
            leading = V5LibraryLeading.File(kind.icon(), DownloadsLibraryV5Rules.tile(kind, failed)),
            trailing = DownloadsLibraryV5Rules.trailing(entry, time),
            deletable = DownloadsLibraryV5Rules.swipeCancels(entry),
        )
    }

    val todayLabel = stringResource(R.string.history_today)
    val yesterdayLabel = stringResource(R.string.downloads_yesterday)
    val active = visible.filter { entry -> entry.status.isActive }
    val sections = buildList {
        if (active.isNotEmpty()) add(V5LibrarySection(title = null, rows = active.map { row(it) }))
        LibraryFileRules.daySections(visible, zoneId).forEach { section ->
            add(
                V5LibrarySection(
                    title = when (section.date) {
                        today -> todayLabel
                        today.minusDays(1) -> yesterdayLabel
                        else -> dateFormatter.format(section.date)
                    },
                    rows = section.entries.map { row(it) },
                ),
            )
        }
    }
    val state = V5LibraryState(
        segment = V5LibrarySegment.Downloads,
        query = query,
        sections = sections,
        emptyMessage = stringResource(if (query.isBlank()) R.string.downloads_empty_message else R.string.downloads_no_results),
        actions = V5LibraryActions(
            end = stringResource(R.string.v5_history_clear).takeIf { clearable.isNotEmpty() && !isClearing },
            onEnd = { clearing = true },
            endDestructive = true,
        ),
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < DOWNLOADS_DARK_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5Library(
            state = state,
            onSegmentChange = { segment -> if (segment != V5LibrarySegment.Downloads) onSwitch(segment) },
            onQueryChange = { query = it },
            onOpen = { key ->
                val entry = byKey[key] ?: return@V5Library
                val retry = entry.retry
                when {
                    entry.status.isActive -> if (entry.supportsPause) onTogglePause(entry)
                    entry.status == DownloadStatus.Successful -> onOpen(entry)
                    retry == DownloadRetry.Available -> onRetry(entry)
                    retry is DownloadRetry.Unavailable -> retry.pageUrl?.let(onOpenPage)
                }
            },
            onDelete = { key -> byKey[key]?.let(onCancel) },
            onRetry = { key -> byKey[key]?.let(onRetry) },
            onClose = onClose,
        )
        ClearDownloadsSheet(
            visible = clearing,
            onDismiss = { clearing = false },
            onClear = {
                clearing = false
                onClearFinished(clearable)
            },
        )
    }
}

/** «Clear…»: says plainly that the files themselves go, then one destructive row. */
@Composable
private fun ClearDownloadsSheet(visible: Boolean, onDismiss: () -> Unit, onClear: () -> Unit) {
    val title = stringResource(R.string.downloads_clear_title)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.downloads_clear_message))
        V5ListGroup {
            V5ListRow(
                title = stringResource(R.string.downloads_clear),
                titleColor = V5Theme.colors.danger,
                onClick = onClear,
            )
        }
    }
}

private fun DownloadKind.icon(): ImageVector = when (this) {
    DownloadKind.Pdf -> VolaIcons.PictureAsPdf
    DownloadKind.Document -> VolaIcons.Description
    DownloadKind.Spreadsheet -> VolaIcons.TableChart
    DownloadKind.Image -> VolaIcons.Image
    DownloadKind.Video -> VolaIcons.Movie
    DownloadKind.Audio -> VolaIcons.Headphones
    DownloadKind.Archive -> VolaIcons.Inventory2
    DownloadKind.App -> VolaIcons.Apps
    DownloadKind.Other -> VolaIcons.Download
}

private const val DOWNLOADS_DARK_LUMINANCE = 0.5f
