package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppDataArchiveCompatibility
import dev.sk2andy.materialbrowser.data.StagedAppDataArchive

internal data class AppDataImportPreview(
    val staged: StagedAppDataArchive,
    val compatibility: AppDataArchiveCompatibility,
)

@Composable
internal fun AppDataExportWarningDialog(
    hasProtectedProfiles: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val message = stringResource(
        if (hasProtectedProfiles) {
            R.string.data_archive_export_protected_warning_message
        } else {
            R.string.data_archive_export_warning_message
        },
    )
    SettingsPromptV5(
        title = stringResource(R.string.data_archive_export_warning_title),
        message = message,
        confirm = stringResource(R.string.data_archive_export_action),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun AppDataImportConfirmationDialog(
    pending: AppDataImportPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val hasVersionMismatch = pending.compatibility != AppDataArchiveCompatibility.Same
    SettingsPromptV5(
        title = stringResource(R.string.data_archive_import_confirm_title),
        message = stringResource(
            R.string.data_archive_import_confirm_message,
            pending.staged.inspection.manifest.appVersionName,
            pending.staged.inspection.entries.count { entry -> !entry.isDirectory },
        ),
        confirm = stringResource(
            if (hasVersionMismatch) {
                R.string.data_archive_import_mismatch_action
            } else {
                R.string.data_archive_import_action
            },
        ),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        warning = if (hasVersionMismatch) stringResource(R.string.data_archive_import_version_warning) else null,
    )
}
