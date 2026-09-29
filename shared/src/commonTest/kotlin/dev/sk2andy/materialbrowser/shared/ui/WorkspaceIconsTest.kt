package dev.sk2andy.materialbrowser.shared.ui

import dev.sk2andy.materialbrowser.browser.DEFAULT_PROFILE_EMOJI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkspaceIconsTest {
    @Test
    fun everyCatalogIconRoundTripsThroughItsEmojiKey() {
        assertTrue(WorkspaceIcons.FALLBACK_ID in WorkspaceIcons.ids)
        WorkspaceIcons.ids.forEach { id ->
            val emoji = assertNotNull(WorkspaceIcons.emojiFor(id), id)
            assertEquals(id, WorkspaceIcons.idFor(emoji))
        }
    }

    @Test
    fun defaultWorkspaceUsesTheHomeIcon() {
        assertEquals("home", WorkspaceIcons.idFor(DEFAULT_PROFILE_EMOJI))
    }

    @Test
    fun keysIgnoreSpacesAndPresentationSelectors() {
        assertEquals("work", WorkspaceIcons.idFor(" 💼 "))
        assertEquals("travel", WorkspaceIcons.idFor("✈️"))
        assertEquals("travel", WorkspaceIcons.idFor("✈"))
    }

    @Test
    fun unknownKeysFallBackToTheStarIcon() {
        assertEquals(WorkspaceIcons.FALLBACK_ID, WorkspaceIcons.idFor("🦄"))
        assertEquals(WorkspaceIcons.FALLBACK_ID, WorkspaceIcons.idFor(""))
    }
}
