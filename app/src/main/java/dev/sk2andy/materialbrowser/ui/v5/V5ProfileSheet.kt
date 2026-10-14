package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5ProfileSheetTestTags {
    fun profile(id: String) = "v5_profile:$id"
}

/** One profile in the sheet (V11a): its emoji on its color, its name and how many tabs it holds. */
@Immutable
internal data class V5ProfileItem(
    val id: String,
    val emoji: String,
    val name: String,
    val accent: V5Accent,
    val tabCount: Int,
    val active: Boolean,
    /** Behind a biometric lock: choosing it asks to sign in first. */
    val locked: Boolean = false,
    /** A profile mirrored from another device, by that device's name. */
    val device: String? = null,
)

/**
 * «Profiles» (V11a, system-v5.md section 10): the chip in the middle of the overview's bottom edge
 * opens this sheet, as Safari's profile menu; a profile is chosen with one touch and the overview
 * shows its tabs. Profiles from other devices follow under their own header.
 */
@Composable
internal fun V5ProfileSheet(
    visible: Boolean,
    profiles: List<V5ProfileItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.v5_profiles_title)
    V5Sheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        val (local, synced) = profiles.partition { it.device == null }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5ListGroup {
                local.forEach { profile -> ProfileRow(profile, onSelect) }
            }
            if (synced.isNotEmpty()) {
                V5ListGroup(header = stringResource(R.string.v5_profiles_other_devices)) {
                    synced.forEach { profile -> ProfileRow(profile, onSelect) }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: V5ProfileItem, onSelect: (String) -> Unit) {
    val tabs = pluralStringResource(R.plurals.workspace_sheet_tabs, profile.tabCount, profile.tabCount)
    V5ListRow(
        title = profile.name,
        subtitle = profile.device?.let { "$it · $tabs" } ?: tabs,
        leading = { ProfileTile(profile) },
        trailing = if (profile.active) V5RowTrailing.Check else V5RowTrailing.None,
        onClick = { onSelect(profile.id) },
        modifier = Modifier
            .semantics { selected = profile.active }
            .testTag(V5ProfileSheetTestTags.profile(profile.id)),
    )
}

/** The profile's emoji on a soft disc of its color; a lock badge when it is locked. */
@Composable
private fun ProfileTile(profile: V5ProfileItem) {
    val dark = V5Theme.colors.isDark
    Box(Modifier.size(V5Sizes.rowTile), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.full)
                .background(profile.accent.fill(dark).copy(alpha = PROFILE_TILE_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.emoji, style = V5Theme.type.callout)
        }
        if (profile.locked) {
            Icon(
                VolaIcons.Lock,
                contentDescription = stringResource(R.string.profile_locked_title),
                tint = V5Theme.colors.labelSecondary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(V5Sizes.overviewFavicon)
                    .clip(V5Theme.shapes.full)
                    .background(V5Theme.colors.surface),
            )
        }
    }
}

/** How strongly the profile's color shows behind its emoji. */
private const val PROFILE_TILE_ALPHA = 0.22f
