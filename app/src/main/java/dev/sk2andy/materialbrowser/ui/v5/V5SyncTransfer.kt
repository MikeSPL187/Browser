package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5SyncColors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

object V5SyncTestTags {
    const val ShowCode = "v5_sync_show_code"
    const val Code = "v5_sync_code"
    const val Scan = "v5_sync_scan"
    const val ScanStatus = "v5_sync_scan_status"
    const val Done = "v5_sync_done"
    const val Undo = "v5_sync_undo"
}

/**
 * The giving side (canvas «ф1»): the [code] as drawn by the transfer (null while it is made), the
 * Wi-Fi both devices share ([networkName], null when the phone is on none or the name is hidden),
 * and how much will move.
 */
@Immutable
data class V5SyncShowState(
    val code: ImageBitmap?,
    val onWifi: Boolean,
    val networkName: String?,
    val tabs: Int,
    val bookmarks: Int,
)

/** Where the receiving side is (canvas «ф2»). */
enum class V5SyncScanStatus { Scanning, Receiving, CodeExpired, NotFound, Failed, NoCamera }

/** What came over (canvas «ф3»): from [deviceName], [tabs] in a new group, [bookmarksAdded] new favorites. */
@Immutable
data class V5SyncDoneState(
    val deviceName: String,
    val tabs: Int,
    val bookmarksAdded: Int,
    val bookmarksExisting: Int,
)

/** A null action leaves its row without a chevron, or the undo button out, where the binding has none. */
class V5SyncDoneActions(
    val onOpenGroup: (() -> Unit)?,
    val onOpenFavorites: (() -> Unit)?,
    val onDone: () -> Unit,
    val onUndo: (() -> Unit)?,
)

/**
 * «Show code» (V26, canvas «ф1»). One layer: the code, the line that tells where to scan it, what
 * will move and how the code is protected. Off Wi-Fi the page says so instead of showing a code
 * that could not work.
 */
@Composable
fun V5SyncShowCode(
    state: V5SyncShowState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5SyncTestTags.ShowCode),
    ) {
        V5PageBar(title = stringResource(R.string.v5_sync_show_title), onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space6, bottom = V5Spacing.space8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space5),
        ) {
            if (state.onWifi) {
                CodePlate(state.code)
                Column(
                    Modifier.padding(horizontal = V5Spacing.space6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(V5Spacing.space1),
                ) {
                    Text(
                        stringResource(R.string.v5_sync_show_hint),
                        style = V5Theme.type.title,
                        color = colors.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        state.networkName?.let { stringResource(R.string.v5_sync_same_wifi, it) }
                            ?: stringResource(R.string.v5_sync_same_wifi_unnamed),
                        style = V5Theme.type.callout,
                        color = colors.labelSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                NoWifi()
            }
            WhatMoves(state.tabs, state.bookmarks)
        }
    }
}

/**
 * «Scan code» (V26, canvas «ф2»): the camera's [viewfinder] under a dark veil with the frame, the
 * hint, and what the camera does. The viewfinder is the transfer's own; this screen only frames it.
 */
@Composable
fun V5SyncScan(
    status: V5SyncScanStatus,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewfinder: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(V5SyncColors.scanBackground)
            .testTag(V5SyncTestTags.Scan),
    ) {
        if (V5SyncRules.showsViewfinder(status)) Box(Modifier.fillMaxSize()) { viewfinder() }
        Box(Modifier.fillMaxSize().background(V5SyncColors.scanVeil))
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(VolaIcons.Close, contentDescription = stringResource(R.string.v5_sheet_close), tint = V5SyncColors.scanInk)
                }
                Text(
                    stringResource(R.string.v5_sync_scan_title),
                    style = V5Theme.type.headline,
                    color = V5SyncColors.scanInk,
                    modifier = Modifier.align(Alignment.Center).semantics { heading() },
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(V5Sizes.syncScanFrame)
                        .border(V5Sizes.syncScanFrameBorder, V5SyncColors.scanInk, V5Theme.shapes.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    if (status == V5SyncScanStatus.Receiving) CircularProgressIndicator(color = V5SyncColors.scanInk)
                }
            }
            Column(
                Modifier
                    .padding(horizontal = V5Spacing.space6)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                    .testTag(V5SyncTestTags.ScanStatus),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space1),
            ) {
                Text(
                    stringResource(V5SyncRules.scanTitle(status)),
                    style = V5Theme.type.title,
                    color = V5SyncColors.scanInk,
                    textAlign = TextAlign.Center,
                )
                V5SyncRules.scanMessage(status)?.let { message ->
                    Text(stringResource(message), style = V5Theme.type.callout, color = V5SyncColors.scanInkSoft, textAlign = TextAlign.Center)
                }
            }
            Text(
                stringResource(R.string.v5_sync_scan_privacy),
                style = V5Theme.type.footnote,
                color = V5SyncColors.scanInkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space8),
            )
        }
    }
}

/**
 * «Done» (V26, canvas «ф3»): where the tabs and bookmarks went, «Done», and «Undo transfer», which
 * takes back exactly what came over.
 */
