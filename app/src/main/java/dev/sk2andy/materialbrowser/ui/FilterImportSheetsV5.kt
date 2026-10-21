package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontFamily
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyFilterPresets
import dev.sk2andy.materialbrowser.blocking.CandyImportScope
import dev.sk2andy.materialbrowser.blocking.CandyRule
import dev.sk2andy.materialbrowser.blocking.CandyRulePreview
import dev.sk2andy.materialbrowser.blocking.CandySubscriptionClient
import dev.sk2andy.materialbrowser.blocking.CandySubscriptionResult
import dev.sk2andy.materialbrowser.blocking.CandySubscriptionRules
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import dev.sk2andy.materialbrowser.ui.v5.v5FieldLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * The filter studio's import, web list and export under ui.v5 (V28, Б12a-5), as v5 sheets. Each
 * does what its v4 dialog does — parse or fetch off the main thread, preview for a scope, confirm
 * skipped lines, apply — through the same callbacks and test tags.
 */

/** «Import preview»: paste rules, analyze, choose the scope, confirm skipped lines, apply. */
@Composable
internal fun FilterImportSheetV5(
    visible: Boolean,
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    onDismiss: () -> Unit,
    onParse: (String) -> CandyRulePreview,
    onApply: (CandyRulePreview) -> Boolean,
) {
    val opening = rememberFilterSheetOpening(visible)
    if (opening == 0) return
    var text by remember(opening) { mutableStateOf("") }
    var targetProfileId by remember(opening) { mutableStateOf<String?>(currentProfileId) }
    var skippedConfirmed by remember(opening) { mutableStateOf(false) }
    var preview by remember(opening) { mutableStateOf<CandyRulePreview?>(null) }
    var loading by remember(opening) { mutableStateOf(false) }
    var applyFailed by remember(opening) { mutableStateOf(false) }
    var parseJob by remember(opening) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val scopedPreview = remember(preview, targetProfileId) {
        preview?.let { CandyImportScope.apply(it, targetProfileId) }
    }
    val colors = V5Theme.colors
    val title = stringResource(R.string.filter_import_preview)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.filter_import_supported_body))
        SheetBody {
            FilterSourceField(
                value = text,
                placeholder = stringResource(R.string.filter_import_paste),
                onValueChange = {
                    parseJob?.cancel()
                    text = it
                    preview = null
                    skippedConfirmed = false
                    loading = false
                    applyFailed = false
                },
                modifier = Modifier.testTag(FilterStudioTestTags.ImportInput),
            )
            V5ListGroup {
                V5ListRow(
                    title = stringResource(
                        if (loading) R.string.filter_analyzing_import else R.string.filter_analyze_import,
                    ),
                    titleColor = colors.accent,
                    enabled = text.isNotBlank() && !loading,
                    onClick = {
                        val input = text
                        parseJob?.cancel()
                        loading = true
                        applyFailed = false
                        parseJob = scope.launch {
                            val parsed = withContext(Dispatchers.Default) { onParse(input) }
                            if (text == input) {
                                preview = parsed
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.testTag(FilterStudioTestTags.ImportAnalyze),
                )
            }
            scopedPreview?.let { current ->
                PreviewSummary(
                    preview = current,
                    header = stringResource(R.string.filter_import_format, current.format.label()),
                    lines = current.errors.take(FilterImportSheetsV5Rules.SHOWN_LINES).map { error ->
                        stringResource(R.string.filter_error_line, error.line, filterErrorLabel(error.message))
                    } + current.skipped.take(FilterImportSheetsV5Rules.SHOWN_LINES).map { skipped ->
                        stringResource(R.string.filter_skipped_line, skipped.line, filterErrorLabel(skipped.message))
                    },
                )
                ScopeChoice(
                    profiles = profiles,
                    currentProfileId = currentProfileId,
                    targetProfileId = targetProfileId,
                    onTarget = { targetProfileId = it },
                    globalTag = FilterStudioTestTags.ImportScopeGlobal,
                    currentTag = FilterStudioTestTags.ImportScopeCurrent,
                    otherTag = { id -> "filter_studio_import_scope_$id" },
                )
                if (current.skippedCount > 0) {
                    SkippedConfirm(
                        title = stringResource(R.string.filter_confirm_skipped, current.skippedCount),
                        checked = skippedConfirmed,
                        onChecked = { skippedConfirmed = it },
                        tag = FilterStudioTestTags.ImportSkippedConfirm,
                    )
                }
                if (current.rules.isNotEmpty()) {
                    val shown = FilterImportSheetsV5Rules.SHOWN_RULES
                    V5ListGroup(
                        footer = stringResource(R.string.filter_more_rules, current.rules.size - shown)
                            .takeIf { current.rules.size > shown },
                        dividerInset = V5ListDefaults.dividerInsetText,
                    ) {
                        current.rules.take(shown).forEach { rule ->
                            V5ListRow(
                                title = rule.displayTarget(),
                                subtitle = "${rule.action.label()} · ${rule.scopeLabel()}",
                            )
                        }
                    }
                }
            }
            ApplyFailed(applyFailed)
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.filter_apply_preview),
                    titleColor = colors.accent,
                    enabled = FilterImportSheetsV5Rules.canApply(scopedPreview, skippedConfirmed),
                    onClick = { applyFailed = scopedPreview?.let(onApply) == false },
                    modifier = Modifier.testTag(FilterStudioTestTags.ImportConfirm),
                )
            }
        }
    }
}

