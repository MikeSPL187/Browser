package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.SiteCertificate
import dev.sk2andy.materialbrowser.browser.SiteConnectionKind
import dev.sk2andy.materialbrowser.browser.SiteConnectionRules
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarEntry
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionActivity
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSiteInfo

internal object SiteInfoTestTags {
    const val Overview = "site_info_overview"
    const val Trackers = "site_info_trackers"
    const val AllPermissions = "site_info_all_permissions"
    const val Protection = "site_info_protection"
    const val Popups = "site_info_popups"
    const val PageFixes = "site_info_page_fixes"
    const val DeleteSiteData = "site_info_delete_site_data"
    const val Certificate = "site_info_certificate"

    fun permission(permission: SitePermission): String = "site_info_permission_${permission.name}"
}

/**
 * The first page of the site information sheet (board W-SiteInfo): who the site is, how it is
 * reached, what was blocked, which permissions it has and the switches for this site. Each row
 * that leads on opens its page in the same sheet.
 */
@Composable
internal fun SiteInfoOverview(
    pageUrl: String,
    connectionKind: SiteConnectionKind,
    blockedCount: Int,
    permissions: List<PermissionRadarEntry>,
    canChangePermissions: Boolean,
    protectionOn: Boolean,
    canTogglePopups: Boolean,
    popupsBlocked: Boolean,
    onOpenPrivacyXRay: () -> Unit,
    onOpenPermissions: () -> Unit,
    onPermissionDecisionChanged: (SitePermission, SitePermissionDecision) -> Unit,
    onProtectionChange: (Boolean) -> Unit,
    onPopupsBlockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    siteData: SiteInfoSiteData? = null,
    certificate: SiteCertificate? = null,
    onOpenCertificate: () -> Unit = {},
    pageFixes: SiteInfoPageFixes? = null,
) {
    val host = SiteConnectionRules.host(pageUrl)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SiteInfoTestTags.Overview),
        verticalArrangement = Arrangement.spacedBy(VolaSiteInfo.sectionGap),
    ) {
        SiteInfoHeader(host = host, connectionKind = connectionKind)
        SiteInfoCard {
            SiteInfoRow(
                icon = rememberVectorPainter(siteConnectionIcon(connectionKind)),
                title = siteConnectionLabel(connectionKind),
            )
            certificate?.let { cert ->
                SiteInfoDivider()
                SiteInfoRow(
                    icon = painterResource(R.drawable.ic_symbol_verified),
                    title = stringResource(R.string.site_info_certificate),
                    supporting = siteCertificateSummary(cert),
                    onClick = onOpenCertificate,
                    modifier = Modifier.testTag(SiteInfoTestTags.Certificate),
                    trailing = { SiteInfoChevron() },
                )
            }
            SiteInfoDivider()
            SiteInfoRow(
                icon = painterResource(R.drawable.ic_symbol_shield),
                title = if (blockedCount > 0) {
                    pluralStringResource(R.plurals.site_info_trackers_blocked, blockedCount, blockedCount)
                } else {
                    stringResource(R.string.site_info_nothing_blocked)
                },
                supporting = stringResource(R.string.privacy_xray_title),
                onClick = onOpenPrivacyXRay,
                modifier = Modifier.testTag(SiteInfoTestTags.Trackers),
                trailing = { SiteInfoChevron() },
            )
        }

        Text(
            text = stringResource(R.string.site_info_permissions_title),
            modifier = Modifier.padding(start = VolaSiteInfo.labelPadding),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SiteInfoCard {
            permissions.forEach { entry ->
                SitePermissionRow(
                    entry = entry,
                    enabled = canChangePermissions,
                    onDecisionChanged = { decision ->
                        onPermissionDecisionChanged(entry.permission, decision)
                    },
                )
                SiteInfoDivider()
            }
            SiteInfoRow(
                icon = painterResource(R.drawable.ic_symbol_chevron_right),
                title = stringResource(R.string.site_info_all_permissions),
                onClick = onOpenPermissions,
                modifier = Modifier.testTag(SiteInfoTestTags.AllPermissions),
                trailing = { SiteInfoChevron() },
                showIcon = false,
            )
        }

        SiteInfoCard {
            SiteInfoSwitchRow(
                icon = painterResource(R.drawable.ic_symbol_shield),
                title = stringResource(R.string.privacy_site_protection),
                supporting = stringResource(R.string.site_info_protection_summary),
                checked = protectionOn,
                onCheckedChange = onProtectionChange,
                modifier = Modifier.testTag(SiteInfoTestTags.Protection),
            )
            if (canTogglePopups) {
                SiteInfoDivider()
                SiteInfoSwitchRow(
                    icon = painterResource(R.drawable.ic_symbol_block),
                    title = stringResource(R.string.action_always_block_popups),
                    checked = popupsBlocked,
                    onCheckedChange = onPopupsBlockedChange,
                    modifier = Modifier.testTag(SiteInfoTestTags.Popups),
                )
            }
        }
        pageFixes?.let { fixes -> SitePageFixesCard(fixes) }
        siteData?.let { data -> SiteDataCard(data) }
    }
}

