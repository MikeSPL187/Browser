package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BLANK_URL
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.BrowserTab
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.HistoryEntry

/**
 * What the new tab of a fresh profile (canvas screen «EmptyStart») offers: the bookmarks of another
 * profile to copy, and the open tabs of the other profiles to move here. [isFresh] is true while the
 * profile has nothing of its own yet; the tabs stay listed after that, so the sheet moving them does
 * not close under the finger once the first tab arrives.
 */
internal class NewTabFreshStart(
    val isFresh: Boolean,
    val profileName: String,
    /** The profile whose bookmarks can be copied, by name; null when no other profile has any. */
    val copyFromName: String?,
    val onCopyFavorites: () -> Unit,
    /** Open tabs of the other profiles that can move here, by the name of their profile. */
    val movableTabs: List<Pair<String, List<BrowserTab>>>,
    val favicons: Map<String, Bitmap>,
    val onMoveTab: (String) -> Unit,
)

internal object NewTabFreshRules {
    /**
     * A profile is fresh while it has no favorites, no history and no other open page; the pinned
     * sites are checked by the new tab itself. The only profile is never fresh: the screen speaks of
     * cookies of its own, which only mean something next to another profile.
     */
    fun isFresh(
        profileId: String,
        profileCount: Int,
        favorites: List<FavoriteEntry>,
        history: List<HistoryEntry>,
        tabs: List<BrowserTab>,
        currentTabId: String,
    ): Boolean = profileCount > 1 &&
        favorites.isEmpty() &&
        history.none { it.profileId == profileId } &&
        tabs.none { tab ->
            tab.profileId == profileId && tab.id != currentTabId && !tab.isIncognito && tab.url != BLANK_URL
        }

    /** Tabs that could move into [profileId]: open pages, not private, not in a locked profile. */
    fun movableTabs(profileId: String, tabs: List<BrowserTab>, lockedProfileIds: Set<String>): List<BrowserTab> =
        tabs.filter { tab ->
            tab.profileId != profileId &&
                tab.profileId !in lockedProfileIds &&
                !tab.isIncognito &&
                tab.url != BLANK_URL
        }
}

/** Binds [NewTabFreshStart] to the controller for [profileId]'s new tab; null for another profile's tab. */
@Composable
internal fun rememberNewTabFreshStart(
    controller: BrowserController,
    profileId: String,
    currentTabId: String,
): NewTabFreshStart? {
    // Favorites, history and copying all work on the active profile, which owns the selected tab.
    if (!controller.profilesEnabled || profileId != controller.activeProfileId) return null
    val context = LocalContext.current
    val profiles = controller.profiles
    val profile = profiles.firstOrNull { it.id == profileId } ?: return null
    val isFresh = NewTabFreshRules.isFresh(
        profileId = profileId,
        profileCount = profiles.size,
        favorites = controller.favorites,
        history = controller.history,
        tabs = controller.tabs,
        currentTabId = currentTabId,
    )
    val locked = controller.lockedProfileIds
    val otherIds = profiles.map { it.id }.filter { it != profileId && it !in locked }
    // Read once per set of profiles: the other profiles' bookmarks do not change while this one is open.
    val source = remember(profileId, otherIds) {
        otherIds.firstNotNullOfOrNull { id ->
            controller.store.loadFavoriteLibrary(id).favorites.takeIf { it.isNotEmpty() }?.let { id to it }
        }
    }
    val sourceProfile = source?.let { (id, _) -> profiles.firstOrNull { it.id == id } }
    val movable = NewTabFreshRules.movableTabs(profileId, controller.tabs, locked)
        .groupBy(BrowserTab::profileId)
        .mapNotNull { (id, tabs) -> profiles.firstOrNull { it.id == id }?.let { it to tabs } }
    return NewTabFreshStart(
        isFresh = isFresh,
        profileName = profile.workspaceDisplayName(),
        copyFromName = sourceProfile?.workspaceDisplayName(),
        onCopyFavorites = {
            // Folders stay behind: the other profile's folders do not exist here.
            val favorites = source?.second.orEmpty().map { it.copy(parentFolderId = null) }
            controller.importFavoriteBookmarks(favorites) { result ->
                // A failure keeps the button on the new tab, so the message points back to it.
                val message = when {
                    result == null -> context.getString(R.string.v5_fresh_copy_failed)
                    result.importedCount == 0 -> context.getString(R.string.favorite_bookmark_import_no_new)
                    else -> context.resources.getQuantityString(
                        R.plurals.favorite_bookmark_import_success,
                        result.importedCount,
                        result.importedCount,
                    )
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        },
        movableTabs = movable.map { (owner, tabs) -> owner.workspaceDisplayName() to tabs },
        favicons = controller.favicons,
        onMoveTab = { tabId -> controller.moveTabToProfile(tabId, profileId) },
    )
}
