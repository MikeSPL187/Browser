package dev.sk2andy.materialbrowser.settings

import android.content.res.Resources
import androidx.annotation.StringRes
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.settings.SettingLevel
import dev.sk2andy.materialbrowser.shared.settings.SettingLevel.Advanced
import dev.sk2andy.materialbrowser.shared.settings.SettingSearchCandidate
import dev.sk2andy.materialbrowser.shared.settings.SettingSpec
import dev.sk2andy.materialbrowser.ui.SettingsDestination
import dev.sk2andy.materialbrowser.ui.SettingsDestination.AboutLegal
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Appearance
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Browser
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Downloads
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Passwords
import dev.sk2andy.materialbrowser.ui.SettingsDestination.ProtectionAndData
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Search
import dev.sk2andy.materialbrowser.ui.SettingsDestination.SiteCapsules
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Sync
import dev.sk2andy.materialbrowser.ui.SettingsDestination.TabsAndGestures
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Themes
import dev.sk2andy.materialbrowser.ui.SettingsDestination.Userscripts

/** A setting described once, with the words the interface shows for it. */
internal class AndroidSettingSpec(
    val spec: SettingSpec,
    @StringRes val title: Int,
    @StringRes val summary: Int? = null,
)

/** A page that opens from the settings home. */
internal class AndroidSettingsPage(
    val destination: SettingsDestination,
    @StringRes val title: Int,
    @StringRes val summary: Int? = null,
)

/**
 * Every setting of Vola, page by page (docs/vola/tech-plan.md, section 2). Settings search is
 * built from it ([SettingsSearchRules]); pages move onto it as they are redesigned, starting
 * with «Appearance».
 */
internal object SettingsRegistry {
    val pages: List<AndroidSettingsPage> = listOf(
        page(Search, R.string.settings_section_search, R.string.settings_home_search_summary),
        page(
            TabsAndGestures,
            R.string.settings_tabs_gestures_title,
            R.string.settings_home_tabs_gestures_summary,
        ),
        page(Browser, R.string.settings_section_browser, R.string.settings_home_browser_summary),
        page(Downloads, R.string.settings_downloads_title, null),
        page(
            Appearance,
            R.string.settings_appearance_title,
            R.string.settings_home_appearance_summary,
        ),
        page(Themes, R.string.settings_themes_title, R.string.settings_themes_summary),
        page(
            SiteCapsules,
            R.string.capsule_settings_title,
            R.string.settings_home_capsules_summary,
        ),
        page(Userscripts, R.string.userscript_title, R.string.settings_home_userscripts_summary),
        page(Passwords, R.string.passwords_title, R.string.settings_home_passwords_summary),
        page(
            ProtectionAndData,
            R.string.settings_protection_data_title,
            R.string.settings_home_protection_summary,
        ),
        page(Sync, R.string.sync_settings_title, R.string.settings_home_sync_summary),
        page(
            AboutLegal,
            R.string.settings_section_about_legal,
            R.string.settings_home_about_summary,
        ),
    )

