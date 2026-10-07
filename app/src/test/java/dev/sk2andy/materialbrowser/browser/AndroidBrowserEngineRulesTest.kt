package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBrowserEngineRulesTest {
    @Test
    fun `unknown stored engine falls back to GeckoView`() {
        assertEquals(
            AndroidBrowserEngineKind.GeckoView,
            AndroidBrowserEngineKind.fromStableId("future-engine"),
        )
    }

    @Test
    fun `system WebView-only build ignores persisted Gecko selection`() {
        assertEquals(
            AndroidBrowserEngineKind.SystemWebView,
            AndroidBrowserEngineRules.persistedKind(
                stableId = AndroidBrowserEngineKind.GeckoView.stableId,
                systemWebViewOnly = true,
            ),
        )
        assertFalse(
            AndroidBrowserEngineRules.canSelect(
                kind = AndroidBrowserEngineKind.GeckoView,
                systemWebViewOnly = true,
            ),
        )
        assertTrue(
            AndroidBrowserEngineRules.canSelect(
                kind = AndroidBrowserEngineKind.SystemWebView,
                systemWebViewOnly = true,
            ),
        )
    }

    @Test
    fun `system WebView exposes toppings without Firefox extensions`() {
        val capabilities = AndroidBrowserEngineRules.capabilities(
            AndroidBrowserEngineKind.SystemWebView,
        )

        assertTrue(capabilities.toppings)
        assertFalse(capabilities.firefoxExtensions)
        assertTrue(capabilities.nativeAutoplayPolicy)
        assertFalse(capabilities.insecureHttpPasswordManagerSelection)
    }

    @Test
    fun `only GeckoView supports insecure HTTP password manager selection`() {
        assertTrue(
            AndroidBrowserEngineRules.capabilities(AndroidBrowserEngineKind.GeckoView)
                .insecureHttpPasswordManagerSelection,
        )
        assertFalse(
            AndroidBrowserEngineRules.capabilities(AndroidBrowserEngineKind.SystemWebView)
                .insecureHttpPasswordManagerSelection,
        )
    }

    @Test
    fun `site permissions list only what each adapter routes`() {
        val gecko = AndroidBrowserEngineRules.capabilities(AndroidBrowserEngineKind.GeckoView)
        val webView = AndroidBrowserEngineRules.capabilities(AndroidBrowserEngineKind.SystemWebView)

        assertTrue(SitePermission.ProtectedMedia in gecko.sitePermissions)
        assertTrue(SitePermission.Notifications in gecko.sitePermissions)
        assertFalse(SitePermission.MidiSysex in gecko.sitePermissions)
        assertEquals(
            setOf(SitePermission.Camera, SitePermission.Microphone, SitePermission.Location),
            webView.sitePermissions,
        )
    }
}
