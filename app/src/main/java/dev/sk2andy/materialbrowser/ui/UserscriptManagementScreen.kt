package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.topping.ToppingFrameScope
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
