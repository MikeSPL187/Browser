package dev.sk2andy.materialbrowser.shared.transfer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalTransferTest {
    private val now = 1_800_000_000L
    private val pairing = LocalTransferPairing(
        host = "192.168.1.20",
        port = 48_123,
        sessionId = ByteArray(16) { it.toByte() },
        key = ByteArray(32) { (it * 7).toByte() },
        expiresAtEpochSeconds = now + LocalTransferPairingRules.LIFETIME_SECONDS,
    )
    private val payload = LocalTransferPayload(
        deviceName = "Pixel 8",
        tabs = listOf(LocalTransferTab("https://example.com/a", "A"), LocalTransferTab("https://новости.рф/", "Новости")),
        bookmarks = listOf(LocalTransferBookmark("https://example.org/", "Example", folder = "Работа/Docs")),
    )

    @Test
    fun aCodeRoundTripsAndNeverPrintsItsKey() {
        val code = LocalTransferPairingRules.format(pairing)
        val parsed = assertNotNull(LocalTransferPairingRules.parse(code, now))

        assertTrue(code.startsWith("vola-pair:v1?h=192.168.1.20&p=48123&"))
        assertEquals(pairing.host, parsed.host)
        assertEquals(pairing.port, parsed.port)
        assertContentEquals(pairing.sessionId, parsed.sessionId)
        assertContentEquals(pairing.key, parsed.key)
        assertFalse(parsed.toString().contains(code.substringAfter("&k=").substringBefore('&')))
    }

    @Test
    fun onlyAWellFormedOpenLocalCodeParses() {
        val code = LocalTransferPairingRules.format(pairing)
        fun with(name: String, value: String) = code.replace(Regex("$name=[^&]*"), "$name=$value")

        assertNull(LocalTransferPairingRules.parse(code, now + 200), "expired")
        assertNull(LocalTransferPairingRules.parse(with("e", "${now + 3_600}"), now), "lives too long")
        assertNull(LocalTransferPairingRules.parse(with("h", "8.8.8.8"), now), "internet host")
        assertNull(LocalTransferPairingRules.parse(with("h", "127.0.0.1"), now), "loopback")
        assertNull(LocalTransferPairingRules.parse(with("h", "example.com"), now), "a name")
        assertNull(LocalTransferPairingRules.parse(with("p", "80"), now), "privileged port")
        assertNull(LocalTransferPairingRules.parse(with("k", "AAAA"), now), "short key")
        assertNull(LocalTransferPairingRules.parse("$code&k=AAAA", now), "a field twice")
        assertNull(LocalTransferPairingRules.parse(code.replace("&s=", "&x="), now), "unknown field")
        assertNull(LocalTransferPairingRules.parse(code.replace("vola-pair:v1", "vola-pair:v2"), now), "other version")
        assertNull(LocalTransferPairingRules.parse("https://example.com", now))
        assertNotNull(LocalTransferPairingRules.parse(" $code\n", now), "a scanner's whitespace")
    }

    @Test
    fun localHostsArePrivateIpv4Only() {
        listOf("10.0.0.1", "172.16.0.1", "172.31.255.254", "192.168.0.1").forEach { assertTrue(LocalTransferPairingRules.isLocalHost(it), it) }
        listOf("172.15.0.1", "172.32.0.1", "192.169.0.1", "169.254.1.1", "127.0.0.1", "8.8.8.8", "010.0.0.1", "10.0.0", "10.0.0.256", "fe80::1", "")
            .forEach { assertFalse(LocalTransferPairingRules.isLocalHost(it), it) }
    }

    @Test
    fun thePayloadRoundTripsAndKeepsOnlyWebAddresses() {
        val mixed = payload.copy(
            tabs = payload.tabs + LocalTransferTab("javascript:alert(1)", "x") + LocalTransferTab("file:///sdcard/a", "y"),
            bookmarks = payload.bookmarks + LocalTransferBookmark("intent://scan#Intent;end", "z"),
        )

        assertEquals(payload, LocalTransferPayloadCodec.decode(LocalTransferPayloadCodec.encode(mixed)))
    }

    @Test
    fun aPayloadFromAnotherPhoneIsReadStrictly() {
        val good = LocalTransferPayloadCodec.encode(payload)

        assertNull(LocalTransferPayloadCodec.decode(good.replace("\"https://example.com/a\"", "\"javascript:alert(1)\"")))
        assertNull(LocalTransferPayloadCodec.decode(good.replace("\"v\":1", "\"v\":2")))
        assertNull(LocalTransferPayloadCodec.decode(good.replace("\"device\"", "\"extra\":1,\"device\"")))
        assertNull(LocalTransferPayloadCodec.decode("[]"))
        assertNull(LocalTransferPayloadCodec.decode("not json"))
        assertFalse(LocalTransferPayloadCodec.isTransferable("https://"))
        assertFalse(LocalTransferPayloadCodec.isTransferable("https://exa mple.com"))
        assertTrue(LocalTransferPayloadCodec.isTransferable("HTTP://Example.com"))
    }

    @Test
    fun theProvenRequestGetsThePayloadSealedForIt() {
        val crypto = FakeCrypto()
        val request = LocalTransferProtocol.request(pairing, crypto)

        val nonce = assertNotNull(LocalTransferProtocol.checkRequest(pairing, request.body, crypto))
        val answer = LocalTransferProtocol.seal(pairing, nonce, payload, crypto)

        assertEquals(payload, LocalTransferProtocol.open(pairing, request.nonce, answer, crypto))
        assertEquals("/v1/AAECAwQFBgcICQoLDA0ODw", LocalTransferProtocol.path(pairing))
    }

    @Test
    fun aRequestWithoutTheKeyIsRefused() {
        val crypto = FakeCrypto()
        val stranger = LocalTransferPairing(pairing.host, pairing.port, pairing.sessionId, ByteArray(32) { 1 }, pairing.expiresAtEpochSeconds)
        val otherSession = LocalTransferPairing(pairing.host, pairing.port, ByteArray(16) { 9 }, pairing.key, pairing.expiresAtEpochSeconds)

        assertNull(LocalTransferProtocol.checkRequest(pairing, LocalTransferProtocol.request(stranger, crypto).body, crypto))
        assertNull(LocalTransferProtocol.checkRequest(pairing, LocalTransferProtocol.request(otherSession, crypto).body, crypto))
        assertNull(LocalTransferProtocol.checkRequest(pairing, "{\"v\":1}", crypto))
        assertNull(LocalTransferProtocol.checkRequest(pairing, "x".repeat(5_000), crypto))
    }

    @Test
    fun anAnswerOpensOnlyForItsOwnRequestAndKey() {
        val crypto = FakeCrypto()
        val first = LocalTransferProtocol.request(pairing, crypto)
        val second = LocalTransferProtocol.request(pairing, crypto)
        val answer = LocalTransferProtocol.seal(pairing, first.nonce, payload, crypto)
        val stranger = LocalTransferPairing(pairing.host, pairing.port, pairing.sessionId, ByteArray(32) { 1 }, pairing.expiresAtEpochSeconds)

        assertNull(LocalTransferProtocol.open(pairing, second.nonce, answer, crypto), "replayed to another request")
        assertNull(LocalTransferProtocol.open(stranger, first.nonce, answer, crypto), "another key")
        val ct = answer.substringAfter("\"ct\":\"").substringBefore('"')
        val flipped = answer.replace(ct, (if (ct[0] == 'A') 'B' else 'A') + ct.drop(1))
        assertNull(LocalTransferProtocol.open(pairing, first.nonce, flipped, crypto), "changed bytes")
    }

    @Test
    fun anOversizedPayloadIsNotSealed() {
        val huge = payload.copy(
            bookmarks = List(LocalTransferPayloadCodec.MAX_BOOKMARKS) { index ->
                LocalTransferBookmark("https://example.com/" + "a".repeat(2_000) + index, "t".repeat(LocalTransferPayloadCodec.MAX_TITLE_CHARS))
            },
        )
        assertFailsWith<IllegalArgumentException> {
            LocalTransferProtocol.seal(pairing, ByteArray(16), huge, FakeCrypto())
        }
    }

    @Test
    fun constantTimeEqualsComparesWholeArrays() {
        assertTrue(LocalTransferProtocol.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
        assertFalse(LocalTransferProtocol.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertFalse(LocalTransferProtocol.constantTimeEquals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
    }

    @Test
    fun mergingAddsOnlyMissingBookmarksAndAllTabsUnderTheDevice() {
        val incoming = payload.copy(
            deviceName = "  Pixel 8 ",
            bookmarks = listOf(
                LocalTransferBookmark("HTTPS://Example.ORG", "same as existing"),
                LocalTransferBookmark("https://example.org:443/", "same again"),
                LocalTransferBookmark("https://example.org/docs", "new"),
                LocalTransferBookmark("https://example.org/docs", "new, twice"),
                LocalTransferBookmark("https://example.org/docs#part", "another page"),
            ),
        )

        val plan = LocalTransferMergeRules.plan(incoming, existingBookmarkUrls = listOf("https://example.org/"))

        assertEquals(listOf("new", "another page"), plan.bookmarks.map { it.title })
        assertEquals("Pixel 8", plan.tabGroupDeviceName)
        assertEquals(payload.tabs, plan.tabs)
        assertFalse(plan.isEmpty)
        assertTrue(LocalTransferMergeRules.plan(LocalTransferPayload("x", emptyList(), emptyList()), emptyList()).isEmpty)
    }

    @Test
    fun bookmarkKeysIgnoreOnlyWhatDoesNotChangeThePage() {
        assertEquals("https://example.com", LocalTransferMergeRules.bookmarkKey("HTTPS://EXAMPLE.com/"))
        assertEquals("http://example.com", LocalTransferMergeRules.bookmarkKey("http://example.com:80"))
        assertEquals("https://example.com:8443/A", LocalTransferMergeRules.bookmarkKey("https://Example.com:8443/A"))
        assertEquals("https://example.com?q=1", LocalTransferMergeRules.bookmarkKey("https://example.com/?q=1"))
        assertEquals("https://user@example.com/x", LocalTransferMergeRules.bookmarkKey("https://user@EXAMPLE.com/x"))
    }

    @Test
    fun theUnavailablePortFailsAtOnce() {
        var state: LocalTransferOfferState? = null
        var result: LocalTransferReceiveResult? = null
        LocalTransferPort.Unavailable.offer(payload) { state = it }.cancel()
        LocalTransferPort.Unavailable.receive("x") { result = it }

        assertEquals(LocalTransferOfferState.Failed(LocalTransferFailure.Unavailable), state)
        assertEquals(LocalTransferReceiveResult.Failed(LocalTransferFailure.Unavailable), result)
    }

    /**
     * Deterministic stand-in for the platform's crypto: not secure, but like GCM it refuses any
     * change to the key, the nonce, the associated data or the ciphertext.
     */
    private class FakeCrypto : LocalTransferCrypto {
        private var counter = 0

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
            val tag = ciphertext.copyOfRange(ciphertext.size - 16, ciphertext.size)
            if (!tag.contentEquals(tag(key, nonce, aad, body))) return null
            return xor(key, nonce, body)
        }

        private fun xor(key: ByteArray, nonce: ByteArray, data: ByteArray): ByteArray {
            val stream = hmacSha256(key, nonce)
            return ByteArray(data.size) { (data[it].toInt() xor stream[it % stream.size].toInt()).toByte() }
        }

        private fun tag(key: ByteArray, nonce: ByteArray, aad: ByteArray, body: ByteArray): ByteArray =
            hmacSha256(key, nonce + aad + body).copyOf(16)
    }
}