    val settings: List<AndroidSettingSpec> = listOf(
        setting("browser_engine", Browser, R.string.settings_browser_engine_title, null),
        setting(
            "startup_animation",
            Browser,
            R.string.settings_startup_animation_title,
            R.string.settings_startup_animation_subtitle,
        ),
        setting(
            "startup_address_focus",
            Browser,
            R.string.settings_startup_address_focus_title,
            null,
        ),
        setting(
            "open_home_on_startup",
            Browser,
            R.string.settings_open_home_on_startup_title,
            R.string.settings_open_home_on_startup_subtitle,
        ),
        setting(
            "favorite_bookmark_import",
            Browser,
            R.string.settings_favorite_bookmark_import_title,
            R.string.settings_favorite_bookmark_import_summary,
        ),
        setting(
            "full_immersive_mode",
            Browser,
            R.string.settings_full_immersive_mode_title,
            R.string.settings_full_immersive_mode_subtitle,
        ),
        setting(
            "scroll_bar",
            Browser,
            R.string.settings_scroll_bar_title,
            R.string.settings_scroll_bar_subtitle,
        ),
        setting("video_autoplay", Browser, R.string.settings_video_autoplay_title, null),
        setting("inline_media_player", Browser, R.string.settings_inline_media_player_title, null),
        setting("translation_provider", Browser, R.string.settings_translation_provider, null),
        setting("external_app_links", Browser, R.string.settings_external_app_links_title, null),
        setting(
            "external_link_preview",
            Browser,
            R.string.settings_external_link_preview_title,
            R.string.settings_external_link_preview_subtitle,
        ),

        setting("download_manager", Downloads, R.string.settings_download_manager_title, null),
        setting("download_folder", Downloads, R.string.settings_download_folder_title, null),
        setting(
            "download_one_dm_session",
            Downloads,
            R.string.settings_download_one_dm_session_title,
            R.string.settings_download_one_dm_session_summary,
        ),

        setting(
            "protection_card",
            ProtectionAndData,
            R.string.settings_protection_card_title,
            R.string.settings_protection_card_subtitle,
        ),
        setting("https_only", ProtectionAndData, R.string.settings_https_only_title, null),
        setting(
            "block_ads",
            ProtectionAndData,
            R.string.settings_block_ads_title,
            R.string.settings_block_ads_subtitle,
        ),
        setting(
            "hide_cookie_banners",
            ProtectionAndData,
            R.string.settings_hide_cookie_banners_title,
            R.string.settings_hide_cookie_banners_subtitle,
        ),
        setting(
            "block_third_party_cookies",
            ProtectionAndData,
            R.string.settings_block_third_party_cookies_title,
            R.string.settings_block_third_party_cookies_subtitle,
        ),
        setting(
            "do_not_track",
            ProtectionAndData,
            R.string.settings_do_not_track_title,
            R.string.settings_do_not_track_summary,
        ),
        setting(
            "global_privacy_control",
            ProtectionAndData,
            R.string.settings_global_privacy_control_title,
            R.string.settings_global_privacy_control_summary,
        ),
        setting(
            "auto_de_amp",
            ProtectionAndData,
            R.string.settings_auto_de_amp_title,
            R.string.settings_auto_de_amp_summary,
        ),
        setting("dns_over_https", ProtectionAndData, R.string.settings_dns_over_https_title, null),
        setting(
            "webrtc_protection",
            ProtectionAndData,
            R.string.settings_webrtc_protection_title,
            null,
        ),
        setting(
            "history_save",
            ProtectionAndData,
            R.string.history_save_title,
            R.string.history_save_summary,
        ),
        setting(
            "history_clear_on_exit",
            ProtectionAndData,
            R.string.history_clear_on_exit_title,
            R.string.history_clear_on_exit_summary,
        ),
        setting(
            "recall",
            ProtectionAndData,
            R.string.recall_settings_title,
            R.string.recall_settings_summary,
        ),
        setting(
            "data_archive_export",
            ProtectionAndData,
            R.string.data_archive_export_title,
            R.string.data_archive_export_summary,
        ),
        setting(
            "data_archive_import",
            ProtectionAndData,
            R.string.data_archive_import_title,
            R.string.data_archive_import_summary,
        ),

        setting("search_engine", Search, R.string.settings_search_engine, null),
        setting(
            "ai_mode",
            Search,
            R.string.settings_ai_mode_toggle_title,
            R.string.settings_ai_mode_toggle_subtitle,
        ),
        setting(
            "history_suggestions",
            Search,
            R.string.settings_history_suggestions_title,
            R.string.settings_history_suggestions_summary,
        ),
        setting("search_suggestions", Search, R.string.settings_search_suggestions, null),
        setting(
            "searxng_suggestion_fallback",
            Search,
            R.string.settings_searxng_suggestion_fallback,
            null,
        ),

        setting(
            "tab_stack_folder_mode",
            TabsAndGestures,
            R.string.settings_tab_stack_folder_mode,
            null,
        ),
        setting(
            "automatic_tab_sorting",
            TabsAndGestures,
            R.string.settings_automatic_tab_sorting_title,
            R.string.settings_automatic_tab_sorting_subtitle,
        ),
        setting(
            "closed_tab_undo",
            TabsAndGestures,
            R.string.settings_closed_tab_undo_title,
            R.string.settings_closed_tab_undo_summary,
        ),
        setting("auto_close_tabs", TabsAndGestures, R.string.settings_auto_close_tabs, null),
        setting(
            "archive_inactive_tabs",
            TabsAndGestures,
            R.string.settings_archive_inactive_tabs_title,
            R.string.settings_archive_inactive_tabs_summary,
        ),
        setting(
            "profiles",
            TabsAndGestures,
            R.string.settings_profiles_title,
            R.string.settings_profiles_subtitle,
        ),
        setting(
            "address_bar_long_press",
            TabsAndGestures,
            R.string.settings_address_bar_long_press_title,
            null,
        ),
        setting(
            "link_long_press_action",
            TabsAndGestures,
            R.string.settings_link_long_press_action,
            null,
        ),
        setting(
            "link_peek_actions",
            TabsAndGestures,
            R.string.settings_link_peek_actions_title,
            R.string.settings_link_peek_actions_summary,
        ),
        setting(
            "address_bar_actions",
            TabsAndGestures,
            R.string.settings_address_bar_actions_title,
            R.string.settings_address_bar_actions_summary,
        ),
        setting(
            "menu_actions",
            TabsAndGestures,
            R.string.settings_menu_actions_title,
            R.string.settings_menu_actions_summary,
        ),
        setting(
            "address_bar_docking",
            TabsAndGestures,
            R.string.settings_address_bar_docking_title,
            R.string.settings_address_bar_docking_subtitle,
        ),
        setting(
            "tab_dismiss_resistance",
            TabsAndGestures,
            R.string.settings_tab_dismiss_resistance,
            null,
        ),

        setting(
            "sync_server",
            Sync,
            R.string.sync_setup_server_title,
            R.string.sync_setup_server_summary,
        ),
        setting(
            "sync_extension",
            Sync,
            R.string.sync_setup_extension_title,
            R.string.sync_setup_extension_summary,
        ),
        setting(
            "sync_workspace",
            Sync,
            R.string.sync_setup_workspace_title,
            R.string.sync_setup_workspace_summary,
        ),
        setting(
            "sync_android",
            Sync,
            R.string.sync_setup_android_title,
            R.string.sync_setup_android_summary,
        ),

        setting("chrome_style", Appearance, R.string.settings_chrome_style, null),
        setting("appearance_mode", Appearance, R.string.settings_appearance_mode, null),
        setting("color_palette", Themes, R.string.settings_color_palette, null),
        setting("accent_override", Themes, R.string.settings_themes_accent, null),
        setting("shape_style", Themes, R.string.settings_shape_style, null),
        setting(
            "animations",
            Appearance,
            R.string.settings_animations,
            R.string.settings_animations_summary,
            Advanced,
        ),
        setting(
            "force_dark_websites",
            Appearance,
            R.string.settings_force_dark_websites,
            R.string.settings_force_dark_websites_summary,
            Advanced,
        ),
        setting(
            "web_content_font_size",
            Appearance,
            R.string.settings_web_content_font_size,
            null,
            Advanced,
        ),
        setting(
            "address_bar_color",
            Appearance,
            R.string.settings_address_bar_color,
            null,
            Advanced,
        ),
        setting(
            "address_bar_style",
            Appearance,
            R.string.settings_address_bar_style,
            null,
            Advanced,
        ),
        setting(
            "surface_style",
            Appearance,
            R.string.settings_surface_style,
            R.string.settings_surface_style_summary,
            Advanced,
        ),
        setting(
            "frosted_transparency",
            Appearance,
            R.string.settings_frosted_transparency,
            null,
            Advanced,
        ),
        setting(
            "frosted_address_bar_transparency",
            Appearance,
            R.string.settings_frosted_address_bar_transparency,
            null,
            Advanced,
        ),
        setting("frosted_blur", Appearance, R.string.settings_frosted_blur, null, Advanced),
    )

