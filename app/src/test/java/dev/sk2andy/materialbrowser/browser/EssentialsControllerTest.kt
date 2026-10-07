package dev.sk2andy.materialbrowser.browser

import android.graphics.Bitmap
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.data.EssentialsPersistence
import dev.sk2andy.materialbrowser.data.EssentialsRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EssentialsControllerTest {
    private val mail = EssentialEntry("https://mail.example.com/", "Mail")
    private val docs = EssentialEntry("https://docs.example.com/", "Docs")

    @Test
    fun `first run copies the favorites seed to every workspace and saves it`() {
        val store = FakeStore(saved = null)
        val icons = FakeIcons()
        val controller = controller(store, icons, profiles = listOf(DEFAULT_PROFILE_ID, "work"), seed = listOf(mail, docs))

        controller.restore()

        assertEquals(listOf(mail, docs), controller.entriesFor(DEFAULT_PROFILE_ID))
        assertEquals(listOf(mail, docs), controller.entriesFor("work"))
        assertEquals(setOf(DEFAULT_PROFILE_ID, "work"), store.saved?.keys)
        assertEquals(listOf(listOf(mail.url, docs.url)), icons.copied)
    }

    @Test
    fun `saved lists are not migrated again`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to emptyList()))
        val icons = FakeIcons()
        val controller = controller(store, icons, seed = listOf(mail))

        controller.restore()

        assertEquals(emptyList<EssentialEntry>(), controller.entriesFor(DEFAULT_PROFILE_ID))
        assertTrue(icons.copied.isEmpty())
        assertEquals(0, store.saveCount)
    }

    @Test
    fun `a new workspace reads a copy of Personal and keeps it once shown`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to listOf(mail)))
        val controller = controller(store, profiles = listOf(DEFAULT_PROFILE_ID, "new"))
        controller.restore()

        assertEquals(listOf(mail), controller.entriesFor("new"))
        controller.materialize("new")
        controller.add(DEFAULT_PROFILE_ID, docs.url, docs.title, icon = null)

        assertEquals(listOf(mail), controller.entriesFor("new"))
        assertEquals(listOf(mail), store.saved?.get("new"))
    }

    @Test
    fun `remove offers undo that puts the entry back where it was`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to listOf(mail, docs)))
        val controller = controller(store)
        controller.restore()

        assertTrue(controller.remove(DEFAULT_PROFILE_ID, mail.id))
        val removal = controller.removal!!
        assertEquals(listOf(docs), controller.entriesFor(DEFAULT_PROFILE_ID))
        assertEquals(0, removal.index)

        assertTrue(controller.undoRemoval(removal))
        assertEquals(listOf(mail, docs), controller.entriesFor(DEFAULT_PROFILE_ID))
        assertEquals(listOf(mail, docs), store.saved?.get(DEFAULT_PROFILE_ID))
        assertNull(controller.removal)
        assertFalse(controller.undoRemoval(removal))
    }

    @Test
    fun `filling the freed cell ends the pending undo`() {
        val full = (1..EssentialsRules.MAX_ENTRIES).map { EssentialEntry("https://s$it.example.com/", "S$it") }
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to full, "work" to listOf(docs)))
        val icons = FakeIcons()
        val controller = controller(store, icons, profiles = listOf(DEFAULT_PROFILE_ID, "work"))
        controller.restore()

        controller.remove(DEFAULT_PROFILE_ID, full.first().id)
        val removal = controller.removal!!
        controller.add("work", mail.url, mail.title, icon = null)
        assertEquals(removal, controller.removal)

        controller.add(DEFAULT_PROFILE_ID, mail.url, mail.title, icon = null)

        assertNull(controller.removal)
        assertFalse(full.first().url in icons.pruned.last())
        assertFalse(controller.undoRemoval(removal))
        assertEquals(full.drop(1) + mail, controller.entriesFor(DEFAULT_PROFILE_ID))
    }

    @Test
    fun `an undo that can no longer apply still ends and prunes icons`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to listOf(mail, docs)))
        val icons = FakeIcons()
        val controller = controller(store, icons)
        controller.restore()
        controller.remove(DEFAULT_PROFILE_ID, mail.id)
        val removal = controller.removal!!
        controller.add(DEFAULT_PROFILE_ID, mail.url, mail.title, icon = null)

        assertFalse(controller.undoRemoval(removal))

        assertNull(controller.removal)
        assertEquals(setOf(docs.url, mail.url), icons.pruned.last())
    }

    @Test
    fun `a dismissed removal is final and prunes its icon`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to listOf(mail, docs)))
        val icons = FakeIcons()
        val controller = controller(store, icons)
        controller.restore()

        controller.remove(DEFAULT_PROFILE_ID, mail.id)
        controller.dismissRemoval(controller.removal!!)

        assertNull(controller.removal)
        assertEquals(setOf(docs.url), icons.pruned.last())
    }

    @Test
    fun `add captures the tab icon and refuses duplicates`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to listOf(mail)))
        val icons = FakeIcons()
        val controller = controller(store, icons)
        controller.restore()

        val added = controller.add(DEFAULT_PROFILE_ID, docs.url, docs.title, icon = null)
        val duplicate = controller.add(DEFAULT_PROFILE_ID, "https://MAIL.example.com", "", icon = null)

        assertTrue(added is EssentialsRules.AddResult.Added)
        assertEquals(EssentialsRules.AddResult.Duplicate, duplicate)
        assertEquals(listOf(docs.url), icons.captured)
        assertEquals(listOf(mail, docs), store.saved?.get(DEFAULT_PROFILE_ID))
    }

    @Test
    fun `move reorders and a deleted workspace forgets its list`() {
        val store = FakeStore(saved = mapOf(DEFAULT_PROFILE_ID to listOf(mail, docs), "work" to listOf(docs)))
        val controller = controller(store, profiles = listOf(DEFAULT_PROFILE_ID, "work"))
        controller.restore()

        assertTrue(controller.move(DEFAULT_PROFILE_ID, mail.id, 1))
        assertFalse(controller.move(DEFAULT_PROFILE_ID, mail.id, 1))
        controller.forgetProfile("work")

        assertEquals(listOf(docs, mail), store.saved?.get(DEFAULT_PROFILE_ID))
        assertFalse(store.saved!!.containsKey("work"))
    }

    private fun controller(
        store: FakeStore,
        icons: FakeIcons = FakeIcons(),
        profiles: List<String> = listOf(DEFAULT_PROFILE_ID),
        seed: List<EssentialEntry> = emptyList(),
    ) = EssentialsController(
        store = store,
        icons = icons,
        host = object : EssentialsController.Host {
            override val profileIds: List<String> = profiles

            override fun migrationSeed(): List<EssentialEntry> = seed

            override fun post(action: () -> Unit) = action()
        },
    )

    private class FakeStore(var saved: Map<String, List<EssentialEntry>>?) : EssentialsPersistence {
        var saveCount = 0

        override fun load(): Map<String, List<EssentialEntry>>? = saved

        override fun save(entries: Map<String, List<EssentialEntry>>) {
            saveCount++
            saved = entries
        }
    }

    private class FakeIcons : EssentialIconSource {
        val captured = mutableListOf<String>()
        val copied = mutableListOf<List<String>>()
        val pruned = mutableListOf<Set<String>>()

        override fun capture(url: String, bitmap: Bitmap?) {
            captured += url
        }

        override fun copyFromFavorites(urls: List<String>, onDone: () -> Unit) {
            copied += urls
            onDone()
        }

        override fun load(urls: List<String>, onLoaded: (Map<String, Bitmap>) -> Unit) = onLoaded(emptyMap())

        override fun prune(validUrls: Set<String>) {
            pruned += validUrls
        }
    }
}
