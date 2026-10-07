package dev.sk2andy.materialbrowser.ui.passwords

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.credentials.ImportConflict
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.LibraryCardSlice
import dev.sk2andy.materialbrowser.ui.LibraryRow
import dev.sk2andy.materialbrowser.ui.LibraryRules
import dev.sk2andy.materialbrowser.ui.LibrarySectionLabel
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaPasswords
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

/** Where passwords and bookmarks come from; each but [File] has its own steps (board W-ImportChrome). */
internal enum class PasswordImportSource { Chrome, Firefox, Samsung, Managers, File }

/** Why a picked file brought nothing in. */
internal enum class PasswordImportProblem { NotAnExport, Encrypted, TooLarge, NotText, Unreadable, NoLogins, Failed }

/**
 * What an import did, for the result screen: counts and ids, and only the file's own passwords for
 * logins Vola already had ([conflicts]), kept in memory until the user takes them or leaves.
 */
internal data class PasswordImportReport(
    val fileName: String?,
    val fileSizeBytes: Long,
    val added: Int,
    val duplicates: Int,
    val skipped: Int,
    val withTotp: Int,
    val addedIds: List<String>,
    /** Whether a password export was among the files: only then is there one to delete. */
    val hadPasswords: Boolean = true,
    val bookmarks: Int = 0,
    val bookmarksSkipped: Int = 0,
    val bookmarksLimitReached: Boolean = false,
    /** Logins the file has with another password; the saved ones were kept until the user chooses. */
    val conflicts: List<ImportConflict> = emptyList(),
    /** New logins that did not fit: the vault holds at most 10 000. */
    val full: Int = 0,
)

/** The picked file after the import: still there, being deleted, gone, or refused to go. */
internal enum class PasswordImportFileState { Present, Deleting, Deleted, DeleteFailed }

/** The file's other passwords: offered, being written, taken, or the write failed. */
internal enum class PasswordImportConflictState { Offered, Replacing, Replaced, Failed }

internal object PasswordImportTestTags {
    const val Sources = "password_import_sources"
    const val PickFile = "password_import_pick_file"
    const val Guide = "password_import_guide"
    const val Problem = "password_import_problem"
    const val Result = "password_import_result"
    const val DeleteFile = "password_import_delete_file"
    const val UseFilePasswords = "password_import_use_file_passwords"
    const val ToPasswords = "password_import_to_passwords"
    const val Check = "password_import_check"
    fun source(source: PasswordImportSource) = "password_import_source:${source.name}"
}

private class ImportStep(val title: Int, val detail: Int)

private fun PasswordImportSource.title(): Int = when (this) {
    PasswordImportSource.Chrome -> R.string.passwords_import_chrome_title
    PasswordImportSource.Firefox -> R.string.passwords_import_firefox_title
    PasswordImportSource.Samsung -> R.string.passwords_import_samsung_title
    PasswordImportSource.Managers -> R.string.passwords_import_managers_title
    PasswordImportSource.File -> R.string.passwords_import_file_title
}

private fun PasswordImportSource.steps(): List<ImportStep> {
    val pick = ImportStep(R.string.passwords_import_step_pick, R.string.passwords_import_step_pick_detail)
    return when (this) {
        PasswordImportSource.Chrome -> listOf(
            ImportStep(R.string.passwords_import_chrome_step1, R.string.passwords_import_chrome_step1_detail),
            ImportStep(R.string.passwords_import_chrome_bookmarks, R.string.passwords_import_chrome_bookmarks_detail),
            pick,
        )
        PasswordImportSource.Firefox -> listOf(
            ImportStep(R.string.passwords_import_firefox_step1, R.string.passwords_import_firefox_step1_detail),
            ImportStep(R.string.passwords_import_firefox_bookmarks, R.string.passwords_import_firefox_bookmarks_detail),
            ImportStep(R.string.passwords_import_firefox_step2, R.string.passwords_import_firefox_step2_detail),
            pick,
        )
        PasswordImportSource.Samsung -> listOf(
            ImportStep(R.string.passwords_import_samsung_step1, R.string.passwords_import_samsung_step1_detail),
            pick,
        )
        PasswordImportSource.Managers -> listOf(
            ImportStep(R.string.passwords_import_managers_step1, R.string.passwords_import_managers_step1_detail),
            ImportStep(R.string.passwords_import_step_save, R.string.passwords_import_step_save_detail),
            pick,
        )
        PasswordImportSource.File -> listOf(
            ImportStep(R.string.passwords_import_file_step, R.string.passwords_import_file_step_detail),
        )
    }
}

