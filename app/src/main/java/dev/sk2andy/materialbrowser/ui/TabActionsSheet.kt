package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaTabActions
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

internal object TabActionsSheetTestTags {
    const val Sheet = "tab_actions_sheet"
    const val Scrim = "tab_actions_scrim"
    const val Close = "tab_actions_close"
    const val More = "tab_actions_more"
    const val Move = "tab_actions_move"

    fun quick(action: TabQuickAction): String = "tab_actions_quick:${action.name}"

    fun more(action: TabMoreAction): String = "tab_actions_more:${action.name}"
}

/** Which row of the sheet is opened in place. One at a time keeps the sheet short. */
private enum class TabActionsSection { Move, Group, More }

/**
 * Actions for one tab (board W-TabActions): the tab lifted over the blurred overview, its quick
 * actions in a row and the rest in rows below. «More» keeps Candy's other actions two taps away.
 */
@Composable
internal fun TabActionsSheet(
    tab: BrowserTab?,
    facts: TabActionsFacts?,
    favicon: Bitmap?,
    currentWorkspaceName: String,
    otherWorkspaces: List<BrowserProfile>,
    canGroup: Boolean,
    preview: @Composable (BrowserTab) -> Unit,
    groupContent: @Composable ColumnScope.() -> Unit,
    extensionContent: @Composable ColumnScope.() -> Unit,
    onQuickAction: (TabQuickAction) -> Unit,
    onMoreAction: (TabMoreAction) -> Unit,
    onMoveTo: (String) -> Unit,
    onSnooze: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the last tab while the sheet animates out.
    var shownTab by remember { mutableStateOf(tab) }
    var shownFacts by remember { mutableStateOf(facts) }
    if (tab != null && facts != null) {
        shownTab = tab
        shownFacts = facts
    }
    BackHandler(enabled = tab != null, onBack = onDismiss)
    val visible = tab != null && facts != null
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(VolaMotion.effects()),
        exit = fadeOut(VolaMotion.effects()),
        modifier = modifier,
    ) {
        val presentedTab = shownTab ?: return@AnimatedVisibility
        val presentedFacts = shownFacts ?: return@AnimatedVisibility
        val closeMenuLabel = stringResource(R.string.tab_actions_dismiss)
        val scrimColor = MaterialTheme.colorScheme.surfaceContainer
            .copy(alpha = VolaTabActions.SCRIM_ALPHA)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TabActionsSheetTestTags.Sheet),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = closeMenuLabel,
                        onClick = onDismiss,
                    )
                    .semantics { contentDescription = closeMenuLabel }
                    .testTag(TabActionsSheetTestTags.Scrim),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = VolaTabActions.cardTopPadding,
                        bottom = VolaTabActions.panelBottomPadding,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VolaTabActions.cardPanelGap),
            ) {
                LiftedTabCard(
                    tab = presentedTab,
                    favicon = favicon,
                    preview = preview,
                    onClose = onClose.takeIf { TabActionsRules.canClose(presentedFacts) },
                )
                TabActionsPanel(
                    facts = presentedFacts,
                    currentWorkspaceName = currentWorkspaceName,
                    otherWorkspaces = otherWorkspaces,
                    canGroup = canGroup,
                    groupContent = groupContent,
                    extensionContent = extensionContent,
                    onQuickAction = onQuickAction,
                    onMoreAction = onMoreAction,
                    onMoveTo = onMoveTo,
                    onSnooze = onSnooze,
                    onClose = onClose,
                )
            }
        }
    }
}

/** The tab, lifted and ringed like the current tab in the grid. */
@Composable
private fun LiftedTabCard(
    tab: BrowserTab,
    favicon: Bitmap?,
    preview: @Composable (BrowserTab) -> Unit,
    onClose: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .width(VolaTabActions.cardWidth)
            .height(VolaTabActions.cardHeight)
            .shadow(
                elevation = VolaTabOverview.selectedGlowElevation,
                shape = VolaTabOverview.cardShape,
                ambientColor = colors.primary,
                spotColor = colors.primary,
            ),
        shape = VolaTabOverview.cardShape,
        color = VolaTheme.extendedColors.card,
        border = BorderStroke(VolaTabOverview.selectedRingWidth, colors.primary),
    ) {
        Column {
            TabGridCardTitleRow(tab = tab, favicon = favicon, enabled = true, onClose = onClose)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .clearAndSetSemantics { },
            ) {
                preview(tab)
            }
        }
    }
}

