package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarEntry
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarSnapshot
import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionActivity
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5Segmented

/**
 * The permission radar page of site information under ui.v5 (V28, B8c), in the same cards and rows
 * as the rest of the sheet: the workspace's sites with a check on the chosen one, the site's
 * connection, each permission with its state and an «Ask · Allow · Block» segment under it, and
 * «Reset site» in red. What each choice does is the v4 page's.
 */
@Composable
internal fun PermissionRadarContentV5(
    snapshot: PermissionRadarSnapshot,
    workspaceName: String,
    supportedPermissions: Set<SitePermission>,
    onOriginSelected: (String) -> Unit,
    onDecisionChanged: (SitePermission, SitePermissionDecision) -> Unit,
    onResetSite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val site = snapshot.site
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        Text(
            stringResource(R.string.permission_radar_title),
            modifier = Modifier.padding(horizontal = V5Spacing.space4).semantics { heading() },
            style = V5Theme.type.title,
            color = colors.label,
        )
        Text(
            stringResource(
                if (snapshot.isPrivate) {
                    R.string.permission_radar_private_summary
                } else {
                    R.string.permission_radar_profile_summary
                },
                workspaceName,
            ),
            modifier = Modifier.padding(horizontal = V5Spacing.space4),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
        )
        if (snapshot.knownOrigins.size > 1) {
            Spacer(Modifier.height(V5Spacing.space2))
            SiteInfoSectionLabel(stringResource(R.string.permission_radar_sites))
            SiteInfoCard {
                snapshot.knownOrigins.forEachIndexed { index, origin ->
                    if (index > 0) SiteInfoDivider()
                    SiteInfoRow(
                        icon = rememberVectorPainter(VolaIcons.Check),
                        title = origin,
                        showIcon = origin == site?.origin,
                        onClick = { onOriginSelected(origin) },
                    )
                }
            }
        }
        Spacer(Modifier.height(V5Spacing.space2))
        if (site == null) {
            SiteInfoCard {
                Text(
                    stringResource(R.string.permission_radar_no_site),
                    modifier = Modifier.padding(V5Spacing.space4),
                    style = V5Theme.type.body,
                    color = colors.labelSecondary,
                )
            }
            return@Column
        }
        SiteInfoSectionLabel(site.origin)
        SiteInfoCard {
            val isHttps = site.origin.startsWith("https://")
            SiteInfoRow(
                icon = rememberVectorPainter(if (isHttps) VolaIcons.Lock else VolaIcons.WarningFilled),
                title = stringResource(
                    if (isHttps) R.string.permission_site_https else R.string.permission_site_http,
                ),
            )
            val notificationsSupported = SitePermission.Notifications in supportedPermissions
            SiteInfoRules.radarEntries(snapshot.entries, supportedPermissions).forEach { entry ->
                SiteInfoDivider()
                PermissionRowV5(entry, snapshot.isPrivate, notificationsSupported, onDecisionChanged)
            }
        }
        Spacer(Modifier.height(V5Spacing.space2))
        SiteInfoCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = V5Sizes.rowSingleLine)
                    .clickable(role = Role.Button, onClick = onResetSite)
                    .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    stringResource(R.string.permission_radar_reset_site),
                    style = V5Theme.type.body,
                    color = colors.danger,
                )
            }
        }
    }
}

/** One permission: its glyph, name and state, the segment under the text when it can be chosen. */
@Composable
private fun PermissionRowV5(
    entry: PermissionRadarEntry,
    isPrivate: Boolean,
    notificationsSupported: Boolean,
    onDecisionChanged: (SitePermission, SitePermissionDecision) -> Unit,
) {
    val notificationsUnavailable = entry.permission == SitePermission.Notifications && !notificationsSupported
    val privateNotifications = isPrivate && entry.permission == SitePermission.Notifications &&
        !notificationsUnavailable
    val inUse = !notificationsUnavailable && entry.activity != SitePermissionActivity.Idle
    Column {
        SiteInfoRow(
            icon = painterResource(entry.permission.icon()),
            title = entry.permission.displayName(),
            supporting = if (privateNotifications) {
                stringResource(R.string.permission_notifications_private_unavailable)
            } else {
                radarStatus(entry, notificationsUnavailable)
            },
            trailing = {
                if (inUse) {
                    Box(
                        Modifier
                            .size(V5Sizes.dot)
                            .background(
                                if (entry.activity == SitePermissionActivity.Active) {
                                    V5Theme.colors.accent
                                } else {
                                    V5Theme.colors.warning
                                },
                                V5Theme.shapes.full,
                            ),
                    )
                }
            },
        )
        if (!notificationsUnavailable && !privateNotifications) {
            val decisions = SitePermissionDecision.entries
            V5Segmented(
                options = decisions.map { it.displayName() },
                selectedIndex = if (entry.allowedForSession) -1 else decisions.indexOf(entry.decision),
                onSelect = { index -> onDecisionChanged(entry.permission, decisions[index]) },
                modifier = Modifier.padding(
                    start = V5Spacing.space4 + V5Sizes.iconSmall + V5Spacing.space3,
                    end = V5Spacing.space4,
                    bottom = V5Spacing.space3,
                ),
            )
        }
    }
}

@Preview(name = "Permission radar v5 · light", heightDp = 900)
@Composable
private fun PermissionRadarV5LightPreview() = PermissionRadarV5Preview(dark = false)

@Preview(name = "Permission radar v5 · dark", heightDp = 900)
@Composable
private fun PermissionRadarV5DarkPreview() = PermissionRadarV5Preview(dark = true)

@Composable
private fun PermissionRadarV5Preview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        CompositionLocalProvider(LocalSiteInfoV5 provides true) {
            Box(Modifier.background(V5Theme.colors.background)) {
                PermissionRadarContentV5(
                    snapshot = PermissionRadarSnapshot(
                        site = PermissionSiteKey(profileId = "personal", origin = "https://north-guide.ru"),
                        knownOrigins = listOf("https://north-guide.ru", "https://maps.example.com"),
                        entries = listOf(
                            PermissionRadarEntry(
                                SitePermission.Location,
                                SitePermissionDecision.Allow,
                                allowedForSession = false,
                                activity = SitePermissionActivity.Active,
                            ),
                            PermissionRadarEntry(
                                SitePermission.Camera,
                                SitePermissionDecision.Ask,
                                allowedForSession = false,
                                activity = SitePermissionActivity.Idle,
                            ),
                        ),
                        isPrivate = false,
                    ),
                    workspaceName = "Personal",
                    supportedPermissions = SitePermission.entries.toSet(),
                    onOriginSelected = {},
                    onDecisionChanged = { _, _ -> },
                    onResetSite = {},
                    modifier = Modifier.padding(V5Spacing.marginPhone),
                )
            }
        }
    }
}
