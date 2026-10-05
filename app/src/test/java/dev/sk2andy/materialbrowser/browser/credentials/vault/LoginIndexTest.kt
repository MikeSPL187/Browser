package dev.sk2andy.materialbrowser.browser.credentials.vault

import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginHint
import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginIndexTest {
    private val random = SecureRandom()
    private val phrase = "acid acorn acre acts afar affix aged agent agile aging agony ahead"

    @Test
    fun `the index follows every change and keeps no password`() {
        val vaultStorage = MemoryStorage()
        val indexStorage = MemoryStorage()
        val index = LoginIndex(indexStorage, FakeIndexKey())
        val vault = LocalCredentialVault(vaultStorage, random, newId = { "id-${vaultStorage.writes}" }, index = index)

        assertEquals(VaultOpenResult.Opened, vault.create(listOf(recovery())))
        vault.save(VaultLoginDraft(ORIGIN, null, null, "alice", "hunter2"), nowMillis = 10)
        vault.save(VaultLoginDraft(ORIGIN, null, null, "bob", "swordfish"), nowMillis = 11)
        assertFalse("a password is in the index", indexStorage.bytes!!.containsText("hunter2"))
        assertFalse(indexStorage.bytes!!.containsText("swordfish"))

        vault.lock()
        assertEquals(listOf("alice", "bob"), vault.loginHints().map(VaultLoginHint::username))

        assertEquals(VaultOpenResult.Opened, vault.unlock(recovery()))
        val bob = vault.loginsFor(ORIGIN).first { it.username == "bob" }
        vault.markUsed(bob.id, nowMillis = 20)
        vault.delete(vault.loginsFor(ORIGIN).first { it.username == "alice" }.id)
        vault.lock()

        val fresh = LoginIndex(indexStorage, FakeIndexKey())
        assertEquals(listOf("bob"), fresh.hints().map(VaultLoginHint::username))
        assertEquals(20L, fresh.hints().single().lastUsedAtMillis)
    }

    @Test
    fun `no vault means no hints, and erasing the vault erases the index`() {
        val vaultStorage = MemoryStorage()
        val indexStorage = MemoryStorage()
        val key = FakeIndexKey()
        val vault = LocalCredentialVault(vaultStorage, random, index = LoginIndex(indexStorage, key))
        assertEquals(emptyList<VaultLoginHint>(), vault.loginHints())

        vault.create(listOf(recovery()))
        vault.save(VaultLoginDraft(ORIGIN, null, null, "alice", "hunter2"), 1)
        assertTrue(vault.destroy())
        assertNull(indexStorage.bytes)
        assertTrue(key.deleted)
        assertEquals(emptyList<VaultLoginHint>(), vault.loginHints())
    }

    @Test
    fun `a refusing key, a changed file or a bad hint give no hints`() {
        val storage = MemoryStorage()
        val key = FakeIndexKey()
        val index = LoginIndex(storage, key)
        assertTrue(index.replace(listOf(hint("alice"))))

        key.refuse = true
        assertEquals(emptyList<VaultLoginHint>(), LoginIndex(storage, key).hints())
        key.refuse = false

        storage.bytes = storage.bytes!!.also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertEquals(emptyList<VaultLoginHint>(), LoginIndex(storage, key).hints())

        assertNull(LoginIndexCodec.decode(LoginIndexCodec.encode(listOf(hint("alice").copy(origin = "http://example.com")))))
        assertNull(LoginIndexCodec.decode(LoginIndexCodec.encode(listOf(hint("a"), hint("b").copy(id = "id-a")))))
        assertEquals(listOf(hint("alice")), LoginIndexCodec.decode(LoginIndexCodec.encode(listOf(hint("alice")))))
    }

    private fun hint(username: String) = VaultLoginHint("id-$username", ORIGIN, null, null, username, null)

    private fun recovery() = RecoveryPhraseKeyWrapper(phrase, random, iterations = 1_000)

    private fun ByteArray.containsText(text: String): Boolean =
        toString(Charsets.ISO_8859_1).contains(text) || toString(Charsets.UTF_8).contains(text)

    private class MemoryStorage : VaultStorage {
        var bytes: ByteArray? = null
        var writes = 0

        override fun exists() = bytes != null

        override fun read() = bytes?.copyOf()

        override fun write(bytes: ByteArray): Boolean {
            writes++
            this.bytes = bytes.copyOf()
            return true
        }

        override fun delete(): Boolean {
            bytes = null
            return true
        }
    }

    /** Stands in for the Keystore key: a fixed AES key that can refuse, like a locked phone. */
    private class FakeIndexKey : LoginIndexKey {
        private val key = ByteArray(VaultCrypto.KEY_BYTES) { 3 }
        private val random = SecureRandom()
        var refuse = false
        var deleted = false

        override fun seal(plaintext: ByteArray): ByteArray? =
            if (refuse) null else VaultCrypto.seal(key, plaintext, AAD, random)

        override fun open(sealed: ByteArray): ByteArray? =
            if (refuse) null else VaultCrypto.open(key, sealed, AAD)

        override fun delete() {
            deleted = true
        }
    }

    private companion object {
        const val ORIGIN = "https://example.com"
        val AAD = "test-index".toByteArray()
    }
}
