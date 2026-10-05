@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.data.SnoozePreset
import dev.sk2andy.materialbrowser.data.SnoozeTimeRules
import dev.sk2andy.materialbrowser.data.SnoozedTab
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import dev.sk2andy.materialbrowser.shared.ui.theme.densityRowMinHeight
import dev.sk2andy.materialbrowser.shared.ui.theme.densityRowPadding

internal suspend fun showSnoozeUndoFeedback(
    hostState: SnackbarHostState,
    message: String,
    undoLabel: String,
    onUndo: () -> Unit,
) {
    val result = hostState.showSnackbar(
        message = message,
        actionLabel = undoLabel,
        withDismissAction = true,
        duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) onUndo()
}

@Composable
internal fun SnoozeTabDialog(
    tab: BrowserTab?,
    onSnooze: (Long) -> Boolean,
    onDismiss: () -> Unit,
) {
    if (tab == null) return
    val zoneId = remember { ZoneId.systemDefault() }
    var customEditorVisible by remember(tab.id) { mutableStateOf(false) }
    val customInitialMillis = remember(tab.id) {
        System.currentTimeMillis() + 24 * 60 * 60 * 1_000L
    }
    val enabled = !tab.isIncognito
    val applyPreset: (SnoozePreset) -> Unit = { preset ->
        val nowMillis = System.currentTimeMillis()
        if (onSnooze(SnoozeTimeRules.wakeAtMillis(preset, nowMillis, zoneId))) onDismiss()
    }

    if (!customEditorVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(SnoozeTestTags.Dialog),
            title = {
                Column {
                    Text(
                        stringResource(R.string.snooze_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        snoozeDisplayTitle(tab),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (!enabled) {
                        Text(
                            stringResource(R.string.snooze_unavailable_private),
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(SnoozeTestTags.PresetGroup),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SnoozePresetButton(
                            text = stringResource(R.string.snooze_later_today),
                            enabled = enabled,
                            tag = SnoozeTestTags.LaterToday,
                            onClick = { applyPreset(SnoozePreset.LaterToday) },
                            modifier = Modifier.weight(1f),
                        )
                        SnoozePresetButton(
                            text = stringResource(R.string.snooze_tomorrow),
                            enabled = enabled,
                            tag = SnoozeTestTags.Tomorrow,
                            onClick = { applyPreset(SnoozePreset.Tomorrow) },
                            modifier = Modifier.weight(1f),
                        )
                        SnoozePresetButton(
                            text = stringResource(R.string.snooze_next_week),
                            enabled = enabled,
                            tag = SnoozeTestTags.NextWeek,
                            onClick = { applyPreset(SnoozePreset.NextWeek) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    SnoozeChoice(
                        text = stringResource(R.string.snooze_custom),
                        enabled = enabled,
                        tag = SnoozeTestTags.Custom,
                        onClick = { customEditorVisible = true },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    SnoozeDateTimeDialogs(
        visible = customEditorVisible,
        initialMillis = customInitialMillis,
        onDismiss = { customEditorVisible = false },
        onConfirm = { wakeAtMillis ->
            onSnooze(wakeAtMillis).also { accepted ->
                if (accepted) {
                    customEditorVisible = false
                    onDismiss()
                }
            }
        },
    )
}

@Composable
private fun SnoozePresetButton(
    text: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .testTag(tag),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SnoozeChoice(
    text: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(tag),
        enabled = enabled,
    ) {
        Text(text)
    }
}

@Composable
internal fun SnoozedTabsScreen(
    snoozedTabs: List<SnoozedTab>,
    profiles: List<BrowserProfile>,
    onBack: () -> Unit,
    onReschedule: (String, Long) -> Boolean,
    onOpenNow: (String) -> Boolean,
    onDelete: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var editingTab by remember { mutableStateOf<SnoozedTab?>(null) }
    val profilesById = remember(profiles) { profiles.associateBy(BrowserProfile::id) }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(SnoozeTestTags.Management),
        color = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = VolaLibrary.sectionGap),
        ) {
            item(key = "header") {
                Column(Modifier.padding(horizontal = VolaSpacing.x2)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                VolaIcons.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                        Text(
                            stringResource(R.string.snoozed_tabs_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Text(
                        stringResource(R.string.snoozed_tabs_subtitle),
                        modifier = Modifier.padding(horizontal = VolaSpacing.x2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (snoozedTabs.isEmpty()) {
                item(key = "empty") {
                    VolaStateMessage(
                        icon = rememberVectorPainter(VolaIcons.Snooze),
                        title = stringResource(R.string.snoozed_tabs_empty_title),
                        message = stringResource(R.string.snoozed_tabs_empty_body),
                        modifier = Modifier
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap),
                    )
                }
            } else {
                itemsIndexed(snoozedTabs, key = { _, snoozed -> snoozed.tab.id }) { index, snoozed ->
                    LibraryCardSlice(
                        position = LibraryRules.position(index, snoozedTabs.size),
                        modifier = if (index == 0) Modifier.padding(top = VolaLibrary.sectionGap) else Modifier,
                    ) {
                        SnoozedTabRow(
                            snoozed = snoozed,
                            workspace = profilesById[snoozed.tab.profileId],
                            onReschedule = { editingTab = snoozed },
                            onOpenNow = {
                                if (onOpenNow(snoozed.tab.id)) onBack()
                            },
                            onDelete = { onDelete(snoozed.tab.id) },
                        )
                    }
                }
                item(key = "hint") {
                    SnoozedTabsHint(
                        modifier = Modifier
                            .padding(horizontal = VolaLibrary.sidePadding)
                            .padding(top = VolaLibrary.sectionGap),
                    )
                }
            }
        }
    }

    val editing = editingTab
    SnoozeDateTimeDialogs(
        visible = editing != null,
        initialMillis = editing?.wakeAtMillis?.takeUnless { editing.isArchived }
            ?: System.currentTimeMillis(),
        onDismiss = { editingTab = null },
        onConfirm = { wakeAtMillis ->
            val accepted = editing != null && onReschedule(editing.tab.id, wakeAtMillis)
            if (accepted) editingTab = null
            accepted
        },
    )
}

/**
 * A snoozed tab on board W-Snoozed: the site's tile, its title and address, the workspace it
 * returns to and when; «Open now» at the end, the rest in its «⋮» menu.
 */
@Composable
private fun SnoozedTabRow(
    snoozed: SnoozedTab,
    workspace: BrowserProfile?,
    onReschedule: () -> Unit,
    onOpenNow: () -> Unit,
    onDelete: () -> Unit,
) {
    val title = snoozeDisplayTitle(snoozed.tab)
    val host = if (snoozed.tab.url == BLANK_URL) "" else BrowserUriPolicy.displayHttpHost(snoozed.tab.url)
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = densityRowMinHeight(VolaLibrary.rowMinHeight))
            .testTag(SnoozeTestTags.card(snoozed.tab.id))
            .padding(densityRowPadding(VolaLibrary.rowPadding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaLibrary.rowGap),
    ) {
        LibrarySiteTile(label = title, colorKey = host.ifBlank { title })
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(VolaLibrary.textGap),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (host.isNotBlank()) {
                Text(
                    host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
            ) {
                workspace?.let { profile ->
                    WorkspaceGem(
                        workspace = profile,
                        size = VolaLibrary.gemSize,
                        modifier = Modifier.testTag(SnoozeTestTags.workspaceIcon(snoozed.tab.id)),
                    )
                    Text(
                        profile.workspaceDisplayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SnoozeWhenChip(snoozed = snoozed, onClick = onReschedule)
            }
        }
        IconButton(onClick = onOpenNow) {
            Icon(VolaIcons.OpenInNew, contentDescription = stringResource(R.string.action_open_now))
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag(SnoozeTestTags.more(snoozed.tab.id)),
            ) {
                Icon(VolaIcons.MoreVert, contentDescription = stringResource(R.string.snoozed_tab_more, title))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_reschedule)) },
                    onClick = { menuOpen = false; onReschedule() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    leadingIcon = { Icon(VolaIcons.Delete, contentDescription = null) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

/** «Today, 18:00», «Saturday, 09:00», «6 October»; an archived tab tells when it was put away. */
@Composable
private fun SnoozeWhenChip(snoozed: SnoozedTab, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = remember { ZoneId.systemDefault() }
    val label = if (snoozed.isArchived) {
        stringResource(
            R.string.snoozed_archived_at,
            remember(snoozed.createdAtMillis, locale) {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                    .format(Instant.ofEpochMilli(snoozed.createdAtMillis).atZone(zoneId))
            },
        )
    } else {
        val wake = Instant.ofEpochMilli(snoozed.wakeAtMillis).atZone(zoneId)
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(wake)
        when (LibraryFileRules.wakeDay(snoozed.wakeAtMillis, System.currentTimeMillis(), zoneId)) {
            SnoozeWakeDay.Today -> stringResource(R.string.snoozed_today_at, time)
            SnoozeWakeDay.Tomorrow -> stringResource(R.string.snoozed_tomorrow_at, time)
            SnoozeWakeDay.ThisWeek -> DateTimeFormatter.ofPattern("EEEE", locale).format(wake)
                .replaceFirstChar { it.titlecase(locale) } + ", " + time
            SnoozeWakeDay.Later -> DateTimeFormatter.ofPattern("d MMMM", locale).format(wake)
        }
    }
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(VolaIcons.Snooze, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
    )
}

/** The footer of board W-Snoozed: how a tab gets here. */
@Composable
private fun SnoozedTabsHint(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            VolaIcons.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.snoozed_tabs_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class SnoozeDateTimeStep { Date, Time }

@Composable
private fun SnoozeDateTimeDialogs(
    visible: Boolean,
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Boolean,
) {
    if (!visible) return
    val zoneId = remember { ZoneId.systemDefault() }
    val initialLocal = remember(initialMillis, zoneId) {
        Instant.ofEpochMilli(initialMillis).atZone(zoneId)
    }
    var step by remember(initialMillis) { mutableStateOf(SnoozeDateTimeStep.Date) }
    var selectedDate by remember(initialMillis) { mutableStateOf(initialLocal.toLocalDate()) }
    var invalidTime by remember(initialMillis) { mutableStateOf(false) }

    when (step) {
        SnoozeDateTimeStep.Date -> {
            val initialDateMillis = remember(initialLocal) {
                initialLocal.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
            val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMillis = dateState.selectedDateMillis ?: return@TextButton
                            selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            step = SnoozeDateTimeStep.Time
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
                    state = dateState,
                    title = {
                        Text(
                            stringResource(R.string.snooze_select_date),
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        )
                    },
                )
            }
        }
        SnoozeDateTimeStep.Time -> {
            val timeState = rememberTimePickerState(
                initialHour = initialLocal.hour,
                initialMinute = initialLocal.minute,
                is24Hour = true,
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.snooze_select_time)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = timeState)
                        if (invalidTime) {
                            Text(
                                stringResource(R.string.snooze_invalid_time),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val wakeAtMillis = SnoozeTimeRules.customWakeAtMillis(
                                selectedDate,
                                LocalTime.of(timeState.hour, timeState.minute),
                                zoneId,
                            )
                            invalidTime = wakeAtMillis <= System.currentTimeMillis() ||
                                !onConfirm(wakeAtMillis)
                        },
                    ) {
                        Text(stringResource(R.string.action_save))
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
private fun snoozeDisplayTitle(tab: BrowserTab): String =
    if (tab.url == BLANK_URL || tab.title.isBlank()) {
        stringResource(R.string.new_tab_title)
    } else {
        tab.title
    }

internal object SnoozeTestTags {
    const val Dialog = "snooze_dialog"
    const val PresetGroup = "snooze_preset_group"
    const val LaterToday = "snooze_later_today"
    const val Tomorrow = "snooze_tomorrow"
    const val NextWeek = "snooze_next_week"
    const val Custom = "snooze_custom"
    const val TabActions = "tab_actions"
    const val TabActionsSnooze = "tab_actions_snooze"
    const val Management = "snoozed_tabs_management"
    fun overviewTab(tabId: String) = "overview_tab:$tabId"
    fun overviewTitle(tabId: String) = "overview_title:$tabId"
    fun overviewClose(tabId: String) = "overview_close:$tabId"
    fun card(tabId: String) = "snoozed_tab:$tabId"
    fun workspaceIcon(tabId: String) = "snooze_workspace_icon:$tabId"
    fun more(tabId: String) = "snoozed_tab_more:$tabId"
}
