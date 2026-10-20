package dev.sk2andy.materialbrowser.browser.engine

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtension
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionManagementContext
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionMutationResult
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionOptionsTarget
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionPermissionDecision
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionPermissionRequest
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionReadResult
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionRejection
import dev.sk2andy.materialbrowser.browser.engine.GeckoExtensionSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal enum class GeckoExtensionManagerMessage {
    InvalidSignedXpi,
    InstallNetworkFailure,
    InstallIncorrectHash,
    InstallCorruptFile,
    InstallFileAccess,
    InstallUnsigned,
    InstallUnexpectedType,
    InstallUnexpectedVersion,
    InstallIncorrectId,
    InstallInvalidDomain,
    InstallBlocklisted,
    InstallIncompatible,
    InstallUnsupportedType,
    InstallAdminOnly,
    InstallSoftBlocked,
    InstallCancelled,
    InstallPostponed,
    ActionRejected,
    ActionFailed,
}

internal data class GeckoExtensionManagerState(
    val snapshot: GeckoExtensionSnapshot = GeckoExtensionSnapshot(emptyList()),
    val managementContext: GeckoExtensionManagementContext =
        GeckoExtensionManagementContext(profileId = DEFAULT_PROFILE_ID, isPrivate = false),
    val busy: Boolean = false,
    val message: GeckoExtensionManagerMessage? = null,
    val permissionRequest: GeckoExtensionPermissionRequest? = null,
) {
    val canManage: Boolean
        get() = !busy && !managementContext.isPrivate

    private companion object {
        const val DEFAULT_PROFILE_ID = "default"
    }
}

