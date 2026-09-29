package dev.sk2andy.materialbrowser.browser

/**
 * When the engine refuses to load pages over plain HTTP.
 *
 * With HTTPS-only on, every http:// navigation is upgraded to https://. If a site has no working
 * HTTPS, the user sees a warning and decides whether to continue over HTTP for that page.
 */
enum class HttpsOnlyMode(val stableId: String) {
    Always("always"),
    PrivateTabs("private_tabs"),
    Off("off"),
    ;

    companion object {
        val Default = Always

        fun fromStableId(value: String?): HttpsOnlyMode =
            entries.firstOrNull { mode -> mode.stableId == value } ?: Default
    }
}
