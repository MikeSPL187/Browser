package dev.sk2andy.materialbrowser.ui.v5

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * page's title, a row of round actions (Forward, Favorite, Share, Print), «Read» and «Listen»,
 * Downloads, the page's tools and the library as four tiles (canvas «Menu»). New and closed tabs
 * live in the overview; «More» holds the rarer actions and the commands of extensions and user
 * scripts. There is no second, older menu behind it (V28, B4: `design/v28-b4-all-actions.md`).
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
    const val More = "v5_menu_more"
    const val Back = "v5_menu_back"
    const val History = "v5_menu_history"
    const val Favorites = "v5_menu_favorites"
    const val Passwords = "v5_menu_passwords"
    const val Settings = "v5_menu_settings"
}

/** The menu's two pages: everything common, and «More» with the rarer actions (PM-05). */
enum class V5PageMenuPage { Main, More }

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
    /** The passwords screen; null where the browser keeps none, and the tile goes. */
    val onPasswords: (() -> Unit)? = null,
)

@Composable
fun V5PageMenu(
    visible: Boolean,
    state: V5PageMenuState,
    actions: V5PageMenuActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** The page the menu opens on; previews show «More» with it. */
    initialPage: V5PageMenuPage = V5PageMenuPage.Main,
) {
    // The second page, «More», holds what is rarely needed (PM-05); the first stays short.
    var page by rememberSaveable(visible) { mutableStateOf(initialPage) }
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
        // Back steps out of «More» before it closes the sheet.
        BackHandler(enabled = page != V5PageMenuPage.Main) { page = V5PageMenuPage.Main }
        // The pages cross-fade; without system animations the fade is the whole of the motion.
        val animated = V5Motion.animationsEnabled
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (animated) {
                    fadeIn(V5Motion.effects.spec()) togetherWith fadeOut(V5Motion.effects.spec())
                } else {
                    fadeIn(V5Motion.reducedFade()) togetherWith fadeOut(V5Motion.reducedFade())
                }
            },
            label = "page menu page",
        ) { shown ->
            if (shown == V5PageMenuPage.More) {
                Column {
                    V5SheetHeader(
                        title = stringResource(R.string.v5_menu_more),
                        subtitle = state.subtitle,
                        onClose = onDismiss,
                        leading = { MenuBackButton { page = V5PageMenuPage.Main } },
                    )
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
                    ) {
                        MoreGroup(state, actions, ::closing)
                    }
                }
                return@AnimatedContent
            }
            Column {
                V5SheetHeader(
                    title = state.title,
                    subtitle = state.subtitle,
                    onClose = onDismiss,
                    leading = { Favicon(state.favicon, site = state.subtitle.takeIf { state.hasPage }) },
                )
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = V5Spacing.space3, bottom = V5Spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
                ) {
                    RoundActions(state, actions, ::closing)
                    if (state.canRead) {
                        ReadListenTiles(onRead = closing(actions.onRead), onListen = closing(actions.onListen))
                    }
                    // Downloads stand on their own line, with what is downloading now (canvas «Menu»).
                    V5ListGroup {
                        V5ListRow(
                            stringResource(R.string.v5_menu_downloads),
                            value = state.downloads?.let { v5DownloadsSummary(it) },
                            icon = V5RowIcon(VolaIcons.Download),
                            onClick = closing(actions.onDownloads),
                        )
                    }
                    PageGroup(state, actions, ::closing, onMore = { page = V5PageMenuPage.More })
                    // The library as four tiles (canvas «Menu»): one layer, no second list. New and
                    // closed tabs live in the overview, the extensions' commands in «More».
                    LibraryTiles(
                        onHistory = closing(actions.onHistory),
                        onFavorites = closing(actions.onFavorites),
                        onPasswords = actions.onPasswords?.let(::closing),
                        onSettings = closing(actions.onSettings),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageGroup(
    state: V5PageMenuState,
    actions: V5PageMenuActions,
    closing: (() -> Unit) -> () -> Unit,
    onMore: () -> Unit,
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
            stringResource(R.string.v5_menu_more),
            icon = V5RowIcon(VolaIcons.MoreHoriz),
            // What lies behind it, said before it is opened (canvas «MenuV2»).
            value = stringResource(R.string.v5_menu_more_hint),
            trailing = V5RowTrailing.Chevron,
            onClick = onMore,
            modifier = Modifier.testTag(V5PageMenuTestTags.More),
        )
    }
}

