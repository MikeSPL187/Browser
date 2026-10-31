package dev.sk2andy.materialbrowser.shared.credentials

/**
 * «Export passwords»: every login as the CSV that Chrome writes (`name,url,username,password,note`),
 * which Chrome, Firefox, Bitwarden, 1Password, Proton Pass and Vola's own import read. Three more
 * columns keep what Vola has and Chrome's file lacks: the 2FA key and the HTTP realm and form action
 * that tell two logins of one site apart. Every cell is quoted, so commas, quotes and line breaks in
 * a password come back as they were; nothing is changed in a value.
 */
object PasswordExportRules {
    const val FILE_NAME = "Vola Passwords.csv"
    const val MIME_TYPE = "text/csv"

    private val HEADER = listOf("name", "url", "username", "password", "note", "totp", "httpRealm", "formActionOrigin")

    /** The file for [logins]; [name] is the site as the list shows it. */
    fun csv(logins: List<VaultLogin>, name: (VaultLogin) -> String): String = buildString {
        appendRow(HEADER)
        for (login in logins) {
            appendRow(
                listOf(
                    name(login),
                    login.origin,
                    login.username,
                    login.password,
                    "",
                    login.totp.orEmpty(),
                    login.httpRealm.orEmpty(),
                    login.formActionOrigin.orEmpty(),
                ),
            )
        }
    }

    private fun StringBuilder.appendRow(cells: List<String>) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) append(',')
            append('"').append(cell.replace("\"", "\"\"")).append('"')
        }
        append('\n')
    }
}
