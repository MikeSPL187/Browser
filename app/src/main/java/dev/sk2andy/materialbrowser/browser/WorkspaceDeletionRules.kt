package dev.sk2andy.materialbrowser.browser

/** Decisions behind deleting a local workspace, kept free of controller state for JVM tests. */
object WorkspaceDeletionRules {
    /**
     * The local workspace that inherits the tabs of [deletedProfileId]: the active one when it is
     * local and stays, otherwise the first remaining local one. Null means nothing can inherit them,
     * so the deletion must be refused before any data is removed. A synced (remote) active workspace
     * is never a fallback; it stays active and only the moved tabs go to the local fallback.
     */
    fun fallbackProfileId(
        localProfileIds: List<String>,
        activeProfileId: String,
        deletedProfileId: String,
    ): String? {
        val remaining = localProfileIds.filterNot { it == deletedProfileId }
        return activeProfileId.takeIf(remaining::contains) ?: remaining.firstOrNull()
    }
}
