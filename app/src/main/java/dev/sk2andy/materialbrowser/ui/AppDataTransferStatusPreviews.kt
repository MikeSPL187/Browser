package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/** Exporting: the export glyph, what is happening and a spinner. */
@VolaPreviews
@Composable
private fun AppDataTransferExportV5Preview() {
    AppDataTransferStatusV5(AppDataTransferStatusState(R.string.data_archive_export_progress, export = true))
}

/** Restoring. */
@VolaPreviews
@Composable
private fun AppDataTransferRestoreV5Preview() {
    AppDataTransferStatusV5(AppDataTransferStatusState(R.string.data_archive_restore_progress, export = false))
}

/** An interrupted import could not be recovered: a red mark and what to do. */
@VolaPreviews
@Composable
private fun AppDataTransferRecoveryFailedV5Preview() {
    AppDataTransferStatusV5(
        AppDataTransferStatusState(R.string.data_archive_recovery_failed, export = false, failed = true),
    )
}
