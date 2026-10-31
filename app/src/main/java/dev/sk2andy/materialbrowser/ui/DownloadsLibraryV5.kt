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
import dev.sk2andy.materialbrowser.ui.v5.V5FilterChips
import dev.sk2andy.materialbrowser.ui.v5.V5Library
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryActions
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryLeading
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryRow
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySection
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryState
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The downloads segment of the v5 library (V28, B2d; canvas screen «р3»): downloads under way on
 * top with their progress (the ring or a tap pauses or resumes, a swipe cancels), then finished
 * files by day. A tap opens a file; a failed one starts again, or opens the page it came from, or
 * its row says why neither can be done. A
 * swipe deletes one file, and «Clear…» all finished ones; each asks first, since what goes is the
 * files themselves, not a list. While searching or filtering «Clear…» hides: it never clears what is
 * not shown. Chips filter by kind (canvas «Downloads»); a note under the list says where files are kept.
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
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(V5DownloadFilter.All) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    // The download a swipe asks about, by key, so the question survives a rotation.
    var asking by rememberSaveable { mutableStateOf<String?>(null) }
    var askingCancels by rememberSaveable { mutableStateOf(false) }
    val today = rememberLocalToday()
    val zoneId = remember(today) { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val visible = remember(downloads, query, filter, zoneId) {
        DownloadHistoryRules.visibleEntries(downloads, query, DownloadTimeFilter.All, System.currentTimeMillis(), zoneId)
            .filter { entry -> filter.matches(LibraryFileRules.kind(entry.name, entry.mime)) }
    }
    val byKey = remember(visible) { visible.associateBy { entry -> entry.id.toString() } }
    val clearable = remember(downloads) { downloads.filter { entry -> entry.status.isTerminal } }

    val pauseLabel = stringResource(R.string.downloads_pause)
    val resumeLabel = stringResource(R.string.downloads_resume)
    val retryLabel = stringResource(R.string.v5_library_retry)
    val cancelLabel = stringResource(R.string.v5_downloads_cancel_action)
    val openPageLabel = stringResource(R.string.v5_downloads_open_page)

    @Composable
    fun row(entry: DownloadEntry): V5LibraryRow {
        val kind = LibraryFileRules.kind(entry.name, entry.mime)
        val failed = DownloadsLibraryV5Rules.failed(entry)
        val unavailable = DownloadsLibraryV5Rules.unavailable(entry)
        val time = timeFormatter.format(Instant.ofEpochMilli(entry.lastModified).atZone(zoneId))
        val size = Formatter.formatShortFileSize(context, maxOf(entry.bytes, entry.total))
        return V5LibraryRow(
            key = entry.id.toString(),
            title = entry.name,
            subtitle = when {
                entry.status.isActive -> stringResource(downloadStatusLabel(entry.status), downloadProgressLabel(context, entry))
                unavailable != null -> stringResource(unavailable.label())
                failed -> stringResource(downloadStatusLabel(entry.status), time)
                else -> listOfNotNull(size, entry.sourceHost()).joinToString(" · ")
            },
            leading = V5LibraryLeading.File(kind.icon(), DownloadsLibraryV5Rules.tile(kind, failed)),
            trailing = DownloadsLibraryV5Rules.trailing(entry, time),
            // While «Clear…» runs a finished file cannot go on its own: the store takes one clearing at a time.
            deletable = DownloadsLibraryV5Rules.swipes(entry) && (entry.status.isActive || !isClearing),
            openLabel = when {
                entry.status.isActive -> when {
                    !entry.supportsPause -> null
                    entry.status == DownloadStatus.Paused -> resumeLabel
                    else -> pauseLabel
                }
                failed && entry.retry == DownloadRetry.Available -> retryLabel
                unavailable == DownloadUnavailableHint.OpenPage -> openPageLabel
                else -> null
            },
            deleteLabel = cancelLabel.takeIf { DownloadsLibraryV5Rules.swipeCancels(entry) },
            deleteAsks = true,
        )
    }

    val todayLabel = stringResource(R.string.history_today)
    val nowLabel = stringResource(R.string.v5_downloads_now)
    val yesterdayLabel = stringResource(R.string.downloads_yesterday)
    val active = visible.filter { entry -> entry.status.isActive }
    val sections = buildList {
        if (active.isNotEmpty()) add(V5LibrarySection(title = nowLabel, rows = active.map { row(it) }))
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
        emptyMessage = stringResource(
            when {
                query.isNotBlank() -> R.string.downloads_no_results
                filter != V5DownloadFilter.All -> R.string.v5_downloads_no_kind
                else -> R.string.downloads_empty_message
            },
        ),
        emptyTitle = stringResource(R.string.downloads_empty).takeIf { query.isBlank() && filter == V5DownloadFilter.All },
        // Where the files are kept, under the list: other apps see that folder too.
        note = stringResource(R.string.v5_downloads_folder_note).takeIf { visible.isNotEmpty() },
        actions = V5LibraryActions(
            end = stringResource(R.string.v5_history_clear).takeIf {
                clearable.isNotEmpty() && !isClearing && query.isBlank() && filter == V5DownloadFilter.All
            },
            onEnd = { clearing = true },
            endDestructive = true,
        ),
    )
    val filterLabels = V5DownloadFilter.entries.map { kind -> stringResource(kind.label) }
    val dark = MaterialTheme.colorScheme.background.luminance() < DOWNLOADS_DARK_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5Library(
            state = state,
            // Downloads are a screen of their own (canvas «Downloads»): no segment to change to.
            onSegmentChange = {},
            onQueryChange = { query = it },
            onOpen = { key ->
                val entry = byKey[key] ?: return@V5Library
                val retry = entry.retry
                when {
                    entry.status.isActive -> if (entry.supportsPause) onTogglePause(entry)
                    entry.status == DownloadStatus.Successful -> onOpen(entry)
                    retry == DownloadRetry.Available -> onRetry(entry)
                    // Only a web page; the row says why when there is none (DL-03).
                    retry is DownloadRetry.Unavailable -> DownloadRingRules.page(entry)?.let(onOpenPage)
                }
            },
            onDelete = { key ->
                byKey[key]?.let { entry ->
                    askingCancels = DownloadsLibraryV5Rules.swipeCancels(entry)
                    asking = key
                }
            },
            onRetry = { key -> byKey[key]?.let(onRetry) },
            onClose = onClose,
            filters = filterLabels.takeIf { downloads.isNotEmpty() }?.let { labels ->
                @Composable {
                    V5FilterChips(
                        options = labels,
                        selectedIndex = filter.ordinal,
                        onSelect = { index -> filter = V5DownloadFilter.entries[index] },
                    )
                }
            },
        )
        // The entry as it is now. One that finished or went while «Cancel?» was open closes the
        // question rather than turn it into «Delete?» under a tap meant to cancel.
        val asked = asking
            ?.let { key -> downloads.firstOrNull { entry -> entry.id.toString() == key } }
            ?.takeIf { entry -> DownloadsLibraryV5Rules.swipeCancels(entry) == askingCancels }
        DownloadSwipePrompt(
            entry = asked,
            onConfirm = { entry ->
                asking = null
                if (DownloadsLibraryV5Rules.swipeCancels(entry)) onCancel(entry) else onClearFinished(listOf(entry))
            },
            onDismiss = { asking = null },
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

/** The row's line for a failed download that cannot start again. */
private fun DownloadUnavailableHint.label(): Int = when (this) {
    DownloadUnavailableHint.OpenPage -> R.string.v5_downloads_unavailable_open_page
    DownloadUnavailableHint.PrivateClosed -> R.string.v5_downloads_unavailable_private
    DownloadUnavailableHint.FromSite -> R.string.v5_downloads_unavailable_site
}

/** «Clear…»: says plainly that the files themselves go, then one destructive row. */
@Composable
private fun ClearDownloadsSheet(visible: Boolean, onDismiss: () -> Unit, onClear: () -> Unit) {
    val title = stringResource(R.string.downloads_clear_title)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.v5_downloads_clear_message))
        V5ListGroup {
            V5ListRow(
                title = stringResource(R.string.downloads_clear),
                titleColor = V5Theme.colors.danger,
                onClick = onClear,
            )
        }
    }
}

