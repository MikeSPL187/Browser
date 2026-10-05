package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.blocking.BlockerSettings
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsProvider
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsRules
import dev.sk2andy.materialbrowser.browser.DnsOverHttpsSettings
import dev.sk2andy.materialbrowser.browser.HttpsOnlyMode
import dev.sk2andy.materialbrowser.browser.PrivacySignalSettings
import dev.sk2andy.materialbrowser.browser.WebRtcProtectionMode
import dev.sk2andy.materialbrowser.data.HistoryRecordingMode
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCard
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardHeader
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardLinkRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardSwitchRow
import dev.sk2andy.materialbrowser.shared.ui.settings.SettingsCardValueRow
import dev.sk2andy.materialbrowser.shared.ui.theme.SettingsCardTokens
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor

internal object ProtectionSettingsTestTags {
    const val UserCaWarning = "protection_settings_user_ca_warning"
    const val MoveToVola = "protection_settings_move_to_vola"
    const val ExportAppData = "protection_settings_export_app_data"
    const val ImportAppData = "protection_settings_import_app_data"
    const val Recall = "protection_settings_recall"
    const val SaveHistory = "protection_settings_save_history"
    const val ClearHistoryOnExit = "protection_settings_clear_history_on_exit"
    const val DoNotTrack = "protection_settings_do_not_track"
    const val GlobalPrivacyControl = "protection_settings_global_privacy_control"
    const val AutoDeAmp = "protection_settings_auto_de_amp"
    const val DangerousSiteWarnings = "protection_settings_dangerous_site_warnings"
    const val WebRtcProtection = "protection_settings_webrtc_protection"
    const val DnsOverHttps = "protection_settings_dns_over_https"
    const val HttpsOnly = "protection_settings_https_only"
    const val ProtectionCard = "protection_settings_protection_card"
    const val PrivateTabsLock = "protection_settings_private_tabs_lock"
    const val CustomDnsEndpoint = "protection_settings_custom_dns_endpoint"
}

