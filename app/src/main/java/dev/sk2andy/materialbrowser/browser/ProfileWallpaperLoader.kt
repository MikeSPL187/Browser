package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executor

/**
 * The active profile's two wallpapers, the new tab's and the tab switcher's (#500): read off the
 * main thread, shown only while that profile is still the active one, and forgotten when their file
 * is gone. [T] is the decoded image ([android.graphics.Bitmap] in the app).
 */
internal class ProfileWallpaperLoader<T : Any>(
    private val background: Executor,
    private val postToMainThread: (Runnable) -> Unit,
    private val load: (profileId: String, target: ProfileWallpaperTarget) -> T?,
    private val recycle: (T) -> Unit,
    private val host: Host,
) {
    /** The controller state a load depends on. */
    interface Host {
        /** The active profile when it is a local one; a synced profile has no wallpapers here. */
        val activeLocalProfile: BrowserProfile?
        val activeProfileId: String
        val isDestroyed: Boolean

        /** [profileId]'s [wallpaper] for [target] has no file any more: drop it from the profile. */
        fun forgetMissing(profileId: String, target: ProfileWallpaperTarget, wallpaper: ProfileWallpaper)
    }

    var newTab by mutableStateOf<T?>(null)
        private set
    var tabSwitcher by mutableStateOf<T?>(null)
        private set
    private var newTabGeneration = 0
    private var tabSwitcherGeneration = 0

    fun refreshNewTab() {
        val profile = host.activeLocalProfile
        val wallpaper = profile?.newTabWallpaper
        val profileId = profile?.id
        val generation = ++newTabGeneration
        newTab = null
        if (profileId == null || wallpaper == null) return
        loadInBackground(profileId, ProfileWallpaperTarget.NewTab, wallpaper, isCurrent = { generation == newTabGeneration }) {
            newTab = it
        }
    }

    fun loadTabSwitcher(onReady: () -> Unit = {}) {
        val profile = host.activeLocalProfile
        val wallpaper = profile?.tabSwitcherWallpaper
        val profileId = profile?.id
        val generation = ++tabSwitcherGeneration
        tabSwitcher = null
        if (profileId == null || wallpaper == null) {
            onReady()
            return
        }
        loadInBackground(
            profileId,
            ProfileWallpaperTarget.TabSwitcher,
            wallpaper,
            isCurrent = { generation == tabSwitcherGeneration },
            onMissing = onReady,
        ) {
            tabSwitcher = it
            onReady()
        }
    }

    private fun loadInBackground(
        profileId: String,
        target: ProfileWallpaperTarget,
        wallpaper: ProfileWallpaper,
        isCurrent: () -> Boolean,
        onMissing: () -> Unit = {},
        show: (T) -> Unit,
    ) {
        background.executeIfRunning {
            val loaded = load(profileId, target)
            postToMainThread(
                Runnable {
                    if (host.isDestroyed || !isCurrent() || host.activeProfileId != profileId) {
                        loaded?.let(recycle)
                        return@Runnable
                    }
                    if (loaded != null) {
                        show(loaded)
                        return@Runnable
                    }
                    host.forgetMissing(profileId, target, wallpaper)
                    onMissing()
                },
            )
        }
    }

    /** The wallpaper editor takes [profileId]'s images over; nothing loaded earlier may land. */
    fun releaseForEditing(profileId: String) {
        if (profileId != host.activeProfileId) return
        newTabGeneration += 1
        tabSwitcherGeneration += 1
        newTab = null
        tabSwitcher = null
    }

    fun restoreAfterEditing() {
        refreshNewTab()
        loadTabSwitcher()
    }

    fun releaseTabSwitcher() {
        tabSwitcherGeneration += 1
        tabSwitcher = null
    }

    /** The controller is destroyed: a load still running is dropped when it ends. */
    fun close() {
        newTabGeneration++
        tabSwitcherGeneration++
    }
}
