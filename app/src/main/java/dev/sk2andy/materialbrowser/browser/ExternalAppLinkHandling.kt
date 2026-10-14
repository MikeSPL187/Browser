package dev.sk2andy.materialbrowser.browser

enum class ExternalAppLinkHandling(val stableId: String) {
    Automatic("automatic"),
    AskEveryTime("ask_every_time"),
    ;

    companion object {
        val Default = Automatic

        /**
         * A private tab always asks before another app opens: a silent launch would tell that app
         * about the private visit (V29). Regular tabs follow the person's [chosen] setting.
         */
        fun forSource(chosen: ExternalAppLinkHandling, isPrivate: Boolean): ExternalAppLinkHandling =
            if (isPrivate) AskEveryTime else chosen

        fun fromStableId(value: String?): ExternalAppLinkHandling =
            entries.firstOrNull { handling -> handling.stableId == value } ?: Default
    }
}

data class ExternalAppPrompt(
    val id: Long,
    val destination: String,
)
