package dev.sk2andy.materialbrowser.ui.passwords

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.V5PromptTheme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5PageBar
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5RowTrailing
import dev.sk2andy.materialbrowser.ui.v5.V5StateAction
import dev.sk2andy.materialbrowser.ui.v5.V5StatePrimaryAction

/*
 * Bringing passwords in under v5 (canvas «ц4», approved 17.10): where they come from, how to get
 * the export, and what came in. Every source ends in the system file picker; nothing is fetched
 * and nothing leaves the phone. Same callbacks and test tags as the current screens; the tablet
 * keeps those.
 */

/** Where the passwords come from, or a file already on the phone. */
@Composable
internal fun PasswordImportV5(
    onSource: (PasswordImportSource) -> Unit,
    onPickFile: () -> Unit,
    onBack: () -> Unit,
) {
    val sources = listOf(
        Triple(PasswordImportSource.Chrome, V5RowIcon(VolaIcons.Language, V5Tile.Blue), R.string.passwords_import_chrome_detail),
        Triple(PasswordImportSource.Firefox, V5RowIcon(VolaIcons.Public, V5Tile.Orange), R.string.passwords_import_firefox_detail),
        Triple(PasswordImportSource.Samsung, V5RowIcon(VolaIcons.TravelExplore, V5Tile.Indigo), R.string.passwords_import_samsung_detail),
        Triple(PasswordImportSource.Managers, V5RowIcon(VolaIcons.ShieldLock, V5Tile.Gray), R.string.passwords_import_managers_detail),
    )
    ImportPageV5(title = stringResource(R.string.passwords_import_title), onBack = onBack, tag = PasswordImportTestTags.Sources) {
        Text(
            stringResource(R.string.passwords_import_intro),
            style = V5Theme.type.footnote,
            color = V5Theme.colors.labelSecondary,
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
        )
        V5ListGroup(header = stringResource(R.string.passwords_import_from)) {
            sources.forEach { (source, icon, detail) ->
                V5ListRow(
                    title = stringResource(source.label()),
                    subtitle = stringResource(detail),
                    icon = icon,
                    trailing = V5RowTrailing.Chevron,
                    onClick = { onSource(source) },
                    modifier = Modifier.testTag(PasswordImportTestTags.source(source)),
                )
            }
        }
        V5ListGroup(footer = stringResource(R.string.passwords_import_private)) {
            V5ListRow(
                title = stringResource(R.string.passwords_import_file),
                icon = V5RowIcon(VolaIcons.UploadFile),
                titleColor = V5Theme.colors.accent,
                onClick = onPickFile,
                modifier = Modifier.testTag(PasswordImportTestTags.PickFile),
            )
        }
    }
}

/** How to get the export file, numbered, then «Choose file»; a file that brought nothing says why. */
@Composable
internal fun PasswordImportGuideV5(
    source: PasswordImportSource,
    problem: PasswordImportProblem?,
    busy: Boolean,
    onPick: () -> Unit,
    onBack: () -> Unit,
) {
    ImportPageV5(title = stringResource(source.title()), onBack = onBack, tag = PasswordImportTestTags.Guide) {
        V5ListGroup {
            source.steps().forEachIndexed { index, step ->
                V5ListRow(
                    title = stringResource(step.title),
                    subtitle = stringResource(step.detail),
                    leading = { StepNumber(index + 1) },
                )
            }
        }
        if (problem != null) {
            V5ListGroup(Modifier.testTag(PasswordImportTestTags.Problem)) {
                V5ListRow(
                    title = stringResource(problem.message()),
                    icon = V5RowIcon(VolaIcons.Error, V5Tile.Red),
                )
            }
        }
        V5StatePrimaryAction(
            V5StateAction(
                stringResource(R.string.passwords_import_pick),
                onPick,
                icon = VolaIcons.UploadFile,
                enabled = !busy,
                testTag = PasswordImportTestTags.PickFile,
            ),
            Modifier.padding(horizontal = V5Spacing.marginPhone),
        )
    }
}

