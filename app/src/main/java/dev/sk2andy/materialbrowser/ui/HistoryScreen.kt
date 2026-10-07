package dev.sk2andy.materialbrowser.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.data.BrowsingHistoryRules
import dev.sk2andy.materialbrowser.data.HistoryClearRequest
import dev.sk2andy.materialbrowser.data.HistoryEntry
import dev.sk2andy.materialbrowser.data.HistoryRecallRules
import dev.sk2andy.materialbrowser.recall.RecallMatch
import dev.sk2andy.materialbrowser.recall.RecallRules
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HistoryScreen(
    profiles: List<BrowserProfile>,
    activeProfileId: String,
    history: List<HistoryEntry>,
    recallMatches: List<RecallMatch> = emptyList(),
    onRecallCriteriaChanged: (String, Set<String>) -> Unit = { _, _ -> },
    onDeleteEntries: (List<HistoryEntry>) -> Unit,
    onClearHistory: (HistoryClearRequest) -> Unit,
    onOpenEntry: (HistoryEntry) -> Unit,
    onBack: () -> Unit,
    onOpenNewTab: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    // «Today»/«Yesterday» and the zone follow the device day and time zone on an open screen.
    val today = rememberLocalToday()
    val zoneId = remember(today) { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    val profileIds = remember(profiles) { profiles.map(BrowserProfile::id) }
    var selectedProfileIds by rememberSaveable(profiles) {
        mutableStateOf(arrayListOf(activeProfileId))
    }
    var selectedEntryKeys by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var distinctEntries by rememberSaveable { mutableStateOf(false) }
    var clearConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    // Deleted history hides at once and waits behind «Undo» (board W-History).
    var pending by remember { mutableStateOf<HistoryPendingDeletion?>(null) }
    val currentDelete by rememberUpdatedState(onDeleteEntries)
    val currentClear by rememberUpdatedState(onClearHistory)
    fun commit(deletion: HistoryPendingDeletion) {
        when (deletion) {
            is HistoryPendingDeletion.Entries -> currentDelete(deletion.entries)
            is HistoryPendingDeletion.Clear -> currentClear(deletion.request)
        }
    }
    fun defer(deletion: HistoryPendingDeletion) {
        pending?.let(::commit)
        pending = deletion
        selectedEntryKeys = arrayListOf()
    }
    DisposableEffect(Unit) {
        onDispose { pending?.let(::commit) }
    }
    val selectedProfiles = selectedProfileIds.toSet()
    LaunchedEffect(query, selectedProfiles) {
        onRecallCriteriaChanged(query, selectedProfiles)
    }
    val recallSnapshot = remember(history, selectedProfileIds, query, recallMatches) {
        HistoryRecallRules.merge(history, selectedProfiles, query, recallMatches)
    }
    val visibleEntries = remember(recallSnapshot.entries, distinctEntries, pending) {
        val entries = if (distinctEntries) {
            BrowsingHistoryRules.distinctEntries(recallSnapshot.entries)
        } else {
            recallSnapshot.entries
        }
        LibraryRules.withoutPending(entries, pending, BrowsingHistoryRules::entryKey)
    }
    val clearableHistory = remember(history, recallMatches) {
        (history + recallMatches.map { match ->
            HistoryEntry(
                url = match.url,
                title = match.title,
                lastVisitedAt = match.visitedAt,
                profileId = match.profileId,
            )
        }).distinctBy(BrowsingHistoryRules::entryKey)
    }
    val sections = remember(visibleEntries, zoneId) {
        BrowsingHistoryRules.sections(visibleEntries, zoneId)
    }
    val selectedEntries = remember(visibleEntries, selectedEntryKeys) {
        val keys = selectedEntryKeys.toSet()
        visibleEntries.filter { entry -> BrowsingHistoryRules.entryKey(entry) in keys }
    }
    val selecting = selectedEntries.isNotEmpty()
    val profilesById = remember(profiles) { profiles.associateBy(BrowserProfile::id) }

    val snackbarHostState = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.history_cleared)
    val deletedCount = (pending as? HistoryPendingDeletion.Entries)?.entries?.size ?: 0
    val deletedMessage = pluralStringResource(
        R.plurals.history_deleted_count,
        deletedCount,
        deletedCount,
    )
    val undoLabel = stringResource(R.string.action_undo)
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(pending) {
        val deletion = pending ?: return@LaunchedEffect
        val message = if (deletion is HistoryPendingDeletion.Clear) clearedMessage else deletedMessage
        // TalkBack users and longer «Time to take action» settings get the window Android recommends.
        val undoWindow = accessibilityManager?.calculateRecommendedTimeoutMillis(
            LibraryRules.UNDO_WINDOW_MILLIS,
            containsIcons = false,
            containsText = true,
            containsControls = true,
        ) ?: LibraryRules.UNDO_WINDOW_MILLIS
        val result = withTimeoutOrNull(undoWindow) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Indefinite,
            )
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        if (result != SnackbarResult.ActionPerformed) commit(deletion)
        pending = null
    }

    fun handleBack() {
        when {
            selecting -> selectedEntryKeys = arrayListOf()
            else -> onBack()
        }
    }
    BackHandler(onBack = ::handleBack)

    fun toggle(entryKey: String, selected: Boolean) {
        val updated = ArrayList(selectedEntryKeys)
        if (selected) updated.add(entryKey) else updated.remove(entryKey)
        selectedEntryKeys = updated
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (!selecting) {
                            stringResource(R.string.history_title)
                        } else {
                            stringResource(R.string.history_selected_count, selectedEntries.size)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            VolaIcons.ArrowBack,
                            contentDescription = stringResource(R.string.history_back),
                        )
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(
                            onClick = {
                                defer(
                                    HistoryPendingDeletion.Entries(
                                        entries = selectedEntries,
                                        keys = selectedEntries
                                            .map(BrowsingHistoryRules::entryKey)
                                            .toSet(),
                                    ),
                                )
                            },
                            modifier = Modifier.testTag(HistoryScreenTestTags.DeleteSelected),
                        ) {
                            Icon(
                                VolaIcons.Delete,
                                contentDescription = stringResource(R.string.history_delete_selected),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { clearConfirmationVisible = true },
                            enabled = clearableHistory.any { entry ->
                                entry.profileId in selectedProfiles
                            },
                            modifier = Modifier.testTag(HistoryScreenTestTags.Clear),
                        ) {
                            Icon(
                                VolaIcons.DeleteSweep,
                                contentDescription = stringResource(R.string.history_clear),
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuOpen = true },
                                modifier = Modifier.testTag(HistoryScreenTestTags.More),
                            ) {
                                Icon(
                                    VolaIcons.MoreVert,
                                    contentDescription = stringResource(R.string.history_more),
                                )
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                // A switch: the menu stays open to show its new state.
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.history_distinct)) },
                                    trailingIcon = if (distinctEntries) {
                                        { Icon(VolaIcons.Check, contentDescription = null) }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        distinctEntries = !distinctEntries
                                        selectedEntryKeys = arrayListOf()
                                    },
                                    modifier = Modifier
                                        .semantics { selected = distinctEntries }
                                        .testTag(HistoryScreenTestTags.Distinct),
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .navigationBarsPadding()
                .testTag(HistoryScreenTestTags.List),
            verticalArrangement = Arrangement.Top,
        ) {
            item(key = "search") {
                LibrarySearchBar(
                    query = query,
                    placeholder = stringResource(R.string.history_search),
                    clearContentDescription = stringResource(R.string.history_clear_search),
                    testTag = HistoryScreenTestTags.SearchField,
                    onQueryChange = {
                        query = it.take(RecallRules.MAX_QUERY_CHARS)
                    },
                )
            }

            if (profiles.size > 1) {
                item(key = "profiles") {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap),
                        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
                    ) {
                        val all = selectedProfileIds.size == profileIds.size
                        FilterChip(
                            selected = all,
                            onClick = {
                                selectedProfileIds = ArrayList(profileIds)
                                selectedEntryKeys = arrayListOf()
                            },
                            label = { Text(stringResource(R.string.history_all_profiles)) },
                            leadingIcon = if (all) {
                                { Icon(VolaIcons.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            modifier = Modifier.testTag(HistoryScreenTestTags.AllProfiles),
                        )
                        profiles.forEach { profile ->
                            val selected = profile.id in selectedProfileIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val updated = ArrayList(selectedProfileIds)
                                    if (selected) {
                                        if (updated.size > 1) updated.remove(profile.id)
                                    } else {
                                        updated.add(profile.id)
                                    }
                                    selectedProfileIds = updated
                                    selectedEntryKeys = arrayListOf()
                                },
                                label = { Text(profile.workspaceDisplayName()) },
                                leadingIcon = {
                                    WorkspaceGem(workspace = profile, size = VolaLibrary.gemSize)
                                },
                                modifier = Modifier.testTag(
                                    HistoryScreenTestTags.profile(profile.id),
                                ),
                            )
                        }
                    }
                }
            }

            if (sections.isEmpty()) {
                item(key = "empty") {
                    HistoryEmptyState(
                        searching = query.isNotBlank(),
                        onOpenNewTab = onOpenNewTab,
                        modifier = Modifier
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap),
                    )
                }
            } else {
                sections.forEach { section ->
                    item(key = "day:${section.date}") {
                        LibrarySectionLabel(
                            text = when (section.date) {
                                today -> stringResource(R.string.history_today)
                                today.minusDays(1) -> stringResource(R.string.history_yesterday)
                                else -> dateFormatter.format(section.date)
                            },
                            modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                        )
                    }
                    itemsIndexed(
                        items = section.entries,
                        key = { _, entry -> BrowsingHistoryRules.entryKey(entry) },
                    ) { index, entry ->
                        val entryKey = BrowsingHistoryRules.entryKey(entry)
                        val selected = entryKey in selectedEntryKeys
                        LibraryCardSlice(
                            position = LibraryRules.position(index, section.entries.size),
                        ) {
                            HistoryEntryRow(
                                entry = entry,
                                time = timeFormatter.format(
                                    Instant.ofEpochMilli(entry.lastVisitedAt).atZone(zoneId),
                                ),
                                workspace = profilesById[entry.profileId]
                                    .takeIf { selectedProfiles.size > 1 },
                                excerpt = recallSnapshot.excerptsByEntryKey[entryKey],
                                selected = selected,
                                onSelectedChange = { toggle(entryKey, it) },
                                onOpen = {
                                    if (selecting) toggle(entryKey, !selected) else onOpenEntry(entry)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (clearConfirmationVisible) {
        HistoryClearDialog(
            profiles = profiles,
            initialProfileIds = selectedProfiles,
            history = clearableHistory,
            zoneId = zoneId,
            dateFormatter = dateFormatter,
            timeFormatter = timeFormatter,
            onDismiss = { clearConfirmationVisible = false },
            onConfirm = { request ->
                defer(HistoryPendingDeletion.Clear(request))
                clearConfirmationVisible = false
            },
        )
    }
}

internal enum class HistoryClearDateField { Since, Until }

internal data class HistoryClearDateTimeRange(
    val since: LocalDateTime,
    val until: LocalDateTime,
)

internal object HistoryClearDialogRules {
    fun updateMoment(
        range: HistoryClearDateTimeRange,
        field: HistoryClearDateField,
        selectedMoment: LocalDateTime,
        zoneId: ZoneId,
    ): HistoryClearDateTimeRange {
        val normalizedMoment = selectedMoment.atZone(zoneId).toLocalDateTime()
        return when (field) {
            HistoryClearDateField.Since -> HistoryClearDateTimeRange(
                since = normalizedMoment,
                until = maxOf(range.until, normalizedMoment),
            )
            HistoryClearDateField.Until -> HistoryClearDateTimeRange(
                since = minOf(range.since, normalizedMoment),
                until = normalizedMoment,
            )
        }
    }

    fun clearRequest(
        range: HistoryClearDateTimeRange,
        profileIds: Set<String>,
        zoneId: ZoneId,
    ): HistoryClearRequest {
        val normalizedSince = range.since.atZone(zoneId).withEarlierOffsetAtOverlap()
        val normalizedUntil = range.until.atZone(zoneId).withLaterOffsetAtOverlap()
        return HistoryClearRequest(
            profileIds = profileIds,
            sinceInclusiveMillis = normalizedSince.toInstant().toEpochMilli(),
            untilExclusiveMillis = normalizedUntil.plusMinutes(1).toInstant().toEpochMilli(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HistoryClearDialog(
    profiles: List<BrowserProfile>,
    initialProfileIds: Set<String>,
    history: List<HistoryEntry>,
    zoneId: ZoneId,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    onConfirm: (HistoryClearRequest) -> Unit,
) {
    val now = remember(zoneId) {
        LocalDateTime.now(zoneId).withSecond(0).withNano(0)
    }
    val historyMoments = remember(history, zoneId) {
        history.map { entry ->
            Instant.ofEpochMilli(entry.lastVisitedAt)
                .atZone(zoneId)
                .toLocalDateTime()
                .withSecond(0)
                .withNano(0)
        }
    }
    val availableProfileIds = remember(profiles) { profiles.mapTo(linkedSetOf(), BrowserProfile::id) }
    val initialSince = historyMoments.minOrNull() ?: now
    val initialUntil = historyMoments.maxOrNull() ?: now
    var selectedProfileIdList by rememberSaveable {
        mutableStateOf(ArrayList(initialProfileIds.intersect(availableProfileIds)))
    }
    var sinceEpochDay by rememberSaveable {
        mutableStateOf(initialSince.toLocalDate().toEpochDay())
    }
    var sinceMinuteOfDay by rememberSaveable {
        mutableStateOf(initialSince.toLocalTime().toSecondOfDay() / 60)
    }
    var untilEpochDay by rememberSaveable {
        mutableStateOf(initialUntil.toLocalDate().toEpochDay())
    }
    var untilMinuteOfDay by rememberSaveable {
        mutableStateOf(initialUntil.toLocalTime().toSecondOfDay() / 60)
    }
    var editedFieldName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedProfileIds = selectedProfileIdList.toSet()
    val since = LocalDate.ofEpochDay(sinceEpochDay)
        .atStartOfDay()
        .plusMinutes(sinceMinuteOfDay.toLong())
    val until = LocalDate.ofEpochDay(untilEpochDay)
        .atStartOfDay()
        .plusMinutes(untilMinuteOfDay.toLong())
    val editedField = editedFieldName?.let(HistoryClearDateField::valueOf)
    val request = remember(selectedProfileIds, since, until, zoneId) {
        HistoryClearDialogRules.clearRequest(
            range = HistoryClearDateTimeRange(since, until),
            profileIds = selectedProfileIds,
            zoneId = zoneId,
        )
    }
    val hasMatchingEntries = remember(history, request) {
        BrowsingHistoryRules.removeRange(history, request).size < history.size
    }

    editedField?.let { field ->
        HistoryBoundaryDateTimeDialogs(
            field = field,
            initialMoment = if (field == HistoryClearDateField.Since) since else until,
            onDismiss = { editedFieldName = null },
            onConfirm = { selectedMoment ->
                val range = HistoryClearDialogRules.updateMoment(
                    range = HistoryClearDateTimeRange(since, until),
                    field = field,
                    selectedMoment = selectedMoment,
                    zoneId = zoneId,
                )
                sinceEpochDay = range.since.toLocalDate().toEpochDay()
                sinceMinuteOfDay = range.since.toLocalTime().toSecondOfDay() / 60
                untilEpochDay = range.until.toLocalDate().toEpochDay()
                untilMinuteOfDay = range.until.toLocalTime().toSecondOfDay() / 60
                editedFieldName = null
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(HistoryScreenTestTags.ClearDialog),
        title = { Text(stringResource(R.string.history_clear_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.history_clear_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.history_clear_range),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryClearDateButton(
                        label = stringResource(R.string.history_clear_since),
                        date = dateFormatter.format(since),
                        time = timeFormatter.format(since),
                        onClick = { editedFieldName = HistoryClearDateField.Since.name },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(HistoryScreenTestTags.ClearSince),
                    )
                    HistoryClearDateButton(
                        label = stringResource(R.string.history_clear_until),
                        date = dateFormatter.format(until),
                        time = timeFormatter.format(until),
                        onClick = { editedFieldName = HistoryClearDateField.Until.name },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(HistoryScreenTestTags.ClearUntil),
                    )
                }
                Text(
                    stringResource(R.string.history_profiles),
                    style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (profiles.size > 1) {
                        FilterChip(
                            selected = selectedProfileIds.size == profiles.size,
                            onClick = { selectedProfileIdList = ArrayList(availableProfileIds) },
                            label = { Text(stringResource(R.string.history_all_profiles)) },
                            modifier = Modifier.testTag(HistoryScreenTestTags.ClearAllProfiles),
                        )
                    }
                    profiles.forEach { profile ->
                        val selected = profile.id in selectedProfileIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated = if (selected) {
                                    if (selectedProfileIds.size > 1) {
                                        selectedProfileIds - profile.id
                                    } else {
                                        selectedProfileIds
                                    }
                                } else {
                                    selectedProfileIds + profile.id
                                }
                                selectedProfileIdList = ArrayList(updated)
                            },
                            label = { Text(profile.workspaceDisplayName()) },
                                    leadingIcon = {
                                        PlatformProfileEmoji(emoji = profile.emoji, fontSize = 18.sp)
                                    },
                            modifier = Modifier.testTag(
                                HistoryScreenTestTags.clearProfile(profile.id),
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(request) },
                enabled = hasMatchingEntries,
                modifier = Modifier.testTag(HistoryScreenTestTags.ClearConfirm),
            ) {
                Text(stringResource(R.string.history_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun HistoryClearDateButton(
    label: String,
    date: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(date, style = MaterialTheme.typography.bodyMedium)
            Text(time, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private enum class HistoryClearDateTimeStep { Date, Time }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBoundaryDateTimeDialogs(
    field: HistoryClearDateField,
    initialMoment: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    val context = LocalContext.current
    var stepName by rememberSaveable(field) {
        mutableStateOf(HistoryClearDateTimeStep.Date.name)
    }
    var selectedDateEpochDay by rememberSaveable(field) {
        mutableStateOf(initialMoment.toLocalDate().toEpochDay())
    }
    when (HistoryClearDateTimeStep.valueOf(stepName)) {
        HistoryClearDateTimeStep.Date -> {
            val initialDateMillis = remember(initialMoment) {
                initialMoment.toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }
            val state = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMillis = state.selectedDateMillis ?: return@TextButton
                            selectedDateEpochDay = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                            stepName = HistoryClearDateTimeStep.Time.name
                        },
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            ) {
                DatePicker(
                    state = state,
                    modifier = Modifier.testTag(HistoryScreenTestTags.ClearDatePicker),
                    title = {
                        Text(
                            historyClearBoundaryLabel(field),
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        )
                    },
                )
            }
        }
        HistoryClearDateTimeStep.Time -> {
            val state = rememberTimePickerState(
                initialHour = initialMoment.hour,
                initialMinute = initialMoment.minute,
                is24Hour = DateFormat.is24HourFormat(context),
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(historyClearBoundaryLabel(field)) },
                text = {
                    TimePicker(
                        state = state,
                        modifier = Modifier.testTag(HistoryScreenTestTags.ClearTimePicker),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirm(
                                LocalDate.ofEpochDay(selectedDateEpochDay).atTime(
                                    LocalTime.of(state.hour, state.minute),
                                ),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun historyClearBoundaryLabel(field: HistoryClearDateField): String = stringResource(
    if (field == HistoryClearDateField.Since) {
        R.string.history_clear_since
    } else {
        R.string.history_clear_until
    },
)

@Composable
private fun HistoryEntryRow(
    entry: HistoryEntry,
    time: String,
    workspace: BrowserProfile?,
    excerpt: String?,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val host = BrowserUriPolicy.displayHttpHost(entry.url)
    val title = entry.title.ifBlank { host }
    val selectLabel = stringResource(R.string.history_select_entry)
    LibraryRow(
        title = title,
        detail = host,
        extra = excerpt,
        onClick = onOpen,
        onLongClick = { onSelectedChange(!selected) },
        onLongClickLabel = selectLabel,
        modifier = Modifier
            .semantics { this.selected = selected }
            .testTag(HistoryScreenTestTags.entry(entry)),
        leading = {
            LibrarySiteTile(
                label = title,
                colorKey = host,
                selected = selected,
                modifier = Modifier
                    .clickable(
                        role = Role.Checkbox,
                        onClickLabel = selectLabel,
                        onClick = { onSelectedChange(!selected) },
                    )
                    .testTag(HistoryScreenTestTags.select(entry)),
            )
        },
        trailing = {
            Text(
                time,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            workspace?.let { profile ->
                WorkspaceGem(workspace = profile, size = VolaLibrary.gemSize)
            }
        },
    )
}

/** Board W-States: an empty history, or a search that found nothing. */
@Composable
private fun HistoryEmptyState(
    searching: Boolean,
    onOpenNewTab: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // A search that found nothing is answered by changing the search, not by leaving the screen.
    val action = onOpenNewTab?.takeUnless { searching }
    VolaStateMessage(
        icon = painterResource(R.drawable.ic_history),
        title = stringResource(if (searching) R.string.history_no_results else R.string.history_empty),
        message = stringResource(
            if (searching) R.string.history_no_results_message else R.string.history_empty_message,
        ),
        modifier = modifier,
        tone = if (searching) VolaStateTone.Neutral else VolaStateTone.Empty,
        actionLabel = action?.let { stringResource(R.string.history_empty_action) },
        onAction = { action?.invoke() },
    )
}

internal object HistoryScreenTestTags {
    const val List = "history_list"
    const val SearchField = "history_search_field"
    const val Distinct = "history_distinct"
    const val Clear = "history_clear"
    const val ClearDialog = "history_clear_dialog"
    const val ClearSince = "history_clear_since"
    const val ClearUntil = "history_clear_until"
    const val ClearDatePicker = "history_clear_date_picker"
    const val ClearTimePicker = "history_clear_time_picker"
    const val ClearAllProfiles = "history_clear_all_profiles"
    const val ClearConfirm = "history_clear_confirm"
    const val DeleteSelected = "history_delete_selected"
    const val More = "history_more"
    const val AllProfiles = "history_all_profiles"

    fun profile(profileId: String): String = "history_profile:$profileId"

    fun clearProfile(profileId: String): String = "history_clear_profile:$profileId"

    fun entry(entry: HistoryEntry): String = "history_entry:${entry.profileId}:${entry.url}"

    fun select(entry: HistoryEntry): String = "history_select:${entry.profileId}:${entry.url}"
}
