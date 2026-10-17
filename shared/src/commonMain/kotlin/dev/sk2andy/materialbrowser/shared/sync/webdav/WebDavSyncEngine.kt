package dev.sk2andy.materialbrowser.shared.sync.webdav

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferBookmark
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferTab
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** One WebDAV request. */
class WebDavRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
)

/** The server's answer: its status, its `ETag` header and its body. */
class WebDavResponse(val status: Int, val etag: String? = null, val body: ByteArray = ByteArray(0))

/**
 * The platform's HTTPS client. It checks the certificate the system way (never "trust any"),
 * follows no redirect, bounds the body it reads and the time it waits, and throws when no
 * answer arrives (network, TLS, timeout).
 */
fun interface WebDavTransport {
    fun send(request: WebDavRequest): WebDavResponse
}

/**
 * A connection, as the adapter keeps it (in the keystore): the WebDAV root found for the address,
 * the login and app password, the vault key derived from the phrase, the header it belongs to and
 * this device's id. The phrase itself is not kept.
 */
class WebDavAccount(
    val root: String,
    val host: String,
    val login: String,
    val appPassword: String,
    val key: ByteArray,
    val header: WebDavSyncHeader,
    val deviceId: String,
) {
    override fun toString(): String = "WebDavAccount(host=$host, device=$deviceId)"
}

/** What this phone has for a sync: its name, its open web tabs and its bookmarks. */
data class WebDavLocalState(
    val deviceName: String,
    val tabs: List<LocalTransferTab>,
    val bookmarks: List<LocalTransferBookmark>,
)

/** Another device's record as last fetched, with the `ETag` it had. */
data class WebDavCachedDevice(val etag: String?, val record: WebDavDeviceRecord)

/**
 * What the adapter keeps between syncs (app storage, not a secret): the bookmarks after the last
 * sync ([WebDavBookmarkRules.remember]; null before the first one), this device's last sequence
 * and file `ETag`, and the other devices' records.
 */
data class WebDavSyncMemory(
    val remembered: Map<String, WebDavBookmarkEntry>? = null,
    val sequence: Long = 0,
    val ownEtag: String? = null,
    val devices: Map<String, WebDavCachedDevice> = emptyMap(),
)

sealed interface WebDavConnectOutcome {
    data class Connected(val account: WebDavAccount) : WebDavConnectOutcome

    data class Failed(val reason: RemoteSyncFailure) : WebDavConnectOutcome
}

/** The first half of a sync: what the other devices say, before this phone applies it. */
class WebDavSyncPlan internal constructor(
    /** What to change in this phone's bookmarks. */
    val changes: WebDavBookmarkChanges,
    /** The other devices, most recently updated first. */
    val devices: List<WebDavDeviceRecord>,
    internal val merged: Map<String, WebDavBookmarkEntry>,
    internal val local: WebDavLocalState,
    internal val cache: Map<String, WebDavCachedDevice>,
    internal val ownEtag: String?,
    internal val sequence: Long,
)

sealed interface WebDavPullOutcome {
    data class Pulled(val plan: WebDavSyncPlan) : WebDavPullOutcome

    data class Failed(val reason: RemoteSyncFailure) : WebDavPullOutcome
}

/**
 * The end of a sync: the [memory] to keep, whether or not the upload worked, and why it failed.
 * [again] means this device's file changed on the server meanwhile: sync once more.
 */
data class WebDavPushOutcome(val memory: WebDavSyncMemory, val failure: RemoteSyncFailure?, val again: Boolean = false)

/**
 * The WebDAV sync (V27) over a [WebDavTransport]. Blocking: the adapter calls it off the main
 * thread, one call at a time. A sync is [pull], then the adapter applies [WebDavSyncPlan.changes]
 * to the phone's bookmarks, then [push] with what the phone holds afterwards, so a bookmark that
 * did not fit or failed to write is never taken for a deletion.
 */
