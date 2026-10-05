package dev.sk2andy.materialbrowser.data

/**
 * Tells the browser window that another window of this process changed the saved favorites, so it
 * reloads them before its own copy overwrites them. «Move to Vola» (Q22b) imports bookmarks from
 * the Passwords window, which the browser does not wait on for a result.
 */
internal object FavoriteLibrarySignal {
    @Volatile
    private var changed = false

    fun markChanged() {
        changed = true
    }

    /** Whether the favorites changed since the last call. */
    fun consumeChanged(): Boolean = changed.also { changed = false }
}
