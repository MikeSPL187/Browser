package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.EssentialCandidate
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.EssentialsRules
import dev.sk2andy.materialbrowser.data.ProtectionReportRules
import dev.sk2andy.materialbrowser.shared.ui.WorkspaceIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5MovableTabs
import dev.sk2andy.materialbrowser.ui.v5.V5MoveTabsSheet
import dev.sk2andy.materialbrowser.ui.v5.V5NewTab
import dev.sk2andy.materialbrowser.ui.v5.V5NewTabFresh
import dev.sk2andy.materialbrowser.ui.v5.V5NewTabReport
import dev.sk2andy.materialbrowser.ui.v5.V5NewTabRules
import dev.sk2andy.materialbrowser.ui.v5.V5NewTabState
import dev.sk2andy.materialbrowser.ui.v5.V5PinnedAddSheet
import dev.sk2andy.materialbrowser.ui.v5.V5PinnedSite
import dev.sk2andy.materialbrowser.ui.v5.V5RecentTab

/**
 * The v5 new tab (V13a) bound to the profile: its Essentials as pinned sites (the same list the
 * current page edits), the week of the privacy report, which opens the whole v5 report, and the tabs
 * used last. Adding pins an open tab of the profile; editing unpins.
 */
@Composable
internal fun BrowserNewTabV5(
    essentials: List<EssentialEntry>,
    essentialIcons: Map<String, Bitmap>,
    onOpenEssential: (String) -> Unit,
    editor: NewTabEssentialsEditor?,
    profileLabel: String?,
    /** The profile's stored symbol key; Vola draws its Material Symbol. */
    profileIconKey: String?,
    recentTabs: List<BrowserTab>,
    recentTabFavicons: Map<String, Bitmap>,
    onRecentTab: (String) -> Unit,
    protection: NewTabProtection?,
    contentPadding: PaddingValues,
    fresh: NewTabFreshStart? = null,
) {
    var reportVisible by rememberSaveable { mutableStateOf(false) }
    var addVisible by rememberSaveable { mutableStateOf(false) }
    var moveVisible by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val week = protection?.summary?.invoke(ProtectionReportRules.DAYS)
    val pinnedIcons = rememberImageBitmaps(essentialIcons)
    val recentIcons = rememberImageBitmaps(recentTabFavicons)
    val state = V5NewTabState(
        profileLabel = profileLabel,
        pinned = essentials.map { entry ->
            V5PinnedSite(id = entry.id, title = EssentialsRules.label(entry), icon = pinnedIcons[entry.url])
        },
        canAdd = editor != null && !editor.isFull,
        report = week?.let { summary ->
            V5NewTabReport(
                total = summary.total,
                daily = summary.daily,
                weekdays = V5NewTabRules.weekdays(summary.lastEpochDay, summary.daily.size, locale),
            )
        },
        recent = recentTabs.map { tab -> tab.v5Recent(recentIcons[tab.id]) },
        profileIcon = profileIconKey?.let(WorkspaceIcons::vector),
        canEdit = editor != null,
        fresh = fresh?.takeIf { it.isFresh && essentials.isEmpty() }?.let { start ->
            V5NewTabFresh(
                profileName = start.profileName,
                copyFrom = start.copyFromName,
                canMoveTabs = start.movableTabs.isNotEmpty(),
            )
        },
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
    VolaV5Theme(dark = dark) {
        V5NewTab(
            state = state,
            onOpenPinned = { id -> essentials.firstOrNull { it.id == id }?.let { onOpenEssential(it.url) } },
            onRemovePinned = { id -> essentials.firstOrNull { it.id == id }?.let { editor?.remove(it) } },
            onMovePinned = { id, toIndex -> essentials.firstOrNull { it.id == id }?.let { editor?.move(it, toIndex) } },
            onAddPinned = { addVisible = true },
            onOpenReport = { reportVisible = true },
            onHideReport = { protection?.onHideCard?.invoke() },
            onOpenRecent = onRecentTab,
            contentPadding = contentPadding,
            onCopyFavorites = { fresh?.onCopyFavorites?.invoke() },
            onMoveTabs = { moveVisible = true },
        )
        if (fresh != null) {
            val movableIcons = rememberImageBitmaps(fresh.favicons)
            V5MoveTabsSheet(
                visible = moveVisible,
                groups = fresh.movableTabs.map { (profileName, tabs) ->
                    V5MovableTabs(profileName, tabs.map { tab -> tab.v5Recent(movableIcons[tab.id]) })
                },
                onMove = fresh.onMoveTab,
                onDismiss = { moveVisible = false },
            )
        }
        if (editor != null) {
            val candidates = editor.candidates
            val suggested = if (addVisible) editor.suggestions else emptyList<EssentialCandidate>() to emptyList()
            val candidateIcons = rememberImageBitmaps(editor.candidateIcons)
            V5PinnedAddSheet(
                visible = addVisible,
                candidates = candidates.map { candidate -> candidate.v5Row(candidateIcons[candidate.tabId]) },
                onAdd = { id ->
                    (candidates + suggested.first + suggested.second).firstOrNull { it.tabId == id }?.let(editor::add)
                },
                onDismiss = { addVisible = false },
                favorites = suggested.first.map { it.v5Row(null) },
                history = suggested.second.map { it.v5Row(null) },
            )
        }
    }
    if (protection != null) {
        BrowserPrivacyReportV5(
            protection = protection,
            visible = reportVisible,
            onDismiss = { reportVisible = false },
        )
    }
}

private fun EssentialCandidate.v5Row(favicon: ImageBitmap?): V5RecentTab = V5RecentTab(
    id = tabId,
    title = title.ifBlank { EssentialsRules.host(url) },
    host = EssentialsRules.host(url),
    favicon = favicon,
)

private fun BrowserTab.v5Recent(favicon: ImageBitmap?): V5RecentTab = V5RecentTab(
    id = id,
    title = title.ifBlank { EssentialsRules.host(url) },
    host = EssentialsRules.host(url),
    favicon = favicon,
)

/** Bitmaps as Compose images, converted once per bitmap rather than on every frame. */
@Composable
private fun rememberImageBitmaps(bitmaps: Map<String, Bitmap>): Map<String, ImageBitmap> {
    val cache = remember { HashMap<Bitmap, ImageBitmap>() }
    cache.keys.retainAll(bitmaps.values.toSet())
    return bitmaps.mapValues { (_, bitmap) -> cache.getOrPut(bitmap) { bitmap.asImageBitmap() } }
}

/** Below this background luminance the current theme is dark. */
private const val DARK_BACKGROUND_LUMINANCE = 0.5f
