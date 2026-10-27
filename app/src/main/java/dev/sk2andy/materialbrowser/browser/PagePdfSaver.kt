package dev.sk2andy.materialbrowser.browser

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Writes a page the engine rendered to a PDF (`BrowserEngineViewPort.savePageAsPdf`) into
 * the phone's shared Downloads folder, where the Files app and other apps find it (canvas
 * «MenuV2», «Download»). The copy runs off the main thread; [save] answers on it with the file's
 * address, or null when it could not be written, and closes the stream either way.
 */
internal class PagePdfSaver(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val main = Handler(Looper.getMainLooper())

    fun save(pdf: InputStream, pageTitle: String, onDone: (Uri?) -> Unit) {
        io.execute {
            val uri = runCatching { write(pdf, PagePdfRules.fileName(pageTitle)) }
                .onFailure { error -> Log.w(TAG, "Saving the page as PDF failed", error) }
                .getOrNull()
            runCatching { pdf.close() }
            main.post { onDone(uri) }
        }
    }

    private fun write(pdf: InputStream, name: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, PagePdfRules.MIME_TYPE)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            // Hidden from other apps until the whole file is there.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "No Downloads entry" }
        try {
            checkNotNull(resolver.openOutputStream(uri)) { "No output stream" }.use { out -> pdf.copyTo(out) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    private companion object {
        const val TAG = "VolaPagePdf"

        /** One writer for the whole app: two saves never interleave their files. */
        val io = Executors.newSingleThreadExecutor()
    }
}

/** Pure rules of saving a page as PDF, for unit tests. */
internal object PagePdfRules {
    const val MIME_TYPE = "application/pdf"
    private const val MAX_NAME_CHARS = 80
    private const val FALLBACK_NAME = "page"

    /**
     * The file's name from the page's title: characters a file name cannot hold become spaces,
     * runs of spaces one, at most [MAX_NAME_CHARS] characters, and «.pdf» at the end. An empty
     * title gives «page.pdf»; the folder adds « (1)» itself when the name is taken.
     */
    fun fileName(title: String): String {
        val cleaned = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(MAX_NAME_CHARS)
            .trim()
        return "${cleaned.ifEmpty { FALLBACK_NAME }}.pdf"
    }
}
