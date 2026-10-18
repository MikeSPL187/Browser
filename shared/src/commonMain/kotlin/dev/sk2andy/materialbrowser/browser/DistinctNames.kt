package dev.sk2andy.materialbrowser.browser

/**
 * Names the user tells apart: a group or profile named like another one of its kind gets the next
 * free number («Work», «Work 2»), as Finder does, so lists and TalkBack never show two of the same.
 */
object DistinctNames {
    /**
     * [name] as it is when no name in [taken] matches it (case and outer spaces aside), otherwise
     * with the lowest free number from 2; the base is shortened so the result fits [maxLength].
     */
    fun distinct(name: String, taken: Collection<String>, maxLength: Int): String {
        val used = taken.mapTo(HashSet()) { it.trim().lowercase() }
        if (name.trim().lowercase() !in used) return name
        var number = 2
        while (true) {
            val suffix = " $number"
            val candidate = base(name, maxLength - suffix.length) + suffix
            if (candidate.lowercase() !in used) return candidate
            number++
        }
    }

    /** At most [length] UTF-16 units of [name], never half a surrogate pair. */
    private fun base(name: String, length: Int): String {
        val cut = name.take(length.coerceAtLeast(0))
        return (if (cut.isNotEmpty() && cut.last().isHighSurrogate()) cut.dropLast(1) else cut).trimEnd()
    }
}
