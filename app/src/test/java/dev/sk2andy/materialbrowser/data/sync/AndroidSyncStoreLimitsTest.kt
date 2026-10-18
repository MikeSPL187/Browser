package dev.sk2andy.materialbrowser.data.sync

import dev.sk2andy.materialbrowser.sync.SyncBase64
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSyncStoreLimitsTest {
    @Test
    fun `the largest cache a store saves fits the size it reads back`() {
        assertEnvelopeFits(plaintextBytes = 8 * 1_024 * 1_024)
    }

    @Test
    fun `the largest vault a store saves fits the size it reads back`() {
        assertEnvelopeFits(plaintextBytes = 16 * 1_024)
    }

    @Test
    fun `an empty plaintext fits too`() {
        assertEnvelopeFits(plaintextBytes = 0)
    }

    /** The envelope as AndroidSyncSecretProtector.encrypt writes it: GCM adds a 16-byte tag. */
    private fun assertEnvelopeFits(plaintextBytes: Int) {
        val envelope = JSONObject()
            .put("cryptoVersion", 1)
            .put("nonce", SyncBase64.encode(ByteArray(12) { 0x7f }))
            .put("ciphertext", SyncBase64.encode(ByteArray(plaintextBytes + 16) { 0x7f }))
            .toString()
            .toByteArray(Charsets.UTF_8)
        val limit = AndroidSyncSecretProtector.maxEnvelopeBytes(plaintextBytes)
        assertTrue("envelope ${envelope.size} > limit $limit", envelope.size <= limit)
    }
}
