package dev.sk2andy.materialbrowser.data.sync.webdav

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.browser.sync.webdav.AndroidWebDavSyncCrypto
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavAccount
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavBookmarkEntry
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavStoredAccount
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncFormat
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncMemory
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The WebDAV sync's secrets on a real Android (V27): the account and the memory go through the
 * Keystore and stay in `noBackupFilesDir`, and the phrase's key is the real Argon2id.
 */
@RunWith(AndroidJUnit4::class)
class AndroidWebDavSyncStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = AndroidWebDavSyncStore(context)
    private val phrase = "correct horse battery".toByteArray()

    @After
    fun tearDown() = store.clear()

    @Test
    fun theAccountAndMemoryRoundTripEncrypted() {
        val (header, key) = WebDavSyncFormat.newHeader(phrase, AndroidWebDavSyncCrypto)
        val account = WebDavAccount("https://cloud.example.com/", "cloud.example.com", "anna", "app-password-123", key, header, "A".repeat(22))
        val memory = WebDavSyncMemory(
            remembered = mapOf(WebDavBookmarkEntry("https://example.org/", "Example", "Work", 5).let { it.key to it }),
            sequence = 3,
        )

        assertTrue(store.saveAccount(WebDavStoredAccount(account, "profile-1")))
        assertTrue(store.saveMemory(memory))

        val loaded = requireNotNull(store.loadAccount())
        assertEquals("profile-1", loaded.scope)
        assertArrayEquals(key, loaded.account.key)
        assertEquals(header, loaded.account.header)
        assertEquals(memory, store.loadMemory())
        val files = context.noBackupFilesDir.listFiles { file: File -> file.name.startsWith("vola_webdav_") }.orEmpty()
        assertEquals(2, files.size)
        files.forEach { file -> assertFalse("${file.name} holds the password in the clear", file.readText().contains("app-password-123")) }

        store.clear()
        assertNull(store.loadAccount())
        assertNull(store.loadMemory())
    }

    @Test
    fun thePhrasesKeyIsArgon2idAndOpensOnlyItsHeader() {
        val (header, key) = WebDavSyncFormat.newHeader(phrase, AndroidWebDavSyncCrypto)

        assertArrayEquals(key, WebDavSyncFormat.deriveKey(phrase, header, AndroidWebDavSyncCrypto))
        assertTrue(WebDavSyncFormat.opens(header, key, AndroidWebDavSyncCrypto))
        val other = WebDavSyncFormat.deriveKey("another long phrase".toByteArray(), header, AndroidWebDavSyncCrypto)
        assertFalse(WebDavSyncFormat.opens(header, other, AndroidWebDavSyncCrypto))
    }
}
