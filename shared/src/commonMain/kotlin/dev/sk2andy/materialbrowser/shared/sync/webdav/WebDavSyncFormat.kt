package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferCrypto
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferMergeRules
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferPayloadCodec
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferProtocol
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import dev.sk2andy.materialbrowser.sync.SyncBase64
import dev.sk2andy.materialbrowser.sync.parseStrictJsonObject
import dev.sk2andy.materialbrowser.sync.requireArray
import dev.sk2andy.materialbrowser.sync.requireObject
import dev.sk2andy.materialbrowser.sync.strictBoolean
import dev.sk2andy.materialbrowser.sync.strictInt
import dev.sk2andy.materialbrowser.sync.strictString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * The primitives the WebDAV sync needs from the platform: the transfer's symmetric ones and
 * Argon2id for the phrase (on Android, javax.crypto and the Argon2 library `SyncCrypto` uses).
 */
interface WebDavSyncCrypto : LocalTransferCrypto {
    /** Argon2id with [WebDavSyncFormat]'s fixed cost: 64 MiB, 3 passes, 4 lanes, a 32-byte key. */
    fun argon2id(phrase: ByteArray, salt: ByteArray): ByteArray
}

/** The readable header: which space the files belong to and how to turn the phrase into its key. */
data class WebDavSyncHeader(
    /** A random id of this set of files; starting over makes a new one. */
    val spaceId: ByteArray,
    val salt: ByteArray,
    /** A fixed text sealed with the key, so a wrong phrase is told apart without touching any file. */
    val checkNonce: ByteArray,
    val checkCiphertext: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is WebDavSyncHeader &&
        spaceId.contentEquals(other.spaceId) && salt.contentEquals(other.salt) &&
        checkNonce.contentEquals(other.checkNonce) && checkCiphertext.contentEquals(other.checkCiphertext)

    override fun hashCode(): Int = spaceId.contentHashCode()

    override fun toString(): String = "WebDavSyncHeader(space=${SyncBase64.encode(spaceId)})"
}

/**
 * One bookmark's state in the sync, by [LocalTransferMergeRules.bookmarkKey] of [url]: live, or a
 * tombstone ([deleted]) that keeps a deletion from coming back from another device. The newer
 * [modifiedAtMillis] wins ([WebDavBookmarkRules.newer]).
 */
data class WebDavBookmarkEntry(
    val url: String,
    val title: String,
    val folder: String,
    val modifiedAtMillis: Long,
    val deleted: Boolean = false,
) {
    val key: String get() = LocalTransferMergeRules.bookmarkKey(url)
}

/** What one device publishes: its name, its open tabs and its view of every bookmark. */
data class WebDavDeviceRecord(
    val deviceId: String,
    val name: String,
    /** Grows with each publish, so a server cannot hand back an older file unnoticed. */
    val sequence: Long,
    val updatedAtMillis: Long,
    val tabs: List<LocalTransferTab>,
    val bookmarks: Map<String, WebDavBookmarkEntry>,
)

/**
 * The files of the WebDAV sync, version 1 (V27). On the server, in `Vola/`:
 * - `vola.json` — [WebDavSyncHeader] as plain JSON: the space id, the Argon2id salt and a check
 *   value. It holds no secret, and the other devices need it before they have a key.
 * - `devices/<id>.json` — one [WebDavDeviceRecord] per device, AES-256-GCM under a key derived
 *   from the phrase, with the space and the device id as associated data: the server can neither
 *   read a file nor pass one device's file, or another space's, off as another.
 *
 * Each device writes only its own file, so two devices never race for one file; the merge of the
 * bookmarks is [WebDavBookmarkRules.merge].
 */
object WebDavSyncFormat {
    const val VERSION = 1
    const val HEADER_FILE = "vola.json"
    const val DEVICES_FOLDER = "devices/"
    const val DEVICE_FILE_SUFFIX = ".json"
    const val SPACE_BYTES = 16
    const val SALT_BYTES = 16
    const val DEVICE_ID_BYTES = 16
    const val KEY_BYTES = 32

    /** Live bookmarks plus tombstones one device may publish. */
    const val MAX_BOOKMARK_ENTRIES = 2 * LocalTransferPayloadCodec.MAX_BOOKMARKS