private fun PasswordImportProblem.message(): Int = when (this) {
    PasswordImportProblem.NotAnExport -> R.string.passwords_import_not_export
    PasswordImportProblem.Encrypted -> R.string.passwords_import_encrypted
    PasswordImportProblem.TooLarge -> R.string.passwords_import_too_large
    PasswordImportProblem.NotText -> R.string.passwords_import_not_text
    PasswordImportProblem.Unreadable -> R.string.passwords_import_unreadable
    PasswordImportProblem.NoLogins -> R.string.passwords_import_no_logins
    PasswordImportProblem.Failed -> R.string.passwords_import_failed
}

/**
 * Board W-Import: where the passwords come from, or a file already on the phone. Every source
 * ends in the system file picker; nothing is fetched and nothing leaves the phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordImportScreen(
    onSource: (PasswordImportSource) -> Unit,
    onPickFile: () -> Unit,
    onBack: () -> Unit,
) {
    val sources = listOf(
        Triple(PasswordImportSource.Chrome, VolaIcons.Language, R.string.passwords_import_chrome_detail),
        Triple(PasswordImportSource.Firefox, VolaIcons.Public, R.string.passwords_import_firefox_detail),
        Triple(PasswordImportSource.Samsung, VolaIcons.TravelExplore, R.string.passwords_import_samsung_detail),
        Triple(PasswordImportSource.Managers, VolaIcons.ShieldLock, R.string.passwords_import_managers_detail),
    )
    ImportScaffold(title = stringResource(R.string.passwords_import_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .testTag(PasswordImportTestTags.Sources),
            contentPadding = PaddingValues(bottom = VolaPasswords.sectionGap),
        ) {
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.passwords_import_intro),
                    modifier = Modifier.padding(horizontal = VolaPasswords.sidePadding),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "from") {
                LibrarySectionLabel(
                    text = stringResource(R.string.passwords_import_from),
                    modifier = Modifier.padding(top = VolaLibrary.sectionGap),
                )
            }
            sources.forEachIndexed { index, (source, icon, detail) ->
                item(key = source.name) {
                    LibraryCardSlice(position = LibraryRules.position(index, sources.size)) {
                        LibraryRow(
                            title = stringResource(source.label()),
                            detail = stringResource(detail),
                            leading = { SourceTile(icon) },
                            onClick = { onSource(source) },
                            modifier = Modifier.testTag(PasswordImportTestTags.source(source)),
                            trailing = {
                                Icon(
                                    VolaIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
            item(key = "file") {
                OutlinedButton(
                    onClick = onPickFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VolaPasswords.sidePadding)
                        .padding(top = VolaPasswords.sectionGap)
                        .height(VolaPasswords.buttonHeight)
                        .testTag(PasswordImportTestTags.PickFile),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(VolaIcons.UploadFile, contentDescription = null)
                    Text(
                        text = stringResource(R.string.passwords_import_file),
                        modifier = Modifier.padding(start = VolaPasswords.buttonIconGap),
                    )
                }
            }
            item(key = "private") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VolaPasswords.sidePadding)
                        .padding(top = VolaPasswords.sectionGap),
                    horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        VolaIcons.ShieldLock,
                        contentDescription = null,
                        modifier = Modifier.size(VolaPasswords.pointIconSize),
                        tint = VolaTheme.extendedColors.ok,
                    )
                    Text(
                        text = stringResource(R.string.passwords_import_private),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun PasswordImportSource.label(): Int = when (this) {
    PasswordImportSource.Chrome -> R.string.passwords_import_chrome
    PasswordImportSource.Firefox -> R.string.passwords_import_firefox
    PasswordImportSource.Samsung -> R.string.passwords_import_samsung
    PasswordImportSource.Managers -> R.string.passwords_import_managers
    PasswordImportSource.File -> R.string.passwords_import_file
}

/** Board W-ImportChrome, first half: how to get the export file, then «Choose file». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordImportGuideScreen(
    source: PasswordImportSource,
    problem: PasswordImportProblem?,
    busy: Boolean,
    onPick: () -> Unit,
    onBack: () -> Unit,
) {
    val steps = source.steps()
    ImportScaffold(title = stringResource(source.title()), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .testTag(PasswordImportTestTags.Guide),
            contentPadding = PaddingValues(bottom = VolaPasswords.sectionGap),
            verticalArrangement = Arrangement.spacedBy(VolaPasswords.sectionGap),
        ) {
            item(key = "steps") {
                ImportCard {
                    Column(verticalArrangement = Arrangement.spacedBy(VolaPasswords.importStepGap)) {
                        steps.forEachIndexed { index, step -> StepRow(number = index + 1, step = step) }
                    }
                }
            }
            if (problem != null) {
                item(key = "problem") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VolaPasswords.sidePadding)
                            .testTag(PasswordImportTestTags.Problem),
                        shape = VolaPasswords.sheetCardShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(VolaPasswords.importCardPadding),
                            horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
                        ) {
                            Icon(
                                VolaIcons.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = stringResource(problem.message()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
            item(key = "pick") {
                Button(
                    onClick = onPick,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VolaPasswords.sidePadding)
                        .height(VolaPasswords.buttonHeight)
                        .testTag(PasswordImportTestTags.PickFile),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(VolaIcons.UploadFile, contentDescription = null)
                    Text(
                        text = stringResource(R.string.passwords_import_pick),
                        modifier = Modifier.padding(start = VolaPasswords.buttonIconGap),
                    )
                }
            }
        }
    }
}

/**
 * Board W-ImportChrome, second half: what came in, the file to delete (it holds every password in
 * plain text), and the password check for what came in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordImportResultScreen(
    report: PasswordImportReport,
    fileState: PasswordImportFileState,
    healthIssues: Int,
    onDeleteFile: () -> Unit,
    onCheck: () -> Unit,
    onDone: () -> Unit,
    conflictState: PasswordImportConflictState = PasswordImportConflictState.Offered,
    onUseFilePasswords: () -> Unit = {},
) {
    val context = LocalContext.current
    ImportScaffold(title = stringResource(R.string.passwords_import_title), onBack = onDone) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .testTag(PasswordImportTestTags.Result),
            contentPadding = PaddingValues(bottom = VolaPasswords.sectionGap),
            verticalArrangement = Arrangement.spacedBy(VolaPasswords.sectionGap),
        ) {
            item(key = "summary") {
                ImportCard {
                    Column(verticalArrangement = Arrangement.spacedBy(VolaPasswords.importLineGap)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(VolaIcons.CheckCircle, contentDescription = null, tint = VolaTheme.extendedColors.ok)
                            Text(
                                text = stringResource(R.string.passwords_import_done),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        val size = Formatter.formatShortFileSize(context, report.fileSizeBytes)
                        Text(
                            text = report.fileName?.let { stringResource(R.string.passwords_import_file_line, it, size) } ?: size,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (report.hadPasswords) {
                            Text(
                                text = pluralStringResource(R.plurals.passwords_import_added, report.added, report.added),
                                modifier = Modifier.padding(top = VolaPasswords.importLineGap),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            CountLine(R.plurals.passwords_import_with_totp, report.withTotp)
                            CountLine(R.plurals.passwords_import_duplicates, report.duplicates)
                            CountLine(R.plurals.passwords_import_skipped, report.skipped)
                            CountLine(R.plurals.passwords_import_full, report.full)
                        }
                        if (report.bookmarks > 0 || report.bookmarksSkipped > 0) {
                            Text(
                                text = pluralStringResource(R.plurals.passwords_import_bookmarks, report.bookmarks, report.bookmarks),
                                modifier = Modifier.padding(top = VolaPasswords.importLineGap),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            CountLine(R.plurals.passwords_import_bookmarks_skipped, report.bookmarksSkipped)
                            if (report.bookmarksLimitReached) {
                                Text(
                                    text = stringResource(R.string.passwords_import_bookmarks_limit),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            if (report.conflicts.isNotEmpty()) {
                item(key = "conflicts") { ConflictCard(report.conflicts.size, conflictState, onUseFilePasswords) }
            }
            if (report.hadPasswords) item(key = "file") { FileCard(fileState, onDeleteFile) }
            if (report.added > 0) {
                item(key = "check") {
                    LibraryCardSlice(position = LibraryRules.position(0, 1)) {
                        LibraryRow(
                            title = stringResource(R.string.passwords_import_check),
                            detail = if (healthIssues > 0) {
                                pluralStringResource(R.plurals.passwords_health_entry_issues, healthIssues, healthIssues)
                            } else {
                                stringResource(R.string.passwords_health_entry_fine)
                            },
                            detailColor = if (healthIssues > 0) MaterialTheme.colorScheme.error else null,
                            leading = { SourceTile(VolaIcons.HealthAndSafety) },
                            onClick = onCheck,
                            modifier = Modifier.testTag(PasswordImportTestTags.Check),
                            trailing = {
                                Icon(
                                    VolaIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
            item(key = "done") {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VolaPasswords.sidePadding)
                        .height(VolaPasswords.buttonHeight)
                        .testTag(PasswordImportTestTags.ToPasswords),
                ) {
                    Text(stringResource(if (report.hadPasswords) R.string.passwords_import_to_passwords else R.string.passwords_import_done))
                }
            }
        }
    }
}

@Composable
private fun CountLine(plural: Int, count: Int) {
    if (count == 0) return
    Text(
        text = pluralStringResource(plural, count, count),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Logins whose password in the file differs: Vola kept its own, and the user can take the file's. */
