@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.downloads.DownloadSafetyFinding
import dev.sk2andy.materialbrowser.browser.downloads.PendingDownloadSafety
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaDownloadCheck
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

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
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        DownloadSafetyContent(
            fileName = pending.fileName,
            sourceHost = pending.sourceHost,
            findings = pending.findings,
            onDownload = onDownload,
            onCancel = onCancel,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
internal fun DownloadSafetyContent(
    fileName: String,
    sourceHost: String?,
    findings: List<DownloadSafetyFinding>,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DownloadSafetyTestTags.Sheet)
            .padding(
                start = VolaDownloadCheck.sidePadding,
                end = VolaDownloadCheck.sidePadding,
                bottom = VolaDownloadCheck.bottomPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(VolaDownloadCheck.sectionGap),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(VolaDownloadCheck.sectionGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaDownloadCheck.headerGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FindingIcon(icon = VolaIcons.GppMaybe, warning = true)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.download_safety_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = if (sourceHost != null) {
                            stringResource(R.string.download_safety_source, fileName, sourceHost)
                        } else {
                            fileName
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                shape = VolaDownloadCheck.cardShape,
                color = VolaTheme.extendedColors.card,
            ) {
                Column {
                    findings.forEach { finding -> FindingRow(finding) }
                }
            }
            Text(
                text = stringResource(R.string.download_safety_local_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VolaDownloadCheck.buttonGap)) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = VolaDownloadCheck.buttonHeight)
                    .testTag(DownloadSafetyTestTags.Cancel),
                shape = CircleShape,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = VolaIcons.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = ButtonDefaults.IconSpacing)
                        .size(VolaDownloadCheck.buttonIconSize),
                )
                Text(stringResource(R.string.download_safety_cancel), maxLines = 1)
            }
            OutlinedButton(
                onClick = onDownload,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = VolaDownloadCheck.buttonHeight)
                    .testTag(DownloadSafetyTestTags.Download),
                shape = CircleShape,
            ) {
                Text(stringResource(R.string.download_safety_download), maxLines = 1)
            }
        }
    }
}

@Composable
private fun FindingRow(finding: DownloadSafetyFinding) {
    val (icon, title, detail) = when (finding) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(VolaDownloadCheck.rowPadding),
        horizontalArrangement = Arrangement.spacedBy(VolaDownloadCheck.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FindingIcon(icon = icon, warning = finding.isWarning())
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

@Composable
private fun FindingIcon(icon: ImageVector, warning: Boolean) {
    val extended = VolaTheme.extendedColors
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(VolaDownloadCheck.iconContainerSize)
            .background(
                color = if (warning) extended.warnContainer else colors.surfaceContainerHigh,
                shape = VolaDownloadCheck.iconContainerShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(VolaDownloadCheck.iconSize),
            tint = if (warning) extended.onWarnContainer else colors.onSurfaceVariant,
        )
    }
}

@VolaPreviews
@Composable
private fun DownloadSafetyContentPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            DownloadSafetyContent(
                fileName = "invoice.pdf.apk",
                sourceHost = "files.example.net",
                findings = listOf(
                    DownloadSafetyFinding.DisguisedName,
                    DownloadSafetyFinding.AndroidApp,
                    DownloadSafetyFinding.InsecureSource,
                ),
                onDownload = {},
                onCancel = {},
            )
        }
    }
}
