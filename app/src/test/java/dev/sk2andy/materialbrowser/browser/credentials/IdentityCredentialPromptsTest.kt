package dev.sk2andy.materialbrowser.browser.credentials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityCredentialPromptsTest {
    @Test
    fun `a new question dismisses the one before and each is answered once`() {
        val answers = mutableListOf<Pair<Long, Int?>>()
        val first = choice(IdentityCredentialPrompts.nextRequestId())
        val second = choice(IdentityCredentialPrompts.nextRequestId())

        IdentityCredentialPrompts.show(first) { answers += first.id to it }
        IdentityCredentialPrompts.show(second) { answers += second.id to it }
        assertEquals(listOf<Pair<Long, Int?>>(first.id to null), answers)
        assertEquals(second, IdentityCredentialPrompts.current)

        IdentityCredentialPrompts.answer(first.id, 0)
        IdentityCredentialPrompts.answer(second.id, 1)
        IdentityCredentialPrompts.answer(second.id, 0)
        assertEquals(listOf<Pair<Long, Int?>>(first.id to null, second.id to 1), answers)
        assertNull(IdentityCredentialPrompts.current)
    }

    @Test
    fun `only an attached window draws the questions and leaving answers its open one`() {
        val answers = mutableListOf<Int?>()
        assertFalse(IdentityCredentialPrompts.drawsFor(WINDOW))

        IdentityCredentialPrompts.attach(WINDOW)
        assertTrue(IdentityCredentialPrompts.drawsFor(WINDOW))
        IdentityCredentialPrompts.show(choice(IdentityCredentialPrompts.nextRequestId())) { answers += it }
        IdentityCredentialPrompts.detach(WINDOW)

        assertFalse(IdentityCredentialPrompts.drawsFor(WINDOW))
        assertEquals(listOf<Int?>(null), answers)
        assertNull(IdentityCredentialPrompts.current)
    }

    private fun choice(id: Long) = IdentityPromptRequest.Choice(id, windowId = WINDOW, title = "idp.example", options = listOf("a", "b"))

    private companion object {
        const val WINDOW = 7
    }
}