/** «Web filter list»: a trusted HTTPS source (or uBlock's), fetch a preview, see the changes, apply. */
@Composable
internal fun FilterSubscriptionSheetV5(
    visible: Boolean,
    existingRules: List<CandyRule>,
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    isIncognito: Boolean,
    onDismiss: () -> Unit,
    onApply: (String, CandyRulePreview) -> Boolean,
) {
    val opening = rememberFilterSheetOpening(visible)
    if (opening == 0) return
    var source by remember(opening) { mutableStateOf("") }
    var result by remember(opening) { mutableStateOf<CandySubscriptionResult?>(null) }
    var loading by remember(opening) { mutableStateOf(false) }
    var targetProfileId by remember(opening) { mutableStateOf<String?>(currentProfileId) }
    var skippedConfirmed by remember(opening) { mutableStateOf(false) }
    var applyFailed by remember(opening) { mutableStateOf(false) }
    var fetchJob by remember(opening) { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val preview = (result as? CandySubscriptionResult.Preview)?.preview
    val scopedPreview = remember(preview, targetProfileId) {
        preview?.let { CandyImportScope.apply(it, targetProfileId) }
    }
    val previous = existingRules.filter { CandySubscriptionRules.isSameSourceScope(it, source, targetProfileId) }
    val diff = scopedPreview?.let { CandySubscriptionRules.diff(previous, it.rules) }
    val colors = V5Theme.colors
    val title = stringResource(R.string.filter_subscription)
    val resetFor: (String) -> Unit = { value ->
        fetchJob?.cancel()
        source = value
        result = null
        loading = false
        skippedConfirmed = false
        applyFailed = false
    }
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.filter_subscription_warning))
        SheetBody {
            if (isIncognito) {
                Note(
                    text = stringResource(R.string.filter_subscription_private_note),
                    color = colors.accent,
                    modifier = Modifier
                        .testTag(FilterStudioTestTags.SubscriptionPrivateNote)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            V5ListGroup(
                header = stringResource(R.string.filter_ubo_preset_title),
                footer = stringResource(R.string.filter_ubo_preset_body),
            ) {
                V5ListRow(
                    title = stringResource(R.string.filter_use_ubo_preset),
                    titleColor = colors.accent,
                    onClick = { resetFor(CandyFilterPresets.UBLOCK_ORIGIN_BASE_URL) },
                    modifier = Modifier.testTag(FilterStudioTestTags.SubscriptionPreset),
                )
            }
            FilterTextField(
                value = source,
                placeholder = stringResource(R.string.filter_source_url),
                onValueChange = resetFor,
                modifier = Modifier.testTag(FilterStudioTestTags.SubscriptionSource),
            )
            ScopeChoice(
                profiles = profiles,
                currentProfileId = currentProfileId,
                targetProfileId = targetProfileId,
                onTarget = { targetProfileId = it },
                globalTag = FilterStudioTestTags.SubscriptionScopeGlobal,
                currentTag = FilterStudioTestTags.SubscriptionScopeCurrent,
                otherTag = { id -> "filter_studio_subscription_scope_$id" },
            )
            V5ListGroup {
                V5ListRow(
                    title = stringResource(
                        if (loading) R.string.filter_fetching_preview else R.string.filter_fetch_preview,
                    ),
                    titleColor = colors.accent,
                    enabled = source.isNotBlank() && !loading,
                    onClick = {
                        val input = source
                        fetchJob?.cancel()
                        loading = true
                        applyFailed = false
                        fetchJob = scope.launch {
                            val fetched = withContext(Dispatchers.IO) { CandySubscriptionClient.fetch(input) }
                            if (source == input) {
                                result = fetched
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            (result as? CandySubscriptionResult.Error)?.let { error ->
                Note(text = filterErrorLabel(error.reason), color = colors.danger)
            }
            scopedPreview?.let { current ->
                PreviewSummary(
                    preview = current,
                    header = null,
                    lines = current.skipped.take(FilterImportSheetsV5Rules.SHOWN_LINES).map { skipped ->
                        stringResource(R.string.filter_skipped_line, skipped.line, filterErrorLabel(skipped.message))
                    } + current.errors.take(FilterImportSheetsV5Rules.SHOWN_LINES).map { error ->
                        stringResource(R.string.filter_error_line, error.line, filterErrorLabel(error.message))
                    },
                )
                if (current.skippedCount > 0) {
                    SkippedConfirm(
                        title = stringResource(R.string.filter_confirm_subscription_skipped, current.skippedCount),
                        checked = skippedConfirmed,
                        onChecked = { skippedConfirmed = it },
                        tag = FilterStudioTestTags.SubscriptionSkippedConfirm,
                    )
                }
            }
            diff?.let { changes ->
                val shown = FilterImportSheetsV5Rules.SHOWN_RULES
                val hidden = FilterImportSheetsV5Rules.hiddenChanges(changes.added.size, changes.removed.size)
                V5ListGroup(
                    header = stringResource(
                        R.string.filter_diff_summary,
                        changes.added.size,
                        changes.removed.size,
                        changes.unchanged.size,
                    ),
                    footer = stringResource(R.string.filter_more_changes, hidden).takeIf { hidden > 0 },
                    dividerInset = V5ListDefaults.dividerInsetText,
                ) {
                    changes.added.take(shown).forEach { rule -> V5ListRow(title = "+ ${rule.displayTarget()}") }
                    changes.removed.take(shown).forEach { rule ->
                        V5ListRow(title = "− ${rule.displayTarget()}", titleColor = colors.labelSecondary)
                    }
                }
            }
            ApplyFailed(applyFailed)
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.filter_confirm_update),
                    titleColor = colors.accent,
                    enabled = FilterImportSheetsV5Rules.canApply(scopedPreview, skippedConfirmed),
                    onClick = { applyFailed = scopedPreview?.let { onApply(source, it) } == false },
                    modifier = Modifier.testTag(FilterStudioTestTags.SubscriptionConfirm),
                )
            }
        }
    }
}

/** «Export»: the rules as Vola rules v1, selectable to copy, then «Done». */
@Composable
internal fun FilterExportSheetV5(visible: Boolean, text: () -> String, onDismiss: () -> Unit) {
    val opening = rememberFilterSheetOpening(visible)
    if (opening == 0) return
    // Exported once per opening, as the v4 dialog does.
    val exported = remember(opening) { text() }
    val colors = V5Theme.colors
    val title = stringResource(R.string.filter_export)
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.filter_transfer_body))
        SheetBody {
            SelectionContainer(
                Modifier
                    .padding(horizontal = V5Spacing.marginPhone)
                    .fillMaxWidth()
                    .clip(V5Theme.shapes.s)
                    .background(colors.surface)
                    .padding(V5Spacing.space4),
            ) {
                Text(exported, style = V5Theme.type.footnote.copy(fontFamily = FontFamily.Monospace), color = colors.label)
            }
            V5ListGroup {
                V5ListRow(title = stringResource(R.string.action_done), titleColor = colors.accent, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun SheetBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        content = content,
    )
}

/** Pasted rules: a monospace field several rows tall, its [placeholder] read by TalkBack. */
@Composable
private fun FilterSourceField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowSingleLine * FilterImportSheetsV5Rules.SOURCE_MIN_ROWS)
            .clip(V5Theme.shapes.s)
            .background(colors.surface)
            .padding(V5Spacing.space4),
    ) {
        // The field carries the name for TalkBack; this is only what the eye reads (СС-1).
        if (value.isEmpty()) Text(placeholder, style = V5Theme.type.footnote, color = colors.labelTertiary, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = V5Theme.type.footnote.copy(fontFamily = FontFamily.Monospace, color = colors.label),
            cursorBrush = SolidColor(colors.accent),
            // Rules and scripts are code: no autocorrect and no capital at the start of a line, which
            // can turn `let` into `Let` or replace a word of a rule.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = modifier
                .fillMaxWidth()
                .v5FieldLabel(placeholder, empty = value.isEmpty()),
        )
    }
}