    /**
     * The largest device record as UTF-8: 500 tabs and the favorites with room to spare. A
     * bigger file is skipped, so a hostile server cannot fill the phone's memory with one.
     */
    const val MAX_RECORD_BYTES = 4 * 1024 * 1024
    const val MAX_HEADER_CHARS = 2_048

    private const val TAG_BYTES = 16
    private const val MAX_CIPHERTEXT_BYTES = MAX_RECORD_BYTES + TAG_BYTES
    private const val MAX_CIPHERTEXT_CHARS = (MAX_CIPHERTEXT_BYTES * 4 + 2) / 3
    const val MAX_DEVICE_FILE_CHARS = MAX_CIPHERTEXT_CHARS + 1_024

    private val deviceIdPattern = Regex("[A-Za-z0-9_-]{22}")

    fun isDeviceId(value: String): Boolean = deviceIdPattern.matches(value)

    fun newDeviceId(crypto: WebDavSyncCrypto): String = SyncBase64.encode(crypto.randomBytes(DEVICE_ID_BYTES))

    /** The vault key for [phrase] under [header]'s salt; the caller zeroes [phrase] and the key after use. */
    fun deriveKey(phrase: ByteArray, header: WebDavSyncHeader, crypto: WebDavSyncCrypto): ByteArray =
        crypto.argon2id(phrase, header.salt).also { require(it.size == KEY_BYTES) }

    /** A new header and its vault key for [phrase]: a fresh space id and salt. */
    fun newHeader(phrase: ByteArray, crypto: WebDavSyncCrypto): Pair<WebDavSyncHeader, ByteArray> {
        val spaceId = crypto.randomBytes(SPACE_BYTES)
        val salt = crypto.randomBytes(SALT_BYTES)
        val key = crypto.argon2id(phrase, salt).also { require(it.size == KEY_BYTES) }
        return headerFor(spaceId, salt, key, crypto) to key
    }

    /** A header with a fresh space id for an existing [key] and its [salt], to start over keeping the phrase. */
    fun restartedHeader(salt: ByteArray, key: ByteArray, crypto: WebDavSyncCrypto): WebDavSyncHeader =
        headerFor(crypto.randomBytes(SPACE_BYTES), salt, key, crypto)

    /** Whether [key] is the vault key of [header]: its check value opens. */
    fun opens(header: WebDavSyncHeader, key: ByteArray, crypto: WebDavSyncCrypto): Boolean {
        val check = crypto.aesGcmOpen(encryptionKey(key, crypto), header.checkNonce, header.checkCiphertext, CHECK_LABEL + header.spaceId)
        return check != null && LocalTransferProtocol.constantTimeEquals(check, CHECK_LABEL)
    }

    fun encodeHeader(header: WebDavSyncHeader): String = buildJsonObject {
        put("v", VERSION)
        put("space", SyncBase64.encode(header.spaceId))
        put(
            "kdf",
            buildJsonObject {
                put("alg", KDF_ALGORITHM)
                put("salt", SyncBase64.encode(header.salt))
                put("m", KDF_MEMORY_KIB)
                put("t", KDF_ITERATIONS)
                put("p", KDF_PARALLELISM)
            },
        )
        put("iv", SyncBase64.encode(header.checkNonce))
        put("check", SyncBase64.encode(header.checkCiphertext))
    }.toString()

    /**
     * The header in [json]; null for anything else, including another Argon2 cost: a server that
     * lowers it cannot make this phone derive a weaker key, the phrase just stops fitting.
     */
    fun decodeHeader(json: String): WebDavSyncHeader? = runCatching {
        require(json.length <= MAX_HEADER_CHARS)
        val root = parseStrictJsonObject(json)
        require(root.keys == setOf("v", "space", "kdf", "iv", "check"))
        require(root.strictInt("v") == VERSION)
        val kdf = root.requireObject("kdf")
        require(kdf.keys == setOf("alg", "salt", "m", "t", "p"))
        require(kdf.strictString("alg") == KDF_ALGORITHM)
        require(kdf.strictInt("m") == KDF_MEMORY_KIB && kdf.strictInt("t") == KDF_ITERATIONS && kdf.strictInt("p") == KDF_PARALLELISM)
        WebDavSyncHeader(
            spaceId = SyncBase64.decode(root.strictString("space"), expectedBytes = SPACE_BYTES),
            salt = SyncBase64.decode(kdf.strictString("salt"), expectedBytes = SALT_BYTES),
            checkNonce = SyncBase64.decode(root.strictString("iv"), expectedBytes = LocalTransferProtocol.GCM_NONCE_BYTES),
            checkCiphertext = SyncBase64.decode(root.strictString("check"), expectedBytes = CHECK_LABEL.size + TAG_BYTES),
        )
    }.getOrNull()

