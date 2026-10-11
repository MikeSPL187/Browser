package dev.sk2andy.materialbrowser.blocking

import android.content.res.AssetManager
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * The network filter lists (`app/src/blocking/assets`) ship once per build: System WebView keeps
 * them at the asset root, the Gecko build inside Vola's own extension, where its blocker reads them.
 * Both builds open them through here, so neither has to know which one it is.
 */
internal object BlockingListAssets {
    const val GECKO_EXTENSION_DIR = "candy_privacy/rules"

    fun candidates(name: String): List<String> = listOf(name, "$GECKO_EXTENSION_DIR/$name")

    fun open(assets: AssetManager, name: String): InputStream {
        var missing: FileNotFoundException? = null
        for (path in candidates(name)) {
            try {
                return assets.open(path)
            } catch (error: FileNotFoundException) {
                missing = missing ?: error
            }
        }
        throw missing ?: FileNotFoundException(name)
    }
}
