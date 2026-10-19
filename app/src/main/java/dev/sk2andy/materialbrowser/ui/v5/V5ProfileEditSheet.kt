package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5ProfileEditTestTags {
    const val Name = "v5_profile_name"
    const val Isolation = "v5_profile_isolation"
    const val Protection = "v5_profile_protection"
    const val Save = "v5_profile_save"
    const val Delete = "v5_profile_delete"
    fun icon(index: Int) = "v5_profile_icon:$index"
    fun color(index: Int) = "v5_profile_color:$index"
}

/** A profile as the sheet edits it: its name, its symbol (by stored key) and its accent. */
@Immutable
internal data class V5ProfileDraft(
    val name: String,
    val iconKey: String,
    val accent: V5Accent,
)

/** One symbol to choose: its stored key, its drawing and its name for TalkBack. */
@Immutable
internal data class V5ProfileIconChoice(val key: String, val icon: ImageVector, val name: String)

/** The privacy switches of a profile; null leaves the row out (not supported, or not editable). */
@Immutable
internal data class V5ProfilePrivacy(
    /** Its own cookies, sign-ins and site data. */
    val ownStorage: Boolean?,
    /** Its tabs stay hidden until a biometric sign-in. */
    val biometricLock: Boolean?,
)

/**
 * «New profile» and «Edit profile» (V11b, system-v5.md 1.2 and 10): a name, a symbol, one of the six
 * accents and the privacy switches, as Safari's profile editor. A new profile is made by «Create»
 * and dropped by «Close»; an existing one keeps every change, saved by «Done», «Close» or a swipe
 * down alike. [onDelete] null hides «Delete».
 */
@Composable
internal fun V5ProfileEditSheet(
    visible: Boolean,
    creating: Boolean,
    initial: V5ProfileDraft,
    icons: List<V5ProfileIconChoice>,
    privacy: V5ProfilePrivacy,
    onSave: (V5ProfileDraft) -> Unit,
    onOwnStorageChange: (Boolean) -> Unit,
    onBiometricLockChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    // A fresh draft each time the sheet opens for a profile, kept while it slides away.
    val opening = rememberSheetOpening(visible)
    var name by rememberSaveable(opening) { mutableStateOf(initial.name) }
    var iconIndex by rememberSaveable(opening) {
        mutableIntStateOf(icons.indexOfFirst { it.key == initial.iconKey }.coerceAtLeast(0))
    }
    var accentIndex by rememberSaveable(opening) {
        mutableIntStateOf(V5Accent.entries.indexOf(initial.accent))
    }
    val accent = V5Accent.entries[accentIndex]
    val draft = {
        V5ProfileDraft(
            name = name.trim().ifEmpty { initial.name },
            iconKey = icons.getOrNull(iconIndex)?.key ?: initial.iconKey,
            accent = accent,
        )
    }
    val save = {
        onDismiss()
        onSave(draft())
    }
    // An existing profile keeps every change, as its switches do: «Close» and a swipe down save the
    // name, symbol and accent too. Only a new profile is dropped by them.
    val close = if (creating) onDismiss else save
    val title = stringResource(if (creating) R.string.v5_profile_new else R.string.v5_profile_edit)
    V5WindowSheet(visible = visible, onDismiss = close, title = title) {
        V5SheetHeader(title = title, onClose = close)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            V5NameField(
                value = name,
                placeholder = stringResource(R.string.workspace_name_label),
                onValueChange = { name = it },
                onDone = save,
                maxLength = V5_PROFILE_NAME_MAX,
                modifier = Modifier.testTag(V5ProfileEditTestTags.Name),
            )
            IconGrid(icons, iconIndex, accent, onSelect = { iconIndex = it })
            V5SwatchRow(
                title = stringResource(R.string.workspace_accent_label),
                swatches = V5Accent.entries.map { accent ->
                    V5Swatch(accent.fill(V5Theme.colors.isDark), stringResource(accent.nameRes()))
                },
                selected = accentIndex,
                onSelect = { accentIndex = it },
                testTag = V5ProfileEditTestTags::color,
            )
            if (privacy.ownStorage != null || privacy.biometricLock != null) {
                V5ListGroup {
                    privacy.ownStorage?.let { on ->
                        V5ListRow(
                            title = stringResource(R.string.workspace_storage_title),
                            subtitle = stringResource(R.string.workspace_storage_summary),
                            trailing = V5RowTrailing.Toggle(on, onOwnStorageChange),
                            modifier = Modifier.testTag(V5ProfileEditTestTags.Isolation),
                        )
                    }
                    privacy.biometricLock?.let { on ->
                        V5ListRow(
                            title = stringResource(R.string.workspace_biometric_title),
                            subtitle = stringResource(R.string.workspace_biometric_summary_off),
                            trailing = V5RowTrailing.Toggle(on, onBiometricLockChange),
                            modifier = Modifier.testTag(V5ProfileEditTestTags.Protection),
                        )
                    }
                }
            }
            V5ListGroup {
                V5ListRow(
                    title = stringResource(if (creating) R.string.v5_profile_create else R.string.action_done),
                    titleColor = V5Theme.colors.accent,
                    onClick = save,
                    modifier = Modifier.testTag(V5ProfileEditTestTags.Save),
                )
                if (onDelete != null) {
                    V5ListRow(
                        title = stringResource(R.string.v5_profile_delete),
                        titleColor = V5Theme.colors.danger,
                        onClick = onDelete,
                        modifier = Modifier.testTag(V5ProfileEditTestTags.Delete),
                    )
                }
            }
        }
    }
}

/** The symbols as round buttons; the chosen one is filled with the chosen accent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid(icons: List<V5ProfileIconChoice>, selected: Int, accent: V5Accent, onSelect: (Int) -> Unit) {
    val colors = V5Theme.colors
    Column(Modifier.padding(horizontal = V5Spacing.marginPhone)) {
        Text(
            stringResource(R.string.workspace_icon_label),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            modifier = Modifier.padding(start = V5Spacing.space4, bottom = V5Spacing.space2),
        )
        FlowRow(
            Modifier
                .fillMaxWidth()
                .clip(V5Theme.shapes.s)
                .background(colors.surface)
                .padding(V5Spacing.space2)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            icons.forEachIndexed { index, choice ->
                val chosen = index == selected
                Box(
                    Modifier
                        .size(V5Sizes.touchTarget)
                        .clip(V5Theme.shapes.full)
                        .background(if (chosen) accent.fill(colors.isDark) else colors.surface)
                        .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(index) })
                        .semantics { contentDescription = choice.name }
                        .testTag(V5ProfileEditTestTags.icon(index)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        choice.icon,
                        contentDescription = null,
                        tint = if (chosen) colors.onAccentFill else colors.labelSecondary,
                        modifier = Modifier.size(V5Sizes.icon),
                    )
                }
            }
        }
    }
}

/** An accent's name for TalkBack. */
internal fun V5Accent.nameRes(): Int = when (this) {
    V5Accent.Blue -> R.string.v5_accent_blue
    V5Accent.Green -> R.string.v5_accent_green
    V5Accent.Orange -> R.string.v5_accent_orange
    V5Accent.Pink -> R.string.v5_accent_pink
    V5Accent.Purple -> R.string.v5_accent_purple
    V5Accent.Graphite -> R.string.v5_accent_graphite
}

/** A profile's name fits a chip in the overview. */
internal const val V5_PROFILE_NAME_MAX = 32
