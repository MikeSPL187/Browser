package dev.sk2andy.materialbrowser

/**
 * Public coordinates of the Vola project, shared by the updater, the About screen and
 * documentation links so the repository is named in exactly one place.
 */
object VolaProject {
    const val GITHUB_OWNER = "MikeSPL187"
    const val GITHUB_REPOSITORY = "Browser"
    const val REPOSITORY_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPOSITORY"
    const val RAW_CONTENT_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPOSITORY"
    const val DOCS_URL = "$REPOSITORY_URL/blob/main/docs"

    /** Vola is a fork of Candy Browser (MPL-2.0); credited on the About screen. */
    const val UPSTREAM_NAME = "Candy Browser"
    const val UPSTREAM_AUTHOR = "André Naumann"
    const val UPSTREAM_URL = "https://github.com/sk2andy/candy-browser"
}
