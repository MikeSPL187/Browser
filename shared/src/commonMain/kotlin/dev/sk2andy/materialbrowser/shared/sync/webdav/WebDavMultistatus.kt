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
 * never resolves entities or a DTD, so a hostile server gains nothing from XML tricks, and it
 * reads in one pass: each element's end is searched once from its start, and an element that
 * does not end stops the reading, so no answer costs more than linear time. An answer above
 * [MAX_CHARS] or with more than [MAX_RESOURCES] resources is refused.
 */
object WebDavMultistatus {
    const val MAX_CHARS = 4 * 1024 * 1024
    const val MAX_RESOURCES = 1_000

    /** The properties asked for, as the request body. */
    const val PROPFIND_BODY =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/><d:getetag/></d:prop></d:propfind>"

    private const val PREFIX = "(?:[A-Za-z_][A-Za-z0-9_.-]{0,32}:)?"
    private val multistatus = Regex("<${PREFIX}multistatus\\b")
    private val collection = Regex("<${PREFIX}collection\\b")

    /** The resources in [xml]; null when it is not a multistatus answer within the limits. */
    fun parse(xml: String): List<WebDavResource>? {
        if (xml.length > MAX_CHARS || "<!DOCTYPE" in xml || "<!ENTITY" in xml) return null
        if (!multistatus.containsMatchIn(xml)) return null
        val resources = ArrayList<WebDavResource>()
        var position = 0
        while (true) {
            val (body, end) = element(xml, "response", position) ?: break
            position = end
            if (resources.size == MAX_RESOURCES) return null
            val path = element(body, "href", 0)?.first?.let(::unescape)?.trim() ?: continue
            resources += WebDavResource(
                name = path.substringBefore('?').trimEnd('/').substringAfterLast('/'),
                etag = element(body, "getetag", 0)?.first?.let(::unescape)?.trim()?.takeIf(String::isNotEmpty),
                isCollection = collection.containsMatchIn(body),
            )
        }
        return resources
    }

    /** The content of the first [name] element at or after [from], and where it ends; null when none ends. */
    private fun element(text: String, name: String, from: Int): Pair<String, Int>? {
        val open = openTags.getValue(name).find(text, from) ?: return null
        val close = closeTags.getValue(name).find(text, open.range.last + 1) ?: return null
        return text.substring(open.range.last + 1, close.range.first) to close.range.last + 1
    }

    private val names = listOf("response", "href", "getetag")
    private val openTags = names.associateWith { Regex("<$PREFIX$it(?:\\s[^<>]*)?>") }
    private val closeTags = names.associateWith { Regex("</$PREFIX$it\\s*>") }

    private fun unescape(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
