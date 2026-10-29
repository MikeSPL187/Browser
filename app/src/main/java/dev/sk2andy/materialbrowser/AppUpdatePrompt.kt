package dev.sk2andy.materialbrowser

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.browser.actions.BrowserDownloadManager
import dev.sk2andy.materialbrowser.browser.actions.DownloadActionResult
import dev.sk2andy.materialbrowser.data.BrowserDownloadRequest
import dev.sk2andy.materialbrowser.update.AppReleaseChannel
import dev.sk2andy.materialbrowser.update.AvailableAppUpdate
import dev.sk2andy.materialbrowser.update.GitHubAppUpdateChecker

@Composable
internal fun AppUpdatePrompt(
    context: Context,
    visible: Boolean,
    onOpenReleaseNotes: (String) -> Boolean,
) {
    var updateCheckCompleted by rememberSaveable { mutableStateOf(false) }
    var availableUpdateVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var availableUpdateUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var availableUpdateFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    val updateChecker = remember { GitHubAppUpdateChecker() }
    val downloadManager = remember { BrowserDownloadManager(context) }
    val availableUpdate = availableUpdateVersion?.let { version ->
        val url = availableUpdateUrl ?: return@let null
        val fileName = availableUpdateFileName ?: return@let null
        AvailableAppUpdate(version, url, fileName)
    }

    LaunchedEffect(updateCheckCompleted) {
        if (updateCheckCompleted) return@LaunchedEffect
        if (BuildConfig.ENABLE_GITHUB_UPDATES) {
            val releaseChannel = AppReleaseChannel.forBuild(
                systemWebViewOnly = BuildConfig.SYSTEM_WEBVIEW_ONLY,
                trustsUserCertificates = BuildConfig.TRUST_USER_CERTIFICATES,
            )
            updateChecker.findAvailableUpdate(
                currentVersionName = BuildConfig.VERSION_NAME,
                channel = releaseChannel,
            )?.let { update ->
                availableUpdateVersion = update.versionName
                availableUpdateUrl = update.downloadUrl
                availableUpdateFileName = update.fileName
            }
        }
        updateCheckCompleted = true
    }

    if (availableUpdate != null && !dismissed && visible) {
        AppUpdateDialog(
            update = availableUpdate,
            onDismiss = { dismissed = true },
            onOpenReleaseNotes = {
                if (onOpenReleaseNotes(availableUpdate.releaseNotesUrl)) dismissed = true
            },
            onDownload = {
                val result = downloadManager.enqueue(
                    BrowserDownloadRequest(
                        url = availableUpdate.downloadUrl,
                        fileName = availableUpdate.fileName,
                        mimeType = AvailableAppUpdate.APK_MIME_TYPE,
                    ),
                )
                Toast.makeText(
                    context,
                    when (result) {
                        is DownloadActionResult.Enqueued -> context.getString(
                            R.string.toast_download_started,
                            result.fileName,
                        )
                        is DownloadActionResult.HandedOff -> context.getString(
                            R.string.toast_download_handed_off,
                            result.appName,
                        )
                        is DownloadActionResult.Failed -> result.message
                    },
                    Toast.LENGTH_SHORT,
                ).show()
                if (result is DownloadActionResult.Enqueued) dismissed = true
            },
        )
    }
}

@Composable
internal fun AppUpdateDialog(
    update: AvailableAppUpdate,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onOpenReleaseNotes: () -> Unit,
) {
    // V28, B14a: under v5 on a phone the question is the one v5 prompt sheet; «Close» is «Later».
    VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < UPDATE_DARK_LUMINANCE) {
        V5PromptSheet(
            visible = true,
            title = stringResource(R.string.update_available_title),
            message = stringResource(R.string.update_available_message, update.versionName),
            actions = listOf(
                V5PromptAction(stringResource(R.string.action_download_update), onDownload, V5PromptRole.Primary),
                V5PromptAction(stringResource(R.string.action_view_release_notes), onOpenReleaseNotes),
            ),
            onDismiss = onDismiss,
        )
    }
}

private const val UPDATE_DARK_LUMINANCE = 0.5f