    /** [record] sealed for its device file under [header]. */
    fun sealRecord(record: WebDavDeviceRecord, header: WebDavSyncHeader, key: ByteArray, crypto: WebDavSyncCrypto): String {
        require(isDeviceId(record.deviceId))
        val plaintext = encodeRecord(record).encodeToByteArray()
        require(plaintext.size <= MAX_RECORD_BYTES) { "Record too large" }
        val nonce = crypto.randomBytes(LocalTransferProtocol.GCM_NONCE_BYTES)
        val ciphertext = crypto.aesGcmSeal(encryptionKey(key, crypto), nonce, plaintext, recordAad(header, record.deviceId))
        return buildJsonObject {
            put("v", VERSION)
            put("iv", SyncBase64.encode(nonce))
            put("ct", SyncBase64.encode(ciphertext))
        }.toString()
    }

    /**
     * The record in the device file [body] of [deviceId]; null when it does not open with [key]
     * for this space and device, or is not a version-1 record within the limits.
     */
    fun openRecord(body: String, deviceId: String, header: WebDavSyncHeader, key: ByteArray, crypto: WebDavSyncCrypto): WebDavDeviceRecord? =
        runCatching {
            require(isDeviceId(deviceId) && body.length <= MAX_DEVICE_FILE_CHARS)
            val root = parseStrictJsonObject(body)
            require(root.keys == setOf("v", "iv", "ct"))
            require(root.strictInt("v") == VERSION)
            val nonce = SyncBase64.decode(root.strictString("iv"), expectedBytes = LocalTransferProtocol.GCM_NONCE_BYTES)
            val ciphertext = SyncBase64.decode(root.strictString("ct", maximum = MAX_CIPHERTEXT_CHARS), maxBytes = MAX_CIPHERTEXT_BYTES)
            val plaintext = crypto.aesGcmOpen(encryptionKey(key, crypto), nonce, ciphertext, recordAad(header, deviceId))
                ?: return@runCatching null
            decodeRecord(plaintext.decodeToString(throwOnInvalidSequence = true))?.takeIf { it.deviceId == deviceId }
        }.getOrNull()

    internal fun encodeRecord(record: WebDavDeviceRecord): String = buildJsonObject {
        put("v", VERSION)
        put("device", record.deviceId)
        put("name", record.name.take(LocalTransferPayloadCodec.MAX_DEVICE_NAME_CHARS))
        put("seq", record.sequence)
        put("updated", record.updatedAtMillis)
        put(
            "tabs",
            buildJsonArray {
                record.tabs.filter { LocalTransferPayloadCodec.isTransferable(it.url) }.take(LocalTransferPayloadCodec.MAX_TABS).forEach { tab ->
                    add(buildJsonObject { put("u", tab.url); put("t", tab.title.take(LocalTransferPayloadCodec.MAX_TITLE_CHARS)) })
                }
            },
        )
        put(
            "bookmarks",
            buildJsonArray {
                record.bookmarks.values
                    .filter { LocalTransferPayloadCodec.isTransferable(it.url) }
                    .take(MAX_BOOKMARK_ENTRIES)
                    .forEach { entry ->
                        add(
                            buildJsonObject {
                                put("u", entry.url)
                                put("t", entry.title.take(LocalTransferPayloadCodec.MAX_TITLE_CHARS))
                                put("f", entry.folder.take(LocalTransferPayloadCodec.MAX_FOLDER_CHARS))
                                put("m", entry.modifiedAtMillis)
                                put("d", entry.deleted)
                            },
                        )
                    }
            },
        )
    }.toString()