internal enum class SiteInfoPageFix { CookieBanners, VerticalScrolling, PageZooming, SafeArea }

/**
 * The per-site fixes a page may need, moved here from the main menu (S4a): they belong to the
 * site, not to the moment. [cookieBannersHidden] is null while cookie-banner protection is off
 * everywhere or paused for this site.
 */
internal class SiteInfoPageFixes(
    val cookieBannersHidden: Boolean?,
    val forceVerticalScrolling: Boolean,
    val forcePageZooming: Boolean,
    val forceSafeArea: Boolean,
    val onChange: (SiteInfoPageFix, Boolean) -> Unit,
)

@Composable
private fun SitePageFixesCard(fixes: SiteInfoPageFixes) {
    SiteInfoCard(modifier = Modifier.testTag(SiteInfoTestTags.PageFixes)) {
        fixes.cookieBannersHidden?.let { hidden ->
            SiteInfoSwitchRow(
                icon = painterResource(R.drawable.ic_symbol_cookie),
                title = stringResource(R.string.privacy_cookie_banner_remove),
                supporting = stringResource(R.string.privacy_cookie_banner_remove_description),
                checked = hidden,
                onCheckedChange = { fixes.onChange(SiteInfoPageFix.CookieBanners, it) },
            )
            SiteInfoDivider()
        }
        SiteInfoSwitchRow(
            icon = painterResource(R.drawable.ic_symbol_vertical_scroll),
            title = stringResource(R.string.privacy_force_vertical_scrolling),
            supporting = stringResource(R.string.privacy_force_vertical_scrolling_description),
            checked = fixes.forceVerticalScrolling,
            onCheckedChange = { fixes.onChange(SiteInfoPageFix.VerticalScrolling, it) },
        )
        SiteInfoDivider()
        SiteInfoSwitchRow(
            icon = painterResource(R.drawable.ic_symbol_zoom_in),
            title = stringResource(R.string.privacy_force_page_zooming),
            supporting = stringResource(R.string.privacy_force_page_zooming_description),
            checked = fixes.forcePageZooming,
            onCheckedChange = { fixes.onChange(SiteInfoPageFix.PageZooming, it) },
        )
        SiteInfoDivider()
        SiteInfoSwitchRow(
            icon = painterResource(R.drawable.ic_symbol_fit_screen),
            title = stringResource(R.string.compatibility_force_safe_area),
            supporting = stringResource(R.string.compatibility_force_safe_area_description),
            checked = fixes.forceSafeArea,
            onCheckedChange = { fixes.onChange(SiteInfoPageFix.SafeArea, it) },
        )
    }
}

/**
 * «Site data» with «Delete» (board W-SiteInfo). The engine reports no size or cookie count per
 * site, so the row says what goes instead; deleting waits behind «Undo» ([SiteDataDeletion]).
 */
internal class SiteInfoSiteData(
    val baseDomain: String,
    val allWorkspaces: Boolean,
    val onDelete: () -> Unit,
)

@Composable
private fun SiteDataCard(data: SiteInfoSiteData) {
    val deleteLabel = stringResource(R.string.site_info_delete_site_data_description, data.baseDomain)
    SiteInfoCard {
        SiteInfoRow(
            icon = painterResource(R.drawable.ic_symbol_dns),
            title = stringResource(R.string.site_info_site_data),
            supporting = stringResource(
                if (data.allWorkspaces) {
                    R.string.site_info_site_data_summary_all_workspaces
                } else {
                    R.string.site_info_site_data_summary
                },
            ),
            trailing = {
                TextButton(
                    onClick = data.onDelete,
                    modifier = Modifier
                        .testTag(SiteInfoTestTags.DeleteSiteData)
                        .semantics { contentDescription = deleteLabel },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.site_info_delete_site_data))
                }
            },
        )
    }
}

