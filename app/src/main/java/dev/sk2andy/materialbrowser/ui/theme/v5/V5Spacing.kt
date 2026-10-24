package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * v5 spacing and sizes (system-v5.md, section 5). A 4 dp grid; hairlines are the only exception.
 * Heights that hold text are minimums: with nonlinear font scaling (Android 14+) rows grow instead
 * of clipping, so use them with heightIn(min = ...).
 */

object V5Spacing {
    val space1: Dp = 4.dp
    val space2: Dp = 8.dp
    val space3: Dp = 12.dp
    val space4: Dp = 16.dp
    val space5: Dp = 20.dp
    val space6: Dp = 24.dp
    val space8: Dp = 32.dp
    val space10: Dp = 40.dp
    val space12: Dp = 48.dp
    val space16: Dp = 64.dp

    /** Lines between rows, edges of materials and of the bar. */
    val hairline: Dp = 0.5.dp

    /** Side margins: phone, tablet. */
    val marginPhone: Dp = space4
    val marginTablet: Dp = space6
}

object V5Sizes {
    /** Smallest touch target; in the bar the visible capsule and buttons are this size themselves. */
    val touchTarget: Dp = 48.dp
    /** The widest a full-screen state's content gets (a lock screen, B13), so lines stay readable. */
    val stateContentMax: Dp = 480.dp

    // Bottom bar on a phone (section 5.1).
    /** The bar: 8 + 48 + 8, plus the system navigation under it. */
    val bar: Dp = 64.dp
    /** The address capsule and the round buttons beside it. */
    val barCapsule: Dp = 48.dp
    /** The bar collapsed while scrolling; the whole strip stays one 48 dp target. */
    val barCollapsed: Dp = 28.dp
    /** Height the bar may grow to at 200 % font. */
    val barLargeFont: Dp = 72.dp
    /** The loading line along the bar's top edge. */
    val barProgress: Dp = 2.dp
    /** The downloads ring around «•••» (V22): as thin as the loading line. */
    val downloadRing: Dp = 2.dp
    /** The pause or play glyph inside a download's ring in the library (DL-02). */
    val downloadGlyph: Dp = 10.dp
    /** The pull to refresh indicator (V16): the M3 Expressive shape that morphs while it loads. */
    val pullRefreshIndicator: Dp = 40.dp
    /** How far a finger moves on the bar before it is a swipe rather than a tap. */
    val barGestureSlop: Dp = 8.dp

    // Tablet (section 5.3).
    val tabletBar: Dp = 52.dp
    val tabletTabStrip: Dp = 40.dp
    val tabletSidebar: Dp = 280.dp
    val tabletPageInset: Dp = 8.dp

    // Lists and grids (section 5.2).
    val rowSingleLine: Dp = 52.dp
    val rowTwoLine: Dp = 64.dp
    val rowTile: Dp = 30.dp
    val pinnedTile: Dp = 60.dp
    const val PINNED_COLUMNS = 4
    const val PINNED_MAX = 12
    val tabGridGap: Dp = 12.dp
    const val TAB_COLUMNS_PHONE = 2

    // Address editor (system-v5.md, section 9).
    /** A suggestion row; the best match is a little taller. */
    val suggestionRow: Dp = 52.dp
    val suggestionBest: Dp = 56.dp
    /** The icon tile at the start of a suggestion. */
    val suggestionTile: Dp = 32.dp
    /** The best match's tile and its accent outline (canvas «Address»). */
    val suggestionBestTile: Dp = 40.dp
    val suggestionBestBorder: Dp = 1.5.dp
    /** The «clear» circle in the address field. */
    val editorClear: Dp = 20.dp
    /** The «Cancel» button beside the field. */
    val editorCancel: Dp = 72.dp
    /** The round plate of «previous» and «next» beside the find-in-page field (canvas «FindInPage»). */
    val findStep: Dp = 40.dp
    /** The top of the suggestion list fades into the material over this height. */
    val suggestionFade: Dp = 36.dp

    // Tab overview (system-v5.md, section 10; canvas screen «d»).
    /** A tab card's thumbnail: width to height, as the canvas card 173 × 231. */
    const val OVERVIEW_CARD_RATIO = 0.75f
    /** The accent ring around the current tab's card. */
    val overviewRing: Dp = 2.dp
    /** The gap of the background's color between the current tab's card and its ring. */
    val overviewRingGap: Dp = 2.5.dp
    /** The profile's avatar in the overview's chip. */
    val overviewAvatar: Dp = 28.dp
    /** The round plate of a card's close button; its touch target stays [touchTarget]. */
    val overviewClosePlate: Dp = 24.dp
    /** The site icon under a card. */
    val overviewFavicon: Dp = 16.dp

    // Sheets.
    val sheetHandleWidth: Dp = 36.dp
    val sheetHandleHeight: Dp = 4.dp
    /** The handle sits this far below the sheet's top edge. */
    val sheetHandleTop: Dp = 6.dp

    /** A round action of the page menu with its caption (canvas screen «д»). */
    val menuActionWidth: Dp = 72.dp
    /** A tile of the menu's library row: History, Favorites, Passwords, Settings. */
    val menuLibraryTile: Dp = 64.dp

    // Lists (section 3.4): the capsule toggle and its thumb, inset by 2 dp.
    val toggleWidth: Dp = 51.dp
    val toggleHeight: Dp = 31.dp
    val toggleThumb: Dp = 27.dp
    val toggleThumbInset: Dp = 2.dp

