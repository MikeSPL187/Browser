package dev.sk2andy.materialbrowser.shared.settings

import dev.sk2andy.materialbrowser.ui.SettingsDestination

/**
 * How deep a setting sits (docs/vola/tech-plan.md, section 2): «Main» on its page (up to seven
 * rows), «Advanced» folded away, «Developer» only in debug and preview builds.
 */
enum class SettingLevel {
    Main,
    Advanced,
    Developer,
}

/**
 * One setting, described once: where it lives and how deep. The interface and the settings
 * search are built from these descriptions. [key] is stable: the store key or test tag.
 */
data class SettingSpec(
    val key: String,
    val destination: SettingsDestination,
    val level: SettingLevel = SettingLevel.Main,
)

/** A setting or a settings page as search sees it, in the interface language. */
data class SettingSearchCandidate(
    val key: String,
    val destination: SettingsDestination,
    val title: String,
    val summary: String?,
    /** The page it is on; empty for a page itself. */
    val page: String,
)

/**
 * Search over the settings' own words: every word of the query must appear in the title, the
 * description or the page name. Titles that start with the query come first, then titles that
 * hold every word, then the rest, each in the registry's order.
 */
object SettingsSearchRules {
    const val MAX_RESULTS = 30

    fun search(
        query: String,
        candidates: List<SettingSearchCandidate>,
    ): List<SettingSearchCandidate> {
        val normalizedQuery = normalize(query)
        val words = normalizedQuery.split(' ').filter(String::isNotEmpty)
        if (words.isEmpty()) return emptyList()
        return candidates
            .mapNotNull { candidate ->
                val title = normalize(candidate.title)
                val everything = listOf(
                    title,
                    normalize(candidate.summary.orEmpty()),
                    normalize(candidate.page),
                ).joinToString(" ")
                if (words.any { word -> word !in everything }) return@mapNotNull null
                val score = when {
                    title.startsWith(normalizedQuery) -> TITLE_PREFIX
                    words.all { word -> word in title } -> TITLE_WORDS
                    else -> ELSEWHERE
                }
                score to candidate
            }
            .sortedByDescending { (score, _) -> score }
            .map { (_, candidate) -> candidate }
            .distinctBy { candidate -> candidate.key }
            .take(MAX_RESULTS)
    }

    /** Case, «ё» and spacing do not matter. */
    fun normalize(text: String): String = text
        .lowercase()
        .replace('ё', 'е')
        .split(WHITESPACE)
        .filter(String::isNotEmpty)
        .joinToString(" ")

    private val WHITESPACE = Regex("\\s+")
    private const val TITLE_PREFIX = 2
    private const val TITLE_WORDS = 1
    private const val ELSEWHERE = 0
}
