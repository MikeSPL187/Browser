package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtension
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionManagementContext
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagerMessage
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagerState
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionPermissionRequest
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionPermissionRequestKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirefoxExtensionManagerV5RulesTest {
    private val extension = GeckoExtension(
        id = "addon@example.org",
        name = "Example",
        version = "2.1",
        enabled = true,
        allowedInPrivateBrowsing = false,
        isBuiltIn = false,
        baseUrl = "moz-extension://example/",
        optionsPageUrl = "moz-extension://example/options.html",
    )

    @Test
    fun `an extension without a name is shown by its id`() {
        assertEquals("Example", FirefoxExtensionManagerV5Rules.name(extension))
        assertEquals("addon@example.org", FirefoxExtensionManagerV5Rules.name(extension.copy(name = null)))
    }

    @Test
    fun `details give the version and the id, skipping a missing version`() {
        assertEquals("2.1 · addon@example.org", FirefoxExtensionManagerV5Rules.details(extension))
        assertEquals("addon@example.org", FirefoxExtensionManagerV5Rules.details(extension.copy(version = null)))
    }

    @Test
    fun `built-in extensions and a private tab leave the controls read-only`() {
        assertTrue(FirefoxExtensionManagerV5Rules.controlsEnabled(extension, canManage = true))
        assertFalse(FirefoxExtensionManagerV5Rules.controlsEnabled(extension.copy(isBuiltIn = true), canManage = true))
        assertFalse(FirefoxExtensionManagerV5Rules.controlsEnabled(extension, canManage = false))
    }

    @Test
    fun `the options page opens only for an enabled extension that has one`() {
        assertTrue(FirefoxExtensionManagerV5Rules.optionsAvailable(extension, canManage = true))
        assertFalse(FirefoxExtensionManagerV5Rules.optionsAvailable(extension.copy(enabled = false), canManage = true))
        assertFalse(FirefoxExtensionManagerV5Rules.optionsAvailable(extension.copy(optionsPageUrl = null), canManage = true))
        assertFalse(FirefoxExtensionManagerV5Rules.optionsAvailable(extension, canManage = false))
    }

    @Test
    fun `requested access lists each permission once in Gecko's order`() {
        val request = GeckoExtensionPermissionRequest(
            kind = GeckoExtensionPermissionRequestKind.Install,
            extensionId = extension.id,
            extensionName = extension.name,
            permissions = listOf("storage", "tabs"),
            origins = listOf("<all_urls>", "tabs"),
            dataCollectionPermissions = listOf("technicalAndInteraction"),
        )
        assertEquals(
            listOf("storage", "tabs", "<all_urls>", "technicalAndInteraction"),
            FirefoxExtensionManagerV5Rules.requestedAccess(request),
        )
    }

    @Test
    fun `notices say why changes are off before what went wrong last`() {
        assertEquals(emptyList<Int>(), FirefoxExtensionManagerV5Rules.notices(GeckoExtensionManagerState()))
        val state = GeckoExtensionManagerState(
            managementContext = GeckoExtensionManagementContext(profileId = "default", isPrivate = true),
            message = GeckoExtensionManagerMessage.ActionFailed,
        )
        assertEquals(
            listOf(R.string.gecko_extension_private_management_disabled, R.string.gecko_extension_action_failed),
            FirefoxExtensionManagerV5Rules.notices(state),
        )
    }
}
