@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding
import dev.sk2andy.materialbrowser.browser.downloads.PendingDownloadSafety
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons

internal object DownloadSafetyTestTags {
    const val Sheet = "download_safety_sheet"
    const val Cancel = "download_safety_cancel"
    const val Download = "download_safety_download"
}

/**
 * «Check the file before saving» (board W-DownloadCheck): what [DownloadSafetyFinding]s found
 * about a download, with «Don't download» as the main action. Dismissing the sheet cancels.
 */
@Composable
internal fun DownloadSafetySheet(
    pending: PendingDownloadSafety,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    // V28, B9c: under v5 on a phone the one v5 prompt sheet; not downloading is the suggested
    // answer, downloading reads as the risky one, and closing the sheet cancels.
    DownloadSafetySheetV5(pending, onDownload, onCancel)
}

@Composable
private fun DownloadSafetySheetV5(pending: PendingDownloadSafety, onDownload: () -> Unit, onCancel: () -> Unit) {
    val source = pending.sourceHost?.let { host ->
        stringResource(R.string.download_safety_source, pending.fileName, host)
    } ?: pending.fileName
    V5PromptTheme {
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.download_safety_title),
            message = "$source\n\n${stringResource(R.string.download_safety_local_note)}",
            actions = listOf(
                V5PromptAction(
                    stringResource(R.string.download_safety_cancel),
                    onCancel,
                    V5PromptRole.Primary,
                    DownloadSafetyTestTags.Cancel,
                ),
                V5PromptAction(
                    stringResource(R.string.download_safety_download),
                    onDownload,
                    V5PromptRole.Destructive,
                    DownloadSafetyTestTags.Download,
                ),
            ),
            onDismiss = onCancel,
            testTag = DownloadSafetyTestTags.Sheet,
        ) {
            V5ListGroup {
                pending.findings.forEach { finding ->
                    val (icon, title, detail) = finding.describe()
                    V5ListRow(
                        title = stringResource(title),
                        subtitle = stringResource(detail),
                        icon = V5RowIcon(icon, if (finding.isWarning()) V5Tile.Orange else V5Tile.Gray),
                    )
                }
            }
        }
    }
}

/** A finding's glyph, title and explanation. */
private fun DownloadSafetyFinding.describe(): Triple<ImageVector, Int, Int> = when (this) {
    DownloadSafetyFinding.DisguisedName -> Triple(
        VolaIcons.WarningFilled,
        R.string.download_safety_disguised_title,
        R.string.download_safety_disguised_detail,
    )
    DownloadSafetyFinding.TypeMismatch -> Triple(
        VolaIcons.Error,
        R.string.download_safety_mismatch_title,
        R.string.download_safety_mismatch_detail,
    )
    DownloadSafetyFinding.AndroidApp -> Triple(
        VolaIcons.Apps,
        R.string.download_safety_app_title,
        R.string.download_safety_app_detail,
    )
    DownloadSafetyFinding.DesktopProgram -> Triple(
        VolaIcons.DesktopWindows,
        R.string.download_safety_desktop_title,
        R.string.download_safety_desktop_detail,
    )
    DownloadSafetyFinding.InsecureSource -> Triple(
        VolaIcons.LockOpen,
        R.string.download_safety_insecure_title,
        R.string.download_safety_insecure_detail,
    )
}

/** Findings that can harm the phone itself get the warning color; the rest stay neutral. */
private fun DownloadSafetyFinding.isWarning(): Boolean = when (this) {
    DownloadSafetyFinding.DisguisedName,
    DownloadSafetyFinding.TypeMismatch,
    DownloadSafetyFinding.AndroidApp,
    -> true
    DownloadSafetyFinding.DesktopProgram,
    DownloadSafetyFinding.InsecureSource,
    -> false
}
