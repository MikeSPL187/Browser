package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.SmoothCornerShape
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Shapes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

/*
 * The v5 page menu (canvas screen «д», system-v5.md section 10): a sheet over the page with the
 * page's title, a row of round actions (Forward, Favorite, Share, Print), «Read», the page's tools,
 * tabs, the commands of extensions and user scripts when they offer any, and the library. There is
 * no second, older menu behind it (V28, B4: `design/v28-b4-all-actions.md`).
 */

object V5PageMenuTestTags {
    const val Forward = "v5_menu_forward"
    const val Favorite = "v5_menu_favorite"
    const val Share = "v5_menu_share"
    const val Print = "v5_menu_print"
    const val Read = "v5_menu_read"
    const val Listen = "v5_menu_listen"
    const val DuplicateTab = "v5_menu_duplicate_tab"
    const val OpenInApp = "v5_menu_open_in_app"
    fun extra(key: String) = "v5_menu_extra_$key"
    const val HideDistractions = "v5_menu_hide_distractions"
    const val ShowHidden = "v5_menu_show_hidden"
}

/**
 * A command an extension or a user script adds to the page (V28, B4): its [title], who offers it as
 * [subtitle], and whether it can run now.
 */
@Immutable
data class V5PageMenuExtra(
    val key: String,
    val title: String,
    val subtitle: String?,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/** What the menu shows about the selected tab. A null toggle is not available for this page. */
@Immutable
data class V5PageMenuState(
    val title: String,
    /** The page's domain; null on a new tab. */
    val subtitle: String?,
    val favicon: ImageBitmap?,
    /** A web page is open (not a new tab): sharing, printing, finding and translating apply. */
    val hasPage: Boolean,
    val canGoForward: Boolean,
    val canFavorite: Boolean,
    /** A private tab: «Favorite» is off because private tabs keep nothing, and its label says so. */
    val favoriteOffInPrivate: Boolean = false,
    val isFavorite: Boolean,
    val canRead: Boolean,
    val canTranslate: Boolean,
    val isPinned: Boolean,
    val desktopView: Boolean?,
    val siteMuted: Boolean?,
    val canCloseTab: Boolean,
    /**
     * False where a private tab cannot be opened: a profile of another device, or an engine
     * without isolated profiles. The row goes, rather than opening an ordinary tab instead.
     */
    val canNewPrivateTab: Boolean = true,
    /** «Hide distracting items» applies: a web page with an engine that can (GeckoView, V20). */
    val canHideDistractions: Boolean = false,
    /** Blocks hidden on this site; «Show hidden items» from one. */
    val hiddenCount: Int = 0,
    /** What is downloading now (V22): a row above the rest while there is something. */
    val downloads: V5DownloadRingState? = null,
    /** Commands of extensions and user scripts for this page; the group shows only when there are some. */
    val extras: List<V5PageMenuExtra> = emptyList(),
)

/** The menu's actions. Each one but the toggles closes the menu first. */
class V5PageMenuActions(
    val onForward: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onShare: () -> Unit,
    val onPrint: () -> Unit,
    val onRead: () -> Unit,
    val onTranslate: () -> Unit,
    val onFindInPage: () -> Unit,
    val onTogglePinned: () -> Unit,
    val onDesktopViewChange: (Boolean) -> Unit,
    val onSiteMutedChange: (Boolean) -> Unit,
    val onNewTab: () -> Unit,
    val onNewPrivateTab: () -> Unit,
    val onCloseTab: () -> Unit,
    val onFavorites: () -> Unit,
    val onHistory: () -> Unit,
    val onDownloads: () -> Unit,
    val onSettings: () -> Unit,
    val onDuplicateTab: () -> Unit,
    /** The page in the app that handles its address, as Android offers it. */
    val onOpenInApp: () -> Unit,
    val onHideDistractions: () -> Unit = {},
    val onShowHidden: () -> Unit = {},
    /** «Listen» (V19c): the article read aloud without opening the reader. */
    val onListen: () -> Unit = {},
)

@Composable
fun V5PageMenu(
    visible: Boolean,
    state: V5PageMenuState,
    actions: V5PageMenuActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Run [action] after the sheet starts to leave.
    fun closing(action: () -> Unit): () -> Unit = {
        onDismiss()
        action()
    }
    V5WindowSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = stringResource(R.string.v5_menu_title),
        modifier = modifier,
    ) {
        V5SheetHeader(
            title = state.title,
            subtitle = state.subtitle,
            onClose = onDismiss,
            leading = { Favicon(state.favicon) },
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            state.downloads?.let { downloads ->
                V5ListGroup {
                    V5ListRow(
                        stringResource(R.string.v5_menu_downloads),
                        subtitle = v5DownloadsSummary(downloads),
                        icon = V5RowIcon(VolaIcons.Download),
                        onClick = closing(actions.onDownloads),
                    )
                }
            }
            RoundActions(state, actions, ::closing)
            if (state.canRead) {
                ReadListenTiles(onRead = closing(actions.onRead), onListen = closing(actions.onListen))
            }
            PageGroup(state, actions, ::closing)
            V5ListGroup {
                V5ListRow(
                    stringResource(R.string.cd_new_tab),
                    icon = V5RowIcon(VolaIcons.Add),
                    onClick = closing(actions.onNewTab),
                )
                if (state.canNewPrivateTab) {
                    V5ListRow(
                        stringResource(R.string.v5_menu_new_private_tab),
                        icon = V5RowIcon(VolaIcons.Lock),
                        onClick = closing(actions.onNewPrivateTab),
                    )
                }
                if (state.hasPage) {
                    V5ListRow(
                        stringResource(R.string.action_duplicate_tab),
                        icon = V5RowIcon(VolaIcons.ContentCopy),
                        onClick = closing(actions.onDuplicateTab),
                        modifier = Modifier.testTag(V5PageMenuTestTags.DuplicateTab),
                    )
                }
                if (state.canCloseTab) {
                    V5ListRow(
                        stringResource(R.string.cd_close_tab),
                        icon = V5RowIcon(VolaIcons.Close),
                        titleColor = V5Theme.colors.danger,
                        onClick = closing(actions.onCloseTab),
                    )
                }
            }
            if (state.extras.isNotEmpty()) {
                V5ListGroup {
                    state.extras.forEach { extra ->
                        V5ListRow(
                            extra.title,
                            subtitle = extra.subtitle,
                            icon = V5RowIcon(VolaIcons.Extension),
                            enabled = extra.enabled,
                            onClick = closing(extra.onClick),
                            modifier = Modifier.testTag(V5PageMenuTestTags.extra(extra.key)),
                        )
                    }
                }
            }
            V5ListGroup {
                V5ListRow(
                    stringResource(R.string.v5_menu_favorites),
                    icon = V5RowIcon(VolaIcons.Star),
                    onClick = closing(actions.onFavorites),
                )
                V5ListRow(
                    stringResource(R.string.action_history),
                    icon = V5RowIcon(VolaIcons.History),
                    onClick = closing(actions.onHistory),
                )
                // While something downloads, the row on top with its progress is the one way there.
                if (state.downloads == null) {
                    V5ListRow(
                        stringResource(R.string.v5_menu_downloads),
                        icon = V5RowIcon(VolaIcons.Download),
                        onClick = closing(actions.onDownloads),
                    )
                }
                V5ListRow(
                    stringResource(R.string.action_settings),
                    icon = V5RowIcon(VolaIcons.Settings),
                    trailing = V5RowTrailing.Chevron,
                    onClick = closing(actions.onSettings),
                )
            }
        }
    }
}

