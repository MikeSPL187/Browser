package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthReport
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.WeakPasswordKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.LibraryCardSlice
import dev.sk2andy.materialbrowser.ui.LibraryRow
import dev.sk2andy.materialbrowser.ui.LibraryRules
import dev.sk2andy.materialbrowser.ui.LibrarySiteTile
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaLibrary
import dev.sk2andy.materialbrowser.ui.theme.VolaPasswords
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

/** Where the leak check is: off (the default), running, done or failed. */
internal enum class LeakCheckStatus { Off, Checking, Done, Failed }

internal object PasswordHealthTestTags {
    const val Screen = "password_health"
    const val LeakCheck = "password_health_leak_check"
    fun change(id: String) = "password_health_change:$id"
}

/**
 * Board W-PasswordHealth: how many logins are fine, where to start, then leaks, repeats and weak
 * passwords, each with «Change», which opens the site's own change-password page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordHealthScreen(
    report: PasswordHealthReport,
    leakCheck: LeakCheckStatus,
    onLeakCheckChange: (Boolean) -> Unit,
    onOpen: (VaultLogin) -> Unit,
    onChange: (VaultLogin) -> Unit,
    onBack: () -> Unit,
    leakProgress: Float = 0f,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.passwords_health_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .testTag(PasswordHealthTestTags.Screen),
            contentPadding = PaddingValues(bottom = VolaPasswords.sectionGap),
        ) {
            item(key = "summary") { Summary(report) }
            item(key = "leak-check") { LeakCheckCard(leakCheck, leakProgress, onLeakCheckChange) }
            section(
                key = "breached",
                icon = VolaIcons.WarningFilled,
                title = R.string.passwords_health_breached,
                rows = report.breached.map { breached ->
                    breached.login to pluralStringResourceLazy(R.plurals.passwords_health_seen_in_leaks, breached.timesSeen)
                },
                onOpen = onOpen,
                onChange = onChange,
            )
            section(
                key = "reused",
                icon = VolaIcons.ContentCopy,
                title = R.string.passwords_health_reused,
                rows = report.reused.map { reused ->
                    reused.login to pluralStringResourceLazy(R.plurals.passwords_health_same_as, reused.otherSites)
                },
                onOpen = onOpen,
                onChange = onChange,
            )
            section(
                key = "weak",
                icon = VolaIcons.Key,
                title = R.string.passwords_health_weak,
                rows = report.weak.map { weak -> weak.login to weakDetail(weak.length, weak.kind) },
                onOpen = onOpen,
                onChange = onChange,
            )
        }
    }
}

/** A plural line resolved where the row is drawn. */
private fun pluralStringResourceLazy(id: Int, count: Int): @Composable () -> String =
    { pluralStringResource(id, count, count) }

private fun weakDetail(length: Int, kind: WeakPasswordKind): @Composable () -> String = {
    val characters = pluralStringResource(R.plurals.passwords_health_characters, length, length)
    when (kind) {
        WeakPasswordKind.LettersOnly -> stringResource(R.string.passwords_health_letters_only, characters)
        WeakPasswordKind.DigitsOnly -> stringResource(R.string.passwords_health_digits_only, characters)
        WeakPasswordKind.Short, WeakPasswordKind.Other -> stringResource(R.string.passwords_health_easy_to_guess, characters)
    }
}

