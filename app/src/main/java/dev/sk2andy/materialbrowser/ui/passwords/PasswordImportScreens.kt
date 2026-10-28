package dev.sk2andy.materialbrowser.ui.passwords

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.credentials.ImportConflict
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

internal class ImportStep(val title: Int, val detail: Int)

internal fun PasswordImportSource.title(): Int = when (this) {
    PasswordImportSource.Chrome -> R.string.passwords_import_chrome_title
    PasswordImportSource.Firefox -> R.string.passwords_import_firefox_title
    PasswordImportSource.Samsung -> R.string.passwords_import_samsung_title
    PasswordImportSource.Managers -> R.string.passwords_import_managers_title
    PasswordImportSource.File -> R.string.passwords_import_file_title
}

internal fun PasswordImportSource.steps(): List<ImportStep> {
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

internal fun PasswordImportProblem.message(): Int = when (this) {
    PasswordImportProblem.NotAnExport -> R.string.passwords_import_not_export
    PasswordImportProblem.Encrypted -> R.string.passwords_import_encrypted
    PasswordImportProblem.TooLarge -> R.string.passwords_import_too_large
    PasswordImportProblem.NotText -> R.string.passwords_import_not_text
    PasswordImportProblem.Unreadable -> R.string.passwords_import_unreadable
    PasswordImportProblem.NoLogins -> R.string.passwords_import_no_logins
    PasswordImportProblem.Failed -> R.string.passwords_import_failed
}

internal fun PasswordImportSource.label(): Int = when (this) {
    PasswordImportSource.Chrome -> R.string.passwords_import_chrome
    PasswordImportSource.Firefox -> R.string.passwords_import_firefox
    PasswordImportSource.Samsung -> R.string.passwords_import_samsung
    PasswordImportSource.Managers -> R.string.passwords_import_managers
    PasswordImportSource.File -> R.string.passwords_import_file
}

