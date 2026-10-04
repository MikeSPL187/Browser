package dev.sk2andy.materialbrowser.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.data.BrowserChromeStyle
import dev.sk2andy.materialbrowser.data.BrowserColorPalette
import dev.sk2andy.materialbrowser.data.BrowserShapeStyle
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaAppearance
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush
import dev.sk2andy.materialbrowser.ui.theme.color

internal object AppearanceMainTestTags {
    fun chromeStyle(style: BrowserChromeStyle) =
        "appearance_settings_chrome_style:${style.stableId}"
    fun mode(mode: BrowserAppearanceMode) = "appearance_settings_mode:${mode.stableId}"
    fun palette(palette: BrowserColorPalette) = "appearance_settings_palette:${palette.stableId}"
    fun shape(shape: BrowserShapeStyle) = "appearance_settings_shape:${shape.stableId}"
}

/** The board's order for the theme: light, dark, then following the system («Auto»). */
private val MODE_ORDER = listOf(
    BrowserAppearanceMode.Light,
    BrowserAppearanceMode.Dark,
    BrowserAppearanceMode.System,
)

/**
 * The main part of «Appearance» (board W-SetAppearance): the shell's style as two cards with a
 * small phone each, then the theme, the colors and the corners as rows of choices.
 */
@Composable
internal fun ColumnScope.AppearanceMainSections(
    settings: AppearanceSettings,
    onSettingsChanged: (AppearanceSettings) -> Unit,
) {
    Text(
        stringResource(R.string.settings_chrome_style).uppercase(),
        modifier = Modifier.padding(
            start = VolaAppearance.overlinePadding,
            bottom = VolaAppearance.overlinePadding,
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(VolaAppearance.styleCardGap),
    ) {
        BrowserChromeStyle.entries.forEach { style ->
            ChromeStyleCard(
                style = style,
                selected = settings.chromeStyle == style,
                onClick = { onSettingsChanged(settings.copy(chromeStyle = style)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
    Spacer(Modifier.height(VolaAppearance.sectionGap))
    AppearanceGroup {
        GroupTitle(stringResource(R.string.settings_appearance_mode))
        ChoiceRow(
            options = MODE_ORDER,
            selected = settings.appearanceMode,
            label = { mode ->
                if (mode == BrowserAppearanceMode.System) {
                    stringResource(R.string.appearance_mode_auto)
                } else {
                    mode.displayName()
                }
            },
            icon = { mode ->
                when (mode) {
                    BrowserAppearanceMode.Light -> VolaIcons.LightMode
                    BrowserAppearanceMode.Dark -> VolaIcons.DarkMode
                    BrowserAppearanceMode.System -> VolaIcons.Contrast
                }
            },
            testTag = AppearanceMainTestTags::mode,
            onSelect = { mode -> onSettingsChanged(settings.copy(appearanceMode = mode)) },
        )
    }
    Spacer(Modifier.height(VolaAppearance.sectionGap))
    AppearanceGroup {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupTitle(stringResource(R.string.settings_color_palette), Modifier.weight(1f))
            if (settings.colorPalette == BrowserColorPalette.Vola) {
                Text(
                    stringResource(R.string.settings_color_palette_per_space),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PaletteSwatches(
            selected = settings.colorPalette,
            onSelect = { palette -> onSettingsChanged(settings.copy(colorPalette = palette)) },
        )
        GroupTitle(stringResource(R.string.settings_shape_style))
        ChoiceRow(
            options = BrowserShapeStyle.entries,
            selected = settings.shapeStyle,
            label = { shape -> shape.displayName() },
            icon = null,
            testTag = AppearanceMainTestTags::shape,
            onSelect = { shape -> onSettingsChanged(settings.copy(shapeStyle = shape)) },
        )
    }
}

@Composable
private fun AppearanceGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = VolaAppearance.groupShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(VolaAppearance.groupPadding),
            verticalArrangement = Arrangement.spacedBy(VolaAppearance.groupGap),
            content = content,
        )
    }
}

@Composable
private fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ChromeStyleCard(
    style: BrowserChromeStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.RadioButton }
            .testTag(AppearanceMainTestTags.chromeStyle(style)),
        shape = VolaAppearance.styleCardShape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (selected) {
            BorderStroke(
                VolaAppearance.styleCardSelectedBorder,
                MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(VolaAppearance.styleCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VolaAppearance.styleCardTextGap),
        ) {
            ChromeStylePhone(style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(VolaAppearance.choiceIconGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Icon(
                        VolaIcons.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(VolaAppearance.choiceIconSize),
                    )
                }
                Text(
                    style.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                style.summary(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A small phone: the page as a card on the aura («Frame») or edge to edge («Air»). */
@Composable
private fun ChromeStylePhone(style: BrowserChromeStyle) {
    val framed = style == BrowserChromeStyle.Frame
    Box(
        modifier = Modifier
            .size(VolaAppearance.phoneWidth, VolaAppearance.phoneHeight)
            .border(
                VolaAppearance.styleCardHairline,
                MaterialTheme.colorScheme.outlineVariant,
                VolaAppearance.phoneShape,
            )
            .background(
                if (framed) VolaTheme.auraBrush else surfaceBrush(),
                VolaAppearance.phoneShape,
            )
            .padding(if (framed) VolaAppearance.phonePadding else VolaAppearance.styleCardHairline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, VolaAppearance.phonePageShape)
                .padding(VolaAppearance.phoneContentPadding),
            verticalArrangement = Arrangement.spacedBy(VolaAppearance.phoneLineGap),
        ) {
            PhoneLine(fraction = 0.45f, strong = true)
            PhoneLine(fraction = 0.8f, strong = true)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaAppearance.phoneImageHeight)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        VolaAppearance.phonePageShape,
                    ),
            )
            PhoneLine(fraction = 1f, strong = false)
            PhoneLine(fraction = 0.7f, strong = false)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaAppearance.phoneBarHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(VolaAppearance.phoneBarHeight)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun surfaceBrush() = SolidColor(MaterialTheme.colorScheme.surface)

@Composable
private fun PhoneLine(fraction: Float, strong: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(VolaAppearance.phoneLineHeight)
            .background(
                if (strong) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                CircleShape,
            ),
    )
}

/** Choices in a row; the chosen one is a filled pill (board: a connected button group). */
@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    icon: ((T) -> ImageVector)?,
    testTag: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(VolaAppearance.choiceGap),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = VolaAppearance.choiceHeight)
                    .semantics { role = Role.RadioButton }
                    .testTag(testTag(option)),
                shape = if (isSelected) {
                    VolaAppearance.choiceSelectedShape
                } else {
                    VolaAppearance.choiceShape
                },
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = VolaAppearance.choiceIconGap),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon?.let { iconOf ->
                        Icon(
                            iconOf(option),
                            contentDescription = null,
                            modifier = Modifier.size(VolaAppearance.choiceIconSize),
                        )
                        Spacer(Modifier.width(VolaAppearance.choiceIconGap))
                    }
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Palettes as round swatches in their own colors; the chosen one is ringed. */
@Composable
private fun PaletteSwatches(
    selected: BrowserColorPalette,
    onSelect: (BrowserColorPalette) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BrowserColorPalette.entries.forEach { palette ->
            val isSelected = palette == selected
            Column(
                modifier = Modifier
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(palette) },
                        role = Role.RadioButton,
                    )
                    .testTag(AppearanceMainTestTags.palette(palette)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VolaAppearance.swatchLabelGap),
            ) {
                PaletteSwatch(palette, isSelected)
                Text(
                    palette.displayName(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(palette: BrowserColorPalette, selected: Boolean) {
    val colors = paletteColors(palette)
    val ring = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier.size(
            VolaAppearance.swatchSize +
                (VolaAppearance.swatchRing + VolaAppearance.swatchRingGap) * 2,
        ),
    ) {
        val ringWidth = VolaAppearance.swatchRing.toPx()
        val inset = ringWidth + VolaAppearance.swatchRingGap.toPx()
        if (selected) {
            drawCircle(
                color = ring,
                radius = size.minDimension / 2f - ringWidth / 2f,
                style = Stroke(width = ringWidth),
            )
        }
        val diameter = size.minDimension - inset * 2f
        val sweep = 360f / colors.size
        colors.forEachIndexed { index, color ->
            drawArc(
                color = color,
                startAngle = -90f + sweep * index,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(inset, inset),
                size = Size(diameter, diameter),
            )
        }
    }
}

/** What each palette brings: workspace accents, the wallpaper's colors, or black and white. */
@Composable
private fun paletteColors(palette: BrowserColorPalette): List<Color> = when (palette) {
    BrowserColorPalette.Vola -> listOf(
        WorkspaceAccent.Violet.color(),
        WorkspaceAccent.Teal.color(),
        WorkspaceAccent.Coral.color(),
    )
    BrowserColorPalette.Dynamic -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val wallpaper = dynamicLightColorScheme(LocalContext.current)
        listOf(wallpaper.primaryContainer, wallpaper.tertiary)
    } else {
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiary)
    }
    BrowserColorPalette.Neutral -> listOf(VolaAppearance.monoLight, VolaAppearance.monoDark)
}