/** «More» (PM-05): the page's and the tab's rarer actions, a layer below the menu. */
@Composable
private fun MoreGroup(
    state: V5PageMenuState,
    actions: V5PageMenuActions,
    closing: (() -> Unit) -> () -> Unit,
) {
    V5ListGroup {
        V5ListRow(
            stringResource(R.string.action_open_in_app),
            icon = V5RowIcon(VolaIcons.OpenInNew),
            onClick = closing(actions.onOpenInApp),
            modifier = Modifier.testTag(V5PageMenuTestTags.OpenInApp),
        )
        V5ListRow(
            stringResource(R.string.action_duplicate_tab),
            icon = V5RowIcon(VolaIcons.ContentCopy),
            onClick = closing(actions.onDuplicateTab),
            modifier = Modifier.testTag(V5PageMenuTestTags.DuplicateTab),
        )
        V5ListRow(
            stringResource(if (state.isPinned) R.string.v5_menu_unpin_tab else R.string.action_pin_tab),
            icon = V5RowIcon(VolaIcons.PushPin),
            onClick = closing(actions.onTogglePinned),
        )
        state.desktopView?.let { desktop ->
            // What a switch does, said before it is touched: the page reloads, the site stays muted (М-7).
            V5ListRow(
                stringResource(R.string.action_desktop_view),
                subtitle = stringResource(R.string.v5_menu_desktop_view_hint),
                icon = V5RowIcon(VolaIcons.DesktopWindows),
                trailing = V5RowTrailing.Toggle(desktop, actions.onDesktopViewChange),
            )
        }
        state.siteMuted?.let { muted ->
            V5ListRow(
                stringResource(R.string.action_mute_domain),
                subtitle = stringResource(R.string.v5_menu_mute_site_hint),
                icon = V5RowIcon(VolaIcons.VolumeOff),
                trailing = V5RowTrailing.Toggle(muted, actions.onSiteMutedChange),
            )
        }
    }
    // Commands of extensions and user scripts for this page: rare, so they wait here.
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
}

/** Back from «More» to the menu. */
@Composable
private fun MenuBackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.heightIn(min = V5Sizes.touchTarget).testTag(V5PageMenuTestTags.Back),
    ) {
        Icon(VolaIcons.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = V5Theme.colors.label)
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

/** History, favorites, passwords and settings as four tiles in a row; passwords only where they exist. */
@Composable
private fun LibraryTiles(
    onHistory: () -> Unit,
    onFavorites: () -> Unit,
    onPasswords: (() -> Unit)?,
    onSettings: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = V5Spacing.marginPhone),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
    ) {
        LibraryTile(VolaIcons.History, stringResource(R.string.action_history), onHistory, V5PageMenuTestTags.History)
        LibraryTile(VolaIcons.Star, stringResource(R.string.v5_menu_favorites), onFavorites, V5PageMenuTestTags.Favorites)
        onPasswords?.let { LibraryTile(VolaIcons.Key, stringResource(R.string.passwords_title), it, V5PageMenuTestTags.Passwords) }
        LibraryTile(VolaIcons.Settings, stringResource(R.string.action_settings), onSettings, V5PageMenuTestTags.Settings)
    }
}

@Composable
private fun RowScope.LibraryTile(icon: ImageVector, label: String, onClick: () -> Unit, tag: String) {
    val colors = V5Theme.colors
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = V5Sizes.menuLibraryTile)
            .clip(V5Theme.shapes.m)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(V5Spacing.space2)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space1, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = colors.label, modifier = Modifier.size(V5Sizes.icon))
        Text(
            label,
            style = V5Theme.type.caption,
            color = colors.label,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
/** The page's tile in the header: its icon, or its letter; a new tab has no site and keeps the globe. */
private fun Favicon(favicon: ImageBitmap?, site: String?) {
    val colors = V5Theme.colors
    Box(
        Modifier
            .size(V5Sizes.rowTile)
            .clip(V5Theme.shapes.xs)
            .background(colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        V5SiteTile(favicon, site = site, size = V5Sizes.iconSmall)
    }
}
