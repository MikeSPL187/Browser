package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayloadCodec
import dev.sk2andy.materialbrowser.sync.SyncBase64
import dev.sk2andy.materialbrowser.sync.isJsonNull
import dev.sk2andy.materialbrowser.sync.parseStrictJsonObject
import dev.sk2andy.materialbrowser.sync.requireArray
import dev.sk2andy.materialbrowser.sync.strictBoolean
import dev.sk2andy.materialbrowser.sync.strictInt
import dev.sk2andy.materialbrowser.sync.strictString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** A connection as the adapter stores it, with the library [scope] (the profile) it syncs. */
class WebDavStoredAccount(val account: WebDavAccount, val scope: String)

/**
 * The adapter's own files, as JSON both ways: the account (kept encrypted under a keystore key)
 * and the [WebDavSyncMemory] between syncs. Reading is strict and returns null for anything
 * else, so a damaged file means "not connected" or "first sync", never a crash.
 */
object WebDavSyncStateCodec {
    private const val VERSION = 1
    private const val MAX_FIELD_CHARS = 2_048

    fun encodeAccount(stored: WebDavStoredAccount): String = with(stored.account) {
        buildJsonObject {
            put("v", VERSION)
            put("root", root)
            put("host", host)
            put("login", login)
            put("password", appPassword)
            put("key", SyncBase64.encode(key))
            put("header", WebDavSyncFormat.encodeHeader(header))
            put("device", deviceId)
            put("scope", stored.scope)
        }.toString()
    }

    fun decodeAccount(json: String): WebDavStoredAccount? = runCatching {
        val root = parseStrictJsonObject(json)
        require(root.keys == setOf("v", "root", "host", "login", "password", "key", "header", "device", "scope"))
        require(root.strictInt("v") == VERSION)
        val address = root.strictString("root", maximum = MAX_FIELD_CHARS)
        require(address.startsWith("https://") && address.endsWith("/"))
        val header = WebDavSyncFormat.decodeHeader(root.strictString("header", maximum = WebDavSyncFormat.MAX_HEADER_CHARS))
            ?: throw IllegalArgumentException("Invalid header")
        val deviceId = root.strictString("device").also { require(WebDavSyncFormat.isDeviceId(it)) }
        WebDavStoredAccount(
            account = WebDavAccount(
                root = address,
                host = root.strictString("host", maximum = MAX_FIELD_CHARS),
                login = root.strictString("login", maximum = MAX_FIELD_CHARS),
                appPassword = root.strictString("password", minimum = 0, maximum = MAX_FIELD_CHARS),
                key = SyncBase64.decode(root.strictString("key"), expectedBytes = WebDavSyncFormat.KEY_BYTES),
                header = header,
                deviceId = deviceId,
            ),
            scope = root.strictString("scope", minimum = 0, maximum = MAX_FIELD_CHARS),
        )
    }.getOrNull()

    fun encodeMemory(memory: WebDavSyncMemory): String = buildJsonObject {
        put("v", VERSION)
        put("remembered", memory.remembered?.let { entries -> buildJsonArray { entries.values.forEach { add(entry(it)) } } } ?: JsonNull)
        put("seq", memory.sequence)
        put("etag", memory.ownEtag?.let(::JsonPrimitive) ?: JsonNull)
        put(
            "devices",
            buildJsonArray {
                memory.devices.values.forEach { cached ->
                    add(
                        buildJsonObject {
                            put("etag", cached.etag?.let(::JsonPrimitive) ?: JsonNull)
                            put("record", WebDavSyncFormat.encodeRecord(cached.record))
                        },
                    )
                }
            },
        )
    }.toString()

    fun decodeMemory(json: String): WebDavSyncMemory? = runCatching {
        val root = parseStrictJsonObject(json)
        require(root.keys == setOf("v", "remembered", "seq", "etag", "devices"))
        require(root.strictInt("v") == VERSION)
        val remembered = if (root.isJsonNull("remembered")) {
            null
        } else {
            root.requireArray("remembered").also { require(it.size <= WebDavSyncFormat.MAX_BOOKMARK_ENTRIES) }
                .map { decodeEntry(it as? JsonObject ?: throw IllegalArgumentException("Invalid entry")) }
                .associateBy { it.key }
        }
        val devices = root.requireArray("devices").also { require(it.size <= WebDavSyncEngine.MAX_DEVICES) }.map { element ->
            val cached = element as? JsonObject ?: throw IllegalArgumentException("Invalid device")
            require(cached.keys == setOf("etag", "record"))
            val record = WebDavSyncFormat.decodeRecord(cached.strictString("record", maximum = WebDavSyncFormat.MAX_RECORD_BYTES))
                ?: throw IllegalArgumentException("Invalid record")
            WebDavCachedDevice(cached.optionalString("etag"), record)
        }
        WebDavSyncMemory(
            remembered = remembered,
            sequence = (root["seq"] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull?.takeIf { it >= 0 }
                ?: throw IllegalArgumentException("Invalid seq"),
            ownEtag = root.optionalString("etag"),
            devices = devices.associateBy { it.record.deviceId },
        )
    }.getOrNull()

    private fun entry(entry: WebDavBookmarkEntry) = buildJsonObject {
        put("u", entry.url)
        put("t", entry.title)
        put("f", entry.folder)
        put("m", entry.modifiedAtMillis)
        put("d", entry.deleted)
    }

    private fun decodeEntry(entry: JsonObject): WebDavBookmarkEntry {
        require(entry.keys == setOf("u", "t", "f", "m", "d"))
        return WebDavBookmarkEntry(
            url = entry.strictString("u", maximum = LocalTransferPayloadCodec.MAX_URL_CHARS),
            title = entry.strictString("t", minimum = 0, maximum = LocalTransferPayloadCodec.MAX_TITLE_CHARS),
            folder = entry.strictString("f", minimum = 0, maximum = LocalTransferPayloadCodec.MAX_FOLDER_CHARS),
            modifiedAtMillis = (entry["m"] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull?.takeIf { it >= 0 }
                ?: throw IllegalArgumentException("Invalid time"),
            deleted = entry.strictBoolean("d"),
        )
    }

    private fun JsonObject.optionalString(name: String): String? =
        if (isJsonNull(name)) null else strictString(name, maximum = MAX_FIELD_CHARS)
}
