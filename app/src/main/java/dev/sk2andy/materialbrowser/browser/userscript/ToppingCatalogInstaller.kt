package dev.sk2andy.materialbrowser.browser.userscript

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.data.ToppingCatalogRefreshResult
import dev.sk2andy.materialbrowser.data.ToppingDownloadResult

/**
 * The toppings catalog in settings (#500): loading it, and turning a topping on, off or to its
 * latest version. A topping becomes an ordinary user script once downloaded and checked; the
 * user scripts themselves stay in the controller, which lends them through [Host].
 */
internal class ToppingCatalogInstaller(
    /** The repository's side; both answer on the main thread. */
    private val fetchCatalog: (onComplete: (ToppingCatalogRefreshResult) -> Unit) -> Unit,
    private val downloadTopping: (ToppingCatalogEntry, onComplete: (ToppingDownloadResult) -> Unit) -> Unit,
    private val host: Host,
) {
    interface Host {
        val isDestroyed: Boolean
        val userScripts: List<UserScript>

        fun setUserScriptEnabled(id: String, enabled: Boolean, onComplete: (Boolean) -> Unit)

        /** Fetches [script]'s @require and @resource files; answers on the main thread. */
        fun resolveDependencies(script: UserScript, onComplete: (UserScriptDependencyResolution) -> Unit)

        /** Saves [scripts] as the whole user script list and hands them to the engine. */
        fun commitUserScripts(scripts: List<UserScript>, onComplete: (Boolean) -> Unit)
    }

    var result by mutableStateOf<ToppingCatalogRefreshResult?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    /** Toppings with a download or a switch in progress; their row waits. */
    val busyIds = mutableStateListOf<String>()
    private var refreshGeneration = 0

    fun refresh() {
        val generation = ++refreshGeneration
        isLoading = true
        fetchCatalog { result ->
            if (host.isDestroyed || generation != refreshGeneration) return@fetchCatalog
            isLoading = false
            this.result = result
        }
    }

    fun setEnabled(toppingId: String, enabled: Boolean, onComplete: (Boolean) -> Unit = {}) {
        val entry = entry(toppingId)
        if (entry == null || toppingId in busyIds) {
            onComplete(false)
            return
        }
        val scriptId = ToppingCatalogRules.stableScriptId(toppingId)
        val installed = host.userScripts.firstOrNull { script -> script.id == scriptId }
        if (installed != null) {
            busyIds += toppingId
            host.setUserScriptEnabled(scriptId, enabled) { saved ->
                busyIds.remove(toppingId)
                onComplete(saved)
            }
            return
        }
        if (!enabled) {
            onComplete(true)
            return
        }
        downloadAndSave(entry = entry, preserveEnabled = false, onComplete = onComplete)
    }

    fun update(toppingId: String, onComplete: (Boolean) -> Unit = {}) {
        val entry = entry(toppingId)
        val installed = host.userScripts.any { script ->
            script.id == ToppingCatalogRules.stableScriptId(toppingId)
        }
        if (entry == null || !installed || toppingId in busyIds) {
            onComplete(false)
            return
        }
        downloadAndSave(entry = entry, preserveEnabled = true, onComplete = onComplete)
    }

    private fun entry(id: String): ToppingCatalogEntry? {
        val catalog = when (val result = result) {
            is ToppingCatalogRefreshResult.Fresh -> result.catalog
            is ToppingCatalogRefreshResult.Cached -> result.catalog
            is ToppingCatalogRefreshResult.Error,
            null,
            -> return null
        }
        return catalog.toppings.firstOrNull { entry -> entry.id == id }
    }

    private fun downloadAndSave(
        entry: ToppingCatalogEntry,
        preserveEnabled: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        busyIds += entry.id
        downloadTopping(entry) { result ->
            if (host.isDestroyed) return@downloadTopping
            val downloaded = (result as? ToppingDownloadResult.Accepted)?.script
            if (downloaded == null) {
                busyIds.remove(entry.id)
                onComplete(false)
                return@downloadTopping
            }
            val existing = host.userScripts.firstOrNull { script -> script.id == downloaded.id }
            val script = downloaded.copy(
                enabled = if (preserveEnabled) existing?.enabled ?: true else true,
                allowedFrameScope = existing?.allowedFrameScope
                    ?.restrictedTo(downloaded.declaredFrameScope)
                    ?: downloaded.declaredFrameScope,
            )
            host.resolveDependencies(script) { resolution ->
                if (host.isDestroyed) return@resolveDependencies
                val resolved = (resolution as? UserScriptDependencyResolution.Resolved)?.script
                if (resolved == null) {
                    busyIds.remove(entry.id)
                    onComplete(false)
                    return@resolveDependencies
                }
                val proposed = host.userScripts.toMutableList()
                val index = proposed.indexOfFirst { candidate -> candidate.id == resolved.id }
                if (index >= 0) proposed[index] = resolved else proposed += resolved
                if (!UserScriptRules.isWithinCollectionBounds(proposed)) {
                    busyIds.remove(entry.id)
                    onComplete(false)
                    return@resolveDependencies
                }
                host.commitUserScripts(proposed) { persisted ->
                    busyIds.remove(entry.id)
                    onComplete(persisted)
                }
            }
        }
    }
}