/** What the preview found: the counts (in the accent when it can apply, red when not) and its lines. */
@Composable
private fun PreviewSummary(preview: CandyRulePreview, header: String?, lines: List<String>) {
    val colors = V5Theme.colors
    V5ListGroup(
        header = header,
        footer = lines.joinToString("\n").ifEmpty { null },
        dividerInset = V5ListDefaults.dividerInsetText,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        V5ListRow(
            title = stringResource(
                R.string.filter_preview_summary,
                preview.rules.size,
                preview.skippedCount,
                preview.errors.size,
            ),
            titleColor = if (preview.isApplicable) colors.accent else colors.danger,
        )
    }
}

/** «Import for»: every workspace or one, with a check on the chosen one. */
@Composable
private fun ScopeChoice(
    profiles: List<BrowserProfile>,
    currentProfileId: String,
    targetProfileId: String?,
    onTarget: (String?) -> Unit,
    globalTag: String,
    currentTag: String,
    otherTag: (String) -> String,
) {
    V5ListGroup(header = stringResource(R.string.filter_import_scope), dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = stringResource(R.string.filter_scope_global),
            trailing = if (targetProfileId == null) V5RowTrailing.Check else V5RowTrailing.None,
            onClick = { onTarget(null) },
            modifier = Modifier.testTag(globalTag),
        )
        profiles.forEach { profile ->
            val description = stringResource(R.string.filter_profile_description, profile.workspaceDisplayName())
            V5ListRow(
                title = profile.workspaceDisplayName(),
                trailing = if (targetProfileId == profile.id) V5RowTrailing.Check else V5RowTrailing.None,
                onClick = { onTarget(profile.id) },
                modifier = Modifier
                    .testTag(if (profile.id == currentProfileId) currentTag else otherTag(profile.id))
                    .semantics { contentDescription = description },
            )
        }
    }
}

