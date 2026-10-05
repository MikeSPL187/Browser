@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.activity.compose.BackHandler
import dev.sk2andy.materialbrowser.browser.SiteCertificate

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
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
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor
import dev.sk2andy.materialbrowser.ui.theme.browserChromeSurfaceTokens
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
    websiteNotificationsSupported: Boolean,
    backdropSource: CandyChromeBackdropSource? = null,
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
    val chromeTokens = browserChromeSurfaceTokens().copy(
        containerColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerLow),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .testTag(PrivacyXRayTestTags.Sheet)
            .semantics { paneTitle = title },
        containerColor = Color.Transparent,
        dragHandle = {
            CandyChromeSurface(
                backdropSource = backdropSource,
                tokens = chromeTokens,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                blurCornerRadius = 0.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomSheetDefaults.DragHandle()
                }
            }
        },
    ) {
        CandyChromeSurface(
            backdropSource = backdropSource,
            tokens = chromeTokens,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            blurCornerRadius = 0.dp,
        ) {
            Column {
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
                            notificationsSupported = websiteNotificationsSupported,
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
                        websiteNotificationsSupported = websiteNotificationsSupported,
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
        }
    }

    if (pauseWarningVisible) {
        AlertDialog(
            onDismissRequest = { pauseWarningVisible = false },
            modifier = Modifier.testTag(PrivacyXRayTestTags.Warning),
            title = { Text(stringResource(R.string.privacy_pause_warning_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.privacy_pause_warning_message,
                        siteState.host.orEmpty(),
                    ),
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = {
                            pauseWarningVisible = false
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onPause(false)
                        },
                        modifier = Modifier.testTag(PrivacyXRayTestTags.PauseTemporary),
                    ) {
                        Text(stringResource(R.string.privacy_pause_temporary))
                    }
                    if (siteState.canPersist) {
                        TextButton(
                            onClick = {
                                pauseWarningVisible = false
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onPause(true)
                            },
                            modifier = Modifier.testTag(PrivacyXRayTestTags.PausePersistent),
                        ) {
                            Text(stringResource(R.string.privacy_pause_persistent))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pauseWarningVisible = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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

internal fun siteConnectionIcon(kind: SiteConnectionKind): ImageVector = when (kind) {
    SiteConnectionKind.Https -> VolaIcons.Lock
    SiteConnectionKind.Http -> VolaIcons.WarningFilled
    SiteConnectionKind.Unavailable -> VolaIcons.Info
    SiteConnectionKind.Other -> VolaIcons.Info
}

