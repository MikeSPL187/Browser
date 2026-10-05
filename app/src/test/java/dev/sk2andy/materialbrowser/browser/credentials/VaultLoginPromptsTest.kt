package dev.sk2andy.materialbrowser.browser.credentials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultLoginPromptsTest {
    @Test
    fun `a new sheet dismisses the one before and every request is answered once`() {
        val answers = mutableListOf<Pair<Long, VaultLoginAnswer>>()
        val first = save(VaultLoginPrompts.nextRequestId())
        val second = save(VaultLoginPrompts.nextRequestId())

        VaultLoginPrompts.show(first) { answers += first.id to it }
        VaultLoginPrompts.show(second) { answers += second.id to it }
        assertEquals(listOf(first.id to VaultLoginAnswer.Dismiss), answers)
        assertEquals(second, VaultLoginPrompts.current)

        VaultLoginPrompts.answer(first.id, VaultLoginAnswer.Save)
        VaultLoginPrompts.answer(second.id, VaultLoginAnswer.Save)
        VaultLoginPrompts.answer(second.id, VaultLoginAnswer.Dismiss)
        assertEquals(listOf(first.id to VaultLoginAnswer.Dismiss, second.id to VaultLoginAnswer.Save), answers)
        assertNull(VaultLoginPrompts.current)
    }

    private fun save(id: Long) = VaultLoginRequest.Save(id, windowId = 1, site = "example.com", username = "alice", update = false)
}