class WebDavSyncEngine(
    private val transport: WebDavTransport,
    private val crypto: WebDavSyncCrypto,
) {
    /**
     * Finds the WebDAV root for [address] and [login] (Nextcloud's files root first), then opens
     * Vola's data there with [phrase], or creates it when there is none. A server whose data the
     * phrase does not open answers [RemoteSyncFailure.PhraseMismatch] and is left as it is.
     */
    fun connect(address: String, login: String, appPassword: String, phrase: ByteArray, deviceId: String): WebDavConnectOutcome =
        attempt {
            val (root, host) = findRoot(address, login, appPassword)
            val auth = authorization(login, appPassword)
            val (header, created) = openOrCreate(root, auth, phrase)
            val key = created ?: WebDavSyncFormat.deriveKey(phrase, header, crypto)
            if (!WebDavSyncFormat.opens(header, key, crypto)) {
                key.fill(0)
                throw Stop(RemoteSyncFailure.PhraseMismatch)
            }
            WebDavConnectOutcome.Connected(WebDavAccount(root, host, login, appPassword, key, header, deviceId))
        }

    /** Deletes Vola's data at [address] for every device, then connects with [phrase] as the first device. */
    fun startOver(address: String, login: String, appPassword: String, phrase: ByteArray, deviceId: String): WebDavConnectOutcome =
        attempt {
            val (root, _) = findRoot(address, login, appPassword)
            deleteFolder(root, authorization(login, appPassword))
            connect(address, login, appPassword, phrase, deviceId)
        }

    /**
     * Deletes Vola's data on the server and starts it again from this device with the same
     * phrase: a new space, so the other devices hear [RemoteSyncFailure.PhraseMismatch] until
     * they connect again. Start the next sync with a fresh [WebDavSyncMemory].
     */
    fun startOver(account: WebDavAccount): WebDavConnectOutcome = attempt {
        val auth = authorization(account.login, account.appPassword)
        deleteFolder(account.root, auth)
        val header = WebDavSyncFormat.restartedHeader(account.header.salt, account.key, crypto)
        createHeader(account.root, auth, header)
        WebDavConnectOutcome.Connected(
            WebDavAccount(account.root, account.host, account.login, account.appPassword, account.key.copyOf(), header, account.deviceId),
        )
    }

    /** Reads the other devices and merges the bookmarks; changes nothing on the server. */
    fun pull(account: WebDavAccount, local: WebDavLocalState, memory: WebDavSyncMemory, nowMillis: Long): WebDavPullOutcome = attemptPull {
        val auth = authorization(account.login, account.appPassword)
        checkHeader(account, auth)
        val listing = send("PROPFIND", WebDavSyncAddressRules.devices(account.root), auth, depth = "1", body = WebDavMultistatus.PROPFIND_BODY)
        val files = when (listing.status) {
            207 -> WebDavMultistatus.parse(listing.body.decodeToString()) ?: throw Stop(RemoteSyncFailure.NotWebDav)
            404 -> {
                makeFolder(WebDavSyncAddressRules.devices(account.root), auth)
                emptyList()
            }
            else -> throw Stop(failureOf(listing.status))
        }
        val listed = files
            .filter { !it.isCollection && it.name.endsWith(WebDavSyncFormat.DEVICE_FILE_SUFFIX) }
            .associateBy { it.name.removeSuffix(WebDavSyncFormat.DEVICE_FILE_SUFFIX) }
            .filterKeys(WebDavSyncFormat::isDeviceId)
        var sequence = memory.sequence
        val ownEtag = listed[account.deviceId]?.etag
        if (ownEtag != null && ownEtag != memory.ownEtag) {
            fetch(account, auth, account.deviceId)?.let { sequence = maxOf(sequence, it.second.sequence) }
        }
        val cache = LinkedHashMap<String, WebDavCachedDevice>()
        listed.filterKeys { it != account.deviceId }.entries.take(MAX_DEVICES).forEach { (id, resource) ->
            val cached = memory.devices[id]
            cache[id] = if (cached != null && resource.etag != null && cached.etag == resource.etag) {
                cached
            } else {
                val fetched = fetch(account, auth, id)
                when {
                    fetched == null -> cached ?: return@forEach
                    cached != null && fetched.second.sequence < cached.record.sequence -> cached
                    else -> WebDavCachedDevice(fetched.first ?: resource.etag, fetched.second)
                }
            }
        }
        val journal = WebDavBookmarkRules.journal(memory.remembered, local.bookmarks, nowMillis)
        val merged = WebDavBookmarkRules.merge(listOf(journal) + cache.values.map { it.record.bookmarks }, nowMillis)
        WebDavPullOutcome.Pulled(
            WebDavSyncPlan(
                changes = WebDavBookmarkRules.changes(local.bookmarks, merged),
                devices = cache.values.map { it.record }.sortedByDescending { it.updatedAtMillis },
                merged = merged,
                local = local,
                cache = cache,
                ownEtag = ownEtag,
                sequence = sequence,
            ),
        )
    }

    /**
     * Publishes this device's record after the phone applied [plan], holding [applied] bookmarks
     * now. The upload is conditional on the file being as [pull] saw it (`If-Match`, or
     * `If-None-Match: *` for a first upload), so it never overwrites a write it has not seen.
     */
    fun push(account: WebDavAccount, plan: WebDavSyncPlan, applied: List<LocalTransferBookmark>, nowMillis: Long): WebDavPushOutcome {
        val sequence = plan.sequence + 1
        val remembered = WebDavBookmarkRules.remember(plan.merged, applied)
        val kept = WebDavSyncMemory(remembered, plan.sequence, plan.ownEtag, plan.cache)
        val record = WebDavDeviceRecord(
            deviceId = account.deviceId,
            name = plan.local.deviceName,
            sequence = sequence,
            updatedAtMillis = nowMillis,
            tabs = plan.local.tabs,
            bookmarks = plan.merged,
        )
        return try {
            val body = WebDavSyncFormat.sealRecord(record, account.header, account.key, crypto).encodeToByteArray()
            val condition = plan.ownEtag?.let { mapOf("If-Match" to it) } ?: mapOf("If-None-Match" to "*")
            val response = send(
                "PUT",
                WebDavSyncAddressRules.device(account.root, account.deviceId),
                authorization(account.login, account.appPassword),
                extra = condition + ("Content-Type" to "application/octet-stream"),
                bytes = body,
            )
            when (response.status) {
                200, 201, 204 -> WebDavPushOutcome(kept.copy(sequence = sequence, ownEtag = response.etag), failure = null)
                412 -> WebDavPushOutcome(kept.copy(ownEtag = null), failure = null, again = true)
                else -> WebDavPushOutcome(kept, failureOf(response.status))
            }
        } catch (stop: Stop) {
            WebDavPushOutcome(kept, stop.reason)
        } catch (_: Exception) {
            WebDavPushOutcome(kept, RemoteSyncFailure.Unreachable)
        }
    }

    /**
     * Takes this device off the server: its file, or with [everything] all of Vola's data.
     * False when the server could not be reached for it.
     */
    fun remove(account: WebDavAccount, everything: Boolean): Boolean = runCatching {
        val auth = authorization(account.login, account.appPassword)
        if (everything) {
            deleteFolder(account.root, auth)
        } else {
            val status = send("DELETE", WebDavSyncAddressRules.device(account.root, account.deviceId), auth).status
            require(status in 200..299 || status == 404)
        }
        true
    }.getOrDefault(false)

    private fun findRoot(address: String, login: String, appPassword: String): Pair<String, String> {
        val checked = when (val check = WebDavSyncAddressRules.check(address)) {
            is WebDavAddressCheck.Valid -> check
            WebDavAddressCheck.NotHttps -> throw Stop(RemoteSyncFailure.NotHttps)
            WebDavAddressCheck.Invalid -> throw Stop(RemoteSyncFailure.Unreachable)
        }
        val auth = authorization(login, appPassword)
        WebDavSyncAddressRules.roots(checked.base, login).forEach { root ->
            val response = send("PROPFIND", root, auth, depth = "0", body = WebDavMultistatus.PROPFIND_BODY)
            when (response.status) {
                207 -> if (WebDavMultistatus.parse(response.body.decodeToString())?.isNotEmpty() == true) return root to checked.host
                401, 403 -> throw Stop(RemoteSyncFailure.LoginRejected)
            }
        }
        throw Stop(RemoteSyncFailure.NotWebDav)
    }

    /**
     * The header at [root]; a new one for [phrase] with its key when there is none, unless another
     * device makes one first.
     */
    private fun openOrCreate(root: String, auth: String, phrase: ByteArray): Pair<WebDavSyncHeader, ByteArray?> {
        readHeader(root, auth)?.let { return it to null }
        val (header, key) = WebDavSyncFormat.newHeader(phrase, crypto)
        if (createHeader(root, auth, header)) return header to key
        key.fill(0)
        return (readHeader(root, auth) ?: throw Stop(RemoteSyncFailure.Unreachable)) to null
    }

    /** The header, null when there is none; [RemoteSyncFailure.PhraseMismatch] when it is damaged. */
    private fun readHeader(root: String, auth: String): WebDavSyncHeader? {
        val response = send("GET", WebDavSyncAddressRules.header(root), auth)
        return when (response.status) {
            200 -> WebDavSyncFormat.decodeHeader(response.body.decodeToString()) ?: throw Stop(RemoteSyncFailure.PhraseMismatch)
            404 -> null
            else -> throw Stop(failureOf(response.status))
        }
    }

    /** Writes [header] only where there is none; false when another device was first. */
    private fun createHeader(root: String, auth: String, header: WebDavSyncHeader): Boolean {
        makeFolder(WebDavSyncAddressRules.folder(root), auth)
        makeFolder(WebDavSyncAddressRules.devices(root), auth)
        val response = send(
            "PUT",
            WebDavSyncAddressRules.header(root),
            auth,
            extra = mapOf("If-None-Match" to "*", "Content-Type" to "application/json"),
            bytes = WebDavSyncFormat.encodeHeader(header).encodeToByteArray(),
        )
        return when (response.status) {
            200, 201, 204 -> true
            412 -> false
            else -> throw Stop(failureOf(response.status))
        }
    }

    /**
     * The server still holds [account]'s space. A missing header (all data deleted from another
     * device) is written again, as this device is still connected; another space or a header
     * this key does not open is [RemoteSyncFailure.PhraseMismatch].
     */
    private fun checkHeader(account: WebDavAccount, auth: String) {
        val header = readHeader(account.root, auth)
            ?: if (createHeader(account.root, auth, account.header)) account.header else readHeader(account.root, auth)
        if (header != account.header || !WebDavSyncFormat.opens(header, account.key, crypto)) throw Stop(RemoteSyncFailure.PhraseMismatch)
    }

    private fun fetch(account: WebDavAccount, auth: String, deviceId: String): Pair<String?, WebDavDeviceRecord>? {
        val response = send("GET", WebDavSyncAddressRules.device(account.root, deviceId), auth)
        if (response.status == 404) return null
        if (response.status != 200) throw Stop(failureOf(response.status))
        if (response.body.size > WebDavSyncFormat.MAX_DEVICE_FILE_CHARS) return null
        val record = WebDavSyncFormat.openRecord(response.body.decodeToString(), deviceId, account.header, account.key, crypto)
        return record?.let { response.etag to it }
    }

    private fun makeFolder(url: String, auth: String) {
        val status = send("MKCOL", url, auth).status
        if (status != 201 && status != 405) throw Stop(failureOf(status))
    }

    private fun deleteFolder(root: String, auth: String) {
        val status = send("DELETE", WebDavSyncAddressRules.folder(root), auth).status
        if (status !in 200..299 && status != 404) throw Stop(failureOf(status))
    }

    private fun send(
        method: String,
        url: String,
        auth: String,
        depth: String? = null,
        body: String? = null,
        extra: Map<String, String> = emptyMap(),
        bytes: ByteArray? = null,
    ): WebDavResponse {
        val headers = LinkedHashMap<String, String>()
        headers["Authorization"] = auth
        if (depth != null) headers["Depth"] = depth
        if (body != null) headers["Content-Type"] = "application/xml; charset=utf-8"
        headers += extra
        return transport.send(WebDavRequest(method, url, headers, bytes ?: body?.encodeToByteArray()))
    }

    private inline fun attempt(block: () -> WebDavConnectOutcome): WebDavConnectOutcome = try {
        block()
    } catch (stop: Stop) {
        WebDavConnectOutcome.Failed(stop.reason)
    } catch (_: Exception) {
        WebDavConnectOutcome.Failed(RemoteSyncFailure.Unreachable)
    }

    private inline fun attemptPull(block: () -> WebDavPullOutcome): WebDavPullOutcome = try {
        block()
    } catch (stop: Stop) {
        WebDavPullOutcome.Failed(stop.reason)
    } catch (_: Exception) {
        WebDavPullOutcome.Failed(RemoteSyncFailure.Unreachable)
    }

    private class Stop(val reason: RemoteSyncFailure) : Exception()

    companion object {
        /** The most other devices one sync reads. */
        const val MAX_DEVICES = 50

        /** HTTP Basic for [login] and [appPassword], as WebDAV and Nextcloud's app passwords expect. */
        @OptIn(ExperimentalEncodingApi::class)
        fun authorization(login: String, appPassword: String): String =
            "Basic " + Base64.Default.encode("$login:$appPassword".encodeToByteArray())

        private fun failureOf(status: Int): RemoteSyncFailure = when (status) {
            401, 403 -> RemoteSyncFailure.LoginRejected
            405, 501 -> RemoteSyncFailure.NotWebDav
            else -> RemoteSyncFailure.Unreachable
        }
    }
}
