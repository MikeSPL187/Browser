package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation

/** How the own server stands, for the line under the profile's name. */
sealed interface V5ProfileSync {
    /** The build has no own server (System WebView): the card says nothing about it. */
    data object Unavailable : V5ProfileSync

    data object Off : V5ProfileSync

    data object Syncing : V5ProfileSync

    data object Failed : V5ProfileSync

    /** [whenText] is «2 min ago», or null before the first sync. */
    data class Synced(val whenText: String?) : V5ProfileSync
}

@Immutable
data class V5SettingsProfile(val name: String, val icon: ImageVector?, val sync: V5ProfileSync)

internal object V5SettingsProfileCardTestTags {
    const val Card = "v5_settings_profile_card"
}

/**
 * The card on top of Settings (canvas «Settings»): the profile in front, on its accent, and how its
 * sync stands, a green dot when it is fine and an amber one when it failed. A tap lists the profiles.
 */
@Composable
internal fun V5SettingsProfileCard(profile: V5SettingsProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val shape = V5Theme.shapes.m
    val line = when (val sync = profile.sync) {
        V5ProfileSync.Unavailable -> null
        V5ProfileSync.Off -> stringResource(R.string.v5_settings_profile_sync_off)
        V5ProfileSync.Syncing -> stringResource(R.string.v5_settings_profile_syncing)
        V5ProfileSync.Failed -> stringResource(R.string.v5_settings_profile_sync_failed)
        is V5ProfileSync.Synced -> sync.whenText?.let { stringResource(R.string.v5_settings_profile_synced, it) }
            ?: stringResource(R.string.v5_webdav_never_synced)
    }
    val dot = when (profile.sync) {
        is V5ProfileSync.Synced -> colors.success
        V5ProfileSync.Failed -> colors.warning
        else -> null
    }
    Row(
        modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.settingsProfileCard)
            .v5Elevation(V5Elevation.e1, shape)
            .clip(shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.v5_profiles_choose), onClick = onClick)
            .testTag(V5SettingsProfileCardTestTags.Card)
            .padding(horizontal = V5Spacing.space3, vertical = V5Spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(V5Sizes.settingsProfileAvatar)
                .clip(V5Theme.shapes.full)
                .background(colors.accentFill),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.icon != null) {
                Icon(profile.icon, contentDescription = null, tint = colors.onAccentFill, modifier = Modifier.size(V5Sizes.icon))
            } else {
                Text(V5NewTabRules.letter(profile.name), style = V5Theme.type.headline, color = colors.onAccentFill)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
            Text(
                stringResource(R.string.v5_settings_profile_title, profile.name),
                style = V5Theme.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (line != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(V5Spacing.space1), verticalAlignment = Alignment.CenterVertically) {
                    if (dot != null) {
                        Box(Modifier.size(V5Sizes.profileDot).clip(V5Theme.shapes.full).background(dot))
                    }
                    Text(line, style = V5Theme.type.footnote, color = colors.labelSecondary)
                }
            }
        }
        Icon(VolaIcons.KeyboardArrowRight, contentDescription = null, tint = colors.labelTertiary, modifier = Modifier.size(V5Sizes.iconSmall))
    }
}
