package dev.sk2andy.materialbrowser.browser.credentials.vault

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import dev.sk2andy.materialbrowser.shared.credentials.VaultSaveResult
import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCredentialVaultTest {
    private val random = SecureRandom()
    private val phrase = "acid acorn acre acts afar affix aged agent agile aging agony ahead"

    @Test
    fun `a created vault keeps its logins sealed and opens again with either way in`() {
        val storage = MemoryStorage()
        val device = FakeDeviceWrapper()
        val vault = LocalCredentialVault(storage, random) { "id-${storage.writes}" }

        assertEquals(VaultOpenResult.Opened, vault.create(listOf(device, recovery())))
        val saved = vault.save(draft("hunter2"), nowMillis = 10)
        assertTrue(saved is VaultSaveResult.Added)
        assertFalse("the password is in the file in the clear", storage.bytes!!.containsText("hunter2"))
        assertFalse(storage.bytes!!.containsText("alice"))

        vault.lock()
        assertFalse(vault.isUnlocked)
        assertEquals(emptyList<VaultLogin>(), vault.loginsFor(ORIGIN))
        assertEquals(VaultSaveResult.Locked, vault.save(draft("other"), 11))

        val reopened = LocalCredentialVault(storage, random)
        assertEquals(VaultOpenResult.Opened, reopened.unlock(recovery()))
        assertEquals(listOf("hunter2"), reopened.loginsFor(ORIGIN).map(VaultLogin::password))
        reopened.lock()
        assertEquals(VaultOpenResult.Opened, reopened.unlock(device))
        assertEquals(1, reopened.allLogins().size)
    }

    @Test
    fun `wrong ways in, missing slots and changed files are refused`() {
        val storage = MemoryStorage()
        val vault = LocalCredentialVault(storage, random)
        assertEquals(VaultOpenResult.Missing, vault.unlock(recovery()))
        assertEquals(VaultOpenResult.Opened, vault.create(listOf(recovery())))
        assertEquals(VaultOpenResult.AlreadyExists, vault.create(listOf(recovery())))
        vault.save(draft("hunter2"), 1)
        vault.lock()

        assertEquals(VaultOpenResult.NoSlot, vault.unlock(FakeDeviceWrapper()))
        assertEquals(
            VaultOpenResult.Refused,
            vault.unlock(RecoveryPhraseKeyWrapper(phrase.replace("ahead", "aim"), random, iterations = 1_000)),
        )
        assertEquals(VaultOpenResult.NeedsAuthentication, vault.unlock(FakeDeviceWrapper(authenticated = false).asRecovery()))

        val original = storage.bytes!!.copyOf()
        storage.bytes = original.copyOf().also { it[it.size - 3] = (it[it.size - 3].toInt() xor 0x10).toByte() }
        assertEquals(VaultOpenResult.Refused, vault.unlock(recovery()))
        storage.bytes = byteArrayOf(1, 2, 3)
        assertEquals(VaultOpenResult.Unreadable, vault.unlock(recovery()))
        storage.bytes = original
        assertEquals(VaultOpenResult.Opened, vault.unlock(recovery()))
    }

    @Test
    fun `a write that fails leaves the logins as they were`() {
        val storage = MemoryStorage()
        val vault = LocalCredentialVault(storage, random)
        vault.create(listOf(recovery()))
        val first = (vault.save(draft("hunter2"), 1) as VaultSaveResult.Added).login

        storage.failWrites = true
        assertEquals(VaultSaveResult.Failed, vault.save(draft("changed"), 2))
        assertFalse(vault.markUsed(first.id, 3))
        assertFalse(vault.delete(first.id))
        assertEquals(listOf("hunter2"), vault.loginsFor(ORIGIN).map(VaultLogin::password))

        storage.failWrites = false
        assertTrue(vault.markUsed(first.id, 4))
        assertTrue(vault.delete(first.id))
        assertEquals(emptyList<VaultLogin>(), vault.allLogins())
        assertTrue(vault.destroy())
        assertFalse(storage.exists())
    }

    @Test
    fun `creating needs every way in to agree`() {
        val storage = MemoryStorage()
        val vault = LocalCredentialVault(storage, random)

        assertEquals(VaultOpenResult.NeedsAuthentication, vault.create(listOf(FakeDeviceWrapper(authenticated = false), recovery())))
        assertFalse(storage.exists())
        assertFalse(vault.isUnlocked)
    }

    @Test
    fun `edits keep the login and a recovered vault gets a new device key`() {
        val storage = MemoryStorage()
        val vault = LocalCredentialVault(storage, random)
        vault.create(listOf(FakeDeviceWrapper(), recovery()))
        val login = (vault.save(draft("hunter2"), 1) as VaultSaveResult.Added).login
        vault.save(VaultLoginDraft(ORIGIN, null, null, "bob", "pw"), 2)

        val edited = vault.update(login.id, VaultLoginDraft(ORIGIN, null, null, "alice", "changed"), 3)
        assertEquals("changed", (edited as VaultSaveResult.Updated).login.password)
        assertEquals(VaultSaveResult.Rejected, vault.update(login.id, VaultLoginDraft(ORIGIN, null, null, "bob", "x"), 4))
        assertEquals(VaultSaveResult.Rejected, vault.update("missing", draft("x"), 5))

        // The device key is lost: open with the phrase, then wrap a fresh device key.
        vault.lock()
        val newDevice = FakeDeviceWrapper(seed = 9)
        assertEquals(VaultOpenResult.Refused, vault.unlock(newDevice))
        assertEquals(VaultOpenResult.Opened, vault.unlock(recovery()))
        assertEquals(VaultOpenResult.Opened, vault.replaceSlot(newDevice))
        assertEquals(setOf(VaultSlotType.DEVICE, VaultSlotType.RECOVERY_PHRASE), vault.slotTypes())
        vault.lock()
        assertEquals(VaultOpenResult.Opened, vault.unlock(newDevice))
        assertEquals(VaultOpenResult.Refused, vault.unlock(FakeDeviceWrapper()))
        assertEquals(VaultOpenResult.Opened, vault.unlock(recovery()))
    }

    private fun recovery() = RecoveryPhraseKeyWrapper(phrase, random, iterations = 1_000)

    private fun draft(password: String) = VaultLoginDraft(ORIGIN, null, null, "alice", password)

    private fun ByteArray.containsText(text: String): Boolean =
        toString(Charsets.ISO_8859_1).contains(text) || toString(Charsets.UTF_8).contains(text)

    private class MemoryStorage : VaultStorage {
        var bytes: ByteArray? = null
        var writes = 0
        var failWrites = false

        override fun exists() = bytes != null

        override fun read() = bytes?.copyOf()

        override fun write(bytes: ByteArray): Boolean {
            if (failWrites) return false
            writes++
            this.bytes = bytes.copyOf()
            return true
        }

        override fun delete(): Boolean {
            bytes = null
            return true
        }
    }

    /** Stands in for the Keystore key: a fixed key that may demand the user first. */
    private class FakeDeviceWrapper(
        private val authenticated: Boolean = true,
        seed: Byte = 7,
    ) : VaultKeyWrapper {
        private val deviceKey = ByteArray(VaultCrypto.KEY_BYTES) { seed }
        private val random = SecureRandom()

        override val slotType = VaultSlotType.DEVICE

        override fun wrap(vaultKey: ByteArray): VaultKeyOutcome<ByteArray> =
            if (!authenticated) VaultKeyOutcome.NeedsAuthentication
            else VaultKeyOutcome.Done(VaultCrypto.seal(deviceKey, vaultKey, AAD, random))

        override fun unwrap(payload: ByteArray): VaultKeyOutcome<ByteArray> =
            if (!authenticated) VaultKeyOutcome.NeedsAuthentication
            else VaultCrypto.open(deviceKey, payload, AAD)?.let { VaultKeyOutcome.Done(it) } ?: VaultKeyOutcome.Refused

        /** The same refusal, presented on the recovery slot. */
        fun asRecovery(): VaultKeyWrapper = object : VaultKeyWrapper by this {
            override val slotType = VaultSlotType.RECOVERY_PHRASE
        }
    }

    private companion object {
        const val ORIGIN = "https://example.com"
        val AAD = "test-device".toByteArray()
    }
}
