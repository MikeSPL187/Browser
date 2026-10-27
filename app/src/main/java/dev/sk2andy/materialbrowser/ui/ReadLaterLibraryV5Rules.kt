package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.reader.ReaderSnapshot
import java.net.URI
import java.util.Locale
import kotlin.math.roundToInt

/** Pure rules of the v5 «Read later» segment (canvas «History»), for unit tests. */
internal object ReadLaterLibraryV5Rules {
    /** The articles [query] finds by title, site or address, newest saved first. */
    fun shown(snapshots: List<ReaderSnapshot>, query: String): List<ReaderSnapshot> {
        val needle = query.trim().lowercase(Locale.ROOT)
        return snapshots
            .filter { snapshot ->
                needle.isEmpty() || with(snapshot.document) {
                    listOf(title, siteName, sourceUrl).any { it.lowercase(Locale.ROOT).contains(needle) }
                }
            }
            .sortedByDescending(ReaderSnapshot::savedAtMillis)
    }

    /** How much of the article is read, in whole percent; null before the first line and once read through. */
    fun readPercent(progress: Float): Int? =
        (progress.coerceIn(0f, 1f) * 100f).roundToInt().takeIf { it in 1..99 }

    /** The host an article came from, without «www.»; the address itself when it has none. */
    fun host(sourceUrl: String): String =
        runCatching { URI(sourceUrl).host?.removePrefix("www.") }.getOrNull()?.takeIf(String::isNotBlank) ?: sourceUrl
}
