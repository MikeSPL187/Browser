package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.topping.ToppingFrameScope
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
internal data class UserscriptUiItem(
    val id: String,
    val name: String,
    val source: String,
    val enabled: Boolean,
    val runAtLabel: String,
    val urlPatterns: List<String>,
    val declaredFrameScope: ToppingFrameScope = ToppingFrameScope.Top,
    val allowedFrameScope: ToppingFrameScope = ToppingFrameScope.Top,
)

internal object UserscriptManagementTestTags {
    const val Screen = "userscript_management_screen"
    const val List = "userscript_management_list"
    const val Add = "userscript_management_add"
    const val Discover = "userscript_management_discover"
    const val Import = "userscript_management_import"
    const val EmptyState = "userscript_management_empty"
    const val Editor = "userscript_management_editor"
    const val EditorSource = "userscript_management_editor_source"
    const val EditorSave = "userscript_management_editor_save"
    const val ActionError = "userscript_management_action_error"
    const val DeleteConfirmation = "userscript_management_delete_confirmation"
    const val DeleteConfirm = "userscript_management_delete_confirm"
    const val FrameScopeDialog = "userscript_management_frame_scope_dialog"
    const val RuntimeUnsupported = "userscript_management_runtime_unsupported"

    fun script(id: String) = "userscript_management_script_$id"
    fun toggle(id: String) = "userscript_management_toggle_$id"
    fun edit(id: String) = "userscript_management_edit_$id"
    fun delete(id: String) = "userscript_management_delete_$id"
    fun frameScope(id: String) = "userscript_management_frame_scope_$id"
    fun frameScopeOption(id: String, scope: ToppingFrameScope) =
        "userscript_management_frame_scope_${id}_${scope.wireValue}"
}

@Composable
internal fun UserscriptManagementScreen(
    scripts: List<UserscriptUiItem>,
    isRuntimeSupported: Boolean = true,
    onToggle: (id: String, enabled: Boolean, onResult: (String?) -> Unit) -> Unit,
    onSetFrameScope: (
        id: String,
        scope: ToppingFrameScope,
        onResult: (String?) -> Unit,
    ) -> Unit = { _, _, onResult -> onResult(null) },
    onSave: (id: String?, source: String, onResult: (String?) -> Unit) -> Unit,
    onDelete: (id: String, onResult: (String?) -> Unit) -> Unit,
    onImport: () -> Unit,
    onDiscover: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserscriptManagementV5(
        scripts = scripts,
        isRuntimeSupported = isRuntimeSupported,
        onToggle = onToggle,
        onSetFrameScope = onSetFrameScope,
        onSave = onSave,
        onDelete = onDelete,
        onImport = onImport,
        onDiscover = onDiscover,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun UserscriptSafetyCard(isRuntimeSupported: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                stringResource(R.string.userscript_risk_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.userscript_risk_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.userscript_scope_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.userscript_regular_tabs_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(R.string.userscript_no_gm_apis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(R.string.userscript_reload_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (!isRuntimeSupported) {
                Text(
                    stringResource(R.string.userscript_runtime_unsupported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun UserscriptEmptyState(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UserscriptManagementTestTags.EmptyState),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.userscript_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.userscript_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onAdd) {
                Text(stringResource(R.string.userscript_add_first))
            }
        }
    }
}

@Composable
private fun UserscriptCard(
    script: UserscriptUiItem,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onFrameScope: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UserscriptManagementTestTags.script(script.id)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    script.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.userscript_run_at, script.runAtLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(
                        R.string.userscript_matches,
                        script.urlPatterns.takeIf { it.isNotEmpty() }
                            ?.let(::userscriptPatternSummary)
                            ?: stringResource(R.string.userscript_no_patterns),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.userscript_frame_scope_requested,
                        frameScopeLabel(script.declaredFrameScope),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onFrameScope,
                    enabled = enabled && script.declaredFrameScope != ToppingFrameScope.Top,
                    modifier = Modifier.testTag(UserscriptManagementTestTags.frameScope(script.id)),
                ) {
                    Text(
                        stringResource(
                            R.string.userscript_frame_scope_allowed,
                            frameScopeLabel(script.allowedFrameScope),
                        ),
                    )
                }
            }
            Switch(
                checked = script.enabled,
                onCheckedChange = onToggle,
                enabled = enabled,
                modifier = Modifier
                    .testTag(UserscriptManagementTestTags.toggle(script.id))
                    .semantics { contentDescription = script.name },
            )
            IconButton(
                onClick = onEdit,
                enabled = enabled,
                modifier = Modifier.testTag(UserscriptManagementTestTags.edit(script.id)),
            ) {
                Icon(
                    VolaIcons.Edit,
                    contentDescription = stringResource(
                        R.string.userscript_edit_description,
                        script.name,
                    ),
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.testTag(UserscriptManagementTestTags.delete(script.id)),
            ) {
                Icon(
                    VolaIcons.Delete,
                    contentDescription = stringResource(
                        R.string.userscript_delete_description,
                        script.name,
                    ),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun UserscriptEditorDialog(
    initialSource: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (source: String, onResult: (String?) -> Unit) -> Unit,
) {
    var source by remember(initialSource) { mutableStateOf(initialSource) }
    var validationError by remember(initialSource) { mutableStateOf<String?>(null) }
    var saving by remember(initialSource) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(UserscriptManagementTestTags.Editor),
        title = {
            Text(
                stringResource(
                    if (isNew) R.string.userscript_editor_add_title
                    else R.string.userscript_editor_edit_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.userscript_editor_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = {
                        source = it
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UserscriptManagementTestTags.EditorSource),
                    label = { Text(stringResource(R.string.userscript_source_label)) },
                    supportingText = validationError?.let { error ->
                        { Text(error) }
                    },
                    isError = validationError != null,
                    minLines = 8,
                    maxLines = 16,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saving = true
                    onSave(source) { error ->
                        saving = false
                        validationError = error
                    }
                },
                enabled = source.isNotBlank() && !saving,
                modifier = Modifier.testTag(UserscriptManagementTestTags.EditorSave),
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

private fun userscriptPatternSummary(patterns: List<String>): String = when {
    patterns.isEmpty() -> "—"
    patterns.size <= 2 -> patterns.joinToString(separator = "\n")
    else -> patterns.take(2).joinToString(separator = "\n") + "  +${patterns.size - 2}"
}

@Composable
internal fun frameScopeLabel(scope: ToppingFrameScope): String = stringResource(
    when (scope) {
        ToppingFrameScope.Top -> R.string.userscript_frame_scope_top
        ToppingFrameScope.SameOrigin -> R.string.userscript_frame_scope_same_origin
        ToppingFrameScope.AllMatching -> R.string.userscript_frame_scope_all_matching
    },
)

internal fun userscriptTemplate(name: String): String = """
    // ==UserScript==
    // @name $name
    // @match https://example.com/*
    // @candy-frames top
    // @run-at document-end
    // ==/UserScript==

    (() => {
      'use strict';

    })();
""".trimIndent()
