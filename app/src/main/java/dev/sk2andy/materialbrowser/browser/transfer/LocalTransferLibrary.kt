package dev.sk2andy.materialbrowser.browser.transfer

import android.content.Context
import android.os.Build
import android.provider.Settings
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferMergePlan
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayload
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayloadCodec
import java.util.UUID

/** What a confirmed transfer (V26) changed, for the done screen and its «Undo». */
internal class LocalTransferApplied(
    val tabsOpened: Int,
    val bookmarksAdded: Int,
    /** Bookmarks that did not fit the favorites limit. */
    val bookmarksSkipped: Int,
    /** The new tab group, to open the tab overview on it; null when no tab opened or no group fit. */
    val groupId: String?,
    /**
     * Closes the transfer's tabs that are still open and removes the bookmarks and folders it
     * added, keeping what the person changed since. False when the favorites could not be written.
     */
    val undo: () -> Boolean,
)

/** This phone's open web tabs and bookmarks for a transfer, named [deviceName]. */
internal fun BrowserController.localTransferPayload(deviceName: String): LocalTransferPayload = LocalTransferPayload(
    deviceName = deviceName.trim().take(LocalTransferPayloadCodec.MAX_DEVICE_NAME_CHARS),
    tabs = LocalTransferLibraryRules.tabs(activeTabs),
    bookmarks = LocalTransferLibraryRules.bookmarks(favoriteLibrary),
)

/**
 * Applies a confirmed transfer (V26): [plan]'s bookmarks into their folders in the active
 * profile's favorites, its tabs unloaded in one group named after the sending device. Call on the
 * main thread; the favorites write is a small committed preference write, as the favorites
 * screen does it.
 */
internal fun BrowserController.applyTransfer(
    plan: LocalTransferMergePlan,
    nowMillis: Long = System.currentTimeMillis(),
): LocalTransferApplied {
    val profileId = activeProfileId
    val current = store.loadFavoriteLibrary(profileId)
    val applied = LocalTransferLibraryRules.apply(current, plan.bookmarks, nowMillis) { UUID.randomUUID().toString() }
    val saved = applied.added == 0 || store.saveFavoriteLibraryCommitted(applied.library, current, profileId)
    val bookmarks = if (saved) applied else LocalTransferLibraryRules.Applied(current, added = 0, skipped = plan.bookmarks.size)
    if (bookmarks.added > 0) reloadFavorites()
    val tabIds = openTransferredTabs(plan.tabs, plan.tabGroupDeviceName)
    var undone = false
    return LocalTransferApplied(
        tabsOpened = tabIds.size,
        bookmarksAdded = bookmarks.added,
        bookmarksSkipped = bookmarks.skipped,
        groupId = tabIds.firstOrNull()?.let { first -> tabStacks.firstOrNull { first in it.tabIds }?.id },
        undo = undo@{
            if (undone) return@undo true
            tabIds.filter { id -> tabs.any { it.id == id } }.forEach { id -> closeTab(id) }
            undone = bookmarks.added == 0 || undoBookmarks(profileId, bookmarks)
            undone
        },
    )
}

private fun BrowserController.undoBookmarks(profileId: String, applied: LocalTransferLibraryRules.Applied): Boolean {
    repeat(UNDO_SAVE_ATTEMPTS) {
        val current = store.loadFavoriteLibrary(profileId)
        if (store.saveFavoriteLibraryCommitted(LocalTransferLibraryRules.undo(current, applied), current, profileId)) {
            reloadFavorites()
            return true
        }
    }
    return false
}

private const val UNDO_SAVE_ATTEMPTS = 3

/** The name the person gave the phone in the system settings, else its maker and model. */
internal fun localTransferDeviceName(context: Context): String =
    Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()
