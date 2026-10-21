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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
    const val New = "v5_profile_new"
    fun profile(id: String) = "v5_profile:$id"
}

/** One profile in the sheet (V11a): its symbol on its color, its name and how many tabs it holds. */
@Immutable
internal data class V5ProfileItem(
    val id: String,
    /** The profile's symbol: Vola draws a Material Symbol for its stored emoji key, never the emoji. */
    val icon: ImageVector,
    val name: String,
    val accent: V5Accent,
    /** How many tabs it holds; null where the count is beside the point (moving a tab, B11). */
    val tabCount: Int?,
    val active: Boolean,
    /** Behind a biometric lock: choosing it asks to sign in first. */
    val locked: Boolean = false,
    /** A profile mirrored from another device, by that device's name. */
    val device: String? = null,
)

/**
 * «Profiles» (V11a, system-v5.md section 10): the chip in the middle of the overview's bottom edge
 * opens this sheet, as Safari's profile menu; a profile is chosen with one touch and the overview
 * shows its tabs. Profiles from other devices follow under their own header. A long press edits a
 * profile of this device ([onEdit], V11b); «New profile» ([onNew]) makes one.
 */
@Composable
internal fun V5ProfileSheet(
    visible: Boolean,
    profiles: List<V5ProfileItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onNew: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null,
    /** «Move to profile» (V28, B11) reuses the sheet under its own title. */
    title: String = stringResource(R.string.v5_profiles_title),
    /** Null while a profile can be made; else the most there can be, said on «New profile», which is off. */
    profileLimit: Int? = null,
) {
    V5WindowSheet(visible = visible, onDismiss = onDismiss, title = title) {
        V5SheetHeader(title = title, onClose = onDismiss)
        val (local, synced) = profiles.partition { it.device == null }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5ListGroup {
                local.forEach { profile -> ProfileRow(profile, onSelect, onEdit) }
            }
            if (onNew != null) {
                V5ListGroup {
                    V5ListRow(
                        title = stringResource(R.string.v5_profile_new),
                        subtitle = profileLimit?.let { limit -> pluralStringResource(R.plurals.v5_profile_limit, limit, limit) },
                        icon = V5RowIcon(VolaIcons.Add),
                        titleColor = if (profileLimit == null) V5Theme.colors.accent else V5Theme.colors.labelTertiary,
                        enabled = profileLimit == null,
                        onClick = onNew,
                        modifier = Modifier.testTag(V5ProfileSheetTestTags.New),
                    )
                }
            }
            if (synced.isNotEmpty()) {
                // Changed on the device that owns them: the footer says why a long press does nothing (ПР-5).
                V5ListGroup(
                    header = stringResource(R.string.v5_profiles_other_devices),
                    footer = stringResource(R.string.v5_profiles_other_devices_footer),
                ) {
                    synced.forEach { profile -> ProfileRow(profile, onSelect, onEdit = null) }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: V5ProfileItem, onSelect: (String) -> Unit, onEdit: ((String) -> Unit)?) {
    val tabs = profile.tabCount?.let { count -> pluralStringResource(R.plurals.workspace_sheet_tabs, count, count) }
    V5ListRow(
        title = profile.name,
        subtitle = listOfNotNull(profile.device, tabs).joinToString(" · ").ifEmpty { null },
        leading = { ProfileTile(profile) },
        trailing = if (profile.active) V5RowTrailing.Check else V5RowTrailing.None,
        onClick = { onSelect(profile.id) },
        // A locked profile is edited once it is open: no long press that would silently do nothing.
        onLongClick = onEdit?.takeIf { !profile.locked }?.let { edit -> { edit(profile.id) } },
        onLongClickLabel = stringResource(R.string.v5_profile_edit_action),
        modifier = Modifier
            .semantics { selected = profile.active }
            .testTag(V5ProfileSheetTestTags.profile(profile.id)),
    )
}

/** The profile's symbol, white on a disc of its color (`accentFill`, 1.2); a lock badge when locked. */
@Composable
private fun ProfileTile(profile: V5ProfileItem) {
    val colors = V5Theme.colors
    Box(Modifier.size(V5Sizes.rowTile), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(V5Sizes.rowTile)
                .clip(V5Theme.shapes.full)
                .background(profile.accent.fill(colors.isDark)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                profile.icon,
                contentDescription = null,
                tint = colors.onAccentFill,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
        }
        if (profile.locked) {
            Icon(
                VolaIcons.Lock,
                contentDescription = stringResource(R.string.v5_profile_locked),
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
