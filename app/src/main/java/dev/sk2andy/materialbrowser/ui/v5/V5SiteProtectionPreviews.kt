package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 site protection panel as canvas screen «f1», light and dark, and a site with protection off. */

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
)

@Composable
private fun PanelPreview(dark: Boolean, state: V5SiteProtectionState) {
    VolaV5Theme(dark = dark) {
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
