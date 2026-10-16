package dev.sk2andy.materialbrowser.diagnostics

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.ui.rememberDrawsV5
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * After Vola closed with an error, offers the stored reports to copy or share. The scan of
 * Android's exit history runs once per launch, off the main thread.
 */
@Composable
internal fun CrashReportPrompt(visible: Boolean) {
    val context = LocalContext.current
    val journal = remember(context) { CrashJournal(context) }
    val scope = rememberCoroutineScope()
    var reportText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(journal) {
        reportText = withContext(Dispatchers.IO) {
            journal.recordProcessExits()
            if (journal.hasUnseenFatalReport()) journal.combinedText() else null
        }
    }

    val text = reportText
    if (!visible || text == null) return
    val close = {
        reportText = null
        scope.launch(Dispatchers.IO) { journal.markSeen() }
        Unit
    }
    CrashReportDialog(
        onCopy = {
            copyReport(context, text)
            close()
        },
        onShare = {
            shareReport(context, text)
            close()
        },
        onDismiss = close,
    )
}

@Composable
internal fun CrashReportDialog(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    // V28, B14a: under v5 on a phone the question is the one v5 prompt sheet.
    if (rememberDrawsV5()) {
        VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < DARK_LUMINANCE) {
            V5PromptSheet(
                visible = true,
                title = stringResource(R.string.crash_report_title),
                message = stringResource(R.string.crash_report_message),
                actions = listOf(
                    V5PromptAction(stringResource(R.string.action_share), onShare, V5PromptRole.Primary),
                    V5PromptAction(stringResource(R.string.crash_report_copy), onCopy),
                ),
                onDismiss = onDismiss,
            )
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.crash_report_title)) },
        text = {
            Column {
                Text(stringResource(R.string.crash_report_message))
                TextButton(onClick = onCopy) {
                    Text(stringResource(R.string.crash_report_copy))
                }
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.crash_report_close))
            }
        },
    )
}

private const val DARK_LUMINANCE = 0.5f

private fun copyReport(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.crash_report_title), text))
    // Android 13+ confirms a copy itself.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.crash_report_copied, Toast.LENGTH_SHORT).show()
    }
}

private fun shareReport(context: Context, text: String) {
    val target = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_report_title))
        .putExtra(Intent.EXTRA_TEXT, text)
    try {
        context.startActivity(Intent.createChooser(target, null))
    } catch (_: ActivityNotFoundException) {
        copyReport(context, text)
    }
}

@Preview(name = "Crash report · light")
@Composable
private fun CrashReportDialogLightPreview() {
    MaterialBrowserTheme {
        CrashReportDialog(onCopy = {}, onShare = {}, onDismiss = {})
    }
}

@Preview(name = "Crash report · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CrashReportDialogDarkPreview() {
    MaterialBrowserTheme {
        CrashReportDialog(onCopy = {}, onShare = {}, onDismiss = {})
    }
}