    internal fun decodeRecord(json: String): WebDavDeviceRecord? = runCatching {
        require(json.length <= MAX_RECORD_BYTES)
        val root = parseStrictJsonObject(json)
        require(root.keys == setOf("v", "device", "name", "seq", "updated", "tabs", "bookmarks"))
        require(root.strictInt("v") == VERSION)
        val deviceId = root.strictString("device").also { require(isDeviceId(it)) }
        val tabs = root.requireArray("tabs").also { require(it.size <= LocalTransferPayloadCodec.MAX_TABS) }.map { element ->
            val tab = element.asObject(setOf("u", "t"))
            LocalTransferTab(tab.url(), tab.strictString("t", minimum = 0, maximum = LocalTransferPayloadCodec.MAX_TITLE_CHARS))
        }
        val entries = root.requireArray("bookmarks").also { require(it.size <= MAX_BOOKMARK_ENTRIES) }.map { element ->
            val entry = element.asObject(setOf("u", "t", "f", "m", "d"))
            WebDavBookmarkEntry(
                url = entry.url(),
                title = entry.strictString("t", minimum = 0, maximum = LocalTransferPayloadCodec.MAX_TITLE_CHARS),
                folder = entry.strictString("f", minimum = 0, maximum = LocalTransferPayloadCodec.MAX_FOLDER_CHARS),
                modifiedAtMillis = entry.strictTime("m"),
                deleted = entry.strictBoolean("d"),
            )
        }
        val bookmarks = entries.associateBy { it.key }
        require(bookmarks.size == entries.size) { "A bookmark twice" }
        WebDavDeviceRecord(
            deviceId = deviceId,
            name = displayName(root.strictString("name", minimum = 0, maximum = LocalTransferPayloadCodec.MAX_DEVICE_NAME_CHARS)),
            sequence = root.strictTime("seq"),
            updatedAtMillis = root.strictTime("updated"),
            tabs = tabs,
            bookmarks = bookmarks,
        )
    }.getOrNull()

    /**
     * [name] without control and bidirectional-override characters: another device's name is
     * shown on this phone, and such characters could make it read as something else.
     */
    internal fun displayName(name: String): String =
        name.filterNot { it.isISOControl() || it.code in 0x202A..0x202E || it.code in 0x2066..0x2069 || it.code == 0x200F || it.code == 0x200E }

    private fun headerFor(spaceId: ByteArray, salt: ByteArray, key: ByteArray, crypto: WebDavSyncCrypto): WebDavSyncHeader {
        val nonce = crypto.randomBytes(LocalTransferProtocol.GCM_NONCE_BYTES)
        val check = crypto.aesGcmSeal(encryptionKey(key, crypto), nonce, CHECK_LABEL, CHECK_LABEL + spaceId)
        return WebDavSyncHeader(spaceId, salt, nonce, check)
    }

    private fun encryptionKey(key: ByteArray, crypto: WebDavSyncCrypto): ByteArray = crypto.hmacSha256(key, ENCRYPTION_LABEL)

    private fun recordAad(header: WebDavSyncHeader, deviceId: String): ByteArray =
        DEVICE_LABEL + header.spaceId + deviceId.encodeToByteArray()

    private fun JsonElement.asObject(keys: Set<String>): JsonObject =
        (this as? JsonObject)?.takeIf { it.keys == keys } ?: throw IllegalArgumentException("Invalid entry")

    private fun JsonObject.url(): String = strictString("u", maximum = LocalTransferPayloadCodec.MAX_URL_CHARS)
        .takeIf { LocalTransferPayloadCodec.isTransferable(it) } ?: throw IllegalArgumentException("Invalid url")

    /** A non-negative whole number such as a time in milliseconds. */
    private fun JsonObject.strictTime(name: String): Long {
        val primitive = get(name) as? JsonPrimitive ?: throw IllegalArgumentException("Invalid $name")
        require(!primitive.isString)
        return primitive.longOrNull?.takeIf { it >= 0 } ?: throw IllegalArgumentException("Invalid $name")
    }

    private const val KDF_ALGORITHM = "argon2id"
    const val KDF_MEMORY_KIB = 65_536
    const val KDF_ITERATIONS = 3
    const val KDF_PARALLELISM = 4
    private val ENCRYPTION_LABEL = "vola-webdav/v1/encryption-key".encodeToByteArray()
    private val CHECK_LABEL = "vola-webdav/v1/check".encodeToByteArray()
    private val DEVICE_LABEL = "vola-webdav/v1/device".encodeToByteArray()
}