@Composable
private fun SiteInfoHeader(host: String, connectionKind: SiteConnectionKind) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSiteInfo.headerGap),
    ) {
        Box(
            modifier = Modifier
                .size(VolaSiteInfo.gemSize)
                .background(MaterialTheme.colorScheme.primaryContainer, VolaSiteInfo.gemShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = SiteInfoRules.initial(host),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column {
            Text(
                text = host.ifEmpty { siteConnectionLabel(connectionKind) },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SiteInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = VolaSiteInfo.cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SiteInfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = VolaSiteInfo.dividerInset,
            end = VolaSiteInfo.rowHorizontalPadding,
        ),
        thickness = VolaSiteInfo.dividerThickness,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun SiteInfoChevron() {
    Icon(
        painter = painterResource(R.drawable.ic_symbol_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SiteInfoRow(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    showIcon: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VolaSiteInfo.rowMinHeight)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
            )
            .padding(
                horizontal = VolaSiteInfo.rowHorizontalPadding,
                vertical = VolaSiteInfo.rowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaSiteInfo.rowGap),
    ) {
        if (showIcon) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(VolaSiteInfo.rowIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Box(Modifier.size(VolaSiteInfo.rowIconSize))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun SiteInfoSwitchRow(
    icon: Painter,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    SiteInfoRow(
        icon = icon,
        title = title,
        supporting = supporting,
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun SitePermissionRow(
    entry: PermissionRadarEntry,
    enabled: Boolean,
    onDecisionChanged: (SitePermissionDecision) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val value = if (entry.allowedForSession && entry.decision == SitePermissionDecision.Ask) {
        stringResource(R.string.site_info_permission_session)
    } else {
        entry.decision.displayName()
    }
    Box {
        SiteInfoRow(
            icon = painterResource(entry.permission.icon()),
            title = entry.permission.displayName(),
            onClick = if (enabled) ({ menuOpen = true }) else null,
            modifier = Modifier.testTag(SiteInfoTestTags.permission(entry.permission)),
            trailing = {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (enabled) {
                    Icon(
                        imageVector = VolaIcons.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SitePermissionDecision.entries.forEach { decision ->
                DropdownMenuItem(
                    text = { Text(decision.displayName()) },
                    onClick = {
                        menuOpen = false
                        if (decision != entry.decision) onDecisionChanged(decision)
                    },
                    trailingIcon = if (decision == entry.decision) {
                        { Icon(imageVector = VolaIcons.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** The bar over a page the overview leads to: Back returns to the overview. */
@Composable
internal fun SiteInfoPageBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaSiteInfo.labelPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = VolaIcons.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@VolaPreviews
@Composable
private fun SiteInfoOverviewPreview() {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            SiteInfoOverview(
                pageUrl = "https://north-guide.ru/baikal",
                connectionKind = SiteConnectionKind.Https,
                blockedCount = 14,
                permissions = listOf(
                    PermissionRadarEntry(
                        SitePermission.Location,
                        SitePermissionDecision.Allow,
                        allowedForSession = false,
                        activity = SitePermissionActivity.Idle,
                    ),
                    PermissionRadarEntry(
                        SitePermission.Camera,
                        SitePermissionDecision.Ask,
                        allowedForSession = false,
                        activity = SitePermissionActivity.Idle,
                    ),
                    PermissionRadarEntry(
                        SitePermission.Notifications,
                        SitePermissionDecision.Block,
                        allowedForSession = false,
                        activity = SitePermissionActivity.Idle,
                    ),
                ),
                canChangePermissions = true,
                protectionOn = true,
                canTogglePopups = true,
                popupsBlocked = true,
                onOpenPrivacyXRay = {},
                onOpenPermissions = {},
                onPermissionDecisionChanged = { _, _ -> },
                onProtectionChange = {},
                onPopupsBlockedChange = {},
                pageFixes = SiteInfoPageFixes(
                    cookieBannersHidden = true,
                    forceVerticalScrolling = false,
                    forcePageZooming = true,
                    forceSafeArea = false,
                    onChange = { _, _ -> },
                ),
                modifier = Modifier.padding(VolaSiteInfo.sidePadding),
                siteData = SiteInfoSiteData(
                    baseDomain = "north-guide.ru",
                    allWorkspaces = true,
                    onDelete = {},
                ),
            )
        }
    }
}
