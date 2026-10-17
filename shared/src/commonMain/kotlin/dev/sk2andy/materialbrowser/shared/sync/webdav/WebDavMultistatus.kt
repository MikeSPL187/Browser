package dev.sk2andy.materialbrowser.shared.sync.webdav

/** One resource of a `PROPFIND` answer. */
data class WebDavResource(
    /** The last segment of its path, without a trailing `/`. */
    val name: String,
    val etag: String?,
    val isCollection: Boolean,
)

/**
 * Reads the `207 Multi-Status` answer to a `PROPFIND` for the few properties the sync asks for:
 * each response's `href`, `getetag` and whether its `resourcetype` is a collection. Servers pick
 * their own namespace prefixes (`d:`, `D:`, none), so the reader matches local names only. It
 * never resolves entities or a DTD, so a hostile server gains nothing from XML tricks; an answer
 * above [MAX_CHARS] is refused.
 */
object WebDavMultistatus {
    const val MAX_CHARS = 4 * 1024 * 1024

    /** The properties asked for, as the request body. */
    const val PROPFIND_BODY =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/><d:getetag/></d:prop></d:propfind>"

    private const val PREFIX = "(?:[A-Za-z_][A-Za-z0-9_.-]*:)?"
    private val response = Regex("<${PREFIX}response\\b[^>]*>([\\s\\S]*?)</${PREFIX}response\\s*>")
    private val href = Regex("<${PREFIX}href\\b[^>]*>([\\s\\S]*?)</${PREFIX}href\\s*>")
    private val etag = Regex("<${PREFIX}getetag\\b[^>]*>([\\s\\S]*?)</${PREFIX}getetag\\s*>")
    private val collection = Regex("<${PREFIX}collection\\b[^>]*/?>")

    /** The resources in [xml]; null when it is not a multistatus answer within the limit. */
    fun parse(xml: String): List<WebDavResource>? {
        if (xml.length > MAX_CHARS || "<!DOCTYPE" in xml || "<!ENTITY" in xml) return null
        if (!Regex("<${PREFIX}multistatus\\b").containsMatchIn(xml)) return null
        return response.findAll(xml).mapNotNull { match ->
            val body = match.groupValues[1]
            val path = href.find(body)?.groupValues?.get(1)?.let(::unescape)?.trim() ?: return@mapNotNull null
            WebDavResource(
                name = path.substringBefore('?').trimEnd('/').substringAfterLast('/'),
                etag = etag.find(body)?.groupValues?.get(1)?.let(::unescape)?.trim()?.takeIf(String::isNotEmpty),
                isCollection = collection.containsMatchIn(body),
            )
        }.toList()
    }

    private fun unescape(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