@Composable
internal fun ProtectionAndDataSettingsPage(
    blockerSettings: BlockerSettings,
    blockedCount: Int,
    browserEngineKind: AndroidBrowserEngineKind = AndroidBrowserEngineKind.GeckoView,
    isDnsOverHttpsSupported: Boolean = browserEngineKind == AndroidBrowserEngineKind.GeckoView,
    isHttpsOnlySupported: Boolean = browserEngineKind == AndroidBrowserEngineKind.GeckoView,
    webRtcProtectionMode: WebRtcProtectionMode = WebRtcProtectionMode.Default,
    privacySignalSettings: PrivacySignalSettings = PrivacySignalSettings.Default,
    isAutoDeAmpEnabled: Boolean = true,
    isDangerousSiteWarningsEnabled: Boolean = true,
    isProtectionCardVisible: Boolean = true,
    privateTabsLock: PrivateTabLock? = null,
    dnsOverHttpsSettings: DnsOverHttpsSettings = DnsOverHttpsRules.Default,
    httpsOnlyMode: HttpsOnlyMode = HttpsOnlyMode.Default,
    isRecallEnabled: Boolean = false,
    historyRecordingMode: HistoryRecordingMode = HistoryRecordingMode.Enabled,
    trustsUserCertificates: Boolean,
    onBlockerSettingsChanged: (BlockerSettings) -> Unit,
    onWebRtcProtectionModeChanged: (WebRtcProtectionMode) -> Unit = {},
    onPrivacySignalSettingsChanged: (PrivacySignalSettings) -> Unit = {},
    onAutoDeAmpEnabledChanged: (Boolean) -> Unit = {},
    onDangerousSiteWarningsEnabledChanged: (Boolean) -> Unit = {},
    onProtectionCardVisibleChanged: (Boolean) -> Unit = {},
    onDnsOverHttpsSettingsChanged: (DnsOverHttpsSettings) -> Unit = {},
    onHttpsOnlyModeChanged: (HttpsOnlyMode) -> Unit = {},
    onRecallEnabledChanged: (Boolean) -> Unit = {},
    onHistoryRecordingModeChanged: (HistoryRecordingMode) -> Unit = {},
    onPrivacyXRay: () -> Unit,
    onPermissionRadar: () -> Unit,
    onFilterStudio: () -> Unit,
    onExportAppData: () -> Unit = {},
    onImportAppData: () -> Unit = {},
    onMoveToVola: () -> Unit = {},
    onClearData: () -> Unit,
    onBack: () -> Unit,
) {
    var webRtcMenuExpanded by remember { mutableStateOf(false) }
    var dnsMenuExpanded by remember { mutableStateOf(false) }
    var httpsOnlyMenuExpanded by remember { mutableStateOf(false) }
    var customDnsDialogVisible by rememberSaveable { mutableStateOf(false) }
    if (customDnsDialogVisible) {
        CustomDnsEndpointDialog(
            initialEndpoint = dnsOverHttpsSettings.customEndpoint,
            onConfirm = { endpoint ->
                customDnsDialogVisible = false
                onDnsOverHttpsSettingsChanged(
                    DnsOverHttpsSettings(
                        provider = DnsOverHttpsProvider.Custom,
                        customEndpoint = endpoint,
                    ),
                )
            },
            onDismiss = { customDnsDialogVisible = false },
        )
    }
    val cardColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    val dividerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    SettingsPage(
        title = stringResource(R.string.settings_protection_data_title),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsCardTokens.cardGap)) {
            PrivacyXRaySettingsCounter(
                blockedCount = blockedCount,
                onClick = onPrivacyXRay,
            )
            if (trustsUserCertificates) {
                UserCaTrustWarning()
            }
            SettingsCardHeader(stringResource(R.string.settings_section_protection))
            SettingsCard(containerColor = cardColor) {
                Box {
                    SettingsCardValueRow(
                        title = stringResource(R.string.settings_https_only_title),
                        value = if (isHttpsOnlySupported) {
                            httpsOnlyMode.displayName()
                        } else {
                            stringResource(R.string.settings_https_only_unavailable)
                        },
                        summary = stringResource(
                            if (isHttpsOnlySupported) {
                                R.string.settings_https_only_summary
                            } else {
                                R.string.settings_https_only_system_webview_summary
                            },
                        ),
                        dividerColor = dividerColor,
                        divider = true,
                        enabled = isHttpsOnlySupported,
                        onClick = { httpsOnlyMenuExpanded = true },
                        modifier = Modifier.testTag(ProtectionSettingsTestTags.HttpsOnly),
                    )
                    SettingsDropdown(
                        expanded = isHttpsOnlySupported && httpsOnlyMenuExpanded,
                        onDismissRequest = { httpsOnlyMenuExpanded = false },
                    ) {
                        HttpsOnlyMode.entries.forEach { mode ->
                            SettingsDropdownItem(
                                label = mode.displayName(),
                                selected = mode == httpsOnlyMode,
                                onClick = {
                                    httpsOnlyMenuExpanded = false
                                    if (mode != httpsOnlyMode) onHttpsOnlyModeChanged(mode)
                                },
                            )
                        }
                    }
                }
                ProtectionSwitch(
                    title = stringResource(R.string.settings_dangerous_site_warnings_title),
                    summary = stringResource(R.string.settings_dangerous_site_warnings_summary),
                    checked = isDangerousSiteWarningsEnabled,
                    dividerColor = dividerColor,
                    onCheckedChange = onDangerousSiteWarningsEnabledChanged,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.DangerousSiteWarnings),
                )
                ProtectionSwitch(
                    title = stringResource(R.string.settings_block_ads_title),
                    summary = stringResource(R.string.settings_block_ads_subtitle),
                    checked = blockerSettings.blockAdsAndTrackers,
                    dividerColor = dividerColor,
                    onCheckedChange = {
                        onBlockerSettingsChanged(blockerSettings.copy(blockAdsAndTrackers = it))
                    },
                )
                ProtectionSwitch(
                    title = stringResource(R.string.settings_hide_cookie_banners_title),
                    summary = stringResource(R.string.settings_hide_cookie_banners_subtitle),
                    checked = blockerSettings.hideCookieConsent,
                    dividerColor = dividerColor,
                    onCheckedChange = {
                        onBlockerSettingsChanged(blockerSettings.copy(hideCookieConsent = it))
                    },
                )
                ProtectionSwitch(
                    title = stringResource(R.string.settings_block_third_party_cookies_title),
                    summary = stringResource(
                        R.string.settings_block_third_party_cookies_subtitle,
                    ),
                    checked = blockerSettings.blockThirdPartyCookies,
                    dividerColor = dividerColor,
                    onCheckedChange = {
                        onBlockerSettingsChanged(blockerSettings.copy(blockThirdPartyCookies = it))
                    },
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.filter_studio_title),
                    summary = stringResource(R.string.filter_studio_settings_summary),
                    dividerColor = dividerColor,
                    onClick = onFilterStudio,
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_protection_group_network))
            SettingsCard(containerColor = cardColor) {
                Box {
                    SettingsCardValueRow(
                        title = stringResource(R.string.settings_dns_over_https_title),
                        value = if (isDnsOverHttpsSupported) {
                            dnsOverHttpsSettings.provider.displayName()
                        } else {
                            stringResource(R.string.settings_dns_over_https_unavailable)
                        },
                        summary = stringResource(
                            if (!isDnsOverHttpsSupported) {
                                R.string.settings_dns_over_https_system_webview_summary
                            } else if (dnsOverHttpsSettings.provider == DnsOverHttpsProvider.System) {
                                R.string.settings_dns_over_https_system_summary
                            } else {
                                R.string.settings_dns_over_https_gecko_summary
                            },
                        ),
                        dividerColor = dividerColor,
                        divider = true,
                        enabled = isDnsOverHttpsSupported,
                        onClick = { dnsMenuExpanded = true },
                        modifier = Modifier.testTag(ProtectionSettingsTestTags.DnsOverHttps),
                    )
                    SettingsDropdown(
                        expanded = isDnsOverHttpsSupported && dnsMenuExpanded,
                        onDismissRequest = { dnsMenuExpanded = false },
                    ) {
                        DnsOverHttpsProvider.entries.forEach { provider ->
                            SettingsDropdownItem(
                                label = provider.displayName(),
                                selected = provider == dnsOverHttpsSettings.provider,
                                onClick = {
                                    dnsMenuExpanded = false
                                    if (provider == DnsOverHttpsProvider.Custom) {
                                        customDnsDialogVisible = true
                                    } else if (provider != dnsOverHttpsSettings.provider) {
                                        onDnsOverHttpsSettingsChanged(
                                            dnsOverHttpsSettings.copy(provider = provider),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
                Box {
                    SettingsCardValueRow(
                        title = stringResource(R.string.settings_webrtc_protection_title),
                        value = webRtcProtectionMode.displayName(),
                        summary = stringResource(
                            webRtcSummary(webRtcProtectionMode, browserEngineKind),
                        ),
                        dividerColor = dividerColor,
                        divider = true,
                        onClick = { webRtcMenuExpanded = true },
                        modifier = Modifier.testTag(ProtectionSettingsTestTags.WebRtcProtection),
                    )
                    SettingsDropdown(
                        expanded = webRtcMenuExpanded,
                        onDismissRequest = { webRtcMenuExpanded = false },
                    ) {
                        WebRtcProtectionMode.entries.forEach { mode ->
                            SettingsDropdownItem(
                                label = mode.displayName(),
                                selected = mode == webRtcProtectionMode,
                                onClick = {
                                    webRtcMenuExpanded = false
                                    if (mode != webRtcProtectionMode) {
                                        onWebRtcProtectionModeChanged(mode)
                                    }
                                },
                            )
                        }
                    }
                }
                ProtectionSwitch(
                    title = stringResource(R.string.settings_global_privacy_control_title),
                    summary = stringResource(R.string.settings_global_privacy_control_summary),
                    checked = privacySignalSettings.globalPrivacyControlEnabled,
                    dividerColor = dividerColor,
                    onCheckedChange = { enabled ->
                        onPrivacySignalSettingsChanged(
                            privacySignalSettings.copy(globalPrivacyControlEnabled = enabled),
                        )
                    },
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.GlobalPrivacyControl),
                )
                ProtectionSwitch(
                    title = stringResource(R.string.settings_do_not_track_title),
                    summary = stringResource(R.string.settings_do_not_track_summary),
                    checked = privacySignalSettings.doNotTrackEnabled,
                    dividerColor = dividerColor,
                    onCheckedChange = { enabled ->
                        onPrivacySignalSettingsChanged(
                            privacySignalSettings.copy(doNotTrackEnabled = enabled),
                        )
                    },
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.DoNotTrack),
                )
                ProtectionSwitch(
                    title = stringResource(R.string.settings_auto_de_amp_title),
                    summary = stringResource(R.string.settings_auto_de_amp_summary),
                    checked = isAutoDeAmpEnabled,
                    dividerColor = dividerColor,
                    divider = false,
                    onCheckedChange = onAutoDeAmpEnabledChanged,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.AutoDeAmp),
                )
            }
            Text(
                stringResource(R.string.settings_protection_disclaimer),
                modifier = Modifier.padding(SettingsCardTokens.headerPadding),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsCardHeader(stringResource(R.string.settings_protection_group_tools))
            SettingsCard(containerColor = cardColor) {
                SettingsCardLinkRow(
                    title = stringResource(R.string.permission_radar_title),
                    summary = stringResource(R.string.permission_radar_settings_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onPermissionRadar,
                )
                ProtectionSwitch(
                    title = stringResource(R.string.settings_protection_card_title),
                    summary = stringResource(R.string.settings_protection_card_subtitle),
                    checked = isProtectionCardVisible,
                    dividerColor = dividerColor,
                    divider = false,
                    onCheckedChange = onProtectionCardVisibleChanged,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.ProtectionCard),
                )
            }
            if (privateTabsLock != null) {
                SettingsCardHeader(stringResource(R.string.settings_section_private_tabs))
                SettingsCard(containerColor = cardColor) {
                    ProtectionSwitch(
                        title = stringResource(R.string.private_tabs_lock_settings_title),
                        summary = stringResource(
                            if (privateTabsLock.available) {
                                R.string.private_tabs_lock_settings_summary
                            } else {
                                R.string.profile_protection_unavailable
                            },
                        ),
                        checked = privateTabsLock.checked && privateTabsLock.available,
                        enabled = privateTabsLock.available,
                        dividerColor = dividerColor,
                        divider = false,
                        onCheckedChange = privateTabsLock.onCheckedChange,
                        modifier = Modifier.testTag(ProtectionSettingsTestTags.PrivateTabsLock),
                    )
                }
            }
            SettingsCardHeader(stringResource(R.string.history_title))
            SettingsCard(containerColor = cardColor) {
                ProtectionSwitch(
                    title = stringResource(R.string.history_save_title),
                    summary = stringResource(R.string.history_save_summary),
                    checked = historyRecordingMode != HistoryRecordingMode.Disabled,
                    dividerColor = dividerColor,
                    onCheckedChange = { enabled ->
                        onHistoryRecordingModeChanged(
                            if (enabled) {
                                HistoryRecordingMode.Enabled
                            } else {
                                HistoryRecordingMode.Disabled
                            },
                        )
                    },
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.SaveHistory),
                )
                ProtectionSwitch(
                    title = stringResource(R.string.history_clear_on_exit_title),
                    summary = stringResource(R.string.history_clear_on_exit_summary),
                    checked = historyRecordingMode == HistoryRecordingMode.ClearOnExit,
                    enabled = historyRecordingMode != HistoryRecordingMode.Disabled,
                    dividerColor = dividerColor,
                    onCheckedChange = { enabled ->
                        onHistoryRecordingModeChanged(
                            if (enabled) {
                                HistoryRecordingMode.ClearOnExit
                            } else {
                                HistoryRecordingMode.Enabled
                            },
                        )
                    },
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.ClearHistoryOnExit),
                )
                ProtectionSwitch(
                    title = stringResource(R.string.recall_settings_title),
                    summary = stringResource(R.string.recall_settings_summary),
                    checked = isRecallEnabled,
                    dividerColor = dividerColor,
                    divider = false,
                    onCheckedChange = onRecallEnabledChanged,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.Recall),
                )
            }
            SettingsCardHeader(stringResource(R.string.settings_protection_group_app_data))
            SettingsCard(containerColor = cardColor) {
                SettingsCardLinkRow(
                    title = stringResource(R.string.passwords_import_title),
                    summary = stringResource(R.string.settings_move_to_vola_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onMoveToVola,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.MoveToVola),
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.data_archive_export_title),
                    summary = stringResource(R.string.data_archive_export_summary),
                    dividerColor = dividerColor,
                    divider = true,
                    onClick = onExportAppData,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.ExportAppData),
                )
                SettingsCardLinkRow(
                    title = stringResource(R.string.data_archive_import_title),
                    summary = stringResource(R.string.data_archive_import_summary),
                    dividerColor = dividerColor,
                    onClick = onImportAppData,
                    modifier = Modifier.testTag(ProtectionSettingsTestTags.ImportAppData),
                )
            }
            Surface(
                onClick = onClearData,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = SettingsCardTokens.rowMinHeight),
                shape = SettingsCardTokens.shape,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Box(
                    modifier = Modifier.padding(SettingsCardTokens.rowPadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        stringResource(R.string.action_clear_browsing_data),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * A protection switch on a card: its explanation is part of the setting, so it is shown whole,
 * and a line follows it unless it closes the card.
 */
@Composable
private fun ProtectionSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    dividerColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    divider: Boolean = true,
) {
    SettingsCardSwitchRow(
        title = title,
        summary = summary,
        checked = checked,
        dividerColor = dividerColor,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        divider = divider,
        enabled = enabled,
        summaryMaxLines = Int.MAX_VALUE,
    )
}

/** What the chosen WebRTC mode does, in the words of the engine that enforces it. */
@StringRes
private fun webRtcSummary(
    mode: WebRtcProtectionMode,
    engine: AndroidBrowserEngineKind,
): Int = when (mode) {
    WebRtcProtectionMode.Standard -> R.string.settings_webrtc_standard_summary
    WebRtcProtectionMode.HideLocalNetworkIp -> when (engine) {
        AndroidBrowserEngineKind.GeckoView -> R.string.settings_webrtc_hide_local_ip_gecko_summary
        AndroidBrowserEngineKind.SystemWebView -> R.string.settings_webrtc_policy_system_summary
    }
    WebRtcProtectionMode.DisableNonProxiedUdp -> when (engine) {
        AndroidBrowserEngineKind.GeckoView ->
            R.string.settings_webrtc_disable_non_proxied_udp_gecko_summary
        AndroidBrowserEngineKind.SystemWebView -> R.string.settings_webrtc_policy_system_summary
    }
    WebRtcProtectionMode.ProtectIpAddresses -> when (engine) {
        AndroidBrowserEngineKind.GeckoView -> R.string.settings_webrtc_protect_gecko_summary
        AndroidBrowserEngineKind.SystemWebView -> R.string.settings_webrtc_protect_system_summary
    }
    WebRtcProtectionMode.Block -> R.string.settings_webrtc_block_summary
}

@Composable
private fun CustomDnsEndpointDialog(
    initialEndpoint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var endpointDraft by rememberSaveable(initialEndpoint) { mutableStateOf(initialEndpoint) }
    val normalizedEndpoint = DnsOverHttpsRules.normalizedCustomEndpoint(endpointDraft)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dns_over_https_custom_dialog_title)) },
        text = {
            OutlinedTextField(
                value = endpointDraft,
                onValueChange = { value ->
                    endpointDraft = value.take(DnsOverHttpsRules.MAX_ENDPOINT_LENGTH)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProtectionSettingsTestTags.CustomDnsEndpoint),
                label = { Text(stringResource(R.string.settings_dns_over_https_custom_endpoint)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (endpointDraft.isNotBlank() && normalizedEndpoint == null) {
                                R.string.settings_dns_over_https_custom_invalid
                            } else {
                                R.string.settings_dns_over_https_custom_summary
                            },
                        ),
                    )
                },
                isError = endpointDraft.isNotBlank() && normalizedEndpoint == null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = normalizedEndpoint != null,
                onClick = { normalizedEndpoint?.let(onConfirm) },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun WebRtcProtectionMode.displayName(): String = stringResource(
    when (this) {
        WebRtcProtectionMode.Standard -> R.string.settings_webrtc_mode_standard
        WebRtcProtectionMode.HideLocalNetworkIp ->
            R.string.settings_webrtc_mode_hide_local_network_ip
        WebRtcProtectionMode.DisableNonProxiedUdp ->
            R.string.settings_webrtc_mode_disable_non_proxied_udp
        WebRtcProtectionMode.ProtectIpAddresses -> R.string.settings_webrtc_mode_protect
        WebRtcProtectionMode.Block -> R.string.settings_webrtc_mode_block
    },
)

@Composable
private fun HttpsOnlyMode.displayName(): String = stringResource(
    when (this) {
        HttpsOnlyMode.Always -> R.string.settings_https_only_mode_always
        HttpsOnlyMode.PrivateTabs -> R.string.settings_https_only_mode_private_tabs
        HttpsOnlyMode.Off -> R.string.settings_https_only_mode_off
    },
)

@Composable
private fun DnsOverHttpsProvider.displayName(): String = stringResource(
    when (this) {
        DnsOverHttpsProvider.System -> R.string.settings_dns_over_https_provider_system
        DnsOverHttpsProvider.Cloudflare -> R.string.settings_dns_over_https_provider_cloudflare
        DnsOverHttpsProvider.Google -> R.string.settings_dns_over_https_provider_google
        DnsOverHttpsProvider.Quad9 -> R.string.settings_dns_over_https_provider_quad9
        DnsOverHttpsProvider.Custom -> R.string.settings_dns_over_https_provider_custom
    },
)

@Composable
private fun UserCaTrustWarning(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ProtectionSettingsTestTags.UserCaWarning),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                stringResource(R.string.settings_user_ca_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.settings_user_ca_summary),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Protection and data on cards (board W-SetPrivacy): protection, network, tools, history, data. */
@VolaPreviews
@Composable
private fun ProtectionSettingsPagePreview() {
    MaterialBrowserTheme {
        ProtectionAndDataSettingsPage(
            blockerSettings = BlockerSettings(),
            blockedCount = 128,
            httpsOnlyMode = HttpsOnlyMode.Always,
            privateTabsLock = PrivateTabLock(checked = true, available = true, onCheckedChange = {}),
            trustsUserCertificates = false,
            onBlockerSettingsChanged = {},
            onPrivacyXRay = {},
            onPermissionRadar = {},
            onFilterStudio = {},
            onClearData = {},
            onBack = {},
        )
    }
}
