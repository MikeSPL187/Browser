package dev.sk2andy.materialbrowser.shared.credentials

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** One login read from another manager's export, before the vault checks it. */
data class ImportedLogin(
    val draft: VaultLoginDraft,
    /** A canonical two-factor link ([TotpRules.canonical]) when the export had a usable key. */
    val totp: String? = null,
) {
    override fun toString(): String = "ImportedLogin(draft=$draft, totp=${if (totp == null) "none" else "<redacted>"})"
}

/** What an export file turned out to be. */
sealed interface PasswordImportParse {
    /** Logins to hand to the vault; [skipped] rows were not web logins or had no password. */
    data class Read(val logins: List<ImportedLogin>, val skipped: Int) : PasswordImportParse {
        override fun toString(): String = "Read(logins=${logins.size}, skipped=$skipped)"
    }

    /** A Bitwarden export sealed with its own password: it has to be exported unencrypted. */
    data object Encrypted : PasswordImportParse

    /** Neither a password CSV nor a Bitwarden JSON export. */
    data object NotAnExport : PasswordImportParse
}

/**
 * Exports of other password managers (Q22a): the CSV files of Chrome, Firefox, Bitwarden, Proton
 * Pass and 1Password, and Bitwarden's unencrypted JSON. Columns are found by their header, so the
 * order and extra columns of each manager do not matter. Everything happens on the phone; the site
 * check comes in as [parse]'s `origin`, the one the vault uses for sites typed in by hand.
 */
object PasswordImportRules {
    /** Exports with ten thousand logins stay far below this. */
    const val MAX_FILE_BYTES = 16 * 1024 * 1024
    private const val MAX_ROWS = 50_000

    private val URL_COLUMNS = listOf("url", "login_uri", "website", "web site", "uri", "login url", "urls")
    private val USERNAME_COLUMNS = listOf("username", "login_username", "user name", "login", "user")
    private val EMAIL_COLUMNS = listOf("email", "e-mail")
    private val PASSWORD_COLUMNS = listOf("password", "login_password")
    private val TOTP_COLUMNS = listOf("totp", "login_totp", "otpauth", "one-time password", "otp")
    private val LOGIN_TYPES = setOf("login", "password")
    private const val BITWARDEN_LOGIN_TYPE = 1

    private val json = Json { ignoreUnknownKeys = true }

    /** The logins in [text]; [origin] turns a site as the export writes it into a vault origin. */
    fun parse(text: String, origin: (String) -> String?): PasswordImportParse {
        val clean = text.removePrefix("﻿")
        val trimmed = clean.trimStart()
        return if (trimmed.startsWith("{")) parseBitwardenJson(trimmed, origin) else parseCsv(clean, origin)
    }

