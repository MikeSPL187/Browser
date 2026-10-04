package dev.sk2andy.materialbrowser.reader

interface ReaderLibraryDataSource {
    fun load(isPrivate: Boolean, onLoaded: (ReaderLibraryState) -> Unit)

    fun updateSettings(
        settings: ReaderSettings,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    )

    fun updateProgress(sourceUrl: String, progress: Float, isPrivate: Boolean)

    fun saveSnapshot(
        document: ReaderDocument,
        progress: Float,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    )

    fun deleteSnapshot(
        snapshotId: String,
        isPrivate: Boolean,
        onUpdated: (ReaderLibraryState) -> Unit,
    )
}

/**
 * Reading themes (board W-Reader): light, paper and dark, which is pure black like the rest of
 * Vola's dark theme. [System] is the default and follows the browser: light or dark.
 */
enum class ReaderTheme {
    System,
    Light,
    Paper,
    Dark,
    ;

    fun resolved(browserDark: Boolean): ReaderTheme = when (this) {
        System -> if (browserDark) Dark else Light
        else -> this
    }

    companion object {
        /** A saved theme, including the names of earlier versions («Night» is now [Dark]). */
        fun fromStoredName(name: String?): ReaderTheme = when (name) {
            "Night" -> Dark
            else -> entries.firstOrNull { it.name == name } ?: System
        }
    }
}

enum class ReaderTextAlignment { Start, Justified }

data class ReaderSettings(
    val fontScale: Float = 1f,
    val theme: ReaderTheme = ReaderTheme.System,
    val textAlignment: ReaderTextAlignment = ReaderTextAlignment.Start,
    /** Literata (with serifs) or the interface font (without). */
    val serif: Boolean = true,
    /** Wider page margins: shorter lines on a wide or large screen. */
    val wideMargins: Boolean = false,
)

data class ReaderSnapshot(
    val id: String,
    val document: ReaderDocument,
    val progress: Float,
    val savedAtMillis: Long,
)

data class ReaderLibraryState(
    val settings: ReaderSettings = ReaderSettings(),
    val progressByUrl: Map<String, Float> = emptyMap(),
    val snapshots: List<ReaderSnapshot> = emptyList(),
)

object ReaderLibraryRules {
    const val MAX_SNAPSHOTS = 20

    fun visibleState(state: ReaderLibraryState, isPrivate: Boolean): ReaderLibraryState =
        if (isPrivate) ReaderLibraryState() else state

    fun updateSettings(
        state: ReaderLibraryState,
        settings: ReaderSettings,
        isPrivate: Boolean,
    ): ReaderLibraryState = if (isPrivate) {
        state
    } else {
        state.copy(
            settings = settings.copy(
                fontScale = settings.fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE),
            ),
        )
    }

    fun updateProgress(
        state: ReaderLibraryState,
        sourceUrl: String,
        progress: Float,
        isPrivate: Boolean,
    ): ReaderLibraryState = if (isPrivate || ReaderExtractionContract.safeHttpUrl(sourceUrl) == null) {
        state
    } else {
        state.copy(
            progressByUrl = (state.progressByUrl + (sourceUrl to progress.coerceIn(0f, 1f)))
                .entries
                .toList()
                .takeLast(MAX_PROGRESS_ENTRIES)
                .associate { it.toPair() },
        )
    }

    fun saveSnapshot(
        state: ReaderLibraryState,
        snapshot: ReaderSnapshot,
        isPrivate: Boolean,
    ): ReaderLibraryState = if (isPrivate) {
        state
    } else {
        state.copy(
            snapshots = (listOf(snapshot) + state.snapshots.filterNot {
                it.id == snapshot.id || it.document.sourceUrl == snapshot.document.sourceUrl
            })
                .sortedByDescending(ReaderSnapshot::savedAtMillis)
                .take(MAX_SNAPSHOTS),
        )
    }

    fun deleteSnapshot(
        state: ReaderLibraryState,
        snapshotId: String,
        isPrivate: Boolean,
    ): ReaderLibraryState = if (isPrivate) {
        state
    } else {
        state.copy(snapshots = state.snapshots.filterNot { it.id == snapshotId })
    }

    /** Minutes to read [text] at an unhurried pace, never less than one. */
    fun readingMinutes(text: String): Int {
        val words = text.split(Regex("\\s+")).count(String::isNotBlank)
        return ((words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE).coerceAtLeast(1)
    }

    const val MIN_FONT_SCALE = 0.8f
    const val MAX_FONT_SCALE = 1.6f

    /** Stops between the ends of the text size slider, so sizes go 0.1 apart. */
    const val FONT_SCALE_STEPS = 7
    private const val WORDS_PER_MINUTE = 200

    fun progress(scrollValue: Int, maxScrollValue: Int): Float = when {
        maxScrollValue <= 0 -> 0f
        else -> (scrollValue.toFloat() / maxScrollValue).coerceIn(0f, 1f)
    }

    fun resumeProgress(
        state: ReaderLibraryState,
        snapshotProgress: Float?,
        sourceUrl: String,
    ): Float = state.progressByUrl[sourceUrl] ?: snapshotProgress ?: 0f

    fun shouldJustify(kind: ReaderBlockKind, alignment: ReaderTextAlignment): Boolean =
        alignment == ReaderTextAlignment.Justified &&
            (kind == ReaderBlockKind.Paragraph || kind == ReaderBlockKind.ListItem)

    private const val MAX_PROGRESS_ENTRIES = 500
}

object ReaderStorageBudget {
    fun <T> evictOldestUntil(
        newestFirst: List<T>,
        maxBytes: Int,
        encodedBytes: (List<T>) -> Int,
    ): List<T> {
        var bounded = newestFirst
        while (bounded.isNotEmpty() && encodedBytes(bounded) > maxBytes) {
            bounded = bounded.dropLast(1)
        }
        return bounded
    }
}
