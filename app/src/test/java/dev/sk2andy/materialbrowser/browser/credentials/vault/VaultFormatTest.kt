package dev.sk2andy.materialbrowser.browser.credentials.vault

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultFormatTest {
    private val random = SecureRandom()

    @Test
    fun `a seal opens only with its key and its additional data`() {
        val key = VaultCrypto.newKey(random)
        val sealed = VaultCrypto.seal(key, "secret".toByteArray(), "here".toByteArray(), random)

        assertArrayEquals("secret".toByteArray(), VaultCrypto.open(key, sealed, "here".toByteArray()))
        assertNull(VaultCrypto.open(key, sealed, "there".toByteArray()))
        assertNull(VaultCrypto.open(VaultCrypto.newKey(random), sealed, "here".toByteArray()))
        val flipped = sealed.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertNull(VaultCrypto.open(key, flipped, "here".toByteArray()))
        assertFalse(
            "two seals of the same text share a nonce",
            sealed.copyOf(VaultCrypto.NONCE_BYTES).contentEquals(
                VaultCrypto.seal(key, "secret".toByteArray(), "here".toByteArray(), random).copyOf(VaultCrypto.NONCE_BYTES),
            ),
        )
    }

    @Test
    fun `the envelope round-trips and refuses anything else`() {
        val slots = listOf(VaultSlot(VaultSlotType.DEVICE, ByteArray(60) { 1 }), VaultSlot(VaultSlotType.RECOVERY_PHRASE, ByteArray(80) { 2 }))
        val envelope = VaultEnvelope(slots, ByteArray(40) { 3 })

        val decoded = requireNotNull(VaultEnvelope.decode(envelope.encode()))
        assertEquals(listOf(VaultSlotType.DEVICE, VaultSlotType.RECOVERY_PHRASE), decoded.slots.map(VaultSlot::type))
        assertArrayEquals(envelope.body, decoded.body)
        assertArrayEquals(VaultEnvelope.header(slots), VaultEnvelope.header(decoded.slots))

        assertNull(VaultEnvelope.decode("NOTAVAULT".toByteArray() + envelope.encode().drop(9)))
        assertNull(VaultEnvelope.decode(envelope.encode().copyOf(20)))
        val twins = VaultEnvelope.header(slots.take(1)).also { it[10] = 2 } +
            byteArrayOf(VaultSlotType.DEVICE.toByte(), 0, 60) + ByteArray(60) + ByteArray(40)
        assertNull("two slots of one kind", VaultEnvelope.decode(twins))
    }

    @Test
    fun `the logins inside survive a round trip and a bad one spoils the file`() {
        val logins = listOf(
            login("1", "https://example.com", realm = null, lastUsed = null),
            login("2", "https://accounts.example.com:8443", realm = "Members", lastUsed = 77),
        )

        assertEquals(logins, VaultDocumentCodec.decode(VaultDocumentCodec.encode(logins)))
        assertEquals(emptyList<VaultLogin>(), VaultDocumentCodec.decode(VaultDocumentCodec.encode(emptyList())))
        assertNull(VaultDocumentCodec.decode(VaultDocumentCodec.encode(listOf(login("1", "http://example.com")))))
        assertNull(VaultDocumentCodec.decode(VaultDocumentCodec.encode(logins + logins.first())))
        assertNull(VaultDocumentCodec.decode(VaultDocumentCodec.encode(logins) + 0))
    }

    @Test
    fun `a two-factor key round-trips and a version 1 file still opens`() {
        val key = "otpauth://totp/?secret=JBSWY3DPEHPK3PXP&algorithm=SHA1&digits=6&period=30"
        val withKey = listOf(login("1", "https://example.com").copy(totp = key))
        assertEquals(withKey, VaultDocumentCodec.decode(VaultDocumentCodec.encode(withKey)))
        assertNull(VaultDocumentCodec.decode(VaultDocumentCodec.encode(listOf(login("1", "https://example.com").copy(totp = "otpauth://totp/?secret=bad")))))

        val versionOne = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(1)
                output.writeInt(1)
                output.writeUTF("1")
                output.writeUTF("https://example.com")
                output.writeBoolean(false)
                output.writeBoolean(false)
                output.writeUTF("alice")
                output.writeUTF("hunter2")
                output.writeLong(1)
                output.writeLong(2)
                output.writeLong(Long.MIN_VALUE)
                output.writeInt(0)
            }
        }.toByteArray()
        val opened = requireNotNull(VaultDocumentCodec.decode(versionOne)).single()
        assertEquals("hunter2", opened.password)
        assertNull(opened.totp)
    }

    @Test
    fun `the shipped word list is EFF's short list and a phrase is 12 of its words`() {
        val wordlist = requireNotNull(RecoveryWordlist.from(File("src/main/assets/${RecoveryWordlist.ASSET}").readBytes()))
        assertEquals(RecoveryWordlist.SIZE, wordlist.words.size)
        assertTrue("yo-yo" in wordlist)
        assertNull(RecoveryWordlist.from("acid\nacorn\n".toByteArray()))

        val phrase = RecoveryPhrase.generate(wordlist, random)
        assertEquals(RecoveryPhrase.WORD_COUNT, phrase.split(' ').size)
        assertEquals(phrase, RecoveryPhrase.normalize("  ${phrase.uppercase().replace(" ", ",\n ")} ", wordlist))
        // Pasted from a notes app: no-break, narrow and ideographic spaces, numbered lines.
        assertEquals(phrase, RecoveryPhrase.normalize(phrase.replace(" ", "\u00A0"), wordlist))
        assertEquals(phrase, RecoveryPhrase.normalize(phrase.replace(" ", "\u202F"), wordlist))
        assertEquals(phrase, RecoveryPhrase.normalize(phrase.replace(" ", "\u3000"), wordlist))
        val numbered = phrase.split(' ').mapIndexed { index, word -> "${index + 1}. $word" }.joinToString("\n")
        assertEquals(phrase, RecoveryPhrase.normalize(numbered, wordlist))
        assertEquals(phrase, RecoveryPhrase.normalize(phrase.split(' ').mapIndexed { index, word -> "${index + 1})$word" }.joinToString(" "), wordlist))
        assertNull(RecoveryPhrase.normalize(phrase.substringBeforeLast(' '), wordlist))
        assertNull(RecoveryPhrase.normalize(phrase.substringBeforeLast(' ') + " zzzzz", wordlist))
    }

    @Test
    fun `the recovery slot opens with the phrase only`() {
        val key = VaultCrypto.newKey(random)
        val wrapper = RecoveryPhraseKeyWrapper("acid acorn acre", random, iterations = 1_000)
        val payload = (wrapper.wrap(key) as VaultKeyOutcome.Done).value

        assertArrayEquals(key, (wrapper.unwrap(payload) as VaultKeyOutcome.Done).value)
        assertEquals(
            VaultKeyOutcome.Refused,
            RecoveryPhraseKeyWrapper("acid acorn acts", random, iterations = 1_000).unwrap(payload),
        )
        val tooCostly = payload.copyOf().also { bytes -> bytes[16] = 0x7f }
        assertEquals(VaultKeyOutcome.Refused, wrapper.unwrap(tooCostly))
        assertEquals(600_000, RecoveryPhraseKeyWrapper.DEFAULT_ITERATIONS)
    }

    private fun login(id: String, origin: String, realm: String? = null, lastUsed: Long? = null) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = realm,
        username = "alice",
        password = "pässwörd ✓",
        createdAtMillis = 1,
        updatedAtMillis = 2,
        lastUsedAtMillis = lastUsed,
        timesUsed = 3,
    )
}
