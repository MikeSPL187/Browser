package dev.sk2andy.materialbrowser.browser

/**
 * What the clipboard chip next to the address field offers.
 *
 * Only the clip's description is read before a tap: on Android 12 and later reading the clip itself
 * shows a system "pasted from your clipboard" notice, which must follow the user's own action.
 */
enum class ClipboardOffer {
    /** The system classified the copied text as a link: the chip opens it. */
    Link,

    /** Text without a link classification: the chip pastes it into the field. */
    Text,
}

/** What [ClipboardOfferRules] needs from a `ClipDescription`, without Android types. */
data class ClipboardDescription(
    val hasText: Boolean,
    /** The system's confidence that the text is a URL, or null before classification. */
    val linkConfidence: Float?,
    /** When the clip was copied, on the `SystemClock.elapsedRealtime` clock. */
    val copiedAtElapsedMillis: Long,
)

object ClipboardOfferRules {
    /** Copied longer ago than this, a clip is old news and is not offered. */
    const val FRESH_MILLIS = 5 * 60 * 1000L

    /** The confidence above which a classified clip counts as a link. */
    const val LINK_CONFIDENCE = 0.5f

    fun offer(
        description: ClipboardDescription?,
        nowElapsedMillis: Long,
        usedCopiedAtElapsedMillis: Long?,
    ): ClipboardOffer? {
        if (description == null || !description.hasText) return null
        if (description.copiedAtElapsedMillis == usedCopiedAtElapsedMillis) return null
        val age = nowElapsedMillis - description.copiedAtElapsedMillis
        if (age !in 0..FRESH_MILLIS) return null
        val confidence = description.linkConfidence
        return if (confidence != null && confidence >= LINK_CONFIDENCE) {
            ClipboardOffer.Link
        } else {
            ClipboardOffer.Text
        }
    }

    /** What a tap on the chip does with the clip's text, read only then. */
    fun action(offer: ClipboardOffer, text: String): ClipboardAction? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return if (offer == ClipboardOffer.Link && !AddressResolver.isSearchQuery(trimmed)) {
            ClipboardAction.Open(trimmed)
        } else {
            ClipboardAction.Paste(trimmed.replace(LINE_BREAKS, " "))
        }
    }

    private val LINE_BREAKS = Regex("\\s*[\\r\\n]+\\s*")
}

sealed interface ClipboardAction {
    data class Open(val address: String) : ClipboardAction

    data class Paste(val text: String) : ClipboardAction
}
