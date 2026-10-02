package dev.sk2andy.materialbrowser.browser

import android.content.Context
import android.graphics.Bitmap
import dev.sk2andy.materialbrowser.data.FavoriteFaviconRepository

/** [EssentialIconSource] on the device icon stores; Essentials keep a directory of their own. */
internal class EssentialIconRepository(context: Context) : EssentialIconSource {
    private val essentials = FavoriteFaviconRepository.essentials(context)
    private val favorites = FavoriteFaviconRepository.get(context)

    override fun capture(url: String, bitmap: Bitmap?) = essentials.capture(url, bitmap)

    override fun copyFromFavorites(urls: List<String>, onDone: () -> Unit) {
        if (urls.isEmpty()) return
        favorites.loadAll(urls) { loaded ->
            // capture() copies the bitmap before it returns, so the loaded ones can go right away.
            loaded.forEach { (url, bitmap) -> essentials.capture(url, bitmap) }
            loaded.values.toSet().forEach(Bitmap::recycle)
            onDone()
        }
    }

    override fun load(urls: List<String>, onLoaded: (Map<String, Bitmap>) -> Unit) =
        essentials.loadAll(urls, onLoaded)

    override fun prune(validUrls: Set<String>) = essentials.prune(validUrls)
}
