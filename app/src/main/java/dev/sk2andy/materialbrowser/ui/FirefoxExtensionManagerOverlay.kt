@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtension
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagerMessage
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagerState

internal object FirefoxExtensionManagerTestTags {
    const val Overlay = "firefox_extension_manager"
    const val InstallUrl = "gecko_extension_install_url"
    const val PermissionPrompt = "gecko_extension_permission_prompt"
    const val UninstallConfirm = "gecko_extension_uninstall_confirm"

    fun optionsPage(extensionId: String): String = "gecko_extension_options:$extensionId"
}

@Composable
internal fun FirefoxExtensionManagerOverlay(
    state: GeckoExtensionManagerState,
    onInstall: (String) -> Unit,
    onSetEnabled: (GeckoExtension, Boolean) -> Unit,
    onSetPrivate: (GeckoExtension, Boolean) -> Unit,
    onUpdate: (GeckoExtension) -> Unit,
    onUninstall: (GeckoExtension) -> Unit,
    onOpenOptionsPage: (GeckoExtension) -> Unit,
    onPermissionDecision: (Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    FirefoxExtensionManagerV5(
        state = state,
        onInstall = onInstall,
        onSetEnabled = onSetEnabled,
        onSetPrivate = onSetPrivate,
        onUpdate = onUpdate,
        onUninstall = onUninstall,
        onOpenOptionsPage = onOpenOptionsPage,
        onPermissionDecision = onPermissionDecision,
        onDismiss = onDismiss,
    )
}

/** The text of a manager message, shared by the v4 and v5 managers. */
internal fun extensionManagerMessageRes(message: GeckoExtensionManagerMessage): Int = when (message) {
    GeckoExtensionManagerMessage.InvalidSignedXpi ->
        R.string.gecko_extension_invalid_xpi
    GeckoExtensionManagerMessage.InstallNetworkFailure ->
        R.string.gecko_extension_install_network_failure
    GeckoExtensionManagerMessage.InstallIncorrectHash ->
        R.string.gecko_extension_install_incorrect_hash
    GeckoExtensionManagerMessage.InstallCorruptFile ->
        R.string.gecko_extension_install_corrupt_file
    GeckoExtensionManagerMessage.InstallFileAccess ->
        R.string.gecko_extension_install_file_access
    GeckoExtensionManagerMessage.InstallUnsigned ->
        R.string.gecko_extension_install_unsigned
    GeckoExtensionManagerMessage.InstallUnexpectedType ->
        R.string.gecko_extension_install_unexpected_type
    GeckoExtensionManagerMessage.InstallUnexpectedVersion ->
        R.string.gecko_extension_install_unexpected_version
    GeckoExtensionManagerMessage.InstallIncorrectId ->
        R.string.gecko_extension_install_incorrect_id
    GeckoExtensionManagerMessage.InstallInvalidDomain ->
        R.string.gecko_extension_install_invalid_domain
    GeckoExtensionManagerMessage.InstallBlocklisted ->
        R.string.gecko_extension_install_blocklisted
    GeckoExtensionManagerMessage.InstallIncompatible ->
        R.string.gecko_extension_install_incompatible
    GeckoExtensionManagerMessage.InstallUnsupportedType ->
        R.string.gecko_extension_install_unsupported_type
    GeckoExtensionManagerMessage.InstallAdminOnly ->
        R.string.gecko_extension_install_admin_only
    GeckoExtensionManagerMessage.InstallSoftBlocked ->
        R.string.gecko_extension_install_soft_blocked
    GeckoExtensionManagerMessage.InstallCancelled ->
        R.string.gecko_extension_install_cancelled
    GeckoExtensionManagerMessage.InstallPostponed ->
        R.string.gecko_extension_install_postponed
    GeckoExtensionManagerMessage.ActionRejected ->
        R.string.gecko_extension_action_rejected
    GeckoExtensionManagerMessage.ActionFailed ->
        R.string.gecko_extension_action_failed
}
