package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadManagerApp
import dev.sk2andy.materialbrowser.data.BrowserDownloadSettings
import dev.sk2andy.materialbrowser.data.DownloadDirectoryRules
import dev.sk2andy.materialbrowser.data.DownloadManagerMode
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardLinkRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

/** Who downloads: the built-in downloader, a question every time, or one installed app. */
internal data class DownloadManagerChoice(
    val mode: DownloadManagerMode,
    val managerId: String? = null,
)

internal object DownloadSettingsRules {
    /** The built-in downloader and the question first, then every installed manager. */
    fun choices(externalManagers: List<ExternalDownloadManagerApp>): List<DownloadManagerChoice> =
        listOf(
            DownloadManagerChoice(DownloadManagerMode.BuiltIn),
            DownloadManagerChoice(DownloadManagerMode.AskEveryTime),
        ) + externalManagers.map { DownloadManagerChoice(DownloadManagerMode.External, it.id) }

    /** A manager id only counts with the external mode; the other modes keep none. */
    fun selected(settings: BrowserDownloadSettings): DownloadManagerChoice = DownloadManagerChoice(
        mode = settings.managerMode,
        managerId = settings.externalManagerId
            .takeIf { settings.managerMode == DownloadManagerMode.External },
    )

    fun apply(settings: BrowserDownloadSettings, choice: DownloadManagerChoice) =
        settings.copy(managerMode = choice.mode, externalManagerId = choice.managerId)

    /** 1DM can take the page's cookies only when it may be the one that downloads. */
    fun oneDmRelevant(
        settings: BrowserDownloadSettings,
        externalManagers: List<ExternalDownloadManagerApp>,
    ): Boolean = when (settings.managerMode) {
        DownloadManagerMode.BuiltIn -> false
        DownloadManagerMode.AskEveryTime -> externalManagers.any(ExternalDownloadManagerApp::isOneDm)
        DownloadManagerMode.External ->
            externalManagers.firstOrNull { it.id == settings.externalManagerId }?.isOneDm == true
    }
}

/** Download settings on cards (board W-Settings): who downloads, then where the files go. */
@Composable
internal fun DownloadsSettingsPage(
    settings: BrowserDownloadSettings,
    externalManagers: List<ExternalDownloadManagerApp>,
    onSettingsChanged: (BrowserDownloadSettings) -> Unit,
    onBack: () -> Unit,
) {
    var directoryDialogVisible by remember { mutableStateOf(false) }
    var directoryDraft by remember { mutableStateOf("") }
    val cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val oneDmRelevant = DownloadSettingsRules.oneDmRelevant(settings, externalManagers)
    SettingsPage(
        title = stringResource(R.string.settings_downloads_title),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            SettingsCard(containerColor = cardColor) {
                SettingsCardDropdownRow(
                    title = stringResource(R.string.settings_download_manager_title),
                    selected = DownloadSettingsRules.selected(settings),
                    options = DownloadSettingsRules.choices(externalManagers),
                    label = { choice ->
                        settings.copy(
                            managerMode = choice.mode,
                            externalManagerId = choice.managerId,
                        ).displayName(externalManagers)
                    },
                    summary = stringResource(R.string.settings_download_no_external_managers)
                        .takeIf { externalManagers.isEmpty() },
                    dividerColor = dividerColor,
                    divider = oneDmRelevant,
                    onSelected = { choice ->
                        onSettingsChanged(DownloadSettingsRules.apply(settings, choice))
                    },
                )
                if (oneDmRelevant) {
                    SettingsCardSwitchRow(
                        title = stringResource(R.string.settings_download_one_dm_session_title),
                        summary = stringResource(R.string.settings_download_one_dm_session_summary),
                        checked = settings.shareSessionDataWithOneDm,
                        summaryMaxLines = Int.MAX_VALUE,
                        dividerColor = dividerColor,
                        onCheckedChange = {
                            onSettingsChanged(settings.copy(shareSessionDataWithOneDm = it))
                        },
                    )
                }
            }
            if (settings.managerMode == DownloadManagerMode.BuiltIn) {
                SettingsCard(containerColor = cardColor) {
                    SettingsCardLinkRow(
                        title = stringResource(R.string.settings_download_folder_title),
                        summary = settings.downloadSubdirectory?.let { relativePath ->
                            stringResource(R.string.settings_download_folder_path, relativePath)
                        } ?: stringResource(R.string.settings_download_folder_default),
                        dividerColor = dividerColor,
                        onClick = {
                            directoryDraft = settings.downloadSubdirectory.orEmpty()
                            directoryDialogVisible = true
                        },
                        modifier = Modifier.testTag(DownloadSettingsTestTags.Directory),
                    )
                }
                Column(modifier = Modifier.padding(SettingsCardTokens.headerPadding)) {
                    Text(
                        stringResource(R.string.settings_download_folder_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (settings.downloadSubdirectory != null) {
                        TextButton(
                            onClick = {
                                onSettingsChanged(settings.copy(downloadSubdirectory = null))
                            },
                            modifier = Modifier.testTag(DownloadSettingsTestTags.DirectoryReset),
                        ) {
                            Text(stringResource(R.string.settings_download_folder_reset))
                        }
                    }
                }
            }
        }
    }
    if (directoryDialogVisible) {
        val validDirectory = DownloadDirectoryRules.isValidSubdirectoryInput(directoryDraft)
        AlertDialog(
            onDismissRequest = { directoryDialogVisible = false },
            modifier = Modifier.testTag(DownloadSettingsTestTags.DirectoryDialog),
            title = { Text(stringResource(R.string.settings_download_folder_title)) },
            text = {
                OutlinedTextField(
                    value = directoryDraft,
                    onValueChange = { directoryDraft = it },
                    modifier = Modifier.testTag(DownloadSettingsTestTags.DirectoryInput),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_download_folder_input)) },
                    isError = !validDirectory,
                    supportingText = if (validDirectory) {
                        null
                    } else {
                        { Text(stringResource(R.string.settings_download_folder_invalid)) }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        directoryDialogVisible = false
                        onSettingsChanged(
                            settings.copy(
                                downloadSubdirectory = DownloadDirectoryRules
                                    .normalizedSubdirectory(directoryDraft),
                            ),
                        )
                    },
                    enabled = validDirectory,
                    modifier = Modifier.testTag(DownloadSettingsTestTags.DirectoryConfirm),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { directoryDialogVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

internal object DownloadSettingsTestTags {
    const val Directory = "download_settings_directory"
    const val DirectoryReset = "download_settings_directory_reset"
    const val DirectoryDialog = "download_settings_directory_dialog"
    const val DirectoryInput = "download_settings_directory_input"
    const val DirectoryConfirm = "download_settings_directory_confirm"
}
