package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sk2andy.materialbrowser.shared.ui.BrowserMainMenuTileStyle

/*
 * v4 design tokens from the Tokens board (docs/vola/design/canvas/W-Tokens.dc.html, vola4.css).
 * Colors live in VolaColors.kt and VolaSchemes.kt, type in VolaType.kt.
 */

/** Corner radii: --r-xs … --r-sheet in vola4.css. */
internal object VolaShapes {
    val extraSmallRadius = 8.dp
    val smallRadius = 12.dp
    val mediumRadius = 16.dp
    val largeRadius = 20.dp
    val cardRadius = 24.dp
    val extraLargeRadius = 28.dp
    val sheetRadius = 32.dp

    /** Cards and tiles. */
    val card = RoundedCornerShape(cardRadius)

    /** Bottom sheets: rounded on top, flush with the screen edge below. */
    val sheet = RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius)

    /** Material shape roles on the v4 radii. */
    val material = Shapes(
        extraSmall = RoundedCornerShape(extraSmallRadius),
        small = RoundedCornerShape(smallRadius),
        medium = RoundedCornerShape(mediumRadius),
        large = RoundedCornerShape(largeRadius),
        extraLarge = RoundedCornerShape(extraLargeRadius),
    )
}

/** Spacing on a 4 dp grid. Each step notes its typical use from the Tokens board. */
internal object VolaSpacing {
    /** Inside an icon container. */
    val x1 = 4.dp

    /** Between chips. */
    val x2 = 8.dp

    /** Inside a list row. */
    val x3 = 12.dp

    /** Screen edges. */
    val x4 = 16.dp

    /** Inside a card. */
    val x5 = 20.dp

    /** Between groups. */
    val x6 = 24.dp

    /** Between sections. */
    val x8 = 32.dp

    /** Above a screen title. */
    val x12 = 48.dp
}

/** The framed shell («Рама»): the page card on the workspace aura (`.page-card` in vola4.css). */
internal object VolaFrame {
    /** Aura visible left and right of the page card. */
    val sideGutter = 6.dp

    /** Aura between the page card and the address bar below it. */
    val barGap = 8.dp

    /** Page card corners, a little tighter than the screen corners around them. */
    val pageRadius = 26.dp

    val pageShape = RoundedCornerShape(pageRadius)

    /** Hairline around the card: onSurface at this opacity. */
    const val OUTLINE_ALPHA = 0.06f
    val outlineWidth = 1.dp

    /** Soft drop shadow under the card: rgba(10, 20, 24, 0.08) 0 8px 24px. */
    val shadowColor = Color(0xFF0A1418)
    const val SHADOW_ALPHA = 0.08f
    val shadowOffsetY = 8.dp
    val shadowBlur = 24.dp
}

/** The address island (boards Main, Scrolled, V4B-Page). */
internal object VolaIsland {
    /** The scrolled-away bar: a small capsule with the lock and the site. */
    val compactHeight = 40.dp

    /** The aura rim of the island in the Air layout (`.halo` in vola4.css). */
    val rimWidth = 1.5.dp

    /** Glow of the aura under the island in the Air layout. */
    val glowElevation = 14.dp
}

/** Shadow depth: --e1 … --e3 in vola4.css. */
internal object VolaElevation {
    /** Cards and tiles. */
    val level1 = 2.dp

    /** The address island and menus. */
    val level2 = 6.dp

    /** Sheets and dialogs. */
    val level3 = 12.dp
}

/**
 * M3 Expressive springs. Pair workspace switches, swipe-to-close and long presses with haptic
 * feedback.
 */
internal object VolaMotion {
    const val FAST_DAMPING_RATIO = 0.9f
    const val FAST_STIFFNESS = 1400f
    const val STANDARD_DAMPING_RATIO = 0.9f
    const val STANDARD_STIFFNESS = 700f
    const val SLOW_DAMPING_RATIO = 0.9f
    const val SLOW_STIFFNESS = 300f
    const val EFFECTS_DAMPING_RATIO = 1f
    const val EFFECTS_STIFFNESS = 1600f

