package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 site protection panel as canvas screen «f1»: light, dark, protection off, and the reset question. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val GeckoSite = V5SiteProtectionState(
    host = "polar-notes.ru",
    protectionOn = true,
    strict = false,
    blocked = 23,
    privateTab = false,
    thirdPartyCookiesAllowed = false,
    cookieBannersHidden = true,
    deAmp = true,
    fingerprintingProtection = true,
    httpsOnly = "Во всех вкладках",
    connection = V5SiteConnection.Secure,
)

private val NoActions = V5SiteProtectionActions(
    onProtectionChange = {},
    onStrictChange = {},
    onThirdPartyCookiesChange = {},
    onCookieBannersChange = {},
    onDeAmpChange = {},
    onFingerprintingChange = {},
    onHttpsOnly = {},
    onCopyReport = {},
    onReset = {},
    onSiteInfo = {},
    onPrivacyReport = {},
)

@Composable
private fun PanelPreview(dark: Boolean, state: V5SiteProtectionState, accent: V5Accent = V5Accent.Default) {
    VolaV5Theme(dark = dark, accent = accent) {
        V5SiteProtection(visible = true, state = state, actions = NoActions, onDismiss = {})
    }
}

@Preview(name = "Site protection · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SiteProtectionLightPreview() {
    PanelPreview(dark = false, state = GeckoSite)
}

@Preview(name = "Site protection · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SiteProtectionDarkPreview() {
    PanelPreview(dark = true, state = GeckoSite)
}

@Preview(name = "Site protection · off · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SiteProtectionOffPreview() {
    PanelPreview(dark = true, state = GeckoSite.copy(protectionOn = false, blocked = 0))
}

@Preview(name = "Site protection · reset question · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SiteResetPromptPreview() {
    VolaV5Theme(dark = true) {
        V5SiteResetPrompt(visible = true, host = GeckoSite.host, privateTab = false, onReset = {}, onDismiss = {})
    }
}

/** A plain HTTP site in a green profile: the connection warns, the switch takes the accent. */
@Preview(name = "Site protection · HTTP, green", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5SiteProtectionHttpPreview() {
    PanelPreview(dark = false, state = GeckoSite.copy(connection = V5SiteConnection.NotSecure, strict = true), accent = V5Accent.Green)
}

@Preview(name = "Site protection · largest font", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP, fontScale = 2f)
@Composable
private fun V5SiteProtectionLargeFontPreview() {
    PanelPreview(dark = true, state = GeckoSite)
}
