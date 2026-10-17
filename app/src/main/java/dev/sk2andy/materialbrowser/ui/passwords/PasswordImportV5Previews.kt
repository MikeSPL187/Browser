package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/* Bringing passwords in under v5 (canvas «ц4»): the sources, Chrome's steps with a problem, the result. */

@Composable
private fun PreviewScheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

@Preview(name = "Import v5 · sources", heightDp = 760)
@Composable
private fun PasswordImportV5Preview() = PreviewScheme(dark = false) {
    PasswordImportV5(onSource = {}, onPickFile = {}, onBack = {})
}

@Preview(name = "Import v5 · Chrome, not an export, dark", heightDp = 760)
@Composable
private fun PasswordImportGuideV5DarkPreview() = PreviewScheme(dark = true) {
    PasswordImportGuideV5(
        source = PasswordImportSource.Chrome,
        problem = PasswordImportProblem.NotAnExport,
        busy = false,
        onPick = {},
        onBack = {},
    )
}

@Preview(name = "Import v5 · result", heightDp = 900)
@Composable
private fun PasswordImportResultV5Preview() = PreviewScheme(dark = false) {
    PasswordImportResultV5(
        report = PasswordImportReport(
            fileName = "Chrome Passwords.csv",
            fileSizeBytes = 18_432,
            added = 42,
            duplicates = 3,
            skipped = 1,
            withTotp = 2,
            addedIds = emptyList(),
            bookmarks = 120,
        ),
        fileState = PasswordImportFileState.Present,
        healthIssues = 4,
        onDeleteFile = {},
        onCheck = {},
        onDone = {},
        conflictState = PasswordImportConflictState.Offered,
        onUseFilePasswords = {},
    )
}
