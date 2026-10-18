package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.topping.ToppingFrameScope
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5ListDefaults
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet

/**
 * Toppings (userscripts) under ui.v5 (V28, Б12a), laid out as Safari's Extensions settings: the
 * catalog, a new script and import on top, every saved script a row that says whether it is on,
 * and a tap opens its own page (the switch, its addresses, embedded-frame access, edit, delete).
 * The editor and the delete question are v5 sheets. What each action does is the current
 * screen's, through the same callbacks; v4 is unchanged.
 */
@Composable
internal fun UserscriptManagementV5(
    scripts: List<UserscriptUiItem>,
    isRuntimeSupported: Boolean,
    onToggle: (id: String, enabled: Boolean, onResult: (String?) -> Unit) -> Unit,
    onSetFrameScope: (id: String, scope: ToppingFrameScope, onResult: (String?) -> Unit) -> Unit,
    onSave: (id: String?, source: String, onResult: (String?) -> Unit) -> Unit,
    onDelete: (id: String, onResult: (String?) -> Unit) -> Unit,
    onImport: () -> Unit,
    onDiscover: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** A script whose page is open at first, for previews. */
    initialOpenId: String? = null,
    /** An editor open at first, for previews. */
    initialEditor: UserscriptEditorRequest? = null,
) {
    val newScriptName = stringResource(R.string.userscript_new_script_name)
    var openId by remember { mutableStateOf(initialOpenId) }
    var editor by remember { mutableStateOf(initialEditor) }
    var pendingDelete by remember { mutableStateOf<UserscriptUiItem?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    // A deleted script's page has nothing left to show: the list comes back.
    val open = scripts.firstOrNull { it.id == openId }
    BackHandler(enabled = open != null) { openId = null }
    val finish: (String?) -> Unit = { error ->
        busy = false
        actionError = error
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < USERSCRIPT_DARK_SHELL_LUMINANCE
    VolaV5Theme(dark = dark) {
        val colors = V5Theme.colors
        Column(
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag(UserscriptManagementTestTags.Screen),
        ) {
            V5PageBar(
                title = open?.name ?: stringResource(R.string.userscript_title),
                onBack = { if (open != null) openId = null else onDismiss() },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = V5Spacing.space4, bottom = V5Spacing.space8)
                    .testTag(UserscriptManagementTestTags.List),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
            ) {
                UserscriptManagementV5Rules.notices(isRuntimeSupported, actionError).forEach { notice ->
                    Text(
                        notice.text ?: stringResource(R.string.userscript_runtime_unsupported),
                        style = V5Theme.type.footnote,
                        color = colors.danger,
                        modifier = Modifier
                            .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
                            .testTag(
                                if (notice.text != null) {
                                    UserscriptManagementTestTags.ActionError
                                } else {
                                    UserscriptManagementTestTags.RuntimeUnsupported
                                },
                            ),
                    )
                }
                if (open == null) {
                    UserscriptList(
                        scripts = scripts,
                        onDiscover = onDiscover,
                        onAdd = {
                            actionError = null
                            editor = UserscriptEditorRequest(null, userscriptTemplate(newScriptName))
                        },
                        onImport = onImport,
                        onOpen = {
                            actionError = null
                            openId = it.id
                        },
                    )
                } else {
                    UserscriptPage(
                        script = open,
                        busy = busy,
                        onToggle = { enabled ->
                            busy = true
                            actionError = null
                            onToggle(open.id, enabled, finish)
                        },
                        onSetFrameScope = { scope ->
                            busy = true
                            actionError = null
                            onSetFrameScope(open.id, scope, finish)
                        },
                        onEdit = {
                            actionError = null
                            editor = UserscriptEditorRequest(open.id, open.source)
                        },
                        onDelete = {
                            actionError = null
                            pendingDelete = open
                        },
                    )
                }
            }
        }
        UserscriptEditorSheet(
            request = editor,
            onDismiss = { editor = null },
            onSave = { request, source, onResult ->
                onSave(request.id, source) { error ->
                    if (error == null) editor = null
                    onResult(error)
                }
            },
        )
        UserscriptDeleteSheet(
            script = pendingDelete,
            onDismiss = { pendingDelete = null },
            onDelete = { script, onResult ->
                onDelete(script.id) { error ->
                    if (error == null) {
                        pendingDelete = null
                        openId = null
                    }
                    onResult(error)
                }
            },
        )
    }
}

/** What the editor opens with: the script to replace ([id], none for a new one) and its source. */
internal data class UserscriptEditorRequest(val id: String?, val source: String)

@Composable
private fun UserscriptList(
    scripts: List<UserscriptUiItem>,
    onDiscover: () -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onOpen: (UserscriptUiItem) -> Unit,
) {
    val colors = V5Theme.colors
    V5ListGroup(
        header = stringResource(R.string.userscript_risk_title),
        footer = stringResource(R.string.userscript_risk_body),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(
            title = stringResource(R.string.topping_discover),
            trailing = V5RowTrailing.Chevron,
            onClick = onDiscover,
            modifier = Modifier.testTag(UserscriptManagementTestTags.Discover),
        )
        V5ListRow(
            title = stringResource(R.string.userscript_add),
            titleColor = colors.accent,
            onClick = onAdd,
            modifier = Modifier.testTag(UserscriptManagementTestTags.Add),
        )
        V5ListRow(
            title = stringResource(R.string.userscript_import),
            titleColor = colors.accent,
            onClick = onImport,
            modifier = Modifier.testTag(UserscriptManagementTestTags.Import),
        )
    }
    val scope = listOf(
        stringResource(R.string.userscript_regular_tabs_only),
        stringResource(R.string.userscript_no_gm_apis),
    ).joinToString(" ")
    if (scripts.isEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4, vertical = V5Spacing.space4)
                .testTag(UserscriptManagementTestTags.EmptyState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Text(
                stringResource(R.string.userscript_empty_title),
                style = V5Theme.type.headline,
                color = colors.label,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.userscript_empty_body),
                style = V5Theme.type.callout,
                color = colors.labelSecondary,
                textAlign = TextAlign.Center,
            )
            Text(scope, style = V5Theme.type.footnote, color = colors.labelSecondary, textAlign = TextAlign.Center)
        }
        return
    }
    V5ListGroup(
        header = stringResource(R.string.userscript_saved_section),
        footer = scope,
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        scripts.forEach { script ->
            // As in Safari, the switch is on the script's page; the row says whether it is on.
            V5ListRow(
                title = script.name,
                subtitle = stringResource(R.string.userscript_run_at, script.runAtLabel),
                value = stringResource(if (script.enabled) R.string.userscript_v5_on else R.string.userscript_v5_off),
                trailing = V5RowTrailing.Chevron,
                onClick = { onOpen(script) },
                modifier = Modifier.testTag(UserscriptManagementTestTags.script(script.id)),
            )
        }
    }
}

