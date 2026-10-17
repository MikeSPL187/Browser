package dev.sk2andy.materialbrowser.browser.translation

/** Engine-free decisions of on-device translation, so the UI and the tests share them. */
internal object OnDeviceTranslationRules {
    /**
     * The pair to offer for the page: from its detected language to the language the user reads
     * (the engine's guess first, then the app languages in order). Null when the page language is
     * unknown or unsupported, or already one the user reads.
     */
    fun suggestedPair(
        state: PageTranslationState,
        appLanguageTags: List<String>,
        supported: TranslationLanguages? = null,
    ): TranslationPair? {
        val from = state.pageLanguage?.takeIf { state.pageLanguageSupported && it.isNotBlank() } ?: return null
        val readable = listOfNotNull(state.userLanguage) + appLanguageTags
        if (readable.any { sameLanguage(it, from) }) return null
        val to = readable.firstNotNullOfOrNull { tag ->
            if (supported == null) primaryTag(tag) else matchSupported(tag, supported.to)
        } ?: return null
        return TranslationPair(from, to)
    }

    /** Whether to show the offer: an untranslated page with a pair, on a site not set to «never». */
    fun shouldOffer(
        state: PageTranslationState,
        pair: TranslationPair?,
        neverTranslateSite: Boolean,
    ): Boolean = pair != null && !neverTranslateSite && state.requested == null && !state.isTranslated

    /**
     * The tag in [languages] for [tag]: the same tag, else the same script, else the same base
     * language (`pt-BR` → `pt`). Null when the engine has no model for it.
     */
    fun matchSupported(tag: String, languages: List<TranslationLanguage>): String? {
        val wanted = Tag.parse(tag) ?: return null
        val parsed = languages.mapNotNull { language -> Tag.parse(language.code)?.let { it to language.code } }
        return parsed.firstOrNull { (candidate, _) -> candidate == wanted }?.second
            ?: parsed.firstOrNull { (candidate, _) ->
                candidate.language == wanted.language && candidate.script != null && candidate.script == wanted.script
            }?.second
            ?: parsed.firstOrNull { (candidate, _) ->
                candidate.language == wanted.language && (candidate.script == null || wanted.script == null)
            }?.second
    }

    /**
     * Whether two tags name a language the same reader reads: the same base language and, when both
     * give one, the same script (`zh-Hans` and `zh-Hant` differ, `en-GB` and `en` do not).
     */
    fun sameLanguage(first: String, second: String): Boolean {
        val a = Tag.parse(first) ?: return false
        val b = Tag.parse(second) ?: return false
        return a.language == b.language && (a.script == null || b.script == null || a.script == b.script)
    }

    /**
     * The site of an engine origin (`https://example.com^privateBrowsingId=1&…`): the part before
     * `^` is the address and the part after it the origin's attributes. Null for a private tab's
     * origin, which a settings list must not show, and for one without a host.
     */
    fun neverTranslateSite(origin: String): NeverTranslateSite? {
        val address = origin.substringBefore('^')
        val attributes = origin.substringAfter('^', missingDelimiterValue = "").split('&')
        if (attributes.any { it.startsWith("privateBrowsingId=") && it != "privateBrowsingId=0" }) return null
        val authority = address.substringAfter("://", missingDelimiterValue = "").substringBefore('/')
        val host = (if (authority.startsWith('[')) authority.substringBefore(']') + "]" else authority.substringBefore(':'))
            .takeIf { it.isNotBlank() && it != "]" } ?: return null
        return NeverTranslateSite(origin, host)
    }

    /** Whether translating needs a download first, so the UI should say how much. */
    fun needsDownload(downloadBytes: Long?): Boolean = downloadBytes != null && downloadBytes > 0

    private fun primaryTag(tag: String): String? = Tag.parse(tag)?.let { parsed ->
        if (parsed.script == null) parsed.language else "${parsed.language}-${parsed.script}"
    }

    /** The base language (lower case) and script (title case) of a BCP 47 tag; region dropped. */
    private data class Tag(val language: String, val script: String?) {
        companion object {
            fun parse(tag: String): Tag? {
                val parts = tag.trim().replace('_', '-').split('-').filter(String::isNotEmpty)
                val language = parts.firstOrNull()?.lowercase()?.takeIf { it.length in 2..3 && it.all(Char::isLetter) }
                    ?: return null
                val script = parts.drop(1).firstOrNull { it.length == 4 && it.all(Char::isLetter) }
                    ?.lowercase()?.replaceFirstChar(Char::uppercaseChar)
                return Tag(language, script)
            }
        }
    }
}