@Composable
private fun SkippedConfirm(title: String, checked: Boolean, onChecked: (Boolean) -> Unit, tag: String) {
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = title,
            trailing = V5RowTrailing.Toggle(checked, onChecked),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun ApplyFailed(failed: Boolean) {
    if (!failed) return
    Note(
        text = stringResource(R.string.filter_import_apply_failed),
        color = V5Theme.colors.danger,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

@Composable
private fun Note(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = V5Theme.type.footnote,
        color = color,
        modifier = modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
    )
}

/** What the import sheets decide, apart from drawing, for unit tests. */
internal object FilterImportSheetsV5Rules {
    /** Problem lines of each kind the preview lists, as v4 does. */
    const val SHOWN_LINES = 3

    /** Rules, or added and removed rules each, the preview lists, as v4 does. */
    const val SHOWN_RULES = 4

    /** The pasted-rules field is at least this many list rows tall. */
    const val SOURCE_MIN_ROWS = 4

    /** Apply needs a preview without errors, and skipped lines confirmed when there are any. */
    fun canApply(preview: CandyRulePreview?, skippedConfirmed: Boolean): Boolean =
        preview != null && preview.isApplicable && (preview.skippedCount == 0 || skippedConfirmed)

    /** Added and removed rules past the ones listed. */
    fun hiddenChanges(added: Int, removed: Int): Int =
        (added - SHOWN_RULES).coerceAtLeast(0) + (removed - SHOWN_RULES).coerceAtLeast(0)
}