@Composable
private fun UserscriptPage(
    script: UserscriptUiItem,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
    onSetFrameScope: (ToppingFrameScope) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = V5Theme.colors
    V5ListGroup(
        footer = stringResource(R.string.userscript_reload_notice),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        V5ListRow(
            title = stringResource(R.string.userscript_v5_enabled),
            trailing = V5RowTrailing.Toggle(script.enabled, onToggle),
            enabled = !busy,
            modifier = Modifier
                .testTag(UserscriptManagementTestTags.toggle(script.id))
                .semantics { contentDescription = script.name },
        )
    }
    V5ListGroup(
        header = stringResource(R.string.userscript_v5_matches),
        footer = stringResource(R.string.userscript_run_at, script.runAtLabel),
        dividerInset = V5ListDefaults.dividerInsetText,
    ) {
        val patterns = UserscriptManagementV5Rules.patterns(script)
        if (patterns.isEmpty()) {
            V5ListRow(title = stringResource(R.string.userscript_no_patterns), titleColor = colors.labelSecondary)
        } else {
            patterns.forEach { pattern -> V5ListRow(title = pattern) }
        }
    }
    V5ListGroup(
        header = stringResource(R.string.userscript_frame_scope_title),
        footer = stringResource(R.string.userscript_frame_scope_explanation) + " " +
            stringResource(R.string.userscript_frame_scope_requested, frameScopeLabel(script.declaredFrameScope)),
        dividerInset = V5ListDefaults.dividerInsetText,
        modifier = Modifier.testTag(UserscriptManagementTestTags.frameScope(script.id)),
    ) {
        val editable = UserscriptManagementV5Rules.frameScopeEditable(script, busy)
        UserscriptManagementV5Rules.frameChoices(script).forEach { scope ->
            val chosen = script.allowedFrameScope == scope
            V5ListRow(
                title = UserscriptManagementV5Rules.capitalized(frameScopeLabel(scope)),
                trailing = if (chosen) V5RowTrailing.Check else V5RowTrailing.None,
                enabled = editable,
                onClick = { if (!chosen) onSetFrameScope(scope) },
                modifier = Modifier.testTag(UserscriptManagementTestTags.frameScopeOption(script.id, scope)),
            )
        }
    }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = stringResource(R.string.userscript_v5_edit),
            titleColor = colors.accent,
            enabled = !busy,
            onClick = onEdit,
            modifier = Modifier.testTag(UserscriptManagementTestTags.edit(script.id)),
        )
    }
    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
        V5ListRow(
            title = stringResource(R.string.userscript_v5_delete),
            titleColor = colors.danger,
            enabled = !busy,
            onClick = onDelete,
            modifier = Modifier.testTag(UserscriptManagementTestTags.delete(script.id)),
        )
    }
}

