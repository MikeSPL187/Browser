package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.capsule.CapsuleIconMode
import dev.sk2andy.materialbrowser.capsule.SiteCapsule
import dev.sk2andy.materialbrowser.capsule.SiteCapsuleDraft
import dev.sk2andy.materialbrowser.capsule.SiteCapsuleRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteCapsuleLibraryTest {
    private var stored = listOf<SiteCapsule>()
    private val rendered = mutableMapOf<String, String>()
    private val sources = mutableMapOf<String, String>()
    private val customs = mutableMapOf<String, String>()
    private var customSaveWorks = true
    private val pinned = mutableSetOf<String>()
    private var pinningSupported = true
    private val pinRequests = mutableListOf<String>()
    private val updates = mutableListOf<Pair<String, String>>()
    private val disabled = mutableListOf<String>()
    private var nextId = 0

    private val capsules = mutableListOf<SiteCapsule>()
    private val library = SiteCapsuleLibrary(
        capsules = capsules,
        store = object : SiteCapsuleLibrary.Store {
            override fun load() = stored
            override fun save(capsules: List<SiteCapsule>) {
                stored = capsules.toList()
            }
        },
        icons = object : SiteCapsuleLibrary.Icons<String> {
            override fun load(capsuleId: String) = rendered[capsuleId]
            override fun loadSource(capsuleId: String) = sources[capsuleId]
            override fun loadCustom(capsuleId: String) = customs[capsuleId]
            override fun save(capsuleId: String, icon: String): Boolean {
                rendered[capsuleId] = icon
                return true
            }
            override fun saveSource(capsuleId: String, icon: String): Boolean {
                sources[capsuleId] = icon
                return true
            }
            override fun saveCustom(capsuleId: String, icon: String): Boolean {
                if (!customSaveWorks) return false
                customs[capsuleId] = icon
                return true
            }
            override fun delete(capsuleId: String) {
                rendered -= capsuleId
                sources -= capsuleId
                customs -= capsuleId
            }
            override fun cleanup(knownCapsuleIds: Set<String>) {
                rendered.keys.retainAll(knownCapsuleIds)
            }
        },
        shortcuts = object : SiteCapsuleLibrary.Shortcuts<String> {
            override fun isPinningSupported() = pinningSupported
            override fun requestPin(capsule: SiteCapsule, icon: String): Boolean {
                pinRequests += capsule.id
                pinned += capsule.id
                return true
            }
            override fun update(capsule: SiteCapsule, icon: String): Boolean {
                updates += capsule.id to icon
                return true
            }
            override fun isPinned(capsule: SiteCapsule) = capsule.id in pinned
            override fun disable(capsule: SiteCapsule) {
                disabled += capsule.id
            }
            override fun reportUsed(capsule: SiteCapsule) = Unit
        },
        renderIcon = { name, _, _, favicon, customIcon -> "$name|${favicon ?: "-"}|${customIcon ?: "-"}" },
        sameImage = { a, b -> a == b },
        newId = { "capsule-%032d".format(++nextId) },
        now = { 1_000L },
    )

    private fun draft(id: String? = null, profileId: String = "home", iconMode: CapsuleIconMode = CapsuleIconMode.Favicon) =
        SiteCapsuleDraft(id = id, name = "Mail", startUrl = "https://mail.example", profileId = profileId, iconMode = iconMode)

    private fun capsule(id: String, profileId: String = "home") = SiteCapsule(
        id = id,
        name = "Docs",
        startUrl = "https://docs.example/",
        profileId = profileId,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    @Test
    fun aNewCapsuleIsSavedWithAFaviconIconAndAPinRequest() {
        val result = library.upsert(draft(), isProfileValid = true, multiProfileSupported = true, sourceFavicon = "fav")

        assertEquals(CapsuleSaveResult.PinRequested, result)
        val saved = capsules.single()
        assertEquals(listOf(saved), stored)
        assertEquals(CapsuleIconMode.Favicon, saved.iconMode)
        assertEquals("Mail|fav|-", rendered[saved.id])
        assertEquals("fav", sources[saved.id])
    }

    @Test
    fun withoutAFaviconTheIconFallsBackToTheEmoji() {
        library.upsert(draft(), isProfileValid = true, multiProfileSupported = true)

        assertEquals(CapsuleIconMode.ProfileFallback, capsules.single().iconMode)
    }

    @Test
    fun editingAPinnedCapsuleUpdatesItsShortcut() {
        library.upsert(draft(), isProfileValid = true, multiProfileSupported = true, sourceFavicon = "fav")
        val id = capsules.single().id

        val result = library.upsert(
            draft(id = id, iconMode = CapsuleIconMode.Custom),
            isProfileValid = true,
            multiProfileSupported = true,
            customIcon = "photo",
        )

        assertEquals(CapsuleSaveResult.Updated, result)
        assertEquals(CapsuleIconMode.Custom, capsules.single().iconMode)
        assertEquals(listOf(id to "Mail|-|photo"), updates)
        assertEquals(listOf(id), pinRequests)
    }

    @Test
    fun theLimitAndAnInvalidWorkspaceSaveNothing() {
        capsules += (1..SiteCapsuleRules.MAX_CAPSULES).map { capsule("existing-%029d".format(it)) }
        assertEquals(
            CapsuleSaveResult.LimitReached,
            library.upsert(draft(), isProfileValid = false, multiProfileSupported = true),
        )
        capsules.clear()

        assertEquals(
            CapsuleSaveResult.Invalid,
            library.upsert(draft(), isProfileValid = false, multiProfileSupported = true),
        )
        assertTrue(capsules.isEmpty())
    }

    @Test
    fun aCustomIconThatCannotBeWrittenStopsTheSave() {
        customSaveWorks = false

        val result = library.upsert(
            draft(iconMode = CapsuleIconMode.Custom),
            isProfileValid = true,
            multiProfileSupported = true,
            customIcon = "photo",
        )

        assertEquals(CapsuleSaveResult.IconSaveFailed, result)
        assertTrue(capsules.isEmpty())
    }

    @Test
    fun withoutPinningTheCapsuleIsSavedAndSaysSo() {
        pinningSupported = false

        assertEquals(
            CapsuleSaveResult.PinningUnsupported,
            library.upsert(draft(), isProfileValid = true, multiProfileSupported = true),
        )
        assertEquals(1, capsules.size)
    }

    @Test
    fun removingDropsTheIconsAndDisablesTheShortcut() {
        library.upsert(draft(), isProfileValid = true, multiProfileSupported = true, sourceFavicon = "fav")
        val id = capsules.single().id

        assertTrue(library.remove(id))

        assertTrue(capsules.isEmpty())
        assertTrue(stored.isEmpty())
        assertNull(rendered[id])
        assertEquals(listOf(id), disabled)
        assertFalse(library.remove(id))
    }

    @Test
    fun aDeletedWorkspacesCapsulesMoveToTheFallbackWithAFreshShortcutIcon() {
        val docs = "docs-%032d".format(1)
        val notes = "notes-%031d".format(2)
        capsules += capsule(docs, profileId = "work")
        capsules += capsule(notes, profileId = "work")
        rendered[docs] = "old-docs-icon"

        library.reassign("work", "home", excludedCapsuleId = notes)

        assertEquals("home", capsules.first { it.id == docs }.profileId)
        assertEquals("work", capsules.first { it.id == notes }.profileId)
        assertEquals(listOf(docs to "old-docs-icon"), updates)
    }

    @Test
    fun restoringDropsCapsulesOfMissingWorkspacesAndTheirIcons() {
        val kept = "kept-%032d".format(1)
        val orphan = "orphan-%030d".format(2)
        stored = listOf(capsule(kept, profileId = "home"), capsule(orphan, profileId = "gone"))
        rendered[orphan] = "icon"

        library.restore(setOf("home"))

        assertEquals(listOf(kept), capsules.map(SiteCapsule::id))
        assertEquals(listOf(kept), stored.map(SiteCapsule::id))
        assertFalse(orphan in rendered)
    }
}
