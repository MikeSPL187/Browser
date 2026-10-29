@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet
import androidx.activity.compose.BackHandler
import dev.sk2andy.materialbrowser.browser.SiteCertificate

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.CandyRuleAction
import dev.sk2andy.materialbrowser.blocking.PrivacyXRaySnapshot
import dev.sk2andy.materialbrowser.blocking.SiteProtectionState
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.browser.SiteConnectionRules
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarSnapshot
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import dev.sk2andy.materialbrowser.data.ProtectionWeek
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaSiteInfo

internal object PrivacyXRayTestTags {
    const val SettingsCounter = "privacy_xray_settings_counter"
    const val Sheet = "privacy_xray_sheet"
    const val Total = "privacy_xray_total"
    const val Domains = "privacy_xray_domains"
    const val ToggleDetails = "privacy_xray_toggle_details"
    const val Pause = "privacy_xray_pause"
    const val Warning = "privacy_xray_warning"
    const val PauseTemporary = "privacy_xray_pause_temporary"
    const val PausePersistent = "privacy_xray_pause_persistent"
    const val XRayTitle = "privacy_xray_title"
    const val Connection = "site_connection"
    const val XRayTab = "site_info_xray_tab"
    const val PermissionsTab = "site_info_permissions_tab"
}

@Composable
internal fun PrivacyXRaySheet(
    pageUrl: String,
    connectionKind: SiteConnectionKind,
    snapshot: PrivacyXRaySnapshot,
    siteState: SiteProtectionState,
    permissionSnapshot: PermissionRadarSnapshot,
    workspaceName: String,
    supportedPermissions: Set<SitePermission>,
    onPause: (persistently: Boolean) -> Unit,
    onResume: () -> Unit,
    onRevokeThirdPartyCookieCompatibility: () -> Unit = {},
    onRuleAction: (domain: String, action: CandyRuleAction, siteScoped: Boolean) -> Unit =
        { _, _, _ -> },
    onOpenStudio: (ruleId: String?) -> Unit = {},
    onPermissionOriginSelected: (String) -> Unit,
    onPermissionDecisionChanged: (SitePermission, SitePermissionDecision) -> Unit,
    onResetSitePermissions: () -> Unit,
    onDismiss: () -> Unit,
    canTogglePopups: Boolean = false,
    popupsBlocked: Boolean = false,
    onPopupsBlockedChange: (Boolean) -> Unit = {},
    pageFixes: SiteInfoPageFixes? = null,
    siteData: SiteInfoSiteData? = null,
    certificate: SiteCertificate? = null,
    week: ProtectionWeek? = null,
    onOpenProtectionSettings: (() -> Unit)? = null,
) {
    val title = stringResource(R.string.site_info_title)
    var page by remember(pageUrl) { mutableStateOf(SiteInfoPage.Overview) }
    var pauseWarningVisible by remember(siteState.host) { mutableStateOf(false) }
    val view = LocalView.current
    val pages: @Composable () -> Unit = {
                // Inside the sheet's window, so Back returns to the overview before it closes.
                BackHandler(enabled = page != SiteInfoPage.Overview) { page = SiteInfoPage.Overview }
                if (page != SiteInfoPage.Overview) {
                    SiteInfoPageBar(
                        title = stringResource(
                            when (page) {
                                SiteInfoPage.PrivacyXRay -> R.string.privacy_xray_title
                                SiteInfoPage.Certificate -> R.string.site_info_certificate
                                else -> R.string.permission_radar_title
                            },
                        ),
                        onBack = { page = SiteInfoPage.Overview },
                    )
                }
                if (page == SiteInfoPage.Overview) {
                    SiteInfoOverview(
                        pageUrl = pageUrl,
                        connectionKind = connectionKind,
                        blockedCount = snapshot.totalBlocked,
                        permissions = SiteInfoRules.visiblePermissions(
                            entries = permissionSnapshot.entries,
                            supportedPermissions = supportedPermissions,
                            isPrivate = permissionSnapshot.isPrivate,
                        ),
                        canChangePermissions = permissionSnapshot.site != null,
                        protectionOn = !siteState.isPaused,
                        canTogglePopups = canTogglePopups,
                        popupsBlocked = popupsBlocked,
                        onOpenPrivacyXRay = { page = SiteInfoPage.PrivacyXRay },
                        onOpenPermissions = { page = SiteInfoPage.Permissions },
                        onPermissionDecisionChanged = onPermissionDecisionChanged,
                        onProtectionChange = { on ->
                            if (on) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onResume()
                            } else {
                                pauseWarningVisible = true
                            }
                        },
                        onPopupsBlockedChange = onPopupsBlockedChange,
                        pageFixes = pageFixes,
                        siteData = siteData,
                        certificate = certificate,
                        onOpenCertificate = { page = SiteInfoPage.Certificate },
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(
                                start = VolaSiteInfo.sidePadding,
                                end = VolaSiteInfo.sidePadding,
                                bottom = VolaSiteInfo.bottomPadding,
                            ),
                    )
                } else if (page == SiteInfoPage.Certificate) {
                    certificate?.let { cert ->
                        SiteCertificateContent(
                            certificate = cert,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(
                                    start = VolaSiteInfo.sidePadding,
                                    end = VolaSiteInfo.sidePadding,
                                    bottom = VolaSiteInfo.bottomPadding,
                                ),
                        )
                    }
                } else if (page == SiteInfoPage.PrivacyXRay) {
                    PrivacyXRayContent(
                        snapshot = snapshot,
                        host = SiteConnectionRules.host(pageUrl).ifEmpty { null },
                        siteState = siteState,
                        week = week,
                        onRevokeThirdPartyCookieCompatibility =
                            onRevokeThirdPartyCookieCompatibility,
                        onRuleAction = onRuleAction,
                        onOpenStudio = onOpenStudio,
                        onOpenProtectionSettings = onOpenProtectionSettings,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(
                                start = VolaSiteInfo.sidePadding,
                                end = VolaSiteInfo.sidePadding,
                                bottom = VolaSiteInfo.bottomPadding,
                            ),
                    )
                } else {
                    PermissionRadarContent(
                        snapshot = permissionSnapshot,
                        workspaceName = workspaceName,
                        supportedPermissions = supportedPermissions,
                        onOriginSelected = onPermissionOriginSelected,
                        onDecisionChanged = onPermissionDecisionChanged,
                        onResetSite = onResetSitePermissions,
                        modifier = Modifier
                            .heightIn(max = 720.dp)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    )
                }
    }
    // V28, B8: the pages in a v5 sheet, their pieces drawn the v5 way.
    VolaV5Theme(dark = MaterialTheme.colorScheme.background.luminance() < SITE_INFO_DARK_LUMINANCE) {
        CompositionLocalProvider(LocalSiteInfoV5 provides true) {
            V5WindowSheet(
                visible = true,
                onDismiss = onDismiss,
                title = title,
                modifier = Modifier.testTag(PrivacyXRayTestTags.Sheet),
            ) {
                V5SheetHeader(title = title, onClose = onDismiss)
                pages()
            }
        }
    }

    if (pauseWarningVisible) {
        val pause = { persistent: Boolean ->
            pauseWarningVisible = false
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onPause(persistent)
        }
        // The question is the v5 prompt sheet: both answers turn protection off, so red.
        V5PromptTheme {
            V5PromptSheet(
                visible = true,
                title = stringResource(R.string.privacy_pause_warning_title),
                message = stringResource(R.string.privacy_pause_warning_message, siteState.host.orEmpty()),
                actions = listOfNotNull(
                    V5PromptAction(
                        stringResource(R.string.privacy_pause_temporary),
                        { pause(false) },
                        V5PromptRole.Destructive,
                        PrivacyXRayTestTags.PauseTemporary,
                    ),
                    V5PromptAction(
                        stringResource(R.string.privacy_pause_persistent),
                        { pause(true) },
                        V5PromptRole.Destructive,
                        PrivacyXRayTestTags.PausePersistent,
                    ).takeIf { siteState.canPersist },
                ),
                onDismiss = { pauseWarningVisible = false },
                testTag = PrivacyXRayTestTags.Warning,
            )
        }
    }
}

@Composable
internal fun siteConnectionLabel(kind: SiteConnectionKind): String = stringResource(
    when (kind) {
        SiteConnectionKind.Https -> R.string.site_connection_https
        SiteConnectionKind.Http -> R.string.site_connection_http
        SiteConnectionKind.Unavailable -> R.string.site_connection_unavailable
        SiteConnectionKind.Other -> R.string.site_connection_other
    },
)

/** Below this background luminance the shell is dark. */
private const val SITE_INFO_DARK_LUMINANCE = 0.5f

internal fun siteConnectionIcon(kind: SiteConnectionKind): ImageVector = when (kind) {
    SiteConnectionKind.Https -> VolaIcons.Lock
    SiteConnectionKind.Http -> VolaIcons.WarningFilled
    SiteConnectionKind.Unavailable -> VolaIcons.Info
    SiteConnectionKind.Other -> VolaIcons.Info
}