@Composable
private fun Summary(report: PasswordHealthReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaPasswords.sidePadding)
            .padding(bottom = VolaPasswords.sectionGap),
        verticalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap)) {
            Text(
                text = stringResource(R.string.passwords_health_percent, report.finePercent),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.displaySmall,
                color = if (report.needsAttention == 0) VolaTheme.extendedColors.ok else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.passwords_health_fine),
                modifier = Modifier.padding(bottom = VolaPasswords.healthSummaryGap),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val advice = when {
            report.breached.isNotEmpty() -> R.string.passwords_health_start_breached
            report.reused.isNotEmpty() -> R.string.passwords_health_start_reused
            report.weak.isNotEmpty() -> R.string.passwords_health_start_weak
            else -> R.string.passwords_health_all_fine
        }
        Text(
            text = buildString {
                append(stringResource(advice))
                if (report.needsAttention > 0 && report.fine > 0) {
                    append(' ')
                    append(pluralStringResource(R.plurals.passwords_health_rest_fine, report.fine, report.fine))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LeakCheckCard(status: LeakCheckStatus, progress: Float, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaPasswords.sidePadding)
            .padding(bottom = VolaPasswords.sectionGap),
        shape = VolaPasswords.sheetCardShape,
        color = VolaTheme.extendedColors.card,
    ) {
        Column(modifier = Modifier.padding(VolaPasswords.generatorOptionsPadding)) {
            val on = status != LeakCheckStatus.Off
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = VolaPasswords.generatorSwitchHeight)
                    .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
                    .testTag(PasswordHealthTestTags.LeakCheck),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.passwords_health_leak_check),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                Switch(checked = on, onCheckedChange = null)
            }
            when (status) {
                LeakCheckStatus.Checking -> LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                LeakCheckStatus.Failed -> Text(
                    text = stringResource(R.string.passwords_health_check_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                LeakCheckStatus.Off, LeakCheckStatus.Done -> Unit
            }
            Row(
                modifier = Modifier.padding(top = VolaPasswords.healthSummaryGap),
                horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
            ) {
                Icon(
                    VolaIcons.Info,
                    contentDescription = null,
                    modifier = Modifier.size(VolaPasswords.sheetVerifiedIcon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.passwords_health_leak_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun LazyListScope.section(
    key: String,
    icon: ImageVector,
    title: Int,
    rows: List<Pair<VaultLogin, @Composable () -> String>>,
    onOpen: (VaultLogin) -> Unit,
    onChange: (VaultLogin) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "$key-title") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VolaLibrary.sidePadding)
                .padding(VolaLibrary.sectionLabelPadding),
            horizontalArrangement = Arrangement.spacedBy(VolaPasswords.healthSummaryGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(VolaPasswords.pointIconSize))
            Text(
                text = stringResource(title),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = rows.size.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
    itemsIndexed(rows, key = { _, (login, _) -> "$key-${login.id}" }) { index, (login, detail) ->
        LibraryCardSlice(position = LibraryRules.position(index, rows.size)) {
            val site = PasswordsRules.displaySite(login.origin)
            LibraryRow(
                title = site,
                detail = listOfNotNull(login.username.takeIf(String::isNotEmpty), detail()).joinToString(" · "),
                leading = { LibrarySiteTile(label = site, colorKey = login.origin) },
                onClick = { onOpen(login) },
                trailing = {
                    TextButton(
                        onClick = { onChange(login) },
                        modifier = Modifier.testTag(PasswordHealthTestTags.change(login.id)),
                    ) {
                        Text(stringResource(R.string.passwords_health_change))
                    }
                },
            )
        }
    }
}

/** Board W-PasswordHealth with a leak, a repeat and a weak password. */
@VolaPreviews
@Composable
private fun PasswordHealthPreview() {
    val logins = listOf(
        previewLogin("1", "https://forum.example.org", "snowfox", "Tr0ub4dor&3"),
        previewLogin("2", "https://tasks.example.com", "anna@example.com", "Same-Pass-42!"),
        previewLogin("3", "https://mail.example.com", "anna@example.com", "Same-Pass-42!"),
        previewLogin("4", "https://cinema.example.ru", "anna", "sunshine"),
        previewLogin("5", "https://bank.example.com", "anna", "kV7#qe2Lm!Tz9pWf"),
    )
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System)) {
        PasswordHealthScreen(
            report = PasswordHealthRules.report(logins, mapOf("1" to 12)),
            leakCheck = LeakCheckStatus.Done,
            onLeakCheckChange = {},
            onOpen = {},
            onChange = {},
            onBack = {},
        )
    }
}

private fun previewLogin(id: String, origin: String, username: String, password: String) = VaultLogin(
    id = id,
    origin = origin,
    formActionOrigin = null,
    httpRealm = null,
    username = username,
    password = password,
    createdAtMillis = 0,
    updatedAtMillis = 0,
    lastUsedAtMillis = null,
    timesUsed = 0,
)
