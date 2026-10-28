package dev.sk2andy.materialbrowser.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.AndroidBrowserEngineKind
/** Where a private tab keeps cookies and site data, which decides what the page may promise. */
internal enum class PrivateTabStorage {
    /** GeckoView private browsing: nothing reaches the disk. */
    Memory,

    /** System WebView with profiles: a profile of its own, deleted after the last private tab. */
    SeparateProfile,

    /** System WebView without profiles: cookies are shared with regular tabs. */
    SharedWithRegularTabs,
}

/** One line of the private tab page. */
internal data class PrivateTabFact(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val caption: Int,
    val warning: Boolean = false,
)

/**
 * «Lock on exit» on the private page (board PrivateTab). [available] is false without a strong
 * biometric: the row stays, switched off, and says why.
 */
internal class PrivateTabLock(
    val checked: Boolean,
    val available: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

/** What the private new tab may honestly say for each engine. */
internal object PrivateTabRules {
    fun storage(engine: AndroidBrowserEngineKind, profilesSupported: Boolean): PrivateTabStorage = when {
        engine == AndroidBrowserEngineKind.GeckoView -> PrivateTabStorage.Memory
        profilesSupported -> PrivateTabStorage.SeparateProfile
        else -> PrivateTabStorage.SharedWithRegularTabs
    }

    fun facts(storage: PrivateTabStorage): List<PrivateTabFact> = listOf(
        PrivateTabFact(R.drawable.ic_history, R.string.private_tab_fact_history, R.string.private_tab_fact_history_caption),
        when (storage) {
            PrivateTabStorage.Memory -> PrivateTabFact(
                R.drawable.ic_symbol_cookie,
                R.string.private_tab_fact_memory,
                R.string.private_tab_fact_memory_caption,
            )
            PrivateTabStorage.SeparateProfile -> PrivateTabFact(
                R.drawable.ic_symbol_cookie,
                R.string.private_tab_fact_profile,
                R.string.private_tab_fact_profile_caption,
            )
            PrivateTabStorage.SharedWithRegularTabs -> PrivateTabFact(
                R.drawable.ic_symbol_cookie,
                R.string.private_tab_fact_shared,
                R.string.private_tab_fact_shared_caption,
                warning = true,
            )
        },
        PrivateTabFact(R.drawable.ic_symbol_shield, R.string.private_tab_fact_protection, R.string.private_tab_fact_protection_caption),
        PrivateTabFact(R.drawable.ic_reader_download, R.string.private_tab_fact_downloads, R.string.private_tab_fact_downloads_caption),
    )
}

