package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateTabRulesTest {
    @Test
    fun `storage follows the engine and its profile support`() {
        assertEquals(
            PrivateTabStorage.Memory,
            PrivateTabRules.storage(AndroidBrowserEngineKind.GeckoView, profilesSupported = false),
        )
        assertEquals(
            PrivateTabStorage.SeparateProfile,
            PrivateTabRules.storage(AndroidBrowserEngineKind.SystemWebView, profilesSupported = true),
        )
        assertEquals(
            PrivateTabStorage.SharedWithRegularTabs,
            PrivateTabRules.storage(AndroidBrowserEngineKind.SystemWebView, profilesSupported = false),
        )
    }

    @Test
    fun `only GeckoView promises nothing on disk, shared cookies are a warning`() {
        val memory = PrivateTabRules.facts(PrivateTabStorage.Memory)
        val profile = PrivateTabRules.facts(PrivateTabStorage.SeparateProfile)
        val shared = PrivateTabRules.facts(PrivateTabStorage.SharedWithRegularTabs)

        assertTrue(memory.any { it.title == R.string.private_tab_fact_memory })
        assertTrue(profile.none { it.title == R.string.private_tab_fact_memory })
        assertTrue(shared.none { it.title == R.string.private_tab_fact_memory })
        assertEquals(listOf(R.string.private_tab_fact_shared), shared.filter { it.warning }.map { it.title })
        assertTrue(memory.none { it.warning } && profile.none { it.warning })
        assertEquals(4, memory.size)
    }
}