    /** Pages and settings in the interface language, for [SettingsSearchRules]. */
    fun searchCandidates(resources: Resources): List<SettingSearchCandidate> {
        val pageTitles = pages.associate { page ->
            page.destination to resources.getString(page.title)
        }
        return pages.map { page ->
            SettingSearchCandidate(
                key = "page:${page.destination.name}",
                destination = page.destination,
                title = resources.getString(page.title),
                summary = page.summary?.let(resources::getString),
                page = "",
            )
        } + settings.map { setting ->
            SettingSearchCandidate(
                key = setting.spec.key,
                destination = setting.spec.destination,
                title = resources.getString(setting.title),
                summary = setting.summary?.let(resources::getString),
                page = pageTitles[setting.spec.destination].orEmpty(),
            )
        }
    }

    fun page(destination: SettingsDestination): AndroidSettingsPage? =
        pages.firstOrNull { page -> page.destination == destination }

    fun settingsOn(
        destination: SettingsDestination,
        level: SettingLevel,
    ): List<AndroidSettingSpec> = settings.filter { setting ->
        setting.spec.destination == destination && setting.spec.level == level
    }

    private fun page(
        destination: SettingsDestination,
        @StringRes title: Int,
        @StringRes summary: Int?,
    ) = AndroidSettingsPage(destination, title, summary)

    private fun setting(
        key: String,
        destination: SettingsDestination,
        @StringRes title: Int,
        @StringRes summary: Int?,
        level: SettingLevel = SettingLevel.Main,
    ) = AndroidSettingSpec(SettingSpec(key, destination, level), title, summary)
}