/**
 * The question after a swipe (DL-01, DL-02): cancelling a download under way loses what it has
 * fetched, deleting a finished one removes the file from the phone. Neither has an undo, so the
 * row springs back and nothing happens until the red action.
 */
@Composable
private fun DownloadSwipePrompt(entry: DownloadEntry?, onConfirm: (DownloadEntry) -> Unit, onDismiss: () -> Unit) {
    // The question keeps its words while the sheet lowers.
    var shown by remember { mutableStateOf<DownloadEntry?>(null) }
    if (entry != null) shown = entry
    val current = shown ?: return
    val cancels = DownloadsLibraryV5Rules.swipeCancels(current)
    V5PromptSheet(
        visible = entry != null,
        title = stringResource(if (cancels) R.string.v5_downloads_cancel_title else R.string.v5_downloads_delete_title, current.name),
        message = stringResource(
            when {
                cancels -> R.string.v5_downloads_cancel_message
                current.status == DownloadStatus.Successful -> R.string.v5_downloads_delete_message
                else -> R.string.v5_downloads_delete_failed_message
            },
        ),
        actions = listOf(
            V5PromptAction(
                label = stringResource(if (cancels) R.string.v5_downloads_cancel_action else R.string.v5_library_delete),
                onClick = { onConfirm(current) },
                role = V5PromptRole.Destructive,
                testTag = DownloadsLibraryV5TestTags.Confirm,
            ),
        ),
        onDismiss = onDismiss,
    )
}

internal object DownloadsLibraryV5TestTags {
    const val Confirm = "v5_downloads_confirm"
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

private val V5DownloadFilter.label: Int
    get() = when (this) {
        V5DownloadFilter.All -> R.string.v5_downloads_filter_all
        V5DownloadFilter.Documents -> R.string.v5_downloads_filter_documents
        V5DownloadFilter.Images -> R.string.v5_downloads_filter_images
        V5DownloadFilter.Archives -> R.string.v5_downloads_filter_archives
    }