@Composable
private fun ConflictCard(count: Int, state: PasswordImportConflictState, onUse: () -> Unit) {
    ImportCard {
        if (state == PasswordImportConflictState.Replaced) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(VolaIcons.CheckCircle, contentDescription = null, tint = VolaTheme.extendedColors.ok)
                Text(stringResource(R.string.passwords_import_replaced), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap)) {
                    Icon(VolaIcons.WarningFilled, contentDescription = null, tint = VolaTheme.extendedColors.warn)
                    Text(
                        text = if (state == PasswordImportConflictState.Failed) {
                            stringResource(R.string.passwords_failed)
                        } else {
                            pluralStringResource(R.plurals.passwords_import_conflicts, count, count)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FilledTonalButton(
                    onClick = onUse,
                    enabled = state != PasswordImportConflictState.Replacing,
                    modifier = Modifier.testTag(PasswordImportTestTags.UseFilePasswords),
                ) {
                    Text(stringResource(R.string.passwords_import_use_file_passwords))
                }
            }
        }
    }
}

@Composable
private fun FileCard(state: PasswordImportFileState, onDelete: () -> Unit) {
    ImportCard {
        if (state == PasswordImportFileState.Deleted) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(VolaIcons.CheckCircle, contentDescription = null, tint = VolaTheme.extendedColors.ok)
                Text(stringResource(R.string.passwords_import_deleted), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap)) {
                    Icon(VolaIcons.WarningFilled, contentDescription = null, tint = VolaTheme.extendedColors.warn)
                    Text(
                        text = stringResource(
                            if (state == PasswordImportFileState.DeleteFailed) {
                                R.string.passwords_import_delete_failed
                            } else {
                                R.string.passwords_import_delete_warning
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state != PasswordImportFileState.DeleteFailed) {
                    FilledTonalButton(
                        onClick = onDelete,
                        enabled = state == PasswordImportFileState.Present,
                        modifier = Modifier.testTag(PasswordImportTestTags.DeleteFile),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(VolaIcons.Delete, contentDescription = null)
                        Text(
                            text = stringResource(R.string.passwords_import_delete),
                            modifier = Modifier.padding(start = VolaPasswords.buttonIconGap),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, step: ImportStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(VolaPasswords.importStepGap)) {
        Box(
            modifier = Modifier
                .size(VolaPasswords.importStepBadge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(VolaPasswords.fieldLabelGap)) {
            Text(stringResource(step.title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(step.detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(VolaLibrary.tileSize)
            .clip(VolaLibrary.tileShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ImportCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaPasswords.sidePadding),
        shape = VolaPasswords.sheetCardShape,
        color = VolaTheme.extendedColors.card,
    ) {
        Box(modifier = Modifier.padding(VolaPasswords.importCardPadding)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        content = content,
    )
}

/** Board W-Import. */
@VolaPreviews
@Composable
private fun PasswordImportPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PasswordImportScreen(onSource = {}, onPickFile = {}, onBack = {})
    }
}

/** Board W-ImportChrome: the steps, after a file that was not an export. */
@VolaPreviews
@Composable
private fun PasswordImportGuidePreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PasswordImportGuideScreen(
            source = PasswordImportSource.Chrome,
            problem = PasswordImportProblem.NotAnExport,
            busy = false,
            onPick = {},
            onBack = {},
        )
    }
}

/** Board W-ImportChrome: done, with the file still to delete. */
@VolaPreviews
@Composable
private fun PasswordImportResultPreview() {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PasswordImportResultScreen(
            report = PasswordImportReport(
                fileName = "Chrome Passwords.csv",
                fileSizeBytes = 38_912,
                added = 126,
                duplicates = 3,
                skipped = 2,
                withTotp = 4,
                addedIds = emptyList(),
                bookmarks = 214,
                bookmarksSkipped = 5,
                conflicts = listOf(ImportConflict("1", "preview"), ImportConflict("2", "preview")),
            ),
            fileState = PasswordImportFileState.Present,
            healthIssues = 8,
            onDeleteFile = {},
            onCheck = {},
            onDone = {},
        )
    }
}
