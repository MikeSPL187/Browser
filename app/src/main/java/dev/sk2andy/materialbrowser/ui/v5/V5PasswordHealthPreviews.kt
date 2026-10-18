package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.shared.credentials.WeakPasswordKind
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The password check as canvas screens «у1»–«у3», light and dark. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val Reused = V5PasswordFinding("1", "mail.example.ru", "anna.k", V5PasswordFindingKind.Reused, count = 2)

private val Sample = V5PasswordHealthState(
    total = 46,
    findings = listOf(
        Reused,
        V5PasswordFinding("2", "shop.example.com", "anna.k", V5PasswordFindingKind.Reused, count = 2),
        V5PasswordFinding("3", "router.example.net", "admin", V5PasswordFindingKind.Insecure),
        V5PasswordFinding("4", "forum.example.org", "anna", V5PasswordFindingKind.Weak, 8, WeakPasswordKind.LettersOnly),
    ),
    leakCheck = V5LeakCheck.NotChecked,
)

private val AllFine = V5PasswordHealthState(46, emptyList(), V5LeakCheck.Done, checkedAt = "08:12")

private val NoActions = V5PasswordHealthActions(onBack = {}, onFinding = {}, onCheckLeaks = {})

@Preview(name = "Password check · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthLightPreview() {
    VolaV5Theme(dark = false) { V5PasswordHealth(Sample, NoActions) }
}

@Preview(name = "Password check · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthDarkPreview() {
    VolaV5Theme(dark = true) { V5PasswordHealth(Sample.copy(leakCheck = V5LeakCheck.Checking, leakProgress = 0.4f), NoActions) }
}

@Preview(name = "Password finding · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordFindingLightPreview() {
    VolaV5Theme(dark = false) { V5PasswordFindingPage(Reused, onBack = {}, onChange = {}, onOpenLogin = {}) }
}

@Preview(name = "Password finding · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordFindingDarkPreview() {
    VolaV5Theme(dark = true) { V5PasswordFindingPage(Sample.findings[2], onBack = {}, onChange = {}, onOpenLogin = {}) }
}

@Preview(name = "All good · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthAllFineLightPreview() {
    VolaV5Theme(dark = false) { V5PasswordHealth(AllFine, NoActions) }
}

@Preview(name = "All good · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthAllFineDarkPreview() {
    VolaV5Theme(dark = true) { V5PasswordHealth(AllFine.copy(leakCheck = V5LeakCheck.Failed), NoActions) }
}

@Preview(name = "All good, a changed password unchecked · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthUncheckedLightPreview() {
    VolaV5Theme(dark = false) { V5PasswordHealth(AllFine.copy(unchecked = 1), NoActions) }
}

@Preview(name = "All good, passwords unchecked · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthUncheckedDarkPreview() {
    VolaV5Theme(dark = true) { V5PasswordHealth(AllFine.copy(unchecked = 3), NoActions) }
}

@Preview(name = "Checking, with Stop and an empty user name · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthCheckingLightPreview() {
    VolaV5Theme(dark = false) {
        V5PasswordHealth(
            Sample.copy(
                findings = Sample.findings + V5PasswordFinding("5", "wiki.example.org", "", V5PasswordFindingKind.Reused, count = 3),
                leakCheck = V5LeakCheck.Checking,
                leakProgress = 0.42f,
            ),
            NoActions,
        )
    }
}

@Preview(name = "Checking, with Stop · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PasswordHealthCheckingDarkPreview() {
    VolaV5Theme(dark = true) { V5PasswordHealth(Sample.copy(leakCheck = V5LeakCheck.Checking, leakProgress = 0.42f), NoActions) }
}