@Composable
fun V5SyncDone(
    state: V5SyncDoneState,
    actions: V5SyncDoneActions,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5SyncTestTags.Done),
    ) {
        V5PageBar(title = stringResource(R.string.v5_sync_title), onBack = actions.onDone)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space8, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = V5Spacing.space6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            ) {
                Box(
                    Modifier
                        .size(V5Sizes.privateGem)
                        .clip(V5Theme.shapes.full)
                        .background(colors.success.copy(alpha = SUCCESS_WASH_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        VolaIcons.CheckCircle,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(V5Sizes.privateGemIcon),
                    )
                }
                Text(
                    stringResource(R.string.v5_sync_done_title, state.deviceName),
                    style = V5Theme.type.title,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                if (state.bookmarksExisting > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.v5_sync_done_existing,
                            state.bookmarksExisting,
                            state.bookmarksExisting,
                        ),
                        style = V5Theme.type.callout,
                        color = colors.labelSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (state.tabs > 0 || state.bookmarksAdded > 0) {
                V5ListGroup(dividerInset = V5ListDefaults.dividerInsetWithIcon) {
                    if (state.tabs > 0) {
                        V5ListRow(
                            title = stringResource(R.string.v5_sync_group, state.deviceName),
                            icon = V5RowIcon(VolaIcons.Tab, V5Tile.Blue),
                            value = state.tabs.toString(),
                            trailing = if (actions.onOpenGroup != null) V5RowTrailing.Chevron else V5RowTrailing.None,
                            onClick = actions.onOpenGroup,
                        )
                    }
                    if (state.bookmarksAdded > 0) {
                        V5ListRow(
                            title = stringResource(R.string.favorites_title),
                            icon = V5RowIcon(VolaIcons.Bookmark, V5Tile.Amber),
                            value = stringResource(R.string.v5_sync_added, state.bookmarksAdded),
                            trailing = if (actions.onOpenFavorites != null) V5RowTrailing.Chevron else V5RowTrailing.None,
                            onClick = actions.onOpenFavorites,
                        )
                    }
                }
            }
            Box(Modifier.padding(horizontal = V5Spacing.marginPhone)) {
                V5StateActions(
                    primary = V5StateAction(label = stringResource(R.string.action_done), onClick = actions.onDone),
                    secondary = listOfNotNull(
                        actions.onUndo?.let { onUndo ->
                            V5StateAction(
                                label = stringResource(R.string.v5_sync_undo),
                                onClick = onUndo,
                                testTag = V5SyncTestTags.Undo,
                            )
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun CodePlate(code: ImageBitmap?) {
    val description = stringResource(R.string.v5_sync_code_description)
    Box(
        Modifier
            .size(V5Sizes.syncCode)
            .clip(V5Theme.shapes.l)
            .background(V5SyncColors.codePaper)
            .padding(V5Spacing.space3)
            .semantics { contentDescription = description }
            .testTag(V5SyncTestTags.Code),
        contentAlignment = Alignment.Center,
    ) {
        if (code == null) {
            CircularProgressIndicator(color = V5Theme.colors.accent)
        } else {
            // Modules stay crisp: no filtering between the code's pixels.
            Image(code, contentDescription = null, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun NoWifi() {
    val colors = V5Theme.colors
    Column(
        Modifier.padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        V5StateGlyph(VolaIcons.WifiOff)
        Text(
            stringResource(R.string.v5_sync_no_wifi_title),
            style = V5Theme.type.title,
            color = colors.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.v5_sync_no_wifi),
            style = V5Theme.type.callout,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WhatMoves(tabs: Int, bookmarks: Int) {
    val colors = V5Theme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
        Text(
            stringResource(R.string.v5_sync_what_moves),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            modifier = Modifier
                .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
                .semantics { heading() },
        )
        V5ListGroup(dividerInset = V5ListDefaults.dividerInsetWithIcon) {
            V5ListRow(
                title = stringResource(R.string.v5_sync_tabs),
                icon = V5RowIcon(VolaIcons.Tab, V5Tile.Blue),
                value = tabs.toString(),
            )
            V5ListRow(
                title = stringResource(R.string.v5_sync_bookmarks),
                icon = V5RowIcon(VolaIcons.Bookmark, V5Tile.Amber),
                value = bookmarks.toString(),
            )
        }
        Text(
            stringResource(R.string.v5_sync_code_note),
            style = V5Theme.type.footnote,
            color = colors.labelSecondary,
            modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
        )
    }
}

private const val SUCCESS_WASH_ALPHA = 0.18f

/** Pure decisions of the sync screens, for unit tests. */
internal object V5SyncRules {
    /** The camera runs only while there is a code to find. */
    fun showsViewfinder(status: V5SyncScanStatus): Boolean =
        status == V5SyncScanStatus.Scanning || status == V5SyncScanStatus.Receiving

    fun scanTitle(status: V5SyncScanStatus): Int = when (status) {
        V5SyncScanStatus.Scanning -> R.string.v5_sync_scan_hint
        V5SyncScanStatus.Receiving -> R.string.v5_sync_receiving
        V5SyncScanStatus.CodeExpired -> R.string.v5_sync_error_expired
        V5SyncScanStatus.NotFound -> R.string.v5_sync_error_not_found
        V5SyncScanStatus.Failed -> R.string.v5_sync_error_failed
        V5SyncScanStatus.NoCamera -> R.string.v5_sync_error_camera
    }

    /** The line under the title; null where the title says it all. */
    fun scanMessage(status: V5SyncScanStatus): Int? = when (status) {
        V5SyncScanStatus.Scanning -> R.string.v5_sync_scan_where
        V5SyncScanStatus.CodeExpired -> R.string.v5_sync_error_expired_hint
        V5SyncScanStatus.NotFound -> R.string.v5_sync_error_not_found_hint
        V5SyncScanStatus.NoCamera -> R.string.v5_sync_error_camera_hint
        V5SyncScanStatus.Receiving, V5SyncScanStatus.Failed -> null
    }

    /**
     * The confirm sheet's line: what the transfer adds, naming only the kinds it brings, or
     * [nothing] when this phone already has all of it.
     */
    fun confirmMessage(tabs: String, bookmarks: String, hasTabs: Boolean, hasBookmarks: Boolean, nothing: String): String =
        listOfNotNull(tabs.takeIf { hasTabs }, bookmarks.takeIf { hasBookmarks })
            .joinToString(", ")
            .ifEmpty { nothing }
}
