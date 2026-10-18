package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthReport
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.WeakPasswordKind
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

object V5PasswordHealthTestTags {
    const val Page = "v5_password_health"
    const val Summary = "v5_password_health_summary"
    const val AllFine = "v5_password_health_all_fine"
    const val LeakCheck = "v5_password_health_leak_check"
    const val Finding = "v5_password_finding"
    const val Change = "v5_password_finding_change"
    const val OpenLogin = "v5_password_finding_open"

    fun row(kind: V5PasswordFindingKind, loginId: String) = "v5_password_health_${kind.name}_$loginId"
}

/** The sections of the check, most urgent first (canvas «у1»). */
enum class V5PasswordFindingKind { Breached, Reused, Insecure, Weak }

/**
 * One login with one finding: [count] is how often the password was seen in leaks, on how many
 * other sites it repeats, or how long a weak one is; [formOnly] tells an HTTP form on an HTTPS site.
 */
@Immutable
data class V5PasswordFinding(
    val loginId: String,
    val site: String,
    val username: String,
    val kind: V5PasswordFindingKind,
    val count: Int = 0,
    val weakKind: WeakPasswordKind? = null,
    val formOnly: Boolean = false,
)

/** Where the leak check is: only ever started by a touch (canvas «у1»). */
enum class V5LeakCheck { NotChecked, Checking, Done, Failed }

/**
 * The whole check: [checkedAt] is the time of the last finished leak check, as the clock shows it;
 * [unchecked] counts passwords no leak check has answered for, as one changed since.
 */
@Immutable
data class V5PasswordHealthState(
    val total: Int,
    val findings: List<V5PasswordFinding>,
    val leakCheck: V5LeakCheck,
    val leakProgress: Float = 0f,
    val checkedAt: String? = null,
    val unchecked: Int = 0,
)

class V5PasswordHealthActions(
    val onBack: () -> Unit,
    val onFinding: (V5PasswordFinding) -> Unit,
    val onCheckLeaks: () -> Unit,
)

/**
 * The password check (V24, canvas «у1» and «у3»). One layer: how many logins need attention, the
 * sections by urgency, and the leak check, which sends the first five characters of each
 * password's hash only when touched. With nothing found the page says so plainly.
 */
@Composable
fun V5PasswordHealth(
    state: V5PasswordHealthState,
    actions: V5PasswordHealthActions,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5PasswordHealthTestTags.Page),
    ) {
        V5PageBar(title = stringResource(R.string.passwords_health_title), onBack = actions.onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            val attention = V5PasswordHealthRules.attention(state.findings)
            val leaksChecked = state.leakCheck == V5LeakCheck.Done && state.unchecked == 0
            if (attention == 0) AllFine(leaksChecked) else Summary(attention, state.total)
            V5PasswordHealthRules.sections(state.findings).forEach { (kind, findings) ->
                Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
                    SectionHeader("${stringResource(kind.title())} · ${findings.size}")
                    V5ListGroup(dividerInset = V5ListDefaults.dividerInsetWithIcon) {
                        findings.forEach { finding -> FindingRow(finding) { actions.onFinding(finding) } }
                    }
                }
            }
            LeakCheck(state, actions.onCheckLeaks)
        }
    }
}

/**
 * A login with its finding (canvas «у2»): what is wrong in one sentence, the login and the site,
 * and «Change password on the site», except over HTTP, where a new password does not help.
 */
