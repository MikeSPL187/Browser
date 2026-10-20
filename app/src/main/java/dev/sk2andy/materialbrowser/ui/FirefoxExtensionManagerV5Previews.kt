package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtension
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagerState
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionPermissionRequest
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionPermissionRequestKind
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionSnapshot
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/* Б12a: the Firefox extension manager under ui.v5. Preview-only sample names are not localized. */

private val previewExtensions = listOf(
    GeckoExtension(
        id = "uBlock0@raymondhill.net",
        name = "uBlock Origin",
        version = "1.62.0",
        enabled = true,
        allowedInPrivateBrowsing = true,
        isBuiltIn = false,
        baseUrl = "moz-extension://ublock/",
        optionsPageUrl = "moz-extension://ublock/dashboard.html",
    ),
    GeckoExtension(
        id = "addon@darkreader.org",
        name = "Dark Reader",
        version = "4.9.99",
        enabled = false,
        allowedInPrivateBrowsing = false,
        isBuiltIn = false,
    ),
    GeckoExtension(
        id = "candy-privacy@vola",
        name = "Vola protection",
        version = "1.0",
        enabled = true,
        allowedInPrivateBrowsing = true,
        isBuiltIn = true,
    ),
)

@Composable
private fun ManagerPreview(state: GeckoExtensionManagerState, openId: String? = null) {
    MaterialBrowserTheme {
        FirefoxExtensionManagerV5(
            state = state,
            onInstall = {},
            onSetEnabled = { _, _ -> },
            onSetPrivate = { _, _ -> },
            onUpdate = {},
            onUninstall = {},
            onOpenOptionsPage = {},
            onPermissionDecision = { _, _ -> },
            onDismiss = {},
            initialOpenId = openId,
        )
    }
}

/** The installed extensions, each with whether it is on. */
@VolaPreviews
@Composable
private fun ExtensionListV5Preview() {
    ManagerPreview(GeckoExtensionManagerState(snapshot = GeckoExtensionSnapshot(previewExtensions)))
}

/** One extension's page: switches, its settings, update and remove. */
@VolaPreviews
@Composable
private fun ExtensionPageV5Preview() {
    ManagerPreview(
        GeckoExtensionManagerState(snapshot = GeckoExtensionSnapshot(previewExtensions)),
        openId = "uBlock0@raymondhill.net",
    )
}

/** A built-in extension: read-only, with the reason under its switches. */
@VolaPreviews
@Composable
private fun BuiltInExtensionPageV5Preview() {
    ManagerPreview(
        GeckoExtensionManagerState(snapshot = GeckoExtensionSnapshot(previewExtensions)),
        openId = "candy-privacy@vola",
    )
}

/** Nothing installed yet. */
@VolaPreviews
@Composable
private fun EmptyExtensionsV5Preview() {
    ManagerPreview(GeckoExtensionManagerState())
}

/** The permission question over the list. */
@VolaPreviews
@Composable
private fun ExtensionPermissionV5Preview() {
    ManagerPreview(
        GeckoExtensionManagerState(
            snapshot = GeckoExtensionSnapshot(previewExtensions),
            permissionRequest = GeckoExtensionPermissionRequest(
                kind = GeckoExtensionPermissionRequestKind.Install,
                extensionId = "addon@darkreader.org",
                extensionName = "Dark Reader",
                permissions = listOf("storage", "tabs"),
                origins = listOf("<all_urls>"),
            ),
        ),
    )
}
