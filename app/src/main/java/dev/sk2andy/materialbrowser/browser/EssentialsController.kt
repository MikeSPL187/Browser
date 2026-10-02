package dev.sk2andy.materialbrowser.browser

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.EssentialsPersistence
import dev.sk2andy.materialbrowser.data.EssentialsRules

/** Site icons of Essentials, kept on the device next to the favorites icons. */
interface EssentialIconSource {
    /** Saves [bitmap] for [url]; without one the icon is fetched once, after the user added it. */
    fun capture(url: String, bitmap: Bitmap?)

    /** Copies icons the favorites already have, without the network. */
    fun copyFromFavorites(urls: List<String>, onDone: () -> Unit)

    fun load(urls: List<String>, onLoaded: (Map<String, Bitmap>) -> Unit)

    fun prune(validUrls: Set<String>)
}

/** A removed Essential the snackbar can still put back. */
data class EssentialRemoval(
    val profileId: String,
    val entry: EssentialEntry,
    val index: Int,
    val revision: Long,
)

/**
 * The Essentials grid of every workspace (ROADMAP Q5): add, remove with undo, reorder, and the
 * first-run copy of the favorites grid it replaces. Kept out of [BrowserController], which only
 * answers the [Host] questions.
 */
class EssentialsController internal constructor(
    private val store: EssentialsPersistence,
    private val icons: EssentialIconSource,
    private val host: Host,
) {
    internal interface Host {
        /** Local workspaces in their order; the first is «Personal» unless it was deleted. */
        val profileIds: List<String>

        /** The top level of the favorites grid the first run copies. */
        fun migrationSeed(): List<EssentialEntry>

        /** Runs [action] on the main thread. */
        fun post(action: () -> Unit)
    }

    private val entriesByProfile = mutableStateMapOf<String, List<EssentialEntry>>()

    /** Icons by page url; a tile without one shows its letter. */
    val iconsByUrl = mutableStateMapOf<String, Bitmap>()

    var removal by mutableStateOf<EssentialRemoval?>(null)
        private set
    private var revision = 0L
    private var destroyed = false

    /** Loads the saved lists; on the very first run copies the favorites grid to every workspace. */
    fun restore() {
        val saved = store.load()
        if (saved == null) {
            val seed = EssentialsRules.normalize(host.migrationSeed())
            host.profileIds.forEach { profileId -> entriesByProfile[profileId] = seed }
            persist()
            icons.copyFromFavorites(seed.map(EssentialEntry::url)) { host.post(::refreshIcons) }
        } else {
            entriesByProfile.putAll(saved)
            refreshIcons()
        }
    }

    /**
     * The workspace's Essentials. A workspace that has none yet reads a copy of «Personal»;
     * [materialize] keeps that copy, so later changes to «Personal» stay there.
     */
    fun entriesFor(profileId: String): List<EssentialEntry> =
        entriesByProfile[profileId] ?: templateEntries()

    /** Saves the copy a new workspace shows the first time its new tab opens. */
    fun materialize(profileId: String) {
        if (profileId in entriesByProfile) return
        entriesByProfile[profileId] = templateEntries()
        persist()
    }

    fun add(profileId: String, url: String, title: String, icon: Bitmap?): EssentialsRules.AddResult {
        val result = EssentialsRules.add(entriesFor(profileId), url, title)
        if (result is EssentialsRules.AddResult.Added) {
            update(profileId, result.entries)
            icons.capture(result.entries.last().url, icon)
            host.post(::refreshIcons)
        }
        return result
    }

    fun remove(profileId: String, id: String): Boolean {
        val current = entriesFor(profileId)
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return false
        update(profileId, current.filterIndexed { position, _ -> position != index })
        removal = EssentialRemoval(profileId, current[index], index, ++revision)
        return true
    }

    fun undoRemoval(token: EssentialRemoval): Boolean {
        if (removal != token) return false
        removal = null
        val restored = EssentialsRules.restore(entriesFor(token.profileId), token.entry, token.index)
            ?: return false
        update(token.profileId, restored)
        return true
    }

    /** The snackbar went away: the removal is final and its icon can go. */
    fun dismissRemoval(token: EssentialRemoval) {
        if (removal != token) return
        removal = null
        pruneIcons()
    }

    fun move(profileId: String, id: String, toIndex: Int): Boolean {
        val current = entriesFor(profileId)
        val moved = EssentialsRules.move(current, id, toIndex)
        if (moved == current) return false
        update(profileId, moved)
        return true
    }

    /** A deleted workspace takes its Essentials with it. */
    fun forgetProfile(profileId: String) {
        if (entriesByProfile.remove(profileId) == null) return
        if (removal?.profileId == profileId) removal = null
        persist()
        pruneIcons()
    }

    fun destroy() {
        destroyed = true
    }

    private fun templateEntries(): List<EssentialEntry> =
        EssentialsRules.templateProfileId(entriesByProfile, host.profileIds, DEFAULT_PROFILE_ID)
            ?.let(entriesByProfile::get)
            .orEmpty()

    private fun update(profileId: String, entries: List<EssentialEntry>) {
        entriesByProfile[profileId] = entries
        persist()
    }

    private fun persist() {
        store.save(entriesByProfile.toMap())
    }

    private fun allUrls(): Set<String> =
        entriesByProfile.values.flatten().mapTo(linkedSetOf(), EssentialEntry::url) +
            listOfNotNull(removal?.entry?.url)

    private fun pruneIcons() {
        val urls = allUrls()
        iconsByUrl.keys.filterNot(urls::contains).forEach(iconsByUrl::remove)
        icons.prune(urls)
    }

    private fun refreshIcons() {
        if (destroyed) return
        val missing = allUrls().filterNot(iconsByUrl::containsKey)
        if (missing.isEmpty()) return
        icons.load(missing) { loaded ->
            host.post {
                if (destroyed) return@post
                val current = allUrls()
                loaded.filterKeys(current::contains).forEach { (url, bitmap) ->
                    iconsByUrl.putIfAbsent(url, bitmap)
                }
            }
        }
    }
}
