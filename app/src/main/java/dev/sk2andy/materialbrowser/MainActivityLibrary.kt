package dev.sk2andy.materialbrowser

import android.content.Intent
import dev.sk2andy.materialbrowser.browser.integration.FavoritesActivityContract
import dev.sk2andy.materialbrowser.browser.integration.HistoryActivityContract
import dev.sk2andy.materialbrowser.ui.v5.V5LibrarySegment

/**
 * Favorites, history or downloads, each with its own launcher so its result reaches the code that
 * reads it; [instant] when one segment of the v5 library hands over to another (V28, B2), so the
 * switch shows no screen transition.
 */
internal fun MainActivity.openLibrary(segment: V5LibrarySegment, instant: Boolean = false) {
    when (segment) {
        V5LibrarySegment.Favorites -> favoritesLauncher.launch(FavoritesActivityContract.launchIntent(this))
        V5LibrarySegment.History -> historyLauncher.launch(HistoryActivityContract.launchIntent(this))
        V5LibrarySegment.Downloads -> startActivity(Intent(this, DownloadsActivity::class.java))
    }
    @Suppress("DEPRECATION")
    if (instant) overridePendingTransition(0, 0)
}