@Composable
private fun PageGroup(
    state: V5PageMenuState,
    actions: V5PageMenuActions,
    closing: (() -> Unit) -> () -> Unit,
) {
    if (!state.hasPage) return
    V5ListGroup {
        if (state.canTranslate) {
            V5ListRow(
                stringResource(R.string.action_translate_page),
                icon = V5RowIcon(VolaIcons.Language),
                onClick = closing(actions.onTranslate),
            )
        }
        V5ListRow(
            stringResource(R.string.action_find_in_page),
            icon = V5RowIcon(VolaIcons.Search),
            onClick = closing(actions.onFindInPage),
        )
        V5ListRow(
            stringResource(R.string.action_open_in_app),
            icon = V5RowIcon(VolaIcons.OpenInNew),
            onClick = closing(actions.onOpenInApp),
            modifier = Modifier.testTag(V5PageMenuTestTags.OpenInApp),
        )
        if (state.canHideDistractions) {
            V5ListRow(
                stringResource(R.string.v5_distractions_hide),
                icon = V5RowIcon(VolaIcons.VisibilityOff),
                onClick = closing(actions.onHideDistractions),
                modifier = Modifier.testTag(V5PageMenuTestTags.HideDistractions),
            )
            if (state.hiddenCount > 0) {
                V5ListRow(
                    stringResource(R.string.v5_distractions_show_all),
                    icon = V5RowIcon(VolaIcons.Visibility),
                    onClick = closing(actions.onShowHidden),
                    modifier = Modifier.testTag(V5PageMenuTestTags.ShowHidden),
                )
            }
        }
        V5ListRow(
            stringResource(if (state.isPinned) R.string.v5_menu_unpin_tab else R.string.action_pin_tab),
            icon = V5RowIcon(VolaIcons.PushPin),
            onClick = closing(actions.onTogglePinned),
        )
        state.desktopView?.let { desktop ->
            V5ListRow(
                stringResource(R.string.action_desktop_view),
                icon = V5RowIcon(VolaIcons.DesktopWindows),
                trailing = V5RowTrailing.Toggle(desktop, actions.onDesktopViewChange),
            )
        }
        state.siteMuted?.let { muted ->
            V5ListRow(
                stringResource(R.string.action_mute_domain),
                icon = V5RowIcon(VolaIcons.VolumeOff),
                trailing = V5RowTrailing.Toggle(muted, actions.onSiteMutedChange),
            )
        }
    }
}

