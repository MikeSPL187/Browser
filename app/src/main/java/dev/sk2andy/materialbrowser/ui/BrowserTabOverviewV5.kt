package dev.sk2andy.materialbrowser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.data.TabDeletionRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewState
import dev.sk2andy.materialbrowser.ui.v5.V5OverviewTab
import dev.sk2andy.materialbrowser.ui.v5.V5TabCard
import dev.sk2andy.materialbrowser.ui.v5.V5TabOverview
import dev.sk2andy.materialbrowser.ui.v5.V5TabOverviewRules

/**
 * The v5 tab overview (V10a) bound to the browser: the profile's tabs, their snapshots and icons,
 * and the same select, close, new tab and «Close all» as the current overview. It fades in from a
 * slightly smaller scale on the overviewEnter spring; the page shrinking into its card is V10b.
 */
@Composable
internal fun BrowserTabOverviewV5(
    controller: BrowserController,
    visible: Boolean,
    onClose: () -> Unit,
    onNewTab: () -> Unit,
    onEntryHeroStarted: (Boolean) -> Unit,
    onEntryHeroCompleted: () -> Unit,
) {
    // The current bar waits for the overview's entry hero before it gets out of the way; v5 has
    // no hero yet, so the entry is reported done at once.
    LaunchedEffect(visible) {
        if (visible) {
            onEntryHeroStarted(false)
            onEntryHeroCompleted()
        }
    }
    val darkShell = MaterialTheme.colorScheme.background.luminance() < DARK_SHELL_LUMINANCE
    val moves = V5Motion.animationsEnabled
    AnimatedVisibility(
        visible = visible,
        enter = if (moves) {
            fadeIn(V5Motion.overviewEnter.spec<Float>()) +
                scaleIn(V5Motion.overviewEnter.spec<Float>(), initialScale = ENTER_SCALE)
        } else {
            fadeIn(V5Motion.reducedFade<Float>())
        },
        exit = fadeOut(if (moves) V5Motion.effects.spec<Float>() else V5Motion.reducedFade<Float>()),
    ) {
        OverviewContent(controller, darkShell, onClose, onNewTab)
    }
}

@Composable
private fun OverviewContent(
    controller: BrowserController,
    darkShell: Boolean,
    onClose: () -> Unit,
    onNewTab: () -> Unit,
) {
    val rootView = LocalView.current
    val browserTabs = controller.activeTabs
    val byId = browserTabs.associateBy { it.id }
    val tabs = browserTabs.map { V5OverviewTab(it.id, it.title, it.url, it.isIncognito) }
    val lockedIds = browserTabs.filter(controller.privateTabsLock::hides).mapTo(hashSetOf()) { it.id }
    val hidden: (V5OverviewTab) -> Boolean = { it.id in lockedIds }
    val counts = V5TabOverviewRules.counts(tabs, hidden)
    var query by rememberSaveable { mutableStateOf("") }
    var privateSegment by rememberSaveable {
        mutableStateOf(V5TabOverviewRules.initialSegmentIsPrivate(controller.selectedTab.isIncognito, counts))
    }
    var closeAllCount by rememberSaveable { mutableIntStateOf(0) }
    val cards = V5TabOverviewRules.visibleTabs(tabs, privateSegment, query, hidden).mapNotNull { item ->
        val tab = byId[item.id] ?: return@mapNotNull null
        V5TabCard(
            id = tab.id,
            title = displayTabTitle(tab),
            selected = tab.id == controller.selectedTabId,
            // A private tab's page is not shown in the overview, as in the current one.
            preview = controller.previews[tab.id]?.takeUnless { tab.isIncognito }?.asImageBitmap(),
            favicon = controller.favicons[tab.id]?.asImageBitmap(),
        )
    }
    val profile = controller.profiles.firstOrNull { it.id == controller.activeProfileId }
    VolaV5Theme(dark = darkShell, privateBrowsing = privateSegment) {
        V5TabOverview(
            state = V5OverviewState(
                cards = cards,
                privateSegment = privateSegment,
                counts = counts,
                query = query,
                profileLabel = profile?.let { "${it.emoji} ${it.name}".trim() }.orEmpty(),
            ),
            onQueryChange = { query = it },
            onSegmentChange = { privateSegment = it },
            onSelect = { tabId ->
                controller.selectTab(tabId)
                onClose()
            },
            onClose = { tabId -> controller.closeTabFromUser(tabId) },
            onNewTab = {
                onNewTab()
                if (privateSegment) controller.setBlankTabIncognito(true)
            },
            onDone = onClose,
            onCloseAll = { closeAllCount = TabDeletionRules.deletableTabIds(controller.activeTabs).size },
        )
    }
    // «Close all tabs» asks first, like the current overview's menu.
    if (closeAllCount > 0) {
        val dismiss = { closeAllCount = 0 }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(pluralStringResource(R.plurals.close_all_tabs_title, closeAllCount, closeAllCount)) },
            text = { Text(stringResource(R.string.close_all_tabs_pinned_supporting_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeAllCount = 0
                        if (controller.closeAllTabs() > 0) rootView.performConfirmHaptic()
                    },
                ) { Text(stringResource(R.string.action_close_all_tabs)) }
            },
            dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_SHELL_LUMINANCE = 0.5f

/** Cards grow into place from this scale (system-v5.md, section 7, overviewEnter). */
private const val ENTER_SCALE = 0.92f