@Composable
private fun TabActionsPanel(
    facts: TabActionsFacts,
    currentWorkspaceName: String,
    otherWorkspaces: List<BrowserProfile>,
    canGroup: Boolean,
    groupContent: @Composable ColumnScope.() -> Unit,
    extensionContent: @Composable ColumnScope.() -> Unit,
    onQuickAction: (TabQuickAction) -> Unit,
    onMoreAction: (TabMoreAction) -> Unit,
    onMoveTo: (String) -> Unit,
    onSnooze: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var openSection by remember { mutableStateOf<TabActionsSection?>(null) }
    fun toggle(section: TabActionsSection) {
        openSection = if (openSection == section) null else section
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolaTabActions.panelSideMargin),
        shape = VolaTabActions.panelShape,
        color = VolaTheme.extendedColors.card,
        shadowElevation = VolaTabActions.panelElevation,
    ) {
        Column(Modifier.padding(VolaTabActions.panelPadding)) {
            val quickActions = TabActionsRules.quickActions(facts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = VolaTabActions.panelPadding),
                horizontalArrangement = Arrangement.spacedBy(VolaTabActions.quickActionSpacing),
            ) {
                quickActions.forEach { action ->
                    QuickActionButton(
                        action = action,
                        onClick = { onQuickAction(action) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(VolaTabActions.dividerWidth)
                    .background(colors.surfaceContainerHigh),
            )
            if (TabActionsRules.canMove(facts)) {
                ActionRow(
                    icon = VolaIcons.Workspaces,
                    label = stringResource(R.string.action_move_tab_to_profile),
                    value = currentWorkspaceName,
                    expandable = true,
                    expanded = openSection == TabActionsSection.Move,
                    onClick = { toggle(TabActionsSection.Move) },
                    modifier = Modifier.testTag(TabActionsSheetTestTags.Move),
                )
                NestedSection(visible = openSection == TabActionsSection.Move) {
                    otherWorkspaces.forEach { workspace ->
                        WorkspaceTargetRow(
                            workspace = workspace,
                            onClick = { onMoveTo(workspace.id) },
                        )
                    }
                }
            }
            if (canGroup) {
                ActionRow(
                    icon = VolaIcons.TabGroup,
                    label = stringResource(R.string.tab_actions_add_to_group),
                    expandable = true,
                    expanded = openSection == TabActionsSection.Group,
                    onClick = { toggle(TabActionsSection.Group) },
                )
                NestedSection(visible = openSection == TabActionsSection.Group) {
                    groupContent()
                }
            }
            if (TabActionsRules.canSnooze(facts)) {
                ActionRow(
                    icon = VolaIcons.Snooze,
                    label = stringResource(R.string.action_snooze_tab),
                    onClick = onSnooze,
                    modifier = Modifier.testTag(SnoozeTestTags.TabActionsSnooze),
                )
            }
            val moreActions = TabActionsRules.moreActions(facts)
            ActionRow(
                icon = VolaIcons.MoreHoriz,
                label = stringResource(R.string.tab_actions_more),
                expandable = true,
                expanded = openSection == TabActionsSection.More,
                onClick = { toggle(TabActionsSection.More) },
                modifier = Modifier.testTag(TabActionsSheetTestTags.More),
            )
            NestedSection(visible = openSection == TabActionsSection.More) {
                moreActions.forEach { action ->
                    ActionRow(
                        icon = null,
                        label = stringResource(action.labelRes()),
                        onClick = { onMoreAction(action) },
                        color = if (action == TabMoreAction.CloseAll) {
                            colors.error
                        } else {
                            colors.onSurface
                        },
                        modifier = Modifier.testTag(TabActionsSheetTestTags.more(action)),
                    )
                }
                extensionContent()
            }
            if (TabActionsRules.canClose(facts)) {
                ActionRow(
                    icon = VolaIcons.Close,
                    label = stringResource(R.string.cd_close_tab),
                    onClick = onClose,
                    color = colors.error,
                    modifier = Modifier.testTag(TabActionsSheetTestTags.Close),
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    action: TabQuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = VolaTabActions.quickActionHeight)
            .clip(VolaTabActions.quickActionShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(TabActionsSheetTestTags.quick(action)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            VolaTabActions.quickActionGap,
            Alignment.CenterVertically,
        ),
    ) {
        Icon(action.icon(), contentDescription = null, tint = colors.onSurface)
        Text(
            text = stringResource(action.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    expandable: Boolean = false,
    expanded: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = VolaTabActions.rowMinHeight)
            .clip(VolaTabActions.rowShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = VolaTabActions.rowHorizontalPadding,
                vertical = VolaTabActions.rowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaTabActions.rowGap),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(VolaTabActions.rowIconSize),
                tint = if (color == colors.onSurface) colors.onSurfaceVariant else color,
            )
        } else {
            Spacer(Modifier.width(VolaTabActions.rowIconSize))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (expandable) {
            Icon(
                if (expanded) VolaIcons.KeyboardArrowUp else VolaIcons.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NestedSection(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(VolaMotion.standard()) + fadeIn(VolaMotion.effects()),
        exit = shrinkVertically(VolaMotion.standard()) + fadeOut(VolaMotion.effects()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = VolaTabActions.nestedIndent),
            content = content,
        )
    }
}

@Composable
private fun WorkspaceTargetRow(workspace: BrowserProfile, onClick: () -> Unit) {
    val name = workspace.syncedDisplayName ?: workspace.workspaceDisplayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaTabActions.rowMinHeight)
            .clip(VolaTabActions.rowShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = VolaTabActions.rowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaTabActions.rowGap),
    ) {
        Box(Modifier.clearAndSetSemantics { }) {
            WorkspaceGem(workspace = workspace, size = VolaTabActions.nestedGemSize)
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun TabQuickAction.icon(): ImageVector = when (this) {
    TabQuickAction.AddToEssentials, TabQuickAction.RemoveFromEssentials -> VolaIcons.Star
    TabQuickAction.Duplicate -> VolaIcons.ContentCopy
    TabQuickAction.Pin, TabQuickAction.Unpin -> VolaIcons.PushPin
    TabQuickAction.Share -> VolaIcons.Share
}

private fun TabQuickAction.labelRes(): Int = when (this) {
    TabQuickAction.AddToEssentials -> R.string.tab_actions_add_essential
    TabQuickAction.RemoveFromEssentials -> R.string.tab_actions_remove_essential
    TabQuickAction.Duplicate -> R.string.tab_actions_duplicate
    TabQuickAction.Pin -> R.string.tab_actions_pin
    TabQuickAction.Unpin -> R.string.action_remove_pin
    TabQuickAction.Share -> R.string.tab_actions_share
}

private fun TabMoreAction.labelRes(): Int = when (this) {
    TabMoreAction.AddBookmark -> R.string.action_add_favorite
    TabMoreAction.RemoveBookmark -> R.string.action_remove_favorite
    TabMoreAction.MuteSite -> R.string.action_mute_domain
    TabMoreAction.UnmuteSite -> R.string.tab_actions_unmute_site
    TabMoreAction.OpenInApp -> R.string.action_open_in_app
    TabMoreAction.Print -> R.string.action_print
    TabMoreAction.Summarize -> R.string.action_summarize
    TabMoreAction.CandyTrail -> R.string.action_open_candy_trail
    TabMoreAction.SiteCapsule -> R.string.action_add_site_capsule
    TabMoreAction.CloseAll -> R.string.action_close_all_tabs
}
