package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.BrowsingHistoryRules
import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.data.HistoryEntry
import dev.sk2andy.materialbrowser.data.HistoryRecallRules
import dev.sk2andy.materialbrowser.recall.RecallMatch
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5Library
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryActions
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryLeading
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryRow
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySection
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryState
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The history segment of the v5 library (V28, B2b; canvas screen «р1»): the current profile's
 * visits by day, newest first, a search that also finds pages by their text («Page memory»), a row
 * swiped left deleted at once, and «Clear…» asking for one of four periods.
 */
@Composable
internal fun HistoryLibraryV5(
    profileId: String,
    history: List<HistoryEntry>,
    recallMatches: List<RecallMatch>,
    onRecallQuery: (String, Set<String>) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    onClear: (HistoryClearRequest) -> Unit,
    onOpen: (HistoryEntry) -> Unit,
    onSwitch: (V5LibrarySegment) -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var clearing by rememberSaveable { mutableStateOf(false) }
    val profiles = remember(profileId) { setOf(profileId) }
    LaunchedEffect(query, profiles) { onRecallQuery(query, profiles) }
    val locale = LocalConfiguration.current.locales[0]
    // «Today» and «Yesterday» follow the device's day and time zone on an open screen.
    val today = rememberLocalToday()
    val zoneId = remember(today) { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val snapshot = remember(history, profiles, query, recallMatches) {
        HistoryRecallRules.merge(history, profiles, query, recallMatches)
    }
    val byKey = remember(snapshot) { snapshot.entries.associateBy(BrowsingHistoryRules::entryKey) }
    val todayLabel = stringResource(R.string.history_today)
    val yesterdayLabel = stringResource(R.string.history_yesterday)
    val sections = remember(snapshot, zoneId, today, locale) {
        BrowsingHistoryRules.sections(snapshot.entries, zoneId).map { section ->
            V5LibrarySection(
                title = when (section.date) {
                    today -> todayLabel
                    today.minusDays(1) -> yesterdayLabel
                    else -> dateFormatter.format(section.date)
                },
                rows = section.entries.map { entry ->
                    val host = hostOf(entry.url)
                    val key = BrowsingHistoryRules.entryKey(entry)
                    V5LibraryRow(
                        key = key,
                        title = entry.title.ifBlank { host },
                        // A page found by its text shows the words that matched instead of the address.
                        subtitle = snapshot.excerptsByEntryKey[key] ?: host,
                        leading = V5LibraryLeading.Site(
                            HistoryLibraryV5Rules.siteLetter(host),
                            HistoryLibraryV5Rules.siteTile(host),
                        ),
                        trailing = V5LibraryTrailing.Value(
                            timeFormatter.format(Instant.ofEpochMilli(entry.lastVisitedAt).atZone(zoneId)),
                        ),
                    )
                },
            )
        }
    }
    val state = V5LibraryState(
        segment = V5LibrarySegment.History,
        query = query,
        sections = sections,
        emptyMessage = stringResource(if (query.isBlank()) R.string.history_empty_message else R.string.history_no_results),
        actions = V5LibraryActions(
            end = stringResource(R.string.v5_history_clear).takeIf { history.any { it.profileId == profileId } },
            onEnd = { clearing = true },
            endDestructive = true,
        ),
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < HISTORY_DARK_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5Library(
            state = state,
            onSegmentChange = { segment -> if (segment != V5LibrarySegment.History) onSwitch(segment) },
            onQueryChange = { query = it },
            onOpen = { key -> byKey[key]?.let(onOpen) },
            onDelete = { key -> byKey[key]?.let(onDelete) },
            onRetry = {},
            onClose = onClose,
        )
        ClearHistorySheet(
            visible = clearing,
            onDismiss = { clearing = false },
            onClear = { range ->
                clearing = false
                onClear(HistoryLibraryV5Rules.clearRequest(range, profileId, System.currentTimeMillis(), zoneId))
            },
        )
    }
}

/** «Clear history»: four periods of the current profile, each one tap; the rows are destructive. */
@Composable
private fun ClearHistorySheet(visible: Boolean, onDismiss: () -> Unit, onClear: (V5HistoryClearRange) -> Unit) {
    val title = stringResource(R.string.v5_history_clear_title)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.v5_history_clear_subtitle))
        V5ListGroup {
            V5HistoryClearRange.entries.forEach { range ->
                V5ListRow(
                    title = stringResource(
                        when (range) {
                            V5HistoryClearRange.LastHour -> R.string.v5_history_clear_hour
                            V5HistoryClearRange.Today -> R.string.v5_history_clear_today
                            V5HistoryClearRange.TodayAndYesterday -> R.string.v5_history_clear_two_days
                            V5HistoryClearRange.AllTime -> R.string.v5_history_clear_all
                        },
                    ),
                    titleColor = V5Theme.colors.danger,
                    onClick = { onClear(range) },
                )
            }
        }
    }
}

private fun hostOf(url: String): String =
    runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()?.takeIf(String::isNotBlank) ?: url

private const val HISTORY_DARK_LUMINANCE = 0.5f
