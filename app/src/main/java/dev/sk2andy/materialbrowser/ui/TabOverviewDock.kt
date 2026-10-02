package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.EssentialsRules
import dev.sk2andy.materialbrowser.shared.ui.TabOverviewChromeTestTags
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaElevation
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaTabOverview
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme

internal object TabOverviewDockTestTags {
    const val Dock = "tab_overview_dock"
    const val Essentials = "tab_overview_essentials"

    fun essential(id: String): String = "tab_overview_essential:$id"
}

/**
 * The bottom of the tab overview, within reach of the thumb (board W-Tabs): the workspace's
 * Essentials, then the workspaces and the new-tab button.
 */
@Composable
internal fun TabOverviewDock(
    essentials: List<EssentialEntry>,
    essentialIcons: Map<String, Bitmap>,
    workspaces: List<BrowserProfile>,
    activeWorkspaceId: String,
    showWorkspaces: Boolean,
    enabled: Boolean,
    onOpenEssential: (EssentialEntry) -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onWorkspaceLongClick: (String) -> Unit,
    onAddWorkspace: () -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TabOverviewDockTestTags.Dock)
            .padding(
                start = VolaTabOverview.dockSideMargin,
                end = VolaTabOverview.dockSideMargin,
                bottom = VolaTabOverview.dockBottomMargin,
            ),
        verticalArrangement = Arrangement.spacedBy(VolaTabOverview.sectionGap),
    ) {
        if (essentials.isNotEmpty()) {
            TabOverviewEssentialsRow(
                entries = essentials,
                icons = essentialIcons,
                enabled = enabled,
                onOpen = onOpenEssential,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VolaTabOverview.dockGap),
        ) {
            if (showWorkspaces) {
                WorkspaceDock(
                    workspaces = workspaces,
                    activeWorkspaceId = activeWorkspaceId,
                    enabled = enabled,
                    onSelect = onSelectWorkspace,
                    onLongClick = onWorkspaceLongClick,
                    onAdd = onAddWorkspace,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            NewTabButton(enabled = enabled, onClick = onNewTab)
        }
    }
}

/** Essentials as one row of tiles: five fill the width, more scroll sideways. */
@Composable
private fun TabOverviewEssentialsRow(
    entries: List<EssentialEntry>,
    icons: Map<String, Bitmap>,
    enabled: Boolean,
    onOpen: (EssentialEntry) -> Unit,
) {
    val tileColor = VolaTheme.extendedColors.card.copy(alpha = VolaTabOverview.ESSENTIAL_TILE_ALPHA)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = VolaTabOverview.ESSENTIALS_COLUMNS
        val tileWidth = (maxWidth - VolaTabOverview.essentialGap * (columns - 1)) / columns
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TabOverviewDockTestTags.Essentials),
            horizontalArrangement = Arrangement.spacedBy(VolaTabOverview.essentialGap),
        ) {
            items(entries, key = EssentialEntry::id) { entry ->
                val label = EssentialsRules.label(entry)
                Box(
                    modifier = Modifier
                        .width(tileWidth)
                        .height(VolaTabOverview.essentialTileHeight)
                        .shadow(VolaElevation.level1, VolaTabOverview.essentialTileShape)
                        .clip(VolaTabOverview.essentialTileShape)
                        .background(tileColor)
                        .clickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.tab_overview_open_essential),
                            onClick = { onOpen(entry) },
                        )
                        .semantics { contentDescription = label }
                        .testTag(TabOverviewDockTestTags.essential(entry.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    // The tile reads its site name; the monogram inside would repeat a letter.
                    Box(Modifier.clearAndSetSemantics { }) {
                        EssentialIcon(
                            entry = entry,
                            icon = icons[entry.url],
                            colorIndex = entries.indexOf(entry),
                            size = VolaTabOverview.essentialIconSize,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The workspaces as gems in one pill. The current one shows its name; a tap switches, a long
 * press opens its settings, + creates one.
 */
@Composable
private fun WorkspaceDock(
    workspaces: List<BrowserProfile>,
    activeWorkspaceId: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onLongClick: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val card = VolaTheme.extendedColors.card
    val shape = CircleShape
    Row(
        modifier = modifier
            .height(VolaTabOverview.dockHeight)
            .clip(shape)
            .background(card.copy(alpha = VolaTabOverview.DOCK_ALPHA))
            .border(
                BorderStroke(
                    VolaTabOverview.dockOutlineWidth,
                    colors.onSurface.copy(alpha = VolaTabOverview.DOCK_OUTLINE_ALPHA),
                ),
                shape,
            )
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = VolaTabOverview.dockPadding)
            .testTag(ProfileSwitcherTestTags.Switcher),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        workspaces.forEach { workspace ->
            WorkspaceDockItem(
                workspace = workspace,
                active = workspace.id == activeWorkspaceId,
                enabled = enabled,
                onClick = { onSelect(workspace.id) },
                onLongClick = { onLongClick(workspace.id) },
            )
        }
        IconButton(
            onClick = onAdd,
            enabled = enabled,
            modifier = Modifier.testTag(ProfileSwitcherTestTags.Add),
        ) {
            Icon(
                VolaIcons.Add,
                contentDescription = stringResource(R.string.cd_add_profile),
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkspaceDockItem(
    workspace: BrowserProfile,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val name = workspace.syncedDisplayName ?: workspace.workspaceDisplayName()
    val editLabel = stringResource(R.string.action_edit_profile)
    val gemSize: Dp = if (active) {
        VolaTabOverview.activeWorkspaceGemSize
    } else {
        VolaTabOverview.workspaceGemSize
    }
    Row(
        modifier = Modifier
            .height(VolaTabOverview.dockItemSize)
            .then(
                if (active) {
                    Modifier
                        .shadow(VolaElevation.level1, CircleShape)
                        .clip(CircleShape)
                        .background(VolaTheme.extendedColors.card)
                } else {
                    Modifier.clip(CircleShape)
                },
            )
            .combinedClickable(
                enabled = enabled,
                role = Role.Tab,
                onLongClickLabel = editLabel,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics {
                contentDescription = name
                selected = active
            }
            .testTag(ProfileSwitcherTestTags.profile(workspace.id))
            .animateContentSize(VolaMotion.standard())
            .padding(
                start = VolaTabOverview.dockPadding,
                end = if (active) {
                    VolaTabOverview.activeWorkspaceEndPadding
                } else {
                    VolaTabOverview.dockPadding
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VolaTabOverview.activeWorkspaceGap),
    ) {
        Box(
            modifier = Modifier
                .size(VolaTabOverview.dockItemSize - VolaTabOverview.dockPadding * 2)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            WorkspaceGem(workspace = workspace, size = gemSize)
        }
        if (active) {
            Text(
                text = name,
                modifier = Modifier.clearAndSetSemantics { },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The new-tab button: the workspace accent with a soft glow of the same color. */
@Composable
private fun NewTabButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(VolaTabOverview.newTabButtonSize)
            .shadow(
                elevation = VolaTabOverview.newTabGlowElevation,
                shape = VolaTabOverview.newTabButtonShape,
                ambientColor = colors.primary,
                spotColor = colors.primary,
            )
            .testTag(TabOverviewChromeTestTags.NewTab),
        shape = VolaTabOverview.newTabButtonShape,
        color = colors.primary,
        contentColor = colors.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(VolaIcons.Add, contentDescription = stringResource(R.string.cd_new_tab))
        }
    }
}