/** What came in, the file to delete (it holds every password in plain text), and the check for what came in. */
@Composable
internal fun PasswordImportResultV5(
    report: PasswordImportReport,
    fileState: PasswordImportFileState,
    healthIssues: Int,
    onDeleteFile: () -> Unit,
    onCheck: () -> Unit,
    onDone: () -> Unit,
    conflictState: PasswordImportConflictState,
    onUseFilePasswords: () -> Unit,
) {
    val context = LocalContext.current
    val colors = V5Theme.colors
    ImportPageV5(title = stringResource(R.string.passwords_import_title), onBack = onDone, tag = PasswordImportTestTags.Result) {
        V5ListGroup {
            val size = Formatter.formatShortFileSize(context, report.fileSizeBytes)
            V5ListRow(
                title = stringResource(R.string.passwords_import_done),
                subtitle = report.fileName?.let { stringResource(R.string.passwords_import_file_line, it, size) } ?: size,
                icon = V5RowIcon(VolaIcons.CheckCircle, V5Tile.Green),
            )
            if (report.hadPasswords) {
                V5ListRow(pluralStringResource(R.plurals.passwords_import_added, report.added, report.added))
                CountRow(R.plurals.passwords_import_with_totp, report.withTotp)
                CountRow(R.plurals.passwords_import_duplicates, report.duplicates)
                CountRow(R.plurals.passwords_import_skipped, report.skipped)
                CountRow(R.plurals.passwords_import_full, report.full)
            }
            if (report.bookmarks > 0 || report.bookmarksSkipped > 0) {
                V5ListRow(pluralStringResource(R.plurals.passwords_import_bookmarks, report.bookmarks, report.bookmarks))
                CountRow(R.plurals.passwords_import_bookmarks_skipped, report.bookmarksSkipped)
                if (report.bookmarksLimitReached) {
                    V5ListRow(stringResource(R.string.passwords_import_bookmarks_limit), titleColor = colors.danger)
                }
            }
        }
        if (report.conflicts.isNotEmpty()) {
            V5ListGroup {
                if (conflictState == PasswordImportConflictState.Replaced) {
                    V5ListRow(stringResource(R.string.passwords_import_replaced), icon = V5RowIcon(VolaIcons.CheckCircle, V5Tile.Green))
                } else {
                    V5ListRow(
                        title = if (conflictState == PasswordImportConflictState.Failed) {
                            stringResource(R.string.passwords_failed)
                        } else {
                            pluralStringResource(R.plurals.passwords_import_conflicts, report.conflicts.size, report.conflicts.size)
                        },
                        icon = V5RowIcon(VolaIcons.WarningFilled, V5Tile.Orange),
                    )
                    V5ListRow(
                        title = stringResource(R.string.passwords_import_use_file_passwords),
                        titleColor = colors.accent,
                        enabled = conflictState != PasswordImportConflictState.Replacing,
                        onClick = onUseFilePasswords,
                        modifier = Modifier.testTag(PasswordImportTestTags.UseFilePasswords),
                    )
                }
            }
        }
        if (report.hadPasswords) {
            V5ListGroup {
                when (fileState) {
                    PasswordImportFileState.Deleted ->
                        V5ListRow(stringResource(R.string.passwords_import_deleted), icon = V5RowIcon(VolaIcons.CheckCircle, V5Tile.Green))
                    PasswordImportFileState.DeleteFailed ->
                        V5ListRow(stringResource(R.string.passwords_import_delete_failed), icon = V5RowIcon(VolaIcons.WarningFilled, V5Tile.Orange))
                    PasswordImportFileState.Present, PasswordImportFileState.Deleting -> {
                        V5ListRow(stringResource(R.string.passwords_import_delete_warning), icon = V5RowIcon(VolaIcons.WarningFilled, V5Tile.Orange))
                        V5ListRow(
                            title = stringResource(R.string.passwords_import_delete),
                            titleColor = colors.danger,
                            enabled = fileState == PasswordImportFileState.Present,
                            onClick = onDeleteFile,
                            modifier = Modifier.testTag(PasswordImportTestTags.DeleteFile),
                        )
                    }
                }
            }
        }
        if (report.added > 0) {
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.passwords_import_check),
                    subtitle = if (healthIssues > 0) {
                        pluralStringResource(R.plurals.passwords_health_entry_issues, healthIssues, healthIssues)
                    } else {
                        stringResource(R.string.passwords_health_entry_fine)
                    },
                    icon = V5RowIcon(VolaIcons.HealthAndSafety, if (healthIssues > 0) V5Tile.Orange else V5Tile.Green),
                    trailing = V5RowTrailing.Chevron,
                    onClick = onCheck,
                    modifier = Modifier.testTag(PasswordImportTestTags.Check),
                )
            }
        }
        V5StatePrimaryAction(
            V5StateAction(
                stringResource(if (report.hadPasswords) R.string.passwords_import_to_passwords else R.string.passwords_import_done),
                onDone,
                testTag = PasswordImportTestTags.ToPasswords,
            ),
            Modifier.padding(horizontal = V5Spacing.marginPhone),
        )
    }
}

@Composable
private fun CountRow(plural: Int, count: Int) {
    if (count == 0) return
    V5ListRow(pluralStringResource(plural, count, count), titleColor = V5Theme.colors.labelSecondary)
}

/** A step's number on a soft accent disc; it grows with the font so a large digit is never cut. */
@Composable
private fun StepNumber(number: Int) {
    Box(
        Modifier
            .sizeIn(minWidth = V5Sizes.rowTile, minHeight = V5Sizes.rowTile)
            .clip(V5Theme.shapes.full)
            .background(V5Theme.colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), style = V5Theme.type.footnote, color = V5Theme.colors.accent)
    }
}

@Composable
private fun ImportPageV5(title: String, onBack: () -> Unit, tag: String, content: @Composable ColumnScope.() -> Unit) {
    V5PromptTheme {
        val colors = V5Theme.colors
        Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
            V5PageBar(title = title, onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = V5Spacing.space3)
                    .testTag(tag),
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
                content = content,
            )
        }
    }
}
