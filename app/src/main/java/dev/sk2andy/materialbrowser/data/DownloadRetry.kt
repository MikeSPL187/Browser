package dev.sk2andy.materialbrowser.data

/**
 * Whether a failed or cancelled download can start again (V22). A retry fetches the file from the
 * start; only rows the browser still knows how to fetch offer one, and nothing about it is kept on
 * disk. Meaningful only for [DownloadStatus.Failed] and [DownloadStatus.Cancelled] rows.
 */
internal sealed interface DownloadRetry {
    data object Available : DownloadRetry

    /** Why not; the screen may offer to open [pageUrl], the page the download came from, instead. */
    data class Unavailable(
        val reason: DownloadRetryUnavailable,
        val pageUrl: String? = null,
    ) : DownloadRetry
}

internal enum class DownloadRetryUnavailable {
    /** A `blob:` address lives only as long as its page. */
    Blob,

    /** Its private tab is closed. */
    PrivateClosed,

    /** The browser no longer knows how to fetch it: a download from before a restart, or an extension's. */
    NotRemembered,

    /** A retry got another file (name or type) than the first time, so nothing was saved. */
    ResponseChanged,
}