/** The source editor: what the metadata needs, the source in monospace, then «Save». */
@Composable
private fun UserscriptEditorSheet(
    request: UserscriptEditorRequest?,
    onDismiss: () -> Unit,
    onSave: (UserscriptEditorRequest, String, (String?) -> Unit) -> Unit,
) {
    // The sheet leaves on its spring after saving, still showing what was saved.
    var shown by remember { mutableStateOf(request) }
    LaunchedEffect(request) { if (request != null) shown = request }
    val edited = request ?: shown ?: return
    var source by remember(edited) { mutableStateOf(edited.source) }
    var error by remember(edited) { mutableStateOf<String?>(null) }
    var saving by remember(edited) { mutableStateOf(false) }
    val colors = V5Theme.colors
    val title = stringResource(
        if (edited.id == null) R.string.userscript_editor_add_title else R.string.userscript_editor_edit_title,
    )
    V5WindowSheet(
        visible = request != null,
        onDismiss = onDismiss,
        title = title,
        modifier = Modifier.testTag(UserscriptManagementTestTags.Editor),
    ) {
        V5SheetHeader(title = title, onClose = onDismiss, subtitle = stringResource(R.string.userscript_editor_help))
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            val label = stringResource(R.string.userscript_source_label)
            BasicTextField(
                value = source,
                onValueChange = {
                    source = it
                    error = null
                },
                textStyle = V5Theme.type.footnote.copy(fontFamily = FontFamily.Monospace, color = colors.label),
                cursorBrush = SolidColor(colors.accent),
                // Rules and scripts are code: no autocorrect and no capital at the start of a line, which
                // can turn `let` into `Let` or replace a word of a rule.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier
                    .padding(horizontal = V5Spacing.marginPhone)
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.rowSingleLine * UserscriptManagementV5Rules.EDITOR_MIN_ROWS)
                    .clip(V5Theme.shapes.s)
                    .background(colors.surface)
                    .padding(V5Spacing.space4)
                    .semantics { contentDescription = label }
                    .testTag(UserscriptManagementTestTags.EditorSource),
            )
            error?.let { message ->
                Text(
                    message,
                    style = V5Theme.type.footnote,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                )
            }
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.action_save),
                    titleColor = colors.accent,
                    enabled = UserscriptManagementV5Rules.canSave(source, saving),
                    onClick = {
                        saving = true
                        onSave(edited, source) { result ->
                            saving = false
                            error = result
                        }
                    },
                    modifier = Modifier.testTag(UserscriptManagementTestTags.EditorSave),
                )
            }
        }
    }
}

