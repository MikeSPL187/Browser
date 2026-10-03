@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.WorkspaceIconResources
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ProfileProtection
import dev.sk2andy.materialbrowser.browser.ProfileWallpaperTarget
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.browser.WorkspaceNameRules
import dev.sk2andy.materialbrowser.browser.isSyncLinked
import dev.sk2andy.materialbrowser.shared.ui.PlatformProfileEmoji
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaWorkspaceSheet
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor
import dev.sk2andy.materialbrowser.ui.theme.volaGemColors

internal object WorkspaceSheetTestTags {
    const val Settings = "workspace_settings_sheet"
    const val Rename = "workspace_settings_rename"
    const val IconRow = "workspace_settings_icon"
    const val AllIcons = "workspace_new_all_icons"
    const val WallpaperRow = "workspace_settings_wallpapers"
    const val Storage = "workspace_settings_storage"
    const val Biometric = "workspace_settings_biometric"
    const val Delete = "workspace_settings_delete"

    fun icon(emoji: String): String = "workspace_icon:$emoji"
}

/**
 * «New workspace» (board W-WorkspaceSheet): the gem previews the name, color and icon as they are
 * picked; storage and the biometric lock sit on one card above the button.
 */
@Composable
internal fun NewWorkspaceSheet(
    visible: Boolean,
    isolationSupported: Boolean,
    profileProtectionSupported: Boolean,
    icons: List<String>,
    onCreate: (emoji: String, isolationEnabled: Boolean, options: ProfileCreationOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }
    var accent by remember { mutableStateOf(WorkspaceAccent.Default) }
    var emoji by remember(icons) { mutableStateOf(WorkspaceSheetRules.defaultIcon(icons)) }
    var allIconsShown by remember { mutableStateOf(false) }
    var isolationEnabled by remember { mutableStateOf(false) }
    var protection by remember { mutableStateOf<ProfileProtection?>(null) }
    var configuringProtection by remember { mutableStateOf(false) }
    val gemColors = volaGemColors(accent)
    WorkspaceSheetFrame(
        onDismiss = onDismiss,
        modifier = Modifier.testTag(ProfileCreationTestTags.Sheet),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .testTag(ProfileCreationTestTags.IconScroll),
        ) {
            WorkspaceSheetHeader(
                emoji = emoji.orEmpty(),
                accent = accent,
                title = stringResource(R.string.workspace_sheet_new_title),
                subtitle = stringResource(R.string.workspace_sheet_new_subtitle),
            )
            Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
            WorkspaceIdentityEditor(
                name = name,
                namePlaceholder = stringResource(R.string.workspace_untitled),
                accent = accent,
                onNameChange = { name = it },
                onAccentChange = { accent = it },
            )
            Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
            WorkspaceSectionLabel(stringResource(R.string.workspace_icon_label))
            val collapsedLimit = VolaWorkspaceSheet.ICON_COLUMNS * VolaWorkspaceSheet.ICON_COLLAPSED_ROWS
            WorkspaceIconGrid(
                icons = if (allIconsShown) {
                    icons
                } else {
                    WorkspaceSheetRules.collapsedIcons(icons, emoji, collapsedLimit)
                },
                selected = emoji,
                accent = accent,
                onSelect = { emoji = it },
            )
            if (icons.size > collapsedLimit) {
                TextButton(
                    onClick = { allIconsShown = !allIconsShown },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(WorkspaceSheetTestTags.AllIcons),
                ) {
                    Text(
                        stringResource(
                            if (allIconsShown) R.string.workspace_icons_fewer else R.string.workspace_icons_all,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
            WorkspaceSecurityGroup(
                isolationSupported = isolationSupported,
                isolationEnabled = isolationEnabled,
                onIsolationChange = { isolationEnabled = it },
                profileProtectionSupported = profileProtectionSupported,
                protection = protection,
                onEnableProtection = { configuringProtection = true },
                onDisableProtection = { protection = null },
                onConfigureProtection = { configuringProtection = true },
                isolationModifier = Modifier.testTag(ProfileCreationTestTags.Isolation),
                protectionModifier = Modifier.testTag(ProfileCreationOptionTestTags.Protection),
            )
        }
        Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
        Button(
            onClick = {
                emoji?.let { chosen ->
                    onCreate(
                        chosen,
                        isolationEnabled && isolationSupported,
                        ProfileCreationOptions(
                            protection = protection,
                            name = name,
                            accent = accent,
                        ),
                    )
                }
            },
            enabled = emoji != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(VolaWorkspaceSheet.buttonHeight)
                .testTag(ProfileCreationTestTags.CreateButton),
            shape = VolaWorkspaceSheet.buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = gemColors.solid,
                contentColor = gemColors.content,
            ),
        ) {
            Text(stringResource(R.string.action_create_profile))
        }
    }
    if (configuringProtection) {
        ProfileProtectionDialog(
            current = protection,
            onSave = { chosen ->
                protection = chosen
                configuringProtection = false
            },
            onDismiss = { configuringProtection = false },
        )
    }
}

/**
 * «Workspace settings» (board W-WorkspaceSettings), from a long press on the workspace's gem.
 * The name is saved when the sheet closes; everything else at once.
 */
@Composable
internal fun WorkspaceSettingsSheet(
    profile: BrowserProfile?,
    tabCount: Int,
    essentialsCount: Int,
    icons: List<String>,
    canDelete: Boolean,
    isolationSupported: Boolean,
    profileProtectionSupported: Boolean,
    onRename: (String) -> Unit,
    onAccentChange: (WorkspaceAccent) -> Unit,
    onIconChange: (String) -> Unit,
    onCustomizeWallpaper: (ProfileWallpaperTarget) -> Unit,
    onIsolationChange: (Boolean) -> Unit,
    onEnableProtection: () -> Unit,
    onDisableProtection: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val workspace = profile ?: return
    val editable = !workspace.isSyncLinked
    var draftName by remember(workspace.id) { mutableStateOf(workspace.name) }
    var renaming by remember(workspace.id) { mutableStateOf(false) }
    var iconsOpen by remember(workspace.id) { mutableStateOf(false) }
    var wallpapersOpen by remember(workspace.id) { mutableStateOf(false) }
    var confirmingDelete by remember(workspace.id) { mutableStateOf(false) }
    val latestDraftName by rememberUpdatedState(draftName)
    val latestOnRename by rememberUpdatedState(onRename)
    if (editable) {
        DisposableEffect(workspace.id) {
            // The name is saved when the sheet closes, not on every keystroke.
            onDispose { latestOnRename(latestDraftName) }
        }
    }
    val displayName = workspace.syncedDisplayName ?: workspace.copy(name = draftName)
        .workspaceDisplayName()
    WorkspaceSheetFrame(
        onDismiss = onDismiss,
        modifier = Modifier.testTag(WorkspaceSheetTestTags.Settings),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            WorkspaceSheetHeader(
                emoji = workspace.emoji,
                accent = workspace.accent,
                title = displayName,
                subtitle = workspaceSummary(tabCount, essentialsCount),
                action = if (editable) {
                    {
                        IconButton(
                            onClick = { renaming = !renaming },
                            modifier = Modifier.testTag(WorkspaceSheetTestTags.Rename),
                        ) {
                            Icon(
                                VolaIcons.Edit,
                                contentDescription = stringResource(
                                    R.string.workspace_sheet_rename,
                                ),
                            )
                        }
                    }
                } else {
                    null
                },
            )
            if (editable) {
                AnimatedVisibility(visible = renaming, enter = sectionEnter, exit = sectionExit) {
                    WorkspaceNameField(
                        name = draftName,
                        placeholder = workspace.copy(name = "").workspaceDisplayName(),
                        onNameChange = { draftName = it },
                        onDone = {
                            renaming = false
                            onRename(draftName)
                        },
                    )
                }
                Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
                WorkspaceAccentPicker(selected = workspace.accent, onSelect = onAccentChange)
            }
            Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
            WorkspaceRowGroup {
                if (editable) {
                    WorkspaceLinkRow(
                        leading = {
                            WorkspaceGem(
                                workspace = workspace,
                                size = VolaWorkspaceSheet.rowGemSize,
                            )
                        },
                        title = stringResource(R.string.workspace_icon_label),
                        expanded = iconsOpen,
                        onClick = { iconsOpen = !iconsOpen },
                        modifier = Modifier.testTag(WorkspaceSheetTestTags.IconRow),
                    )
                    AnimatedVisibility(
                        visible = iconsOpen,
                        enter = sectionEnter,
                        exit = sectionExit,
                    ) {
                        WorkspaceIconGrid(
                            icons = icons,
                            selected = workspace.emoji,
                            accent = workspace.accent,
                            onSelect = onIconChange,
                            modifier = Modifier.padding(
                                horizontal = VolaWorkspaceSheet.rowPadding,
                                vertical = VolaWorkspaceSheet.labelGap,
                            ),
                        )
                    }
                    WorkspaceRowDivider()
                }
                WorkspaceLinkRow(
                    leading = { RowIcon(rememberVectorPainter(VolaIcons.Palette)) },
                    title = stringResource(R.string.workspace_sheet_wallpapers),
                    expanded = wallpapersOpen,
                    onClick = { wallpapersOpen = !wallpapersOpen },
                    modifier = Modifier.testTag(WorkspaceSheetTestTags.WallpaperRow),
                )
                AnimatedVisibility(
                    visible = wallpapersOpen,
                    enter = sectionEnter,
                    exit = sectionExit,
                ) {
                    Column {
                        WorkspaceLinkRow(
                            leading = null,
                            title = stringResource(R.string.action_customize_new_tab_wallpaper),
                            onClick = { onCustomizeWallpaper(ProfileWallpaperTarget.NewTab) },
                        )
                        WorkspaceLinkRow(
                            leading = null,
                            title = stringResource(
                                R.string.action_customize_tab_switcher_wallpaper,
                            ),
                            onClick = { onCustomizeWallpaper(ProfileWallpaperTarget.TabSwitcher) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
            WorkspaceSecurityGroup(
                isolationSupported = isolationSupported,
                isolationEnabled = workspace.isolationEnabled,
                onIsolationChange = onIsolationChange,
                profileProtectionSupported = profileProtectionSupported,
                protection = workspace.protection,
                onEnableProtection = onEnableProtection,
                onDisableProtection = onDisableProtection,
                onConfigureProtection = onEnableProtection,
                isolationModifier = Modifier.testTag(WorkspaceSheetTestTags.Storage),
                protectionModifier = Modifier.testTag(WorkspaceSheetTestTags.Biometric),
            )
            if (canDelete) {
                Spacer(Modifier.height(VolaWorkspaceSheet.sectionGap))
                OutlinedButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(VolaWorkspaceSheet.buttonHeight)
                        .testTag(WorkspaceSheetTestTags.Delete),
                    shape = VolaWorkspaceSheet.buttonShape,
                    border = BorderStroke(
                        VolaWorkspaceSheet.dividerThickness,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Icon(
                        VolaIcons.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.size(VolaWorkspaceSheet.labelGap))
                    Text(
                        stringResource(R.string.workspace_sheet_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.workspace_delete_confirm_title, displayName)) },
            text = { Text(stringResource(R.string.workspace_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private val sectionEnter = expandVertically(VolaMotion.standard()) + fadeIn(VolaMotion.effects())
private val sectionExit = shrinkVertically(VolaMotion.standard()) + fadeOut(VolaMotion.effects())

@Composable
private fun WorkspaceSheetFrame(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
        containerColor = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = VolaWorkspaceSheet.sidePadding,
                    end = VolaWorkspaceSheet.sidePadding,
                    bottom = VolaWorkspaceSheet.bottomPadding,
                ),
            content = content,
        )
    }
}

@Composable
private fun WorkspaceSheetHeader(
    emoji: String,
    accent: WorkspaceAccent,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WorkspaceGemPreview(emoji = emoji, accent = accent, size = VolaWorkspaceSheet.headerGemSize)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = VolaWorkspaceSheet.headerGap),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action?.invoke()
    }
}

/** A gem for a workspace that may not exist yet: the draft icon in the draft color. */
@Composable
private fun WorkspaceGemPreview(
    emoji: String,
    accent: WorkspaceAccent,
    size: androidx.compose.ui.unit.Dp,
) {
    WorkspaceGem(
        workspace = BrowserProfile(id = "", emoji = emoji, accent = accent),
        size = size,
    )
}

@Composable
private fun WorkspaceSectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(bottom = VolaWorkspaceSheet.labelGap),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WorkspaceNameField(
    name: String,
    placeholder: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = name,
        onValueChange = { value -> onNameChange(value.take(WorkspaceNameRules.MAX_LENGTH)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VolaWorkspaceSheet.sectionGap)
            .testTag(WorkspaceIdentityTestTags.Name),
        label = { Text(stringResource(R.string.workspace_name_label)) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onDone()
            },
        ),
    )
}

/** The workspace icons, six a row; the chosen one wears the workspace's gem colors. */
@Composable
private fun WorkspaceIconGrid(
    icons: List<String>,
    selected: String?,
    accent: WorkspaceAccent,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gemColors = volaGemColors(accent)
    val iconFontSize = with(LocalDensity.current) { VolaWorkspaceSheet.iconSize.toSp() }
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(VolaWorkspaceSheet.iconTileGap),
    ) {
        icons.chunked(VolaWorkspaceSheet.ICON_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(VolaWorkspaceSheet.iconTileGap)) {
                row.forEach { emoji ->
                    val isSelected = emoji == selected
                    val description = stringResource(WorkspaceIconResources.nameFor(emoji))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(VolaWorkspaceSheet.iconTileHeight)
                            .clip(VolaWorkspaceSheet.iconTileShape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(gemColors.fill)
                                } else {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    )
                                },
                            )
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(emoji) },
                                role = Role.RadioButton,
                            )
                            .semantics { contentDescription = description }
                            .testTag(WorkspaceSheetTestTags.icon(emoji)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (isSelected) {
                                gemColors.content
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ) {
                            PlatformProfileEmoji(emoji = emoji, fontSize = iconFontSize)
                        }
                    }
                }
                repeat(VolaWorkspaceSheet.ICON_COLUMNS - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Separate storage and the biometric lock, on one card in both sheets. */
@Composable
private fun WorkspaceSecurityGroup(
    isolationSupported: Boolean,
    isolationEnabled: Boolean,
    onIsolationChange: (Boolean) -> Unit,
    profileProtectionSupported: Boolean,
    protection: ProfileProtection?,
    onEnableProtection: () -> Unit,
    onDisableProtection: () -> Unit,
    onConfigureProtection: () -> Unit,
    isolationModifier: Modifier,
    protectionModifier: Modifier,
) {
    WorkspaceRowGroup {
        WorkspaceSwitchRow(
            icon = painterResource(R.drawable.ic_symbol_shield_lock),
            title = stringResource(R.string.workspace_storage_title),
            subtitle = stringResource(
                if (isolationSupported) {
                    R.string.workspace_storage_summary
                } else {
                    R.string.settings_profile_isolation_unsupported
                },
            ),
            checked = isolationEnabled && isolationSupported,
            enabled = isolationSupported,
            onCheckedChange = onIsolationChange,
            modifier = isolationModifier,
        )
        WorkspaceRowDivider()
        WorkspaceSwitchRow(
            icon = rememberVectorPainter(VolaIcons.Lock),
            title = stringResource(R.string.workspace_biometric_title),
            subtitle = when {
                !profileProtectionSupported ->
                    stringResource(R.string.profile_protection_unavailable)
                protection == null -> stringResource(R.string.workspace_biometric_summary_off)
                else -> profileProtectionSummary(protection)
            },
            checked = protection != null,
            enabled = profileProtectionSupported,
            onCheckedChange = { enabled ->
                if (enabled) onEnableProtection() else onDisableProtection()
            },
            modifier = protectionModifier,
        )
        if (protection != null && profileProtectionSupported) {
            WorkspaceLinkRow(
                leading = null,
                title = stringResource(R.string.profile_protection_configure),
                onClick = onConfigureProtection,
            )
        }
    }
}

@Composable
private fun WorkspaceRowGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VolaWorkspaceSheet.groupShape)
            .background(VolaTheme.extendedColors.card),
        content = content,
    )
}

@Composable
private fun WorkspaceRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = VolaWorkspaceSheet.dividerInset),
        thickness = VolaWorkspaceSheet.dividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun RowIcon(painter: Painter) {
    Icon(
        painter,
        contentDescription = null,
        modifier = Modifier.size(VolaWorkspaceSheet.rowIconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** One row of a group: the whole row toggles, so TalkBack meets one labelled switch. */
@Composable
private fun WorkspaceSwitchRow(
    icon: Painter,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VolaWorkspaceSheet.rowMinHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = VolaWorkspaceSheet.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaWorkspaceSheet.rowGap),
    ) {
        RowIcon(icon)
        RowText(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** A row that opens something: a section below it (with [expanded]) or another screen. */
@Composable
private fun WorkspaceLinkRow(
    leading: (@Composable () -> Unit)?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VolaWorkspaceSheet.rowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = VolaWorkspaceSheet.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaWorkspaceSheet.rowGap),
    ) {
        if (leading != null) {
            leading()
        } else {
            Spacer(Modifier.size(VolaWorkspaceSheet.rowIconSize))
        }
        RowText(title = title, subtitle = null, modifier = Modifier.weight(1f))
        Icon(
            when (expanded) {
                null -> VolaIcons.KeyboardArrowRight
                true -> VolaIcons.KeyboardArrowUp
                false -> VolaIcons.KeyboardArrowDown
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowText(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = VolaWorkspaceSheet.labelGap)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun workspaceSummary(tabCount: Int, essentialsCount: Int): String {
    val tabs = pluralStringResource(R.plurals.workspace_sheet_tabs, tabCount, tabCount)
    val essentials = pluralStringResource(
        R.plurals.workspace_sheet_essentials,
        essentialsCount,
        essentialsCount,
    )
    return WorkspaceSheetRules.summary(tabs, essentials.takeIf { essentialsCount > 0 })
}
