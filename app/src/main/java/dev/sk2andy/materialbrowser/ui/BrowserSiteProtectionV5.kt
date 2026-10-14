package dev.sk2andy.materialbrowser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.BuildConfig
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.SitePrivacyOverrides
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.integration.markedSensitive
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionLevel
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionRules
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5SiteProtection
import dev.sk2andy.materialbrowser.ui.v5.V5SiteProtectionActions
import dev.sk2andy.materialbrowser.ui.v5.V5SiteProtectionRules
import dev.sk2andy.materialbrowser.ui.v5.V5SiteProtectionState

/**
 * The v5 site protection panel (V17) bound to the browser: the site's state from the protection
 * port, the count from the engine's log and Vola's blocker (counted once), and every switch through
 * the port or the site's overrides, so private tabs keep their changes in memory. [tabId] null
 * lets the panel slide away; [onSiteInfo] opens the full site sheet.
 */
@Composable
internal fun BrowserSiteProtectionV5(
    controller: BrowserController,
    tabId: String?,
    onDismiss: () -> Unit,
    onSiteInfo: () -> Unit,
    onOpenProtectionSettings: () -> Unit,
) {
    // The panel keeps showing its tab while it slides away.
    val last = remember { LastTab() }
    if (tabId != null) last.id = tabId
    val id = last.id ?: return
    val tab = controller.tabs.firstOrNull { it.id == id } ?: return
    val panel = controller.siteProtection.state(id) ?: return
    val visible = tabId != null
    // The engine's log answers later; until then the blocker's own count shows.
    val blockerBlocked = controller.privacySnapshot(id).totalBlocked
    var engineTotal by remember(id) { mutableStateOf<Int?>(null) }
    LaunchedEffect(id, visible, blockerBlocked, panel.level) {
        if (visible) controller.siteProtection.counts(id) { engineTotal = it.total }
    }
    val on = panel.level != SiteProtectionLevel.Off
    val strict = panel.level == SiteProtectionLevel.Strict
    val policy = panel.policy
    val state = V5SiteProtectionState(
        host = panel.host ?: tab.url,
        protectionOn = on,
        strict = strict,
        blocked = engineTotal ?: panel.counts.total,
        privateTab = !panel.canPersist,
        // Offered only where the browser blocks them at all.
        thirdPartyCookiesAllowed = SiteProtectionRules.acceptsThirdPartyCookies(policy)
            .takeIf { on && policy.blockThirdPartyCookies },
        cookieBannersHidden = policy.hideCookieBanners
            .takeIf { on && controller.supportsPageContentActions && (strict || controller.blockerSettings.hideCookieConsent) },
        deAmp = policy.deAmp.takeIf { on && controller.isAutoDeAmpEnabled },
        fingerprintingProtection = policy.fingerprintingProtection.takeIf { on && panel.perSiteFingerprintingProtection },
        httpsOnly = if (controller.isHttpsOnlySupported) {
            stringResource(
                when (controller.httpsOnlyMode) {
                    HttpsOnlyMode.Always -> R.string.settings_https_only_mode_always
                    HttpsOnlyMode.PrivateTabs -> R.string.settings_https_only_mode_private_tabs
                    HttpsOnlyMode.Off -> R.string.settings_https_only_mode_off
                },
            )
        } else {
            null
        },
    )
    val context = LocalContext.current
    val copied = stringResource(R.string.v5_site_report_copied)
    val actions = V5SiteProtectionActions(
        onProtectionChange = { protect ->
            if (protect) controller.resumeSiteProtection(id) else controller.pauseSiteProtection(id, persistently = panel.canPersist)
        },
        onStrictChange = { toStrict ->
            controller.siteProtection.setLevel(id, if (toStrict) SiteProtectionLevel.Strict else SiteProtectionLevel.Standard)
        },
        onThirdPartyCookiesChange = { allow ->
            if (allow) {
                controller.updateSitePrivacyOverrides(id) { current, _ ->
                    current.copy(thirdPartyLoginAllowed = true, captchaCompatibilityAllowed = true)
                }
            } else {
                controller.revokeThirdPartyCookieCompatibility(id)
            }
        },
        onCookieBannersChange = { hidden -> controller.siteProtection.setCookieBannersHidden(id, hidden) },
        onDeAmpChange = { enabled -> controller.siteProtection.setDeAmp(id, enabled) },
        onFingerprintingChange = { enabled -> controller.siteProtection.setFingerprintingProtection(id, enabled) },
        onHttpsOnly = {
            onDismiss()
            onOpenProtectionSettings()
        },
        onCopyReport = {
            val report = V5SiteProtectionRules.report(
                V5SiteProtectionRules.Report(
                    host = state.host,
                    engine = if (controller.usesGeckoEngine) "GeckoView" else "System WebView",
                    appVersion = BuildConfig.VERSION_NAME,
                    protectionOn = state.protectionOn,
                    strict = state.strict,
                    blocked = state.blocked,
                    thirdPartyCookiesAllowed = state.thirdPartyCookiesAllowed,
                    cookieBannersHidden = state.cookieBannersHidden,
                    deAmp = state.deAmp,
                    fingerprintingProtection = state.fingerprintingProtection,
                    httpsOnly = controller.httpsOnlyMode.name.takeIf { controller.isHttpsOnlySupported },
                ),
            )
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText(copied, report).markedSensitive(tab.isIncognito))
            // Android 13 and later confirm a copy themselves.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        },
        onReset = {
            controller.updateSitePrivacyOverrides(id) { _, _ -> SitePrivacyOverrides() }
            if (!on) controller.resumeSiteProtection(id)
        },
        onSiteInfo = onSiteInfo,
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
    VolaV5Theme(dark = dark, accent = controller.v5Accent, privateBrowsing = tab.isIncognito) {
        V5SiteProtection(visible = visible, state = state, actions = actions, onDismiss = onDismiss)
    }
}

/** Not state: a new tab recomposes anyway, and the old one only outlives it while the panel hides. */
private class LastTab {
    var id: String? = null
}

/** Below this background luminance the current theme is dark. */
private const val DARK_BACKGROUND_LUMINANCE = 0.5f
