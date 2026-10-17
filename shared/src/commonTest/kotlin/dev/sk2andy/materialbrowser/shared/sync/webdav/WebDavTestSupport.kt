package dev.sk2andy.materialbrowser.shared.sync.webdav

/** Deterministic stand-ins for AES-GCM, HMAC and Argon2id: the same contracts, no real secrecy. */
internal class FakeSyncCrypto : WebDavSyncCrypto {
    private var counter = 0
    var argon2Calls = 0
        private set

    override fun randomBytes(size: Int): ByteArray = ByteArray(size) { (it * 31 + ++counter).toByte() }

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        var state = 0x811C9DC5.toInt()
        for (byte in key + byteArrayOf(0x5C) + data) state = (state xor byte.toInt()) * 0x01000193
        return ByteArray(32) { index ->
            state = (state xor index) * 0x01000193
            (state ushr 8).toByte()
        }
    }

    override fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        val body = xor(key, nonce, plaintext)
        return body + tag(key, nonce, aad, body)
    }

    override fun aesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray? {
        if (ciphertext.size < 16) return null
        val body = ciphertext.copyOf(ciphertext.size - 16)
        if (!ciphertext.copyOfRange(ciphertext.size - 16, ciphertext.size).contentEquals(tag(key, nonce, aad, body))) return null
        return xor(key, nonce, body)
    }

    override fun argon2id(phrase: ByteArray, salt: ByteArray): ByteArray {
        argon2Calls++
        return hmacSha256(salt, "argon2id".encodeToByteArray() + phrase)
    }

    private fun xor(key: ByteArray, nonce: ByteArray, data: ByteArray): ByteArray {
        val stream = hmacSha256(key, nonce)
        return ByteArray(data.size) { (data[it].toInt() xor stream[it % stream.size].toInt()).toByte() }
    }

    private fun tag(key: ByteArray, nonce: ByteArray, aad: ByteArray, body: ByteArray): ByteArray =
        hmacSha256(key, "tag".encodeToByteArray() + nonce + aad + body).copyOf(16)
}

/**
 * A WebDAV server in memory, as strict as a real one about what the sync relies on: Basic auth,
 * `MKCOL` only under an existing collection, `If-Match` / `If-None-Match: *` and `ETag`s that change
 * with every write. With [nextcloudUser] it serves only under Nextcloud's files root.
 */
internal class FakeWebDavServer(
    private val origin: String = "https://cloud.example.com",
    private val login: String = "anna",
    private val password: String = "app-password",
    nextcloudUser: String? = null,
) : WebDavTransport {
    val root: String = if (nextcloudUser != null) "$origin/remote.php/dav/files/$nextcloudUser/" else "$origin/"
    val files = LinkedHashMap<String, Pair<ByteArray, String>>()
    val collections = LinkedHashSet<String>().apply { add(root) }
    val requests = ArrayList<WebDavRequest>()
    var offline = false
    var returnsEtagOnPut = true

    /** Called before each request is served, to change the server under the sync's feet. */
    var beforeRequest: (WebDavRequest) -> Unit = {}
    private var version = 0

    override fun send(request: WebDavRequest): WebDavResponse {
        if (offline) throw IllegalStateException("offline")
        requests += request
        beforeRequest(request)
        if (request.headers["Authorization"] != WebDavSyncEngine.authorization(login, password)) return WebDavResponse(401)
        val url = request.url
        if (!url.startsWith(root)) return WebDavResponse(404)
        return when (request.method) {
            "PROPFIND" -> propfind(url, request.headers["Depth"])
            "GET" -> files[url]?.let { WebDavResponse(200, it.second, it.first) } ?: WebDavResponse(404)
            "PUT" -> put(url, request)
            "MKCOL" -> when {
                url in collections || url in files -> WebDavResponse(405)
                parent(url) !in collections -> WebDavResponse(409)
                else -> WebDavResponse(201).also { collections += url }
            }
            "DELETE" -> delete(url)
            else -> WebDavResponse(405)
        }
    }

    fun write(url: String, body: String) {
        files[url] = body.encodeToByteArray() to newEtag()
    }

    private fun put(url: String, request: WebDavRequest): WebDavResponse {
        if (parent(url) !in collections) return WebDavResponse(409)
        val existing = files[url]
        if (request.headers["If-None-Match"] == "*" && existing != null) return WebDavResponse(412)
        request.headers["If-Match"]?.let { expected -> if (existing?.second != expected) return WebDavResponse(412) }
        val etag = newEtag()
        files[url] = (request.body ?: ByteArray(0)) to etag
        return WebDavResponse(if (existing == null) 201 else 204, etag.takeIf { returnsEtagOnPut })
    }

    private fun delete(url: String): WebDavResponse {
        if (files.remove(url) != null) return WebDavResponse(204)
        if (url !in collections || url == root) return WebDavResponse(404)
        collections.removeAll { it.startsWith(url) }
        files.keys.removeAll { it.startsWith(url) }
        return WebDavResponse(204)
    }

    private fun propfind(url: String, depth: String?): WebDavResponse {
        val self = when {
            url in collections -> entry(url, collection = true, etag = null)
            url in files -> entry(url, collection = false, etag = files.getValue(url).second)
            else -> return WebDavResponse(404)
        }
        val children = if (depth == "1" && url in collections) {
            collections.filter { it != url && parent(it) == url }.map { entry(it, collection = true, etag = null) } +
                files.filterKeys { parent(it) == url }.map { (path, file) -> entry(path, collection = false, etag = file.second) }
        } else {
            emptyList()
        }
        val xml = "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\">${(listOf(self) + children).joinToString("")}</d:multistatus>"
        return WebDavResponse(207, body = xml.encodeToByteArray())
    }

    private fun entry(url: String, collection: Boolean, etag: String?): String {
        val type = if (collection) "<d:resourcetype><d:collection/></d:resourcetype>" else "<d:resourcetype/>"
        val tag = etag?.let { "<d:getetag>${it.replace("\"", "&quot;")}</d:getetag>" } ?: ""
        return "<d:response><d:href>${url.removePrefix(origin)}</d:href><d:propstat><d:prop>$type$tag</d:prop>" +
            "<d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>"
    }

    private fun parent(url: String): String = url.trimEnd('/').substringBeforeLast('/') + "/"

    private fun newEtag(): String = "\"e${++version}\""
}
