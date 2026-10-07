package dev.sk2andy.materialbrowser.data

import java.net.URI

/** Where a MediaStore download row says its file lives. */
internal data class MediaStoreFileLocation(
    val mediaId: Long,
    val relativePath: String?,
    val displayName: String,
)

/**
 * Decides whether a MediaStore download row is only the media index of a file the system download
 * manager already lists, so the screen shows that file once.
 *
 * The match is by storage identity, never by name, size or time: the download manager may save
 * `report-1.pdf` while its title stays `report.pdf`, and two different files can share a name and a
 * size. When the identity of either row cannot be read, the rows are kept apart, so no file is
 * hidden from the list or left out of a clear.
 */
internal object DownloadIndexRules {
    private const val FILE_SCHEME = "file"
    private const val CONTENT_SCHEME = "content"
    private const val MEDIA_AUTHORITY = "media"

    /**
     * Whether [systemLocalUri] (the download manager's local URI) names the same file as [media].
     */
    fun isSameFile(systemLocalUri: String?, media: MediaStoreFileLocation): Boolean {
        if (systemLocalUri.isNullOrBlank() || media.displayName.isBlank()) return false
        val uri = runCatching { URI(systemLocalUri) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            FILE_SCHEME -> isSamePath(uri.path, media)
            CONTENT_SCHEME -> uri.authority == MEDIA_AUTHORITY &&
                uri.path?.substringAfterLast('/')?.toLongOrNull() == media.mediaId
            else -> false
        }
    }

    private fun isSamePath(path: String?, media: MediaStoreFileLocation): Boolean {
        if (path.isNullOrBlank()) return false
        val directory = media.relativePath
            ?.trim('/')
            ?.takeIf(String::isNotBlank)
            ?: return false
        if (path.substringAfterLast('/') != media.displayName) return false
        // The volume root differs between devices and cards; the path below it is the identity.
        return path.endsWith("/$directory/${media.displayName}")
    }
}
