package dev.sk2andy.materialbrowser.browser.gecko

import androidx.annotation.UiThread
import dev.sk2andy.materialbrowser.BuildConfig
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsRules
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.credentials.vault.CredentialVaultFeature
import org.mozilla.geckoview.CandyGeckoPrefsBridge
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntimeSettings

internal object GeckoRuntimeSettingsFactory {
    @UiThread
    fun create(
        contentBlocking: ContentBlocking.Settings,
        trustUserCertificates: Boolean = BuildConfig.TRUST_USER_CERTIFICATES,
        dnsOverHttpsSettings: DnsOverHttpsSettings = DnsOverHttpsRules.Default,
        httpsOnlyMode: HttpsOnlyMode = HttpsOnlyMode.Default,
        // With Vola's vault as Gecko's login storage, a login is filled only from the login prompt,
        // never silently on page load.
        loginAutofillEnabled: Boolean = !CredentialVaultFeature.ENABLED,
        globalPrivacyControl: Boolean = true,
        // Passkeys need the system Credential Manager (Android 14+); see [applyWebAuthn].
        webAuthnEnabled: Boolean = false,
    ): GeckoRuntimeSettings = GeckoRuntimeSettings.Builder()
        .contentBlocking(contentBlocking)
        .loginAutofillEnabled(loginAutofillEnabled)
        .automaticFontSizeAdjustment(false)
        // Gecko owns its CA store; Android Network Security Config alone cannot opt it in.
        .enterpriseRootsEnabled(trustUserCertificates)
        // Pinned: Gecko 158 turns Fission on by default, a later default must not turn it off quietly.
        .fissionEnabled(true)
        .build()
        .apply {
            setFingerprintingProtection(true)
            setFingerprintingProtectionPrivateBrowsing(true)
            setGlobalPrivacyControl(globalPrivacyControl)
            applyEngineProtections()
            applyDnsOverHttpsSettings(dnsOverHttpsSettings)
            applyHttpsOnlyMode(httpsOnlyMode)
            applyWebAuthn(webAuthnEnabled)
            // Gecko's own default isolates every site in its own process (1), the most memory there
            // is; Firefox for Android isolates only sites someone signed in to or that ask for it.
            setWebContentIsolationStrategy(GeckoRuntimeSettings.STRATEGY_ISOLATE_HIGH_VALUE)
            CandyGeckoPrefsBridge.setStartupPref(this, PROCESS_PRELAUNCH_PREF, false)
        }
}

/**
 * Gecko keeps a spare content process started for the next page; with one page open it is a whole
 * process of memory doing nothing. Off, a page that needs a new process starts one on demand.
 */
internal const val PROCESS_PRELAUNCH_PREF = "dom.ipc.processPrelaunch.enabled"

/** The Gecko preference that exposes the Web Authentication API (passkeys, security keys). */
internal const val WEB_AUTHN_PREF = "security.webauth.webauthn"

/**
 * GeckoView asks the system Credential Manager for passkeys and falls back to Google Play services
 * FIDO, which Vola does not ship. Missing Play services classes once crashed Gecko on
 * accounts.google.com (#123, H1); Vola now carries stand-ins for them (`src/full/java/com/google`)
 * that refuse every request, so that fallback rejects the page's promise instead.
 *
 * On by [PasskeySupport][dev.sk2andy.materialbrowser.browser.credentials.PasskeySupport] only:
 * without Credential Manager (Android 12 and 13) every request would end in that refusal, so the
 * API stays hidden and sites offer their password sign-in.
 */
@UiThread
internal fun GeckoRuntimeSettings.applyWebAuthn(enabled: Boolean) {
    CandyGeckoPrefsBridge.setStartupPref(this, WEB_AUTHN_PREF, enabled)
}

/**
 * Protections Gecko ships but leaves off or on trial (stage S1, `docs/vola/research-2026-10.md`).
 * Those GeckoView 157 already enforces are pinned, so a later default cannot quietly drop them.
 */
@UiThread
internal fun GeckoRuntimeSettings.applyEngineProtections() {
    contentBlocking
        .setQueryParameterStrippingEnabled(true)
        .setQueryParameterStrippingPrivateBrowsingEnabled(true)
        .setQueryParameterStrippingStripList(*GeckoEngineProtections.strippedQueryParameters.toTypedArray())
        .setBounceTrackingProtectionMode(
            ContentBlocking.BounceTrackingProtectionMode.BOUNCE_TRACKING_PROTECTION_MODE_ENABLED,
        )
    setCertificateTransparencyMode(GeckoEngineProtections.CERTIFICATE_TRANSPARENCY_ENFORCE)
    setPostQuantumKeyExchangeEnabled(true)
    // Public sites may not reach the local network or this device; Gecko denies unless allowed.
    setLnaEnabled(true)
    setLnaBlocking(true)
}

internal object GeckoEngineProtections {
    /** security.pki.certificate_transparency.mode: 2 rejects certificates without CT proofs. */
    const val CERTIFICATE_TRANSPARENCY_ENFORCE = 2

    /**
     * Click and campaign identifiers removed from links before a page loads, after the lists of
     * Firefox and Brave. Analytics tags such as utm_* stay: they do not identify the person.
     */
    val strippedQueryParameters = listOf(
        "__hsfp", "__hssc", "__hstc", "__s", "_hsenc", "_openstat",
        "dclid", "fbclid", "gbraid", "gclid", "hsctatracking", "igshid",
        "li_fat_id", "mc_eid", "mkt_tok", "ml_subscriber", "ml_subscriber_hash", "msclkid",
        "oft_c", "oft_ck", "oft_d", "oft_id", "oft_ids", "oft_k", "oft_lk", "oft_sk",
        "oly_anon_id", "oly_enc_id", "rb_clickid", "s_cid", "ttclid", "twclid",
        "vero_conv", "vero_id", "wbraid", "wickedid", "yclid", "ysclid",
    )
}

@UiThread
internal fun GeckoRuntimeSettings.applyHttpsOnlyMode(mode: HttpsOnlyMode) {
    setAllowInsecureConnections(GeckoHttpsOnlyRules.allowInsecureConnections(mode))
}

internal object GeckoHttpsOnlyRules {
    fun allowInsecureConnections(mode: HttpsOnlyMode): Int = when (mode) {
        HttpsOnlyMode.Always -> GeckoRuntimeSettings.HTTPS_ONLY
        HttpsOnlyMode.PrivateTabs -> GeckoRuntimeSettings.HTTPS_ONLY_PRIVATE
        HttpsOnlyMode.Off -> GeckoRuntimeSettings.ALLOW_ALL
    }
}

@UiThread
internal fun GeckoRuntimeSettings.applyDnsOverHttpsSettings(settings: DnsOverHttpsSettings) {
    setDohAutoselectEnabled(false)
    val endpoint = DnsOverHttpsRules.endpoint(settings)
    if (endpoint == null) {
        setTrustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_DISABLED)
        setTrustedRecursiveResolverUri("")
    } else {
        setTrustedRecursiveResolverUri(endpoint)
        setTrustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_ONLY)
    }
}
