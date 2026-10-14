package dev.sk2andy.materialbrowser.browser

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/** A photo or video the camera takes for a site's file field: a pending MediaStore row. */
internal data class FileCaptureOutput(
    val uri: Uri,
    val mimeType: String,
)

/** Creates the camera's output row and settles it once the site's file prompt is answered. */
internal class FileCaptureStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun create(action: String): FileCaptureOutput? {
        val isVideo = action == MediaStore.ACTION_VIDEO_CAPTURE
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val name = "candy-capture-${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null
        return FileCaptureOutput(uri = uri, mimeType = mimeType)
    }

    /** Publishes the capture to the gallery when [keep], or deletes it. */
    fun settle(output: FileCaptureOutput?, keep: Boolean) {
        output ?: return
        runCatching {
            if (keep) {
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    .let { values -> resolver.update(output.uri, values, null, null) }
            } else {
                resolver.delete(output.uri, null, null)
            }
        }
    }
}
