package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.browser.safety.BlockedSite
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Canvas screen «Danger»: a look-alike, a listed site and a Safe Browsing report; light, dark, an accent, the largest font. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val LookAlike = BlockedSite(url = "https://bank-exarnple.ru/", host = "bank-exarnple.ru", imitatedHost = "bank.example.ru")
private val Listed = BlockedSite(url = "https://secure-akkaunt-login.top/", host = "secure-akkaunt-login.top")
private val Reported = BlockedSite(url = "https://login-bank-help.top/", host = "login-bank-help.top", reportedBySafeBrowsing = true)

@Composable
private fun DangerPreview(site: BlockedSite, dark: Boolean, accent: V5Accent = V5Accent.Blue, whyOpen: Boolean = false) {
    VolaV5Theme(dark = dark, accent = accent) {
        V5DangerousSite(site = site, onBackToSafety = {}, onOpenAnyway = {}, onOpenRealSite = {}, whyInitiallyOpen = whyOpen)
    }
}

@Preview(name = "Danger · look-alike · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5DangerLookAlikeLightPreview() {
    DangerPreview(LookAlike, dark = false)
}

@Preview(name = "Danger · look-alike, why open · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5DangerLookAlikeDarkPreview() {
    DangerPreview(LookAlike, dark = true, whyOpen = true)
}

@Preview(name = "Danger · listed · light · green", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5DangerListedAccentPreview() {
    DangerPreview(Listed, dark = false, accent = V5Accent.Green)
}

@Preview(name = "Danger · Safe Browsing, no way through · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5DangerReportedDarkPreview() {
    DangerPreview(Reported, dark = true)
}

@Preview(name = "Danger · listed · largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5DangerLargeFontPreview() {
    DangerPreview(Listed, dark = false)
}
