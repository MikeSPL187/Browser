package dev.sk2andy.materialbrowser.browser

/** Which restored tabs join the session at start and which wait for their workspace. */
object WorkspaceRestoreRules {
    data class RestoredTabs(
        val live: List<BrowserTab>,
        /** Tabs of a workspace whose stored entry is unreadable: kept on disk, never shown. */
        val held: List<BrowserTab>,
    )

    /**
     * A tab whose workspace is gone normally moves to [fallbackProfileId]. When some stored
     * workspace could not be read, such a tab may belong to a protected workspace, so it is held
     * back instead of moving its title and URL into an unprotected one.
     */
    fun assignOwners(
        tabs: List<BrowserTab>,
        profileIds: Set<String>,
        fallbackProfileId: String,
        storedProfilesUnreadable: Boolean,
    ): RestoredTabs {
        val (owned, orphaned) = tabs.partition { tab -> tab.profileId in profileIds }
        if (storedProfilesUnreadable) return RestoredTabs(live = owned, held = orphaned)
        return RestoredTabs(
            live = tabs.map { tab ->
                if (tab.profileId in profileIds) tab else tab.copy(profileId = fallbackProfileId)
            },
            held = emptyList(),
        )
    }
}
