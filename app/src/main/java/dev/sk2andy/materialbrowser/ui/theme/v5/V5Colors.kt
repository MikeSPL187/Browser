package dev.sk2andy.materialbrowser.ui.theme.v5

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/*
 * v5 color roles (docs/vola/design/system-v5.md, section 1). The shell is neutral gray; color comes
 * only from the site (the bar over the page, V8) and from the profile accent. Dark is dark gray like
 * Safari, never pure black. Every value here is checked for contrast by V5ColorsTest.
 */

/** Every color role of one v5 scheme: light, dark or private. */
@Immutable
data class V5ColorScheme(
    val isDark: Boolean,
    val isPrivate: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighest: Color,
    /** The bar when it carries no site color: new tab, no color found, tinting turned off. */
    val chrome: Color,
    val label: Color,
    val labelSecondary: Color,
    /** Only the hint in an empty field and disabled items; never meaning (3 : 1 at most). */
    val labelTertiary: Color,
    val separator: Color,
    val fill: Color,
    val scrim: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    /** Accent for text and icons: links, "Done", the active card's border. */
    val accent: Color,
    /** Accent fill of buttons and the profile badge; content on it is [onAccentFill]. */
    val accentFill: Color,
    val onAccentFill: Color,
    /** Background of a selected row or chip. */
    val accentSoft: Color,
    /**
     * Behind a page that warns of danger (canvas «Danger», audit question 6): the danger color
     * barely over the ground, as [accentSoft] is made.
     */
    val dangerSoft: Color,
    /** The 0.5 dp top edge of the bar and of every material. */
    val edge: Color,
)

/** The six profile accents (section 1.2): one tone for light, a deep fill and a bright tone for dark. */
enum class V5Accent(
    internal val light: Color,
    internal val darkFill: Color,
    internal val darkAccent: Color,
) {
    Blue(Color(0xFF2563D9), Color(0xFF2563D9), Color(0xFF6E9BFF)),
    Green(Color(0xFF1A7F45), Color(0xFF1A7F45), Color(0xFF4CC47E)),
    Orange(Color(0xFFB4520F), Color(0xFFB4520F), Color(0xFFFF9F5A)),
    Pink(Color(0xFFBE2A68), Color(0xFFBE2A68), Color(0xFFFF7EAE)),
    Purple(Color(0xFF6A45D1), Color(0xFF6A45D1), Color(0xFFA88FFF)),

    /** Raised to #66666F in dark: the light #4B4B53 merged with the background (1.97 : 1). */
    Graphite(Color(0xFF4B4B53), Color(0xFF66666F), Color(0xFFC9C9D1)),
    ;

    /** The deep tone a profile's emoji tile and buttons are filled with. */
    fun fill(dark: Boolean): Color = if (dark) darkFill else light

    companion object {
        val Default = Blue
    }
}

/**
 * Settings section tiles (section 1.5): a white glyph on a colored tile, Safari style. The color
 * belongs to the section, not to the profile accent, and is the same in both themes.
 */
enum class V5Tile(val color: Color) {
    Blue(Color(0xFF2F6FE4)),
    Indigo(Color(0xFF4F5BD5)),
    Purple(Color(0xFF7B57E0)),
    Sky(Color(0xFF1C8FD6)),
    Teal(Color(0xFF0E8C8C)),
    Green(Color(0xFF1F9D55)),
    Amber(Color(0xFFC27A00)),
    Orange(Color(0xFFE0620D)),
    Red(Color(0xFFE5383B)),
    Pink(Color(0xFFD93A7A)),
    Gray(Color(0xFF7A7A82)),
    ;

    val glyph: Color get() = Color.White
}

/**
 * Sync over QR (canvas «ф»): the scanner is a camera screen, dark in either theme as the system
 * camera is, and a QR code reads only dark on light, so its plate stays white in the dark theme.
 */
object V5SyncColors {
    val scanBackground = Color(0xFF1B1D21)
    val scanVeil = Color(0x73000000)
    val scanInk = Color(0xFFFFFFFF)
    val scanInkSoft = Color(0xCCFFFFFF)
    val codePaper = Color(0xFFFFFFFF)
    /** The code's modules: near black on white paper in both themes, as the canvas draws it. */
    val codeInk = Color(0xFF111111)
}

/** Control colors that do not change with the theme: the toggle thumb is white in both, as on the canvas. */
object V5ControlColors {
    val toggleThumb = Color(0xFFFFFFFF)
    val toggleThumbShadow = Color(0x33000000)

    /** Behind the page while it shrinks under a link preview: the device's dark frame (canvas «н1»). */
    val glanceBackdrop = Color(0xFF0B0B0D)

    /**
     * A tab card's «×» (canvas «Overview»): a dark veil and a white cross, the same over any page
     * picture in both themes.
     */
    val cardClosePlate = Color(0x57000000)

