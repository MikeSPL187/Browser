package dev.sk2andy.materialbrowser.browser.gecko

/**
 * GeckoView's auth prompt has no realm field: Gecko writes it into the prompt's message, a whole
 * sentence in Gecko's own language («http://host is requesting your username and password. The
 * site says: “Realm”»). The sign-in sheet names the host itself, so it takes only the quoted realm;
 * a message without one (Gecko's sentence for a challenge with no realm) gives none.
 */
internal object GeckoAuthPromptRealm {
    /** Closing quote at the end of the message → the opening quotes Gecko's locales pair with it. */
    private val openingQuotes = mapOf(
        '”' to charArrayOf('“', '„'),
        '“' to charArrayOf('„'),
        '»' to charArrayOf('«'),
        '«' to charArrayOf('»'),
        '"' to charArrayOf('"'),
        '」' to charArrayOf('「'),
    )

    fun fromMessage(message: String?): String? {
        val text = message?.trim()?.trimEnd('.', '。')?.trimEnd() ?: return null
        val closing = text.lastOrNull() ?: return null
        val openers = openingQuotes[closing] ?: return null
        // The host before the realm is a URL, so the first opening quote starts the realm.
        val start = text.indexOfAny(openers)
        if (start < 0 || start >= text.length - 1) return null
        return text.substring(start + 1, text.length - 1).trim().takeIf(String::isNotEmpty)
    }
}
