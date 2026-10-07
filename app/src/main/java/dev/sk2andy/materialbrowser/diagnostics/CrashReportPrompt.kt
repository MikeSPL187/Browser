package dev.sk2andy.materialbrowser.diagnostics

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