    /** Buttons, switches and chips. */
    fun <T> fast(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(FAST_DAMPING_RATIO, FAST_STIFFNESS, visibilityThreshold)

    /** The address bar, sheets and cards. */
    fun <T> standard(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(STANDARD_DAMPING_RATIO, STANDARD_STIFFNESS, visibilityThreshold)

    /** Workspace switches and Split View. */
    fun <T> slow(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(SLOW_DAMPING_RATIO, SLOW_STIFFNESS, visibilityThreshold)

    /**
     * The page shrinking into its tab card and growing back: critically damped, so the page lands
     * in its card without overshooting. Settles within [TAB_MORPH_SETTLE_MILLIS].
     */
    fun tabMorph(): SpringSpec<Float> = spring(TAB_MORPH_DAMPING_RATIO, TAB_MORPH_STIFFNESS)
    const val TAB_MORPH_DAMPING_RATIO = 1f
    const val TAB_MORPH_STIFFNESS = 800f
    const val TAB_MORPH_SETTLE_MILLIS = 260L

    /** Color, opacity and blur: no overshoot. */
    fun <T> effects(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(EFFECTS_DAMPING_RATIO, EFFECTS_STIFFNESS, visibilityThreshold)
}

/** Essentials on the new tab (boards NewTab and EssentialsEdit). */
internal object VolaEssentials {
    const val COLUMNS = 4

    /** On a tablet the new tab keeps a phone-wide column, so tiles stay together. */
    val maxContentWidth = 560.dp

    /** The tile behind a site icon: lowest surface at this opacity over the aura. */
    val tileSize = 68.dp
    val tileShape = VolaShapes.card
    const val TILE_ALPHA = 0.86f

    /** The site icon, or its letter, centered on the tile. */
    val iconSize = 36.dp
    val iconShape = RoundedCornerShape(VolaShapes.smallRadius)

    val rowGap = 18.dp
    val columnGap = VolaSpacing.x2
    val labelGap = VolaSpacing.x2

    /** The ✕ badge on a tile in edit mode, ringed with the page color, over its top-left corner. */
    val removeBadgeSize = 24.dp
    val removeBadgeInset = 4.dp
    val removeBadgeRing = 2.dp
    val removeIconSize = 15.dp

    /** The «Add» tile: an accent outline. */
    val addOutline = 2.dp

    /** The lifted tile while it is dragged. */
    const val DRAG_SCALE = 1.08f

    /** Rows of the «Add from open tabs» sheet. */
    val sheetIconSize = 40.dp
    val sheetRowMinHeight = 64.dp
}

/** One empty, error or offline message (board States): icon, title, one sentence, one action. */
internal object VolaStateTokens {
    val iconContainerSize = 72.dp
    val iconContainerShape = VolaShapes.card
    val iconSize = 36.dp
    val messageMaxWidth = 250.dp
    val buttonHeight = 44.dp
    val paddingTop = 28.dp
    val paddingHorizontal = 22.dp
    val paddingBottom = VolaSpacing.x6
}

/** The protection card on the new tab and its weekly report (boards NewTab, ProtectionReport). */
/** Settings pages. */
internal object VolaSettings {
    /** Between the rows of one settings group. */
    val rowGap = 2.dp
}

internal object VolaProtection {
    val cardMinHeight = 72.dp
    val cardIconSize = 40.dp
    val cardIconShape = RoundedCornerShape(VolaShapes.smallRadius)

    val heroIconSize = 56.dp
    val heroIconShape = RoundedCornerShape(VolaShapes.largeRadius)
    val heroGlyphSize = 30.dp

    /** The seven day bars. */
    val chartHeight = 88.dp
    val barShape = RoundedCornerShape(6.dp)

    /** The share of a site next to its name. */
    val siteBarHeight = 6.dp
    val siteBarShape = RoundedCornerShape(3.dp)
    val siteCountWidth = 48.dp
}

/** The private new tab (board PrivateTab). */
internal object VolaPrivateTab {
    /** The mask gem above the title, lit with the private accent. */
    val gemSize = 72.dp
    val gemShape = RoundedCornerShape(26.dp)
    val gemGlyphSize = 36.dp
    val gemGlow = 12.dp

    /** One fact: an icon in a tinted square, a title and a caption. */
    val factIconSize = 40.dp
    val factIconShape = RoundedCornerShape(14.dp)
    val factGap = 14.dp
}

/** The address editor at the bottom (boards Editing, V4A-Address, V4B-Address). */
internal object VolaAddressEditor {
    /** The field the island morphs into while typing, a pill with an accent ring. */
    val fieldHeight = 56.dp
    val fieldRingWidth = 2.dp

    /** The card of suggestions above the field. */
    val cardRadius = 26.dp
    val cardPadding = 6.dp

    /** Rows with a site tile and two lines of text. */
    val siteRowMinHeight = 56.dp
    val siteRowRadius = 18.dp
    val siteTileSize = 36.dp
    val siteTileRadius = 12.dp

    /** One-line search rows. */
    val searchRowHeight = 48.dp
    val searchRowRadius = 16.dp

    /** The filled "Switch" pill on an open-tab row. */
    val switchPillHeight = 36.dp

    /** Hairline between groups: outlineVariant at this opacity. */
    const val DIVIDER_ALPHA = 0.5f

    /** Tint of the open-tab row: primaryContainer at this opacity. */
    const val OPEN_TAB_TINT_ALPHA = 0.45f

    /** The clipboard chip and its action pill. */
    val chipHeight = 40.dp
    val chipActionHeight = 32.dp

    /** The workspace gem in the header and on rows from another workspace. */
    val headerGemSize = 32.dp
    val rowGemSize = 18.dp
}

/** Workspace sheets (boards W-WorkspaceSheet, W-WorkspaceSettings). */
internal object VolaWorkspaceSheet {
    val sidePadding = VolaSpacing.x5
    val bottomPadding = VolaSpacing.x5
    val headerGemSize = 56.dp
    val headerGap = VolaSpacing.x3
    val sectionGap = VolaSpacing.x4
    val labelGap = VolaSpacing.x2

    /** The icon grid: six tiles a row, the chosen one in the workspace's own gem colors. */
    const val ICON_COLUMNS = 6

    /** «New workspace» shows two rows until «All icons». */
    const val ICON_COLLAPSED_ROWS = 2
    val iconTileHeight = 48.dp
    val iconTileShape = RoundedCornerShape(VolaShapes.mediumRadius)
    val iconTileGap = VolaSpacing.x2
    val iconSize = 22.dp

    /** A group of rows on one card, divided by hairlines. */
    val groupShape = VolaShapes.card
    val rowMinHeight = 64.dp
    val rowPadding = VolaSpacing.x4
    val rowGap = VolaSpacing.x4
    val rowIconSize = 24.dp
    val rowGemSize = 24.dp
    val dividerInset = 56.dp
    val dividerThickness = 1.dp

    val buttonHeight = 56.dp
    val buttonShape = RoundedCornerShape(percent = 50)
}

/** The workspace swipe of the tab overview (board W-WorkspaceSwipe). */
internal object VolaWorkspaceSwipe {
    /** A flick this fast switches the workspace however short it was. */
    val flingVelocity = 1000.dp
}

/** The workspace gem (`.gem` in vola4.css): the workspace icon on its primary color. */
internal object VolaGem {
    /** Corner radius as a share of the gem's size: 11 dp on a 32 dp gem. */
    const val CORNER_FRACTION = 0.34f

    /** The gradient starts from primary lightened by this much white. */
    const val HIGHLIGHT_FRACTION = 0.22f

    /** The gradient reaches pure primary at this share of the diagonal. */
    const val PRIMARY_STOP = 0.7f

    /** The icon inside, as a share of the gem's size. */
    const val ICON_FRACTION = 0.56f
}

/** The tab overview (boards W-Tabs, W-TabsDark): one card per tab on the workspace aura. */
internal object VolaTabOverview {
    /** The workspace name and the tab count above the grid. */
    val headerHeight = 56.dp
    val headerStartPadding = VolaSpacing.x5
    val headerEndPadding = VolaSpacing.x2
    val headerCountGap = 10.dp

    /** The search field that replaces the header while searching tabs. */
    val searchFieldHeight = 48.dp
    val searchFieldPadding = VolaSpacing.x3
    val searchFieldStartPadding = VolaSpacing.x3

    /** A tab card: a title row over the page, with no frame around the page itself. */
    val cardRadius = 22.dp
    val cardShape = RoundedCornerShape(cardRadius)
    /** 40 dp on the board; 48 dp so the ✕ in it is a full touch target. */
    val cardTitleRowHeight = 48.dp
    val cardTitleStartPadding = VolaSpacing.x3
    val cardTitleEndPadding = 2.dp
    val cardTitleGap = VolaSpacing.x2
    val cardFaviconSize = 20.dp
    val cardFaviconShape = RoundedCornerShape(6.dp)
    val cardCloseIconSize = 18.dp
    val cardDividerWidth = 1.dp

    /** The current tab: an accent ring and an accent glow. */
    val selectedRingWidth = 2.5.dp
    val selectedGlowElevation = 14.dp

    /** The Essentials row over the workspace dock. */
    const val ESSENTIALS_COLUMNS = 5
    val essentialTileHeight = 56.dp
    val essentialTileShape = RoundedCornerShape(18.dp)
    val essentialIconSize = 30.dp
    val essentialGap = 10.dp
    const val ESSENTIAL_TILE_ALPHA = 0.82f

    /** The workspace dock and the new-tab button, at the bottom within reach of the thumb. */
    val dockHeight = 56.dp
    val dockPadding = VolaSpacing.x1
    val dockSideMargin = VolaSpacing.x3
    val dockBottomMargin = VolaSpacing.x3
    val dockGap = 10.dp
    const val DOCK_ALPHA = 0.7f
    const val DOCK_OUTLINE_ALPHA = 0.06f
    val dockOutlineWidth = 1.dp
    val dockItemSize = 48.dp
    val activeWorkspaceGemSize = 40.dp
    val workspaceGemSize = 32.dp
    val activeWorkspaceEndPadding = 14.dp
    val activeWorkspaceGap = VolaSpacing.x2
    val newTabButtonSize = 56.dp
    val newTabButtonRadius = VolaShapes.largeRadius
    val newTabButtonShape = RoundedCornerShape(newTabButtonRadius)
    val newTabGlowElevation = VolaElevation.level3

    /** Space between the grid, the Essentials row and the dock. */
    val sectionGap = VolaSpacing.x2
}

/** The tab actions sheet (board W-TabActions): the tab lifted over a blurred overview. */
internal object VolaTabActions {
    /** The overview behind: blurred and veiled with surfaceContainer at this opacity. */
    val backdropBlur = 8.dp
    const val SCRIM_ALPHA = 0.45f

    /** The lifted card, ringed like the current tab in the grid. */
    val cardWidth = 240.dp
    val cardHeight = 262.dp
    val cardTopPadding = VolaSpacing.x4
    val cardPanelGap = VolaSpacing.x4

    /** The panel of actions below the card. */
    val panelSideMargin = VolaSpacing.x5
    val panelShape = RoundedCornerShape(VolaShapes.extraLargeRadius)
    val panelPadding = 6.dp
    val panelElevation = VolaElevation.level3
    val panelBottomPadding = VolaSpacing.x4

    /** The quick actions: four in a row, an icon over a short label. */
    val quickActionHeight = 72.dp
    val quickActionShape = RoundedCornerShape(VolaShapes.largeRadius)
    val quickActionGap = 6.dp
    val quickActionSpacing = 2.dp
    val dividerWidth = 1.dp

    /** A row: an icon, a label, then a value and a chevron. */
    val rowMinHeight = 52.dp
    val rowHorizontalPadding = VolaSpacing.x4
    val rowVerticalPadding = 10.dp
    val rowGap = VolaSpacing.x4
    val rowIconSize = 24.dp
    val rowShape = RoundedCornerShape(VolaShapes.largeRadius)

    /** Rows opened under a row (workspaces, groups, more) sit a step in. */
    val nestedIndent = VolaSpacing.x5
    val nestedGemSize = 28.dp
}

/** The main menu (boards W-Menu, W-MenuDark): a sheet of tiles over the library rows. */
internal object VolaMenu {
    /** The sheet spans the screen less this margin; on a tablet it stops at [maxWidth]. */
    val screenMargin = VolaSpacing.x2
    val maxWidth = 600.dp
    val sheetRadius = VolaShapes.sheetRadius
    val contentPadding = VolaSpacing.x3

    /** Library rows: Downloads, History, Favorites … Settings. */
    val rowMinHeight = 52.dp
    val rowLabelSize = 15.sp
    val groupRadius = 22.dp

    /** Four tiles a row (fewer at large text): an icon over a short label; a toggle that is on is tinted. */
    val tiles = BrowserMainMenuTileStyle(
        columns = 4,
        tileHeight = 84.dp,
        cornerRadius = groupRadius,
        spacing = VolaSpacing.x2,
        iconSize = 24.dp,
        iconLabelGap = VolaSpacing.x2,
        horizontalPadding = VolaSpacing.x1,
        labelFontSize = 12.sp,
        labelLineHeight = 14.sp,
        labelMaxLines = 3,
        disabledAlpha = 0.38f,
        handleSize = DpSize(36.dp, 4.dp),
        handleTopPadding = 10.dp,
        handleBottomPadding = VolaSpacing.x1,
    )
}

/** Site information (board W-SiteInfo): the site's gem, then cards of rows. */
internal object VolaSiteInfo {
    val sidePadding = VolaSpacing.x4
    val bottomPadding = VolaSpacing.x5
    val sectionGap = VolaSpacing.x3
    val labelPadding = VolaSpacing.x2

    val gemSize = 40.dp
    val gemShape = RoundedCornerShape(14.dp)
    val headerGap = VolaSpacing.x3

    /** A card of rows divided by hairlines (board W-SiteInfo). */
    val cardShape = RoundedCornerShape(22.dp)
    val rowMinHeight = 52.dp
    val rowHorizontalPadding = VolaSpacing.x4
    val rowVerticalPadding = 10.dp
    val rowGap = VolaSpacing.x4
    val rowIconSize = 24.dp
    val dividerInset = 56.dp
    val dividerThickness = 1.dp
}

/**
 * A whole page in one state (boards W-Offline, W-HttpsOnly, W-Locked): the icon in a rounded
 * square, a title and one sentence near the middle, the actions at the bottom under the thumb.
 */
internal object VolaStatePageTokens {
    val maxContentWidth = 560.dp
    val contentPaddingHorizontal = 28.dp
    val contentPaddingVertical = VolaSpacing.x8
    val contentGap = 14.dp

    /** Extra room under the icon, on top of [contentGap]. */
    val iconBottomGap = 6.dp
    val iconContainerSize = 84.dp
    val iconContainerShape = RoundedCornerShape(30.dp)
    val iconSize = 44.dp

    /** The small badge on the icon's corner, such as the lock on a locked workspace's gem. */
    val badgeSize = 36.dp
    val badgeIconSize = 20.dp
    val badgeOffset = 8.dp

    val actionsPaddingHorizontal = VolaSpacing.x5
    val actionsPaddingBottom = VolaSpacing.x6
    val actionGap = 10.dp
    val buttonHeight = 52.dp
    val buttonIconSize = 20.dp
}

/** «Dangerous site» (board W-DangerousSite): the whole page in the danger color. */
internal object VolaDangerousSite {
    /** Secondary text and the facts card, as shares of the content color. */
    const val SECONDARY_TEXT_ALPHA = 0.86f
    const val CARD_ALPHA = 0.14f
    val cardShape = VolaShapes.card
    val cardPadding = VolaSpacing.x4
    val rowGap = VolaSpacing.x3
    val rowIconSize = 20.dp
}
