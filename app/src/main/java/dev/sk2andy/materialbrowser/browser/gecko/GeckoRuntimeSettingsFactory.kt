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
    ): GeckoRuntimeSettings = GeckoRuntimeSettings.Builder()
        .contentBlocking(contentBlocking)
        .loginAutofillEnabled(loginAutofillEnabled)
        .automaticFontSizeAdjustment(false)
        // Gecko owns its CA store; Android Network Security Config alone cannot opt it in.
        .enterpriseRootsEnabled(trustUserCertificates)
        .build()
        .apply {
            setFingerprintingProtection(true)
            setFingerprintingProtectionPrivateBrowsing(true)
            setGlobalPrivacyControl(globalPrivacyControl)
            applyEngineProtections()
            applyDnsOverHttpsSettings(dnsOverHttpsSettings)
            applyHttpsOnlyMode(httpsOnlyMode)
            disableWebAuthn()
        }
}

/** The Gecko preference that exposes the Web Authentication API (passkeys, security keys). */
internal const val WEB_AUTHN_PREF = "security.webauth.webauthn"

/**
 * GeckoView reaches WebAuthn through Google Play services FIDO, which Vola does not ship: always to
 * ask whether a platform authenticator exists, and for a passkey whenever the system Credential
 * Manager has none. accounts.google.com asks on load, and the missing Play services classes crashed
 * Gecko on every visit (#123, H1). Without the API, sites fall back to their password sign-in.
 */
@UiThread
internal fun GeckoRuntimeSettings.disableWebAuthn() {
    CandyGeckoPrefsBridge.setStartupPref(this, WEB_AUTHN_PREF, false)
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
