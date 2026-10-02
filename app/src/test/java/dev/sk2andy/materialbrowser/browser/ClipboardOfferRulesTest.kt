package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardOfferRulesTest {
    private val now = 1_000_000L

    private fun clip(
        hasText: Boolean = true,
        linkConfidence: Float? = 0.9f,
        copiedAt: Long = now - 1_000L,
    ) = ClipboardDescription(hasText, linkConfidence, copiedAt)

    @Test
    fun `a fresh classified link is offered as a link`() {
        assertEquals(ClipboardOffer.Link, ClipboardOfferRules.offer(clip(), now, null))
    }

    @Test
    fun `unclassified or unlikely text is offered for pasting`() {
        assertEquals(
            ClipboardOffer.Text,
            ClipboardOfferRules.offer(clip(linkConfidence = null), now, null),
        )
        assertEquals(
            ClipboardOffer.Text,
            ClipboardOfferRules.offer(clip(linkConfidence = 0.1f), now, null),
        )
    }

    @Test
    fun `nothing is offered without text, for an old clip or for the clip already used`() {
        assertNull(ClipboardOfferRules.offer(null, now, null))
        assertNull(ClipboardOfferRules.offer(clip(hasText = false), now, null))
        assertNull(
            ClipboardOfferRules.offer(
                clip(copiedAt = now - ClipboardOfferRules.FRESH_MILLIS - 1),
                now,
                null,
            ),
        )
        assertNull(ClipboardOfferRules.offer(clip(copiedAt = now + 1), now, null))
        assertNull(ClipboardOfferRules.offer(clip(), now, usedCopiedAtElapsedMillis = now - 1_000L))
    }

    @Test
    fun `a tap opens a link and pastes anything else on one line`() {
        assertEquals(
            ClipboardAction.Open("north-guide.ru/routes"),
            ClipboardOfferRules.action(ClipboardOffer.Link, "  north-guide.ru/routes \n"),
        )
        assertEquals(
            ClipboardAction.Paste("javascript:alert(1)"),
            ClipboardOfferRules.action(ClipboardOffer.Link, "javascript:alert(1)"),
        )
        assertEquals(
            ClipboardAction.Paste("ice on baikal"),
            ClipboardOfferRules.action(ClipboardOffer.Text, "ice\non baikal"),
        )
        assertNull(ClipboardOfferRules.action(ClipboardOffer.Text, "   "))
    }
}
