package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import dev.sk2andy.materialbrowser.ui.theme.v5.SmoothCornerShape
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Shapes
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewPrivateLock
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewState
import dev.sk2andy.materialbrowser.ui.v5.V5PromptAction
import dev.sk2andy.materialbrowser.ui.v5.V5PromptRole
import dev.sk2andy.materialbrowser.ui.v5.V5PromptSheet
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewTab
import dev.sk2andy.materialbrowser.ui.v5.V5TabCard
import dev.sk2andy.materialbrowser.ui.v5.V5TabOverview
import dev.sk2andy.materialbrowser.ui.v5.V5TabOverviewRules

/**
 * The v5 tab overview (V10a) bound to the browser: the profile's tabs, their snapshots and icons,
 * and the same select, close, new tab and «Close all» as the current overview. On the way in the
 * page's snapshot shrinks into its card and on the way out the chosen card grows back into the
 * page, both on the overviewEnter spring (V10b); without a snapshot, or without animations, the
 * overview fades in instead.
 */
@Composable
internal fun BrowserTabOverviewV5(
    controller: BrowserController,
    visible: Boolean,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onNewTab: () -> Unit,
    onEntryHeroStarted: (Boolean) -> Unit,
    onEntryHeroCompleted: () -> Unit,
    /** A new tab straight in private, never an ordinary one turned private (О-2, М-6). */
    onNewPrivateTab: () -> Unit = onNewTab,
) {
    // The current bar waits for the overview's entry hero before it gets out of the way; the v5
    // hero is drawn inside the overview, above the page, so the bar can go at once.
    LaunchedEffect(visible) {
        if (visible) {
            onEntryHeroStarted(false)
            onEntryHeroCompleted()
        }
    }
    val darkShell = MaterialTheme.colorScheme.background.luminance() < DARK_SHELL_LUMINANCE
    val moves = V5Motion.animationsEnabled
    val heroOnEntry = moves && heroPreview(controller, controller.selectedTabId) != null
    AnimatedVisibility(
        visible = visible,
        enter = if (heroOnEntry) {
            EnterTransition.None
        } else if (moves) {
            fadeIn(V5Motion.overviewEnter.spec<Float>()) +
                scaleIn(V5Motion.overviewEnter.spec<Float>(), initialScale = ENTER_SCALE)
        } else {
            fadeIn(V5Motion.reducedFade<Float>())
        },
        exit = fadeOut(if (moves) V5Motion.effects.spec<Float>() else V5Motion.reducedFade<Float>()),
    ) {
        OverviewContent(controller, visible, darkShell, heroOnEntry, onClose, onSelect, onNewTab, onNewPrivateTab)
    }
}

/** The snapshot a hero can fly: none for private tabs, whose page the overview never shows. */
private fun heroPreview(controller: BrowserController, tabId: String?): ImageBitmap? {
    val tab = controller.activeTabs.firstOrNull { it.id == tabId } ?: return null
    if (tab.isIncognito) return null
    return controller.previews[tab.id]?.asImageBitmap()
}

