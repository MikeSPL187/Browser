package dev.sk2andy.materialbrowser.shared.transfer

/**
 * What a received payload adds on this phone (owner's decision, V26): bookmarks this phone lacks,
 * without duplicates, and the sending device's tabs as one new group named after it.
 */
data class LocalTransferMergePlan(
    val bookmarks: List<LocalTransferBookmark>,
    val tabGroupDeviceName: String,
    val tabs: List<LocalTransferTab>,
) {
    val isEmpty: Boolean get() = bookmarks.isEmpty() && tabs.isEmpty()
}

object LocalTransferMergeRules {
    /**
     * The plan for [payload] on a phone whose bookmarks have [existingBookmarkUrls]. Bookmarks
     * are compared by [bookmarkKey], so `HTTPS://Example.com` and `https://example.com/` are one;
     * the first of several equal incoming ones is kept, in the sender's order. Tabs are all kept:
     * the same page open twice is two tabs on the sending phone too.
     */
    fun plan(payload: LocalTransferPayload, existingBookmarkUrls: Collection<String>): LocalTransferMergePlan {
        val seen = existingBookmarkUrls.mapTo(HashSet(), ::bookmarkKey)
        val bookmarks = payload.bookmarks.filter { bookmark -> seen.add(bookmarkKey(bookmark.url)) }
        return LocalTransferMergePlan(bookmarks, payload.deviceName.trim(), payload.tabs)
    }

    /**
     * The address two bookmarks share when they are the same page: scheme and host in lower case,
     * the default port and an empty path's `/` dropped. Path, query and fragment stay as written:
     * they can name different pages.
     */
    fun bookmarkKey(url: String): String {
        val trimmed = url.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return trimmed
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        val rest = trimmed.substring(schemeEnd + 3)
        val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }.let { if (it < 0) rest.length else it }
        var authority = rest.substring(0, authorityEnd)
        val userInfo = authority.substringBeforeLast('@', missingDelimiterValue = "")
        var host = authority.substringAfterLast('@').lowercase()
        when {
            scheme == "https" && host.endsWith(":443") -> host = host.removeSuffix(":443")
            scheme == "http" && host.endsWith(":80") -> host = host.removeSuffix(":80")
        }
        authority = if (userInfo.isEmpty()) host else "$userInfo@$host"
        var tail = rest.substring(authorityEnd)
        if (tail.startsWith("/?") || tail.startsWith("/#") || tail == "/") tail = tail.substring(1)
        return "$scheme://$authority$tail"
    }
}
