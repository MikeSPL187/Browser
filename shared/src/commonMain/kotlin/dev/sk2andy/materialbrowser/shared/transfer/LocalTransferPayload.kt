package dev.sk2andy.materialbrowser.shared.transfer

import dev.sk2andy.materialbrowser.sync.parseStrictJsonObject
import dev.sk2andy.materialbrowser.sync.requireArray
import dev.sk2andy.materialbrowser.sync.strictInt
import dev.sk2andy.materialbrowser.sync.strictString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** One open tab of the sending device. */
data class LocalTransferTab(val url: String, val title: String)

/** One bookmark of the sending device; [folder] is its folder path, empty at the top level. */
data class LocalTransferBookmark(val url: String, val title: String, val folder: String = "")

/**
 * What one local transfer carries (owner's decision, V26): the sending device's name, its open
 * tabs and its bookmarks. Passwords and history are not part of version 1.
 */
data class LocalTransferPayload(
    val deviceName: String,
    val tabs: List<LocalTransferTab>,
    val bookmarks: List<LocalTransferBookmark>,
)

/**
 * The payload as JSON, both ways. Reading is strict, as the bytes come from another phone: exact
 * fields, bounded sizes, and only `http`/`https` addresses, so a transfer cannot plant a
 * `javascript:`, `file:` or `intent:` link in the browser.
 */
object LocalTransferPayloadCodec {
    const val VERSION = 1
    const val MAX_TABS = 500
    const val MAX_BOOKMARKS = 10_000
    const val MAX_URL_CHARS = 8_192
    const val MAX_TITLE_CHARS = 1_024
    const val MAX_FOLDER_CHARS = 1_024
    const val MAX_DEVICE_NAME_CHARS = 80

    /** The largest payload as UTF-8; a real one is far smaller, and anything bigger is refused. */
    const val MAX_PAYLOAD_BYTES = 16 * 1024 * 1024

    /**
     * [payload] as JSON. Entries a receiver would refuse (an address that is not `http`/`https`,
     * oversized text) are left out here already, and the lists are cut at their limits.
     */
    fun encode(payload: LocalTransferPayload): String = buildJsonObject {
        put("v", VERSION)
        put("device", payload.deviceName.take(MAX_DEVICE_NAME_CHARS))
        put(
            "tabs",
            buildJsonArray {
                payload.tabs.filter { isTransferable(it.url) }.take(MAX_TABS).forEach { tab ->
                    add(buildJsonObject { put("u", tab.url); put("t", tab.title.take(MAX_TITLE_CHARS)) })
                }
            },
        )
        put(
            "bookmarks",
            buildJsonArray {
                payload.bookmarks.filter { isTransferable(it.url) }.take(MAX_BOOKMARKS).forEach { bookmark ->
                    add(
                        buildJsonObject {
                            put("u", bookmark.url)
                            put("t", bookmark.title.take(MAX_TITLE_CHARS))
                            put("f", bookmark.folder.take(MAX_FOLDER_CHARS))
                        },
                    )
                }
            },
        )
    }.toString()

    /** The payload in [json], or null when it is not a version-1 payload within the limits. */
    fun decode(json: String): LocalTransferPayload? = runCatching {
        require(json.length <= MAX_PAYLOAD_BYTES)
        val root = parseStrictJsonObject(json)
        require(root.keys == setOf("v", "device", "tabs", "bookmarks"))
        require(root.strictInt("v") == VERSION)
        val device = root.strictString("device", minimum = 0, maximum = MAX_DEVICE_NAME_CHARS)
        val tabs = root.requireArray("tabs").also { require(it.size <= MAX_TABS) }.map { element ->
            val tab = element.asObject(setOf("u", "t"))
            LocalTransferTab(tab.url(), tab.strictString("t", minimum = 0, maximum = MAX_TITLE_CHARS))
        }
        val bookmarks = root.requireArray("bookmarks").also { require(it.size <= MAX_BOOKMARKS) }.map { element ->
            val bookmark = element.asObject(setOf("u", "t", "f"))
            LocalTransferBookmark(
                url = bookmark.url(),
                title = bookmark.strictString("t", minimum = 0, maximum = MAX_TITLE_CHARS),
                folder = bookmark.strictString("f", minimum = 0, maximum = MAX_FOLDER_CHARS),
            )
        }
        LocalTransferPayload(device, tabs, bookmarks)
    }.getOrNull()

    /** Whether [url] may travel: an `http` or `https` address with a host, within the size limit. */
    fun isTransferable(url: String): Boolean {
        if (url.length > MAX_URL_CHARS || url.any { it.isWhitespace() || it.isISOControl() }) return false
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme != "http" && scheme != "https") return false
        val authority = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        return authority.substringAfterLast('@').isNotEmpty()
    }

    private fun JsonElement.asObject(keys: Set<String>): JsonObject =
        (this as? JsonObject)?.takeIf { it.keys == keys } ?: throw IllegalArgumentException("Invalid entry")

    private fun JsonObject.url(): String =
        strictString("u", maximum = MAX_URL_CHARS).takeIf { isTransferable(it) } ?: throw IllegalArgumentException("Invalid url")
}
