package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.WorkspaceAccent

data class AppearanceSettings(
    val appearanceMode: BrowserAppearanceMode = BrowserAppearanceMode.System,
    val animationsEnabled: Boolean = true,
    val forceDarkWebsites: Boolean = false,
    val webContentFontSizePercent: Int = DEFAULT_WEB_CONTENT_FONT_SIZE_PERCENT,
    val colorPalette: BrowserColorPalette = BrowserColorPalette.Vola,
    /** One accent for every workspace; null keeps each workspace's own color. */
    val accentOverride: WorkspaceAccent? = null,
    val chromeStyle: BrowserChromeStyle = BrowserChromeStyle.Frame,
    val surfaceStyle: BrowserSurfaceStyle = BrowserSurfaceStyle.Clear,
    val shapeStyle: BrowserShapeStyle = BrowserShapeStyle.Rounded,
    val density: BrowserDensity = BrowserDensity.Normal,
    val addressBarStyle: BrowserAddressBarStyle = BrowserAddressBarStyle.Classic,
    val addressBarColorPreset: BrowserAddressBarColorPreset = BrowserAddressBarColorPreset.Theme,
    val addressBarCustomColorHex: String = "",
    val frostedTransparencyPercent: Int = DEFAULT_FROSTED_TRANSPARENCY_PERCENT,
    val frostedAddressBarTransparencyPercent: Int =
        DEFAULT_FROSTED_ADDRESS_BAR_TRANSPARENCY_PERCENT,
    val frostedBlurPercent: Int = DEFAULT_FROSTED_BLUR_PERCENT,
) {
    fun usesDarkColors(systemDark: Boolean): Boolean = when (appearanceMode) {
        BrowserAppearanceMode.System -> systemDark
        BrowserAppearanceMode.Light -> false
        BrowserAppearanceMode.Dark -> true
    }

    fun normalized(): AppearanceSettings {
        val normalizedAddressBarColor = AddressBarColorRules.normalizeHex(
            addressBarCustomColorHex,
        )
        return copy(
            addressBarColorPreset = if (
                addressBarColorPreset == BrowserAddressBarColorPreset.Custom &&
                normalizedAddressBarColor == null
            ) {
                BrowserAddressBarColorPreset.Theme
            } else {
                addressBarColorPreset
            },
            addressBarCustomColorHex = normalizedAddressBarColor.orEmpty(),
            webContentFontSizePercent = webContentFontSizePercent
                .coerceIn(
                    MIN_WEB_CONTENT_FONT_SIZE_PERCENT,
                    MAX_WEB_CONTENT_FONT_SIZE_PERCENT,
                )
                .let { value ->
                    val offset = value - MIN_WEB_CONTENT_FONT_SIZE_PERCENT
                    MIN_WEB_CONTENT_FONT_SIZE_PERCENT +
                        (offset + WEB_CONTENT_FONT_SIZE_STEP_PERCENT / 2) /
                        WEB_CONTENT_FONT_SIZE_STEP_PERCENT *
                        WEB_CONTENT_FONT_SIZE_STEP_PERCENT
                },
            frostedTransparencyPercent = frostedTransparencyPercent.coerceIn(
                MIN_FROSTED_TRANSPARENCY_PERCENT,
                MAX_FROSTED_TRANSPARENCY_PERCENT,
            ),
            frostedAddressBarTransparencyPercent = frostedAddressBarTransparencyPercent.coerceIn(
                MIN_FROSTED_TRANSPARENCY_PERCENT,
                MAX_FROSTED_TRANSPARENCY_PERCENT,
            ),
            frostedBlurPercent = frostedBlurPercent.coerceIn(
                MIN_FROSTED_BLUR_PERCENT,
                MAX_FROSTED_BLUR_PERCENT,
            ),
        )
    }

    companion object {
        const val DEFAULT_WEB_CONTENT_FONT_SIZE_PERCENT = 100
        const val MIN_WEB_CONTENT_FONT_SIZE_PERCENT = 50
        const val MAX_WEB_CONTENT_FONT_SIZE_PERCENT = 200
        const val WEB_CONTENT_FONT_SIZE_STEP_PERCENT = 5
        const val DEFAULT_FROSTED_TRANSPARENCY_PERCENT = 40
        const val DEFAULT_FROSTED_ADDRESS_BAR_TRANSPARENCY_PERCENT = 40
        const val MIN_FROSTED_TRANSPARENCY_PERCENT = 0
        const val MAX_FROSTED_TRANSPARENCY_PERCENT = 80
        const val DEFAULT_FROSTED_BLUR_PERCENT = 60
        const val MIN_FROSTED_BLUR_PERCENT = 0
        const val MAX_FROSTED_BLUR_PERCENT = 100
    }
}