    /** The swipe hint under the link preview's buttons, on the dimmed page in both themes (canvas «Glance»). */
    val glanceHint = Color(0xD1FFFFFF)
    val cardCloseGlyph = Color(0xFFFFFFFF)
}

object V5Colors {
    private val White = Color(0xFFFFFFFF)

    fun light(accent: V5Accent = V5Accent.Default): V5ColorScheme {
        val surface = Color(0xFFFFFFFF)
        return V5ColorScheme(
            isDark = false,
            isPrivate = false,
            background = Color(0xFFF4F4F6),
            surface = surface,
            surfaceElevated = Color(0xFFEBEBEF),
            surfaceHighest = Color(0xFFE0E0E5),
            chrome = Color(0xFFF9F9FB),
            label = Color(0xFF111114),
            labelSecondary = Color(0xFF55555D),
            labelTertiary = Color(0xFF808088),
            separator = Color(0xFFD8D8DE),
            fill = Color(0x1F787880),
            scrim = Color(0x52000000),
            success = Color(0xFF1A7F45),
            warning = Color(0xFF9A5600),
            danger = Color(0xFFC42630),
            accent = accent.light,
            accentFill = accent.light,
            onAccentFill = White,
            // Light: the accent at 14 % over surface.
            accentSoft = accent.light.copy(alpha = 0.14f).compositeOver(surface),
            dangerSoft = Color(0xFFC42630).copy(alpha = 0.08f).compositeOver(surface),
            edge = Color(0x1F000000),
        )
    }

    fun dark(accent: V5Accent = V5Accent.Default): V5ColorScheme {
        val background = Color(0xFF1C1C1E)
        return V5ColorScheme(
            isDark = true,
            isPrivate = false,
            background = background,
            surface = Color(0xFF2A2A2D),
            surfaceElevated = Color(0xFF333337),
            surfaceHighest = Color(0xFF3E3E43),
            chrome = Color(0xFF262628),
            label = Color(0xFFF4F4F6),
            labelSecondary = Color(0xFFA9A9B1),
            labelTertiary = Color(0xFF7E7E86),
            separator = Color(0xFF3A3A3E),
            fill = Color(0x388C8C96),
            scrim = Color(0x80000000),
            success = DarkSuccess,
            warning = DarkWarning,
            danger = DarkDanger,
            accent = accent.darkAccent,
            accentFill = accent.darkFill,
            onAccentFill = White,
            // Dark: 20 % accent and 80 % background (the Zen model).
            accentSoft = accent.darkAccent.copy(alpha = 0.20f).compositeOver(background),
            dangerSoft = DarkDanger.copy(alpha = DARK_DANGER_SOFT).compositeOver(background),
            edge = Color(0x14FFFFFF),
        )
    }

    /**
     * Private browsing (section 1.4): always dark, a cool violet gray clearly unlike the normal dark
     * theme. No site color and no profile accent. The spec leaves `surfaceHighest`, `labelTertiary`
     * and `fill` open; they follow the dark scheme's steps in the private hue.
     */
    fun privateBrowsing(): V5ColorScheme {
        val background = Color(0xFF17151D)
        val accent = Color(0xFFA68BFF)
        return V5ColorScheme(
            isDark = true,
            isPrivate = true,
            background = background,
            surface = Color(0xFF221F2B),
            surfaceElevated = Color(0xFF2C2838),
            surfaceHighest = Color(0xFF363145),
            chrome = Color(0xFF241F31),
            label = Color(0xFFF3F1F8),
            labelSecondary = Color(0xFFABA6BA),
            labelTertiary = Color(0xFF7F7A8E),
            separator = Color(0xFF352F44),
            fill = Color(0x38968CAA),
            scrim = Color(0x80000000),
            success = DarkSuccess,
            warning = DarkWarning,
            danger = DarkDanger,
            accent = accent,
            accentFill = Color(0xFF6A45D1),
            onAccentFill = White,
            accentSoft = accent.copy(alpha = 0.20f).compositeOver(background),
            dangerSoft = DarkDanger.copy(alpha = DARK_DANGER_SOFT).compositeOver(background),
            edge = Color(0x14FFFFFF),
        )
    }

    /**
     * Increased contrast (section 8): secondary text becomes primary text and the separator moves
     * twice as far from the background.
     */
    fun V5ColorScheme.highContrast(): V5ColorScheme = copy(
        labelSecondary = label,
        separator = Color(
            red = (2 * separator.red - background.red).coerceIn(0f, 1f),
            green = (2 * separator.green - background.green).coerceIn(0f, 1f),
            blue = (2 * separator.blue - background.blue).coerceIn(0f, 1f),
        ),
    )

    private val DarkSuccess = Color(0xFF4CC47E)
    private val DarkWarning = Color(0xFFF0A53A)
    private val DarkDanger = Color(0xFFFF6B6B)

    /** In the dark the bright danger needs less of itself to read as a red ground. */
    private const val DARK_DANGER_SOFT = 0.06f
}
