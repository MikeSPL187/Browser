package dev.sk2andy.materialbrowser.ui.passwords

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.ui.v5.V5LeakCheck
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordFinding
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordFindingKind
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordFindingPage
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordHealth
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordHealthActions
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordHealthRules
import dev.sk2andy.materialbrowser.ui.v5.V5PasswordHealthState
import dev.sk2andy.materialbrowser.ui.v5.v5BarDomain
import java.util.Date

/**
 * The v5 password check (V24, canvas «у») bound to the vault: the report of [logins] with the leak
 * check's [breaches], which runs only when the user asks ([onCheckLeaks]), never on opening.
 */
@Composable
internal fun PasswordHealthV5(
    logins: List<VaultLogin>,
    breaches: Map<String, Int>,
    leakCheck: LeakCheckStatus,
    leakProgress: Float,
    checkedAt: Long?,
    onCheckLeaks: () -> Unit,
    onFinding: (V5PasswordFinding) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val findings = remember(logins, breaches) { findings(logins, breaches) }
    V5PasswordHealth(
        state = V5PasswordHealthState(
            total = logins.size,
            findings = findings,
            leakCheck = leakCheck.toV5(),
            leakProgress = leakProgress,
            checkedAt = checkedAt?.let { DateFormat.getTimeFormat(context).format(Date(it)) },
        ),
        actions = V5PasswordHealthActions(onBack = onBack, onFinding = onFinding, onCheckLeaks = onCheckLeaks),
    )
}

/**
 * One finding of the check (canvas «у2»); a login that is gone or fixed meanwhile sends the user
 * back to the check ([onGone]).
 */
@Composable
internal fun PasswordFindingV5(
    logins: List<VaultLogin>,
    breaches: Map<String, Int>,
    loginId: String,
    kind: V5PasswordFindingKind,
    onChange: (VaultLogin) -> Unit,
    onOpenLogin: (VaultLogin) -> Unit,
    onBack: () -> Unit,
    onGone: () -> Unit,
) {
    val finding = remember(logins, breaches, loginId, kind) {
        findings(logins, breaches).firstOrNull { it.loginId == loginId && it.kind == kind }
    }
    val login = logins.firstOrNull { it.id == loginId }
    if (finding == null || login == null) {
        LaunchedEffect(loginId, kind) { onGone() }
        return
    }
    V5PasswordFindingPage(
        finding = finding,
        onBack = onBack,
        onChange = { onChange(login) },
        onOpenLogin = { onOpenLogin(login) },
    )
}

private fun findings(logins: List<VaultLogin>, breaches: Map<String, Int>): List<V5PasswordFinding> =
    V5PasswordHealthRules.findings(PasswordHealthRules.report(logins, breaches)) { login ->
        v5BarDomain(login.origin) ?: login.origin
    }

private fun LeakCheckStatus.toV5(): V5LeakCheck = when (this) {
    LeakCheckStatus.Off -> V5LeakCheck.NotChecked
    LeakCheckStatus.Checking -> V5LeakCheck.Checking
    LeakCheckStatus.Done -> V5LeakCheck.Done
    LeakCheckStatus.Failed -> V5LeakCheck.Failed
}