/** Light or dark shell. The dark theme is always pure black, so there is no separate OLED mode. */
enum class BrowserAppearanceMode(val stableId: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        /** Stored by versions that offered a separate OLED mode; it now reads as [Dark]. */
        const val LEGACY_AMOLED_STABLE_ID = "amoled"

        fun fromStableId(value: String?): BrowserAppearanceMode = when (value) {
            LEGACY_AMOLED_STABLE_ID -> Dark
            else -> entries.firstOrNull { it.stableId == value } ?: System
        }
    }
}

/**
 * The theme of the shell (board W-Themes). Vola takes every color from the accent; Ice, Dusk,
 * Paper and Mono lay the accent on their own neutrals; Dynamic follows the wallpaper.
 */
enum class BrowserColorPalette(val stableId: String) {
    Vola("vola"),
    Ice("ice"),
    Dusk("dusk"),
    Paper("paper"),
    Mono("mono"),
    Dynamic("dynamic");

    companion object {
        /** Stored by versions that offered a neutral palette; Mono took its place. */
        const val LEGACY_NEUTRAL_STABLE_ID = "neutral"

        fun fromStableId(value: String?): BrowserColorPalette = when (value) {
            LEGACY_NEUTRAL_STABLE_ID -> Mono
            else -> entries.firstOrNull { it.stableId == value } ?: Vola
        }
    }
}

/**
 * How the shell frames the page. Frame (the default) lays the chrome on the workspace aura and
 * shows the page as a rounded card; Air runs the page edge to edge and keeps the aura in the rim
 * of the address island.
 */
enum class BrowserChromeStyle(val stableId: String) {
    Frame("frame"),
    Air("air");

    companion object {
        fun fromStableId(value: String?): BrowserChromeStyle =
            entries.firstOrNull { it.stableId == value } ?: Frame
    }
}

enum class BrowserSurfaceStyle(val stableId: String) {
    Clear("clear"),
    Frosted("frosted");

    companion object {
        fun fromStableId(value: String?): BrowserSurfaceStyle =
            entries.firstOrNull { it.stableId == value } ?: Clear
    }
}

enum class BrowserShapeStyle(val stableId: String) {
    Angular("angular"),
    Rounded("rounded"),
    ExtraRounded("extra_rounded");

    companion object {
        fun fromStableId(value: String?): BrowserShapeStyle =
            entries.firstOrNull { it.stableId == value } ?: Rounded
    }
}

/** How tight the rows of lists and settings sit (board W-Themes); touch targets stay 48 dp. */
enum class BrowserDensity(val stableId: String) {
    Compact("compact"),
    Normal("normal"),
    Comfortable("comfortable");

    companion object {
        fun fromStableId(value: String?): BrowserDensity =
            entries.firstOrNull { it.stableId == value } ?: Normal
    }
}

enum class BrowserAddressBarStyle(val stableId: String) {
    Classic("classic"),
    Segmented("segmented");

    companion object {
        fun fromStableId(value: String?): BrowserAddressBarStyle =
            entries.firstOrNull { it.stableId == value } ?: Classic
    }
}

enum class BrowserAddressBarColorPreset(val stableId: String) {
    Theme("theme"),
    Dimmed("dimmed"),
    Graphite("graphite"),
    Black("black"),
    Custom("custom");

    companion object {
        fun fromStableId(value: String?): BrowserAddressBarColorPreset =
            entries.firstOrNull { it.stableId == value } ?: Theme
    }
}

object AddressBarColorRules {
    fun normalizeHex(value: String): String? {
        val digits = value.trim().removePrefix("#")
        if (digits.length != SHORT_HEX_LENGTH && digits.length != LONG_HEX_LENGTH) return null
        if (digits.any { it.digitToIntOrNull(16) == null }) return null
        val expanded = if (digits.length == SHORT_HEX_LENGTH) {
            buildString(LONG_HEX_LENGTH) {
                digits.forEach { digit ->
                    append(digit)
                    append(digit)
                }
            }
        } else {
            digits
        }
        return "#${expanded.uppercase()}"
    }

    fun colorArgb(value: String): Long? = normalizeHex(value)
        ?.removePrefix("#")
        ?.toLongOrNull(16)
        ?.or(OPAQUE_ALPHA)

    private const val SHORT_HEX_LENGTH = 3
    private const val LONG_HEX_LENGTH = 6
    private const val OPAQUE_ALPHA = 0xFF000000L
}