@Composable
private fun OverviewContent(
    controller: BrowserController,
    visible: Boolean,
    darkShell: Boolean,
    heroOnEntry: Boolean,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
) {
    val rootView = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The hero: 0 is the page filling the overview, 1 is the page in its card.
    val cardBounds = remember { mutableStateMapOf<String, Rect>() }
    var overviewBounds by remember { mutableStateOf(Rect.Zero) }
    var heroTabId by remember { mutableStateOf(if (heroOnEntry) controller.selectedTabId else null) }
    val hero = remember { Animatable(0f) }
    // Set when the card was not laid out in time: the page then fades out where it is.
    var heroFades by remember { mutableStateOf(false) }
    // Set as a card is chosen: until the overview is gone, further taps do nothing.
    var exiting by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        if (exiting) {
            // Reopened while the last exit still fades out: show the overview as it is.
            exiting = false
            heroTabId = null
            hero.snapTo(0f)
            return@LaunchedEffect
        }
        val tabId = heroTabId ?: return@LaunchedEffect
        // The page covers the overview until its card is laid out; then it flies into the card.
        // Without a card in time the page fades into the overview instead.
        val landed = withTimeoutOrNull(HERO_TARGET_WAIT_MILLIS) {
            snapshotFlow { cardBounds[tabId] }.filterNotNull().first()
        }
        heroFades = landed == null
        hero.animateTo(1f, if (landed != null) V5Motion.overviewEnter.spec() else V5Motion.effects.spec())
        heroTabId = null
        heroFades = false
    }
    val browserTabs = controller.activeTabs
    val byId = browserTabs.associateBy { it.id }
    val tabs = browserTabs.map { V5OverviewTab(it.id, it.title, it.url, it.isIncognito) }
    val lockedIds = browserTabs.filter(controller.privateTabsLock::hides).mapTo(hashSetOf()) { it.id }
    val hidden: (V5OverviewTab) -> Boolean = { it.id in lockedIds }
    val counts = V5TabOverviewRules.counts(tabs)
    val lockedPrivate = V5TabOverviewRules.lockedPrivate(tabs, hidden)
    var query by rememberSaveable { mutableStateOf("") }
    var privateSegment by rememberSaveable {
        mutableStateOf(V5TabOverviewRules.initialSegmentIsPrivate(controller.selectedTab.isIncognito, counts))
    }
    // The tabs «Close all» asked about: the ones on screen when it was chosen (О-1).
    var closeAllIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val cards = V5TabOverviewRules.visibleTabs(tabs, privateSegment, query, hidden).mapNotNull { item ->
        val tab = byId[item.id] ?: return@mapNotNull null
        V5TabCard(
            id = tab.id,
            title = displayTabTitle(tab),
            selected = tab.id == controller.selectedTabId,
            // A private tab's page is not shown in the overview, as in the current one.
            preview = controller.previews[tab.id]?.takeUnless { tab.isIncognito }?.asImageBitmap(),
            favicon = controller.favicons[tab.id]?.asImageBitmap(),
            closable = TabDeletionRules.canDelete(tab),
        )
    }
    // Closed tabs and other segments leave no bounds behind for a hero to fly to.
    val cardIds = cards.mapTo(hashSetOf()) { it.id }
    SideEffect { cardBounds.keys.filter { it !in cardIds }.forEach { cardBounds.remove(it) } }
    // The profile's tab groups (V12a): the same groups the data keeps, in their order.
    val groups = controller.tabStackController.activeStacks.map { it.v5Group() }
    var groupSheet by remember { mutableStateOf<TabGroupSheetTarget?>(null) }
    var profileSheet by remember { mutableStateOf(false) }
    val profile = controller.profiles.firstOrNull { it.id == controller.activeProfileId }
    VolaV5Theme(dark = darkShell, privateBrowsing = privateSegment) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { overviewBounds = it.boundsInRoot() }) {
            V5TabOverview(
                state = V5OverviewState(
                    cards = cards,
                    privateSegment = privateSegment,
                    counts = counts,
                    query = query,
                    profileLabel = profile?.name?.ifBlank { null } ?: stringResource(R.string.v5_profile_untitled),
                    profileIcon = profile?.let { WorkspaceIcons.vector(it.emoji) },
                    groups = groups,
                    canOpenPrivate = controller.canOpenLinkInPrivate,
                    privateLock = V5OverviewPrivateLock(
                        count = counts.private,
                        unlockAvailable = controller.isProfileProtectionSupported,
                    ).takeIf { lockedPrivate > 0 },
                ),
                onUnlockPrivate = controller.privateTabsLock::unlock,
                onCloseAllPrivate = { if (!exiting) controller.closeAllPrivateTabs() },
                onQueryChange = { query = it },
                onSegmentChange = { privateSegment = it },
                // The browser's select hands the card's snapshot over to the page until it draws; first
                // the card grows back into the page.
                onSelect = select@{ tabId ->
                    if (exiting) return@select
                    exiting = true
                    if (V5Motion.animationsEnabled && heroTabId == null &&
                        heroPreview(controller, tabId) != null && cardBounds[tabId] != null
                    ) {
                        scope.launch {
                            hero.snapTo(1f)
                            heroTabId = tabId
                            hero.animateTo(0f, V5Motion.overviewEnter.spec())
                            onSelect(tabId)
                            onClose()
                        }
                    } else {
                        onSelect(tabId)
                        onClose()
                    }
                },
                onClose = { tabId -> if (!exiting) controller.closeTabFromUser(tabId) },
                // The overview stays open when no new tab comes (a blank one is reused), so this
                // does not mark the exit.
                onNewTab = {
                    if (!exiting) {
                        // Checked before the tab is made: a refusal after it would leave an ordinary tab.
                        when {
                            !privateSegment -> onNewTab()
                            controller.canOpenLinkInPrivate -> onNewPrivateTab()
                        }
                    }
                },
                onDone = {
                    if (!exiting) {
                        exiting = true
                        onClose()
                    }
                },
                onCloseAll = {
                    if (!exiting) closeAllIds = V5TabOverviewRules.closeAllIds(cards)
                },
                onCloseRefused = { V5Feedback.show(context, R.string.v5_overview_pinned_stays) },
                modifier = Modifier.graphicsLayer { alpha = if (heroTabId != null) hero.value.coerceIn(0f, 1f) else 1f },
                // Written only when a card moved: a layout pass without a change writes nothing (О-8).
                onCardBounds = { id, bounds -> if (cardBounds[id] != bounds) cardBounds[id] = bounds },
                heroCardId = heroTabId,
                onToggleGroup = { id -> if (!exiting) controller.tabStackController.toggleCollapsed(id) },
                onCardLongPress = { id -> if (!exiting) groupSheet = TabGroupSheetTarget.Move(id) },
                onGroupLongPress = { id -> if (!exiting) groupSheet = TabGroupSheetTarget.Edit(id) },
                onProfiles = { if (!exiting) profileSheet = true }.takeIf { controller.profilesEnabled },
            )
            val heroId = heroTabId
            val heroBitmap = heroId?.let { heroPreview(controller, it) }
            if (heroBitmap != null) {
                OverviewHero(
                    bitmap = heroBitmap,
                    from = overviewBounds,
                    to = if (heroFades) null else cardBounds[heroId],
                    progress = hero.value,
                )
            }
            BrowserTabGroupSheetsV5(controller, groupSheet, onTargetChange = { groupSheet = it })
            BrowserProfileSheetV5(controller, visible = profileSheet, onDismiss = { profileSheet = false })
        }
    }
    // «Close all tabs» asks first, in red as an action that removes, and closes what it counted: the
    // segment's cards in front (О-1). The title keeps its count while the sheet lowers, so it never
    // reads «Close 0 tabs»; the pinned-tabs note shows only where pinned tabs stay.
    var shownCount by remember { mutableIntStateOf(1) }
    if (closeAllIds.isNotEmpty()) shownCount = closeAllIds.size
    val pinnedStay = cards.any { !it.closable }
    VolaV5Theme(dark = darkShell, privateBrowsing = privateSegment) {
        V5PromptSheet(
            visible = closeAllIds.isNotEmpty(),
            title = pluralStringResource(R.plurals.close_all_tabs_title, shownCount, shownCount),
            message = stringResource(R.string.close_all_tabs_pinned_supporting_text).takeIf { pinnedStay },
            actions = listOf(
                V5PromptAction(
                    label = stringResource(R.string.action_close_all_tabs),
                    role = V5PromptRole.Destructive,
                    onClick = {
                        val ids = closeAllIds
                        closeAllIds = emptyList()
                        ids.forEach(controller::closeTab)
                        if (ids.isNotEmpty()) rootView.performConfirmHaptic()
                    },
                ),
            ),
            onDismiss = { closeAllIds = emptyList() },
        )
    }
}

/**
 * The page's snapshot between the whole overview ([from]) and its card ([to]), both in root
 * coordinates; the corners round into the card's as it lands. Until the card is laid out, or the
 * overview itself, the page fills the overview and only fades with [progress].
 */
@Composable
private fun OverviewHero(bitmap: ImageBitmap, from: Rect, to: Rect?, progress: Float) {
    val density = LocalDensity.current
    val travel = progress.coerceIn(0f, 1f)
    val placed = if (to != null && from != Rect.Zero) {
        val rect = lerp(from, to, progress)
        Modifier
            .offset { IntOffset((rect.left - from.left).roundToInt(), (rect.top - from.top).roundToInt()) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            .clip(SmoothCornerShape(V5Shapes.radiusM * travel))
    } else {
        Modifier.fillMaxSize().graphicsLayer { alpha = 1f - travel }
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = placed,
    )
}

/** How long the hero waits for its card to be laid out before the overview just shows. */
private const val HERO_TARGET_WAIT_MILLIS = 300L

/** Below this background luminance the current theme is dark. */
private const val DARK_SHELL_LUMINANCE = 0.5f

/** Cards grow into place from this scale (system-v5.md, section 7, overviewEnter). */
private const val ENTER_SCALE = 0.92f