/** «Delete script?»: what goes, then «Delete» in red; the sheet stays while it is deleting. */
@Composable
private fun UserscriptDeleteSheet(
    script: UserscriptUiItem?,
    onDismiss: () -> Unit,
    onDelete: (UserscriptUiItem, (String?) -> Unit) -> Unit,
) {
    var shown by remember { mutableStateOf(script) }
    LaunchedEffect(script) { if (script != null) shown = script }
    val asked = script ?: shown ?: return
    var deleting by remember(asked.id) { mutableStateOf(false) }
    var error by remember(asked.id) { mutableStateOf<String?>(null) }
    val colors = V5Theme.colors
    val title = stringResource(R.string.userscript_delete_title)
    val dismiss = { if (!deleting) onDismiss() }
    V5WindowSheet(
        visible = script != null,
        onDismiss = dismiss,
        title = title,
        modifier = Modifier.testTag(UserscriptManagementTestTags.DeleteConfirmation),
    ) {
        V5SheetHeader(
            title = title,
            onClose = dismiss,
            subtitle = stringResource(R.string.userscript_delete_body, asked.name),
        )
        Column(
            Modifier.padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            error?.let { message ->
                Text(
                    message,
                    style = V5Theme.type.footnote,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
                )
            }
            V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                V5ListRow(
                    title = stringResource(R.string.action_delete),
                    titleColor = colors.danger,
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        error = null
                        onDelete(asked) { result ->
                            deleting = false
                            error = result
                        }
                    },
                    modifier = Modifier.testTag(UserscriptManagementTestTags.DeleteConfirm),
                )
                V5ListRow(
                    title = stringResource(R.string.action_cancel),
                    enabled = !deleting,
                    onClick = dismiss,
                )
            }
        }
    }
}

/** A line above the page: the host could not start ([text] null) or the last action failed. */
internal data class UserscriptNotice(val text: String?)

/** What the v5 page shows, apart from drawing, for unit tests. */
internal object UserscriptManagementV5Rules {
    /** The editor is at least this many list rows tall, so a template fits without scrolling. */
    const val EDITOR_MIN_ROWS = 5

    /** Every positive address rule, each once, in the script's order. */
    fun patterns(script: UserscriptUiItem): List<String> = script.urlPatterns.filter { it.isNotBlank() }.distinct()

    /** What the script may be narrowed to; never more than its metadata asks for. */
    fun frameChoices(script: UserscriptUiItem): List<ToppingFrameScope> = script.declaredFrameScope.allowedChoices()

    /** A script that asks only for the main page has nothing to narrow. */
    fun frameScopeEditable(script: UserscriptUiItem, busy: Boolean): Boolean =
        !busy && script.declaredFrameScope != ToppingFrameScope.Top

    fun canSave(source: String, saving: Boolean): Boolean = source.isNotBlank() && !saving

    /** The scope labels are mid-sentence in v4 («Allowed: main page only»); a row starts with a capital. */
    fun capitalized(label: String): String = label.replaceFirstChar { it.titlecase() }

    /** The lines above the page: the host first, then what went wrong last. */
    fun notices(isRuntimeSupported: Boolean, actionError: String?): List<UserscriptNotice> = listOfNotNull(
        UserscriptNotice(null).takeIf { !isRuntimeSupported },
        actionError?.let(::UserscriptNotice),
    )
}

private const val USERSCRIPT_DARK_SHELL_LUMINANCE = 0.5f