    private fun parseCsv(text: String, origin: (String) -> String?): PasswordImportParse {
        val rows = csvRows(text) ?: return PasswordImportParse.NotAnExport
        val header = rows.firstOrNull()?.map { it.trim().lowercase() } ?: return PasswordImportParse.NotAnExport
        fun column(names: List<String>): Int? = names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } }
        val url = column(URL_COLUMNS) ?: return PasswordImportParse.NotAnExport
        val password = column(PASSWORD_COLUMNS) ?: return PasswordImportParse.NotAnExport
        val username = column(USERNAME_COLUMNS)
        val email = column(EMAIL_COLUMNS)
        val totp = column(TOTP_COLUMNS)
        val realm = column(listOf("httprealm"))
        val formAction = column(listOf("formactionorigin"))
        val type = column(listOf("type"))
        val logins = ArrayList<ImportedLogin>()
        var skipped = 0
        for (row in rows.drop(1)) {
            if (row.all(String::isBlank)) continue
            fun cell(index: Int?): String = index?.let(row::getOrNull).orEmpty()
            val kind = cell(type).trim().lowercase()
            val login = if (kind.isNotEmpty() && kind !in LOGIN_TYPES) {
                null
            } else {
                imported(
                    // Bitwarden puts every address of a login in one cell, separated by commas.
                    sites = cell(url).split(','),
                    username = cell(username).ifBlank { cell(email) },
                    password = cell(password),
                    totp = cell(totp),
                    realm = cell(realm),
                    formAction = cell(formAction),
                    origin = origin,
                )
            }
            if (login == null) skipped++ else logins += login
        }
        return PasswordImportParse.Read(logins, skipped)
    }

    private fun parseBitwardenJson(text: String, origin: (String) -> String?): PasswordImportParse {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: return PasswordImportParse.NotAnExport
        if (root["encrypted"].boolean() == true) return PasswordImportParse.Encrypted
        val items = root["items"] as? JsonArray ?: return PasswordImportParse.NotAnExport
        val logins = ArrayList<ImportedLogin>()
        var skipped = 0
        for (item in items.take(MAX_ROWS)) {
            val fields = item as? JsonObject
            val login = fields?.get("login") as? JsonObject
            val imported = if (fields == null || login == null || fields["type"].int() != BITWARDEN_LOGIN_TYPE) {
                null
            } else {
                imported(
                    sites = (login["uris"] as? JsonArray).orEmpty().map { uri -> (uri as? JsonObject)?.get("uri").text() },
                    username = login["username"].text(),
                    password = login["password"].text(),
                    totp = login["totp"].text(),
                    realm = "",
                    formAction = "",
                    origin = origin,
                )
            }
            if (imported == null) skipped++ else logins += imported
        }
        return PasswordImportParse.Read(logins, skipped)
    }

    private fun imported(
        sites: List<String>,
        username: String,
        password: String,
        totp: String,
        realm: String,
        formAction: String,
        origin: (String) -> String?,
    ): ImportedLogin? {
        if (password.isEmpty()) return null
        val site = sites.firstNotNullOfOrNull { site -> site.trim().takeIf(String::isNotEmpty)?.let(origin) } ?: return null
        val draft = VaultLoginDraft(
            origin = site,
            formActionOrigin = formAction.trim().takeIf(String::isNotEmpty)?.let(origin),
            httpRealm = realm.takeIf(String::isNotBlank),
            username = username.trim(),
            password = password,
        )
        if (!CredentialVaultRules.accepts(draft)) return null
        // A key the rules cannot read (a Steam code, say) is left out; the login still comes in.
        val key = totp.takeIf(String::isNotBlank)?.let(TotpRules::parse)?.let(TotpRules::canonical)
        return ImportedLogin(draft, key)
    }

    /**
     * The rows of a CSV file (RFC 4180): fields in quotes may hold commas, line breaks and doubled
     * quotes. Null when a quote is never closed or the file has more rows than any export.
     */
    fun csvRows(text: String): List<List<String>>? {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (quoted) {
                when {
                    char == '"' && text.getOrNull(index + 1) == '"' -> {
                        field.append('"')
                        index++
                    }
                    char == '"' -> quoted = false
                    else -> field.append(char)
                }
            } else {
                when (char) {
                    '"' -> quoted = true
                    ',' -> {
                        row += field.toString()
                        field.clear()
                    }
                    '\r', '\n' -> {
                        if (char == '\r' && text.getOrNull(index + 1) == '\n') index++
                        row += field.toString()
                        field.clear()
                        rows += row
                        row = ArrayList()
                        if (rows.size > MAX_ROWS) return null
                    }
                    else -> field.append(char)
                }
            }
            index++
        }
        if (quoted) return null
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }

    private fun JsonElement?.text(): String =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

    private fun JsonElement?.int(): Int? =
        (this as? JsonPrimitive)?.takeUnless { it.isString || it is JsonNull }?.content?.toIntOrNull()

    private fun JsonElement?.boolean(): Boolean? =
        (this as? JsonPrimitive)?.takeUnless { it.isString || it is JsonNull }?.content?.toBooleanStrictOrNull()
}