/** The round actions: Forward, Favorite (its shape squares off once saved), Share and Print. */
@Composable
private fun RoundActions(
    state: V5PageMenuState,
    actions: V5PageMenuActions,
    closing: (() -> Unit) -> () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.marginPhone),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        RoundAction(
            icon = VolaIcons.ArrowForward,
            label = stringResource(R.string.action_forward),
            enabled = state.canGoForward,
            onClick = closing(actions.onForward),
            tag = V5PageMenuTestTags.Forward,
        )
        RoundAction(
            icon = VolaIcons.Star,
            label = stringResource(
                when {
                    state.isFavorite -> R.string.v5_menu_in_favorites
                    state.favoriteOffInPrivate -> R.string.v5_menu_favorite_private
                    else -> R.string.v5_menu_favorite
                },
            ),
            enabled = state.canFavorite,
            filled = state.isFavorite,
            onClick = actions.onToggleFavorite,
            tag = V5PageMenuTestTags.Favorite,
        )
        RoundAction(
            icon = VolaIcons.Share,
            label = stringResource(R.string.action_share),
            enabled = state.hasPage,
            onClick = closing(actions.onShare),
            tag = V5PageMenuTestTags.Share,
        )
        RoundAction(
            icon = VolaIcons.Print,
            label = stringResource(R.string.action_print),
            enabled = state.hasPage,
            onClick = closing(actions.onPrint),
            tag = V5PageMenuTestTags.Print,
        )
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
    filled: Boolean = false,
) {
    val colors = V5Theme.colors
    // Saved: the circle becomes a rounded square (section 10, «Закладка»).
    val corner by animateDpAsState(
        targetValue = if (filled) V5Shapes.radiusM else V5Sizes.touchTarget / 2,
        animationSpec = if (V5Motion.animationsEnabled) V5Motion.press.spec() else V5Motion.reducedFade(),
        label = "v5 menu action shape",
    )
    Column(
        Modifier
            .widthIn(min = V5Sizes.menuActionWidth)
            .clip(V5Theme.shapes.m)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else V5_DISABLED_ALPHA }
            .padding(vertical = V5Spacing.space1)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space1),
    ) {
        Box(
            Modifier
                .size(V5Sizes.touchTarget)
                .clip(SmoothCornerShape(corner, corner, corner, corner))
                .background(if (filled) colors.accentFill else colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (filled) colors.onAccentFill else colors.label,
                modifier = Modifier.size(V5Sizes.icon),
            )
        }
        Text(
            label,
            style = V5Theme.type.caption,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** «Read» and «Listen» side by side (system-v5.md section 10): the reader, or the article aloud. */
@Composable
private fun ReadListenTiles(onRead: () -> Unit, onListen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.marginPhone),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        WideTile(
            icon = VolaIcons.FormatAlignLeft,
            label = stringResource(R.string.v5_menu_read),
            onClick = onRead,
            modifier = Modifier.weight(1f).testTag(V5PageMenuTestTags.Read),
        )
        WideTile(
            icon = VolaIcons.Headphones,
            label = stringResource(R.string.reader_listen),
            onClick = onListen,
            modifier = Modifier.weight(1f).testTag(V5PageMenuTestTags.Listen),
        )
    }
}

@Composable
private fun WideTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    Row(
        modifier
            .heightIn(min = V5Sizes.rowSingleLine)
            .clip(V5Theme.shapes.s)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(V5Sizes.icon))
        Text(
            label,
            style = V5Theme.type.headline,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Favicon(favicon: ImageBitmap?) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .size(V5Sizes.rowTile)
            .clip(V5Theme.shapes.xs)
            .background(colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        if (favicon != null) {
            Image(favicon, contentDescription = null, modifier = Modifier.size(V5Sizes.iconSmall))
        } else {
            Icon(VolaIcons.Public, contentDescription = null, tint = colors.labelSecondary, modifier = Modifier.size(V5Sizes.iconSmall))
        }
    }
}