    // Slider (canvas screen «ц3»): a 4 dp track and the toggle's white 28 dp knob, as on iOS.
    val sliderTrack: Dp = 4.dp
    val sliderThumb: Dp = 28.dp

    // Icons (section 6): next to 17 sp text, next to 15-12 sp text, the cap at 200 % font.
    val icon: Dp = 24.dp
    val iconSmall: Dp = 20.dp
    val iconMax: Dp = 32.dp

    /** The dot of a tab group, and of profiles in an overflowing switcher. */
    val dot: Dp = 8.dp
    /** The border of a tab card in a colored group. */
    val groupBorder: Dp = 2.dp
    /** A collapsed group's fan of pages (canvas `.gcol`): each thumbnail and how far they overlap. */
    val groupPreview: Dp = 32.dp
    val groupPreviewOverlap: Dp = 12.dp

    // Privacy report (canvas screen «л1»).
    /** The bar chart of blocks by day, the gap between its bars and their corners. */
    val reportChart: Dp = 72.dp
    val reportBarGap: Dp = 3.dp
    val reportBarRadius: Dp = 3.dp
    /** The report's own chart of a week (canvas «Report»): taller, with wider gaps and a weekday under each bar. */
    val reportWeekChart: Dp = 86.dp
    val reportWeekBarGap: Dp = 10.dp
    /** A finding's tile in the report and its corners. */
    val reportFindingTile: Dp = 36.dp
    val reportFindingTileRadius: Dp = 10.dp
    /** A row with a caption, a value and a detail line: the report's two findings. */
    val rowThreeLine: Dp = 72.dp

    // New tab (canvas screen «а»).
    /** The site icon inside a pinned tile, over its own blurred copy. */
    val pinnedFavicon: Dp = 28.dp
    /** How far a pinned tile's own icon is blurred into its backdrop (Zen's trick, section 11). */
    val pinnedBlur: Dp = 24.dp
    /** The round «remove» badge on a tile while pinned sites are edited. */
    val pinnedBadge: Dp = 22.dp
    /** The accent wash behind the top of the new tab fades out over this height. */
    val newTabWash: Dp = 280.dp
    // A fresh profile's new tab (canvas screen «EmptyStart»): the dashed outline of an empty pinned
    // slot, the length of its dashes and the bar standing in for the slot's name.
    val freshSlotBorder: Dp = 1.5.dp
    val freshSlotDash: Dp = 4.dp
    val freshSlotName: Dp = 40.dp

    // Private new tab (canvas screen «h1»): the round plate of the mask and the mask on it.
    val privateGem: Dp = 80.dp
    val privateGemIcon: Dp = 40.dp

    // Dangerous site (canvas screen «Danger»): the red tile of the warning, its corners and glyph,
    // and the chip with the site's address.
    val dangerTile: Dp = 72.dp
    val dangerTileRadius: Dp = 22.dp
    val dangerTileIcon: Dp = 36.dp
    val dangerChip: Dp = 32.dp

    // Site protection (canvas screen «Shield»): the shield's tile at the top of the panel.
    val siteShieldBadge: Dp = 52.dp

    // Reading settings sheet (canvas screen «ReaderSettings»): a typeface card, a theme swatch and
    // the ring around the chosen one, which stands off the swatch by its own width.
    val readerFontCard: Dp = 76.dp
    val readerThemeSwatch: Dp = 52.dp
    val readerRing: Dp = 2.dp

    // A Firefox extension's popup in a v5 sheet: room for a small panel, never past the screen.
    val extensionPopupMin: Dp = 160.dp
    val extensionPopupMax: Dp = 560.dp

    // Password check (canvas screen «у1»): the status mark beside the summary.
    val passwordStatus: Dp = 44.dp
    val passwordStatusIcon: Dp = 28.dp

    // Own server sync (canvas screen «х2»): the status mark beside «Synced».
    val syncStatus: Dp = 44.dp
    val syncStatusIcon: Dp = 28.dp

    // First run (canvas screen «п1»): Vola's mark on its accent tile.
    val firstRunMark: Dp = 88.dp
    val firstRunMarkIcon: Dp = 40.dp
    /** The plate of a gesture's hand on the two-gesture sheet (canvas screen «п3»). */
    val firstRunGesture: Dp = 52.dp

    // A profile's editor (canvas «NewProfile»): the profile as it will look, white on its accent.
    val profileAvatar: Dp = 88.dp
    val profileAvatarIcon: Dp = 40.dp

    // An empty list (canvas «EmptyHistory»): the list's glyph on a card over a tilted accent plate.
    val emptyArt: Dp = 120.dp
    val emptyArtCard: Dp = 88.dp
    val emptyArtIcon: Dp = 40.dp

    // Link preview (canvas screen «н1»).
    /** The address line over the live page in the preview card. */
    val glanceHeader: Dp = 40.dp
    /** The card's width on a wide window; a phone gives it the whole width less the margins. */
    val glanceMaxWidth: Dp = 480.dp
    /** The square the card grows from at the touch point, and its corners there. */
    val glanceOrigin: Dp = 48.dp
    val glanceOriginRadius: Dp = 6.dp
    /** The shrunk page's corners, close to a phone screen's own. */
    val glancePageRadius: Dp = 36.dp
    /** Space between the card and its buttons. */
    val glanceActionsGap: Dp = 14.dp

    // Sync over QR (canvas screens «ф1», «ф2»): the code on its white plate and the scanner frame.
    val syncCode: Dp = 232.dp
    val syncScanFrame: Dp = 252.dp
    val syncScanFrameBorder: Dp = 3.dp
}