@Composable
fun V5PasswordFindingPage(
    finding: V5PasswordFinding,
    onBack: () -> Unit,
    onChange: () -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5PasswordHealthTestTags.Finding),
    ) {
        V5PageBar(title = finding.site, onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space5),
        ) {
            Row(
                Modifier
                    .padding(horizontal = V5Spacing.marginPhone)
                    .fillMaxWidth()
                    .clip(V5Theme.shapes.s)
                    .background(colors.surface)
                    .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space3),
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            ) {
                Icon(
                    VolaIcons.WarningFilled,
                    contentDescription = null,
                    tint = finding.kind.tint(),
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
                Text(findingSentence(finding), style = V5Theme.type.callout, color = colors.label)
            }
            V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
                V5ListRow(title = stringResource(R.string.passwords_username), value = finding.username)
                V5ListRow(title = stringResource(R.string.passwords_site), value = finding.site)
                V5ListRow(
                    title = stringResource(R.string.v5_password_health_open_login),
                    trailing = V5RowTrailing.Chevron,
                    onClick = onOpenLogin,
                    modifier = Modifier.testTag(V5PasswordHealthTestTags.OpenLogin),
                )
            }
            if (V5PasswordHealthRules.canChange(finding)) {
                Box(Modifier.padding(horizontal = V5Spacing.marginPhone)) {
                    V5StatePrimaryAction(
                        V5StateAction(
                            label = stringResource(R.string.v5_password_health_change),
                            onClick = onChange,
                            testTag = V5PasswordHealthTestTags.Change,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(attention: Int, total: Int) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .clip(V5Theme.shapes.s)
            .background(colors.surface)
            .padding(V5Spacing.space4)
            .semantics(mergeDescendants = true) {}
            .testTag(V5PasswordHealthTestTags.Summary),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space4),
    ) {
        Box(
            Modifier
                .size(V5Sizes.passwordStatus)
                .clip(V5Theme.shapes.full)
                .background(colors.warning.copy(alpha = STATUS_WASH_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.WarningFilled,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(V5Sizes.passwordStatusIcon),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                pluralStringResource(R.plurals.v5_password_health_attention, attention, attention),
                style = V5Theme.type.title,
                color = colors.label,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.v5_password_health_fine_of, total - attention, total),
                style = V5Theme.type.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun AllFine(leaksChecked: Boolean) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space8)
            .testTag(V5PasswordHealthTestTags.AllFine),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(
            Modifier
                .size(V5Sizes.privateGem)
                .clip(V5Theme.shapes.full)
                .background(colors.success.copy(alpha = STATUS_WASH_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(V5Sizes.privateGemIcon),
            )
        }
        Text(
            stringResource(R.string.passwords_health_entry_fine),
            style = V5Theme.type.title,
            color = colors.label,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(
                if (leaksChecked) R.string.v5_password_health_all_fine else R.string.v5_password_health_all_fine_unchecked,
            ),
            style = V5Theme.type.callout,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FindingRow(finding: V5PasswordFinding, onClick: () -> Unit) {
    val tile = V5NewTabRules.letterTile(finding.site)
    V5ListRow(
        title = finding.site,
        subtitle = "${finding.username} · ${findingDetail(finding)}",
        leading = {
            Box(
                Modifier
                    .size(V5Sizes.rowTile)
                    .clip(V5Theme.shapes.xs)
                    .background(tile.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(V5NewTabRules.letter(finding.site), style = V5Theme.type.callout, color = tile.glyph)
            }
        },
        trailing = V5RowTrailing.Chevron,
        onClick = onClick,
        modifier = Modifier.testTag(V5PasswordHealthTestTags.row(finding.kind, finding.loginId)),
    )
}

@Composable
private fun LeakCheck(state: V5PasswordHealthState, onCheck: () -> Unit) {
    val colors = V5Theme.colors
    Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
        V5ListGroup(dividerInset = V5ListDefaults.dividerInsetText) {
            when (state.leakCheck) {
                V5LeakCheck.NotChecked -> V5ListRow(
                    title = stringResource(R.string.v5_password_health_check_leaks),
                    icon = V5RowIcon(VolaIcons.ShieldLock, V5Tile.Red),
                    titleColor = colors.accent,
                    onClick = onCheck,
                    modifier = Modifier.testTag(V5PasswordHealthTestTags.LeakCheck),
                )
                V5LeakCheck.Checking -> V5ListRow(
                    title = stringResource(
                        R.string.v5_password_health_checking,
                        V5PasswordHealthRules.percent(state.leakProgress),
                    ),
                    titleColor = colors.labelSecondary,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag(V5PasswordHealthTestTags.LeakCheck),
                )
                V5LeakCheck.Done, V5LeakCheck.Failed -> {
                    state.checkedAt?.let { time ->
                        V5ListRow(
                            title = stringResource(R.string.v5_password_health_leaks),
                            value = stringResource(R.string.v5_password_health_checked_at, time),
                        )
                    }
                    V5ListRow(
                        title = stringResource(R.string.v5_password_health_check_again),
                        titleColor = colors.accent,
                        onClick = onCheck,
                        modifier = Modifier.testTag(V5PasswordHealthTestTags.LeakCheck),
                    )
                }
            }
        }
        val checked = state.leakCheck == V5LeakCheck.Done || state.leakCheck == V5LeakCheck.Failed
        if (checked && state.unchecked > 0) {
            Text(
                pluralStringResource(R.plurals.v5_password_health_unchecked, state.unchecked, state.unchecked),
                style = V5Theme.type.footnote,
                color = colors.label,
                modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
            )
        }
        if (state.leakCheck == V5LeakCheck.Failed) {
            Text(
                stringResource(R.string.passwords_health_check_failed),
                style = V5Theme.type.footnote,
                color = colors.danger,
                modifier = Modifier
                    .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Text(
            stringResource(R.string.v5_password_health_note),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(LocalConfiguration.current.locales[0]),
        style = V5Theme.type.footnote,
        color = V5Theme.colors.labelSecondary,
        modifier = Modifier
            .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
            .semantics { heading() },
    )
}

/** The short reason under a row: «same as on 2 other sites», «8 characters, letters only». */
@Composable
private fun findingDetail(finding: V5PasswordFinding): String = when (finding.kind) {
    V5PasswordFindingKind.Breached ->
        pluralStringResource(R.plurals.passwords_health_seen_in_leaks, finding.count, finding.count)
    V5PasswordFindingKind.Reused ->
        pluralStringResource(R.plurals.passwords_health_same_as, finding.count, finding.count)
    V5PasswordFindingKind.Insecure -> stringResource(
        if (finding.formOnly) R.string.v5_password_health_insecure_form else R.string.v5_password_health_insecure_site,
    )
    V5PasswordFindingKind.Weak -> {
        val characters = pluralStringResource(R.plurals.passwords_health_characters, finding.count, finding.count)
        when (finding.weakKind) {
            WeakPasswordKind.LettersOnly -> stringResource(R.string.passwords_health_letters_only, characters)
            WeakPasswordKind.DigitsOnly -> stringResource(R.string.passwords_health_digits_only, characters)
            else -> stringResource(R.string.passwords_health_easy_to_guess, characters)
        }
    }
}

/** What is wrong, in one sentence, at the top of the finding page. */
@Composable
private fun findingSentence(finding: V5PasswordFinding): String = when (finding.kind) {
    V5PasswordFindingKind.Breached -> stringResource(R.string.v5_password_finding_breached)
    V5PasswordFindingKind.Reused ->
        pluralStringResource(R.plurals.v5_password_finding_reused, finding.count, finding.count)
    V5PasswordFindingKind.Insecure -> stringResource(
        if (finding.formOnly) R.string.v5_password_finding_insecure_form else R.string.v5_password_finding_insecure_site,
    )
    V5PasswordFindingKind.Weak -> stringResource(R.string.v5_password_finding_weak)
}

@Composable
private fun V5PasswordFindingKind.tint(): Color = when (this) {
    V5PasswordFindingKind.Breached -> V5Theme.colors.danger
    else -> V5Theme.colors.warning
}

private fun V5PasswordFindingKind.title(): Int = when (this) {
    V5PasswordFindingKind.Breached -> R.string.passwords_health_breached
    V5PasswordFindingKind.Reused -> R.string.passwords_health_reused
    V5PasswordFindingKind.Insecure -> R.string.v5_password_health_insecure
    V5PasswordFindingKind.Weak -> R.string.passwords_health_weak
}

private const val STATUS_WASH_ALPHA = 0.18f

/** Pure decisions of the password check screens, for unit tests. */
internal object V5PasswordHealthRules {
    private const val PERCENT = 100

    /**
     * The report as rows: leaks, repeats and weak passwords as the report sorted them (one section
     * each), and every login sent over HTTP in its own section, since a new password does not fix it.
     */
    fun findings(report: PasswordHealthReport, site: (VaultLogin) -> String): List<V5PasswordFinding> =
        report.breached.map { finding(it.login, site, V5PasswordFindingKind.Breached, it.timesSeen) } +
            report.reused.map { finding(it.login, site, V5PasswordFindingKind.Reused, it.otherSites) } +
            report.insecure.map { finding(it.login, site, V5PasswordFindingKind.Insecure, formOnly = it.formOnly) } +
            report.weak.map { finding(it.login, site, V5PasswordFindingKind.Weak, it.length, it.kind) }

    /** The non-empty sections in order of urgency. */
    fun sections(findings: List<V5PasswordFinding>): List<Pair<V5PasswordFindingKind, List<V5PasswordFinding>>> =
        V5PasswordFindingKind.entries
            .map { kind -> kind to findings.filter { it.kind == kind } }
            .filter { (_, rows) -> rows.isNotEmpty() }

    /** How many logins need attention: a login in two sections counts once. */
    fun attention(findings: List<V5PasswordFinding>): Int = findings.map { it.loginId }.distinct().size

    /** Only a new password helps, so HTTP has no «Change». */
    fun canChange(finding: V5PasswordFinding): Boolean = finding.kind != V5PasswordFindingKind.Insecure

    fun percent(progress: Float): Int = (progress.coerceIn(0f, 1f) * PERCENT).toInt()

    private fun finding(
        login: VaultLogin,
        site: (VaultLogin) -> String,
        kind: V5PasswordFindingKind,
        count: Int = 0,
        weakKind: WeakPasswordKind? = null,
        formOnly: Boolean = false,
    ) = V5PasswordFinding(login.id, site(login), login.username, kind, count, weakKind, formOnly)
}
