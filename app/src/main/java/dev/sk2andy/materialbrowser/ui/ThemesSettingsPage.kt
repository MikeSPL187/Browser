package dev.sk2andy.materialbrowser.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserColorPalette
import dev.sk2andy.materialbrowser.data.BrowserDensity
import dev.sk2andy.materialbrowser.data.BrowserShapeStyle
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.ui.theme.ListDensityRules
import dev.sk2andy.materialbrowser.shared.ui.theme.LocalBrowserDensity
import dev.sk2andy.materialbrowser.ui.theme.VolaColorRules
import dev.sk2andy.materialbrowser.ui.theme.VolaSchemes
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaThemesTokens
import dev.sk2andy.materialbrowser.ui.theme.auraBrush
import dev.sk2andy.materialbrowser.ui.theme.browserChromeColor
import dev.sk2andy.materialbrowser.ui.theme.color

internal object ThemesSettingsTestTags {
    const val Preview = "themes_settings_preview"
    const val AccentWorkspace = "themes_settings_accent:workspace"
    fun theme(palette: BrowserColorPalette) = "themes_settings_theme:${palette.stableId}"
    fun accent(accent: WorkspaceAccent) = "themes_settings_accent:${accent.wireValue}"
    fun shape(shape: BrowserShapeStyle) = "themes_settings_shape:${shape.stableId}"
    fun density(density: BrowserDensity) = "themes_settings_density:${density.stableId}"
}

/** The small page shows a quarter of a row's density step between its lines. */
private const val PREVIEW_DENSITY_SCALE = 4

/** The accent choices: the space's own first, then every workspace accent. */
internal object ThemesSettingsRules {
    val accentChoices: List<WorkspaceAccent?> = listOf(null) + WorkspaceAccent.entries
}

/**
 * «Themes» (board W-Themes): a small phone that shows every choice live, the theme tiles, then
 * the accent and the corners on one card. Opens from «Appearance».
 */
@Composable
internal fun ThemesSettingsPage(
    settings: AppearanceSettings,
    workspaceAccent: WorkspaceAccent,
    onSettingsChanged: (AppearanceSettings) -> Unit,
    onBack: () -> Unit,
) {
    SettingsPage(
        title = stringResource(R.string.settings_themes_title),
        onBack = onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VolaThemesTokens.sectionGap)) {
            ThemePreview(settings.colorPalette)
            ThemeTiles(
                selected = settings.colorPalette,
                accent = VolaColorRules.accent(workspaceAccent, settings.accentOverride),
                onSelect = { palette -> onSettingsChanged(settings.copy(colorPalette = palette)) },
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = VolaThemesTokens.cardShape,
                color = browserChromeColor(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.padding(VolaThemesTokens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(VolaThemesTokens.cardGap),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppearanceGroupTitle(
                            stringResource(R.string.settings_themes_accent),
                            Modifier.weight(1f),
                        )
                        Text(
                            settings.accentOverride?.displayName()
                                ?: stringResource(R.string.settings_themes_accent_workspace),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AccentChoices(
                        selected = settings.accentOverride,
                        onSelect = { accent ->
                            onSettingsChanged(settings.copy(accentOverride = accent))
                        },
                    )
                    AppearanceGroupTitle(stringResource(R.string.settings_shape_style))
                    AppearanceChoiceRow(
                        options = BrowserShapeStyle.entries,
                        selected = settings.shapeStyle,
                        label = { shape -> shape.displayName() },
                        icon = null,
                        testTag = ThemesSettingsTestTags::shape,
                        onSelect = { shape -> onSettingsChanged(settings.copy(shapeStyle = shape)) },
                    )
                    AppearanceGroupTitle(stringResource(R.string.settings_density))
                    AppearanceChoiceRow(
                        options = BrowserDensity.entries,
                        selected = settings.density,
                        label = { density -> density.displayName() },
                        icon = null,
                        testTag = ThemesSettingsTestTags::density,
                        onSelect = { density ->
                            onSettingsChanged(settings.copy(density = density))
                        },
                    )
                }
            }
        }
    }
}

/**
 * The page on the aura as a small phone. It draws with the live theme, so the colors, the accent
 * and the corners change in it as soon as they are picked.
 */
@Composable
private fun ThemePreview(palette: BrowserColorPalette) {
    val label = stringResource(R.string.settings_themes_preview, palette.displayName())
    val colors = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolaThemesTokens.previewHeight)
            .clip(VolaThemesTokens.previewShape)
            .background(VolaTheme.auraBrush)
            .semantics { contentDescription = label }
            .testTag(ThemesSettingsTestTags.Preview),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .size(VolaThemesTokens.previewPhoneWidth, VolaThemesTokens.previewPhoneHeight)
                .background(
                    VolaTheme.extendedColors.card,
                    shapes.extraLarge.copy(
                        bottomStart = CornerSize(0),
                        bottomEnd = CornerSize(0),
                    ),
                )
                .padding(VolaThemesTokens.previewPhonePadding),
            // The rows of the small page move apart or together with the density.
            verticalArrangement = Arrangement.spacedBy(
                VolaThemesTokens.previewPhoneGap +
                    ListDensityRules.step(LocalBrowserDensity.current) / PREVIEW_DENSITY_SCALE,
            ),
        ) {
            PreviewLine(
                fraction = 0.7f,
                height = VolaThemesTokens.previewTitleHeight,
                color = colors.onSurface.copy(alpha = VolaThemesTokens.PREVIEW_TITLE_ALPHA),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaThemesTokens.previewImageHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.primaryContainer, colors.tertiaryContainer),
                        ),
                        shapes.medium,
                    ),
            )
            PreviewLine(fraction = 1f, color = colors.outlineVariant)
            PreviewLine(fraction = 0.85f, color = colors.outlineVariant)
            PreviewLine(fraction = 0.6f, color = colors.outlineVariant)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolaThemesTokens.previewIslandHeight)
                    .background(colors.surfaceContainer, CircleShape)
                    .padding(VolaThemesTokens.previewIslandPadding),
                horizontalArrangement = Arrangement.spacedBy(VolaThemesTokens.previewIslandPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(VolaThemesTokens.previewGemSize)
                        .background(colors.primary, shapes.small),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(VolaThemesTokens.previewGemSize)
                        .background(colors.surfaceContainerLowest, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun PreviewLine(
    fraction: Float,
    color: Color,
    height: Dp = VolaThemesTokens.previewLineHeight,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .background(color, CircleShape),
    )
}

/** The six palettes as tiles in rows of three; the chosen one is ringed. */
@Composable
private fun ThemeTiles(
    selected: BrowserColorPalette,
    accent: WorkspaceAccent,
    onSelect: (BrowserColorPalette) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(VolaThemesTokens.tileRowGap),
    ) {
        BrowserColorPalette.entries.chunked(VolaThemesTokens.TILES_PER_ROW).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { palette ->
                    val isSelected = palette == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(palette) },
                                role = Role.RadioButton,
                            )
                            .testTag(ThemesSettingsTestTags.theme(palette)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(VolaThemesTokens.tileLabelGap),
                    ) {
                        val ring by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "theme tile ring",
                        )
                        Box(
                            modifier = Modifier
                                .border(
                                    VolaThemesTokens.tileRing,
                                    ring,
                                    VolaThemesTokens.tileRingShape,
                                )
                                .padding(VolaThemesTokens.tileRing + VolaThemesTokens.tileRingGap),
                        ) {
                            ThemeTile(
                                palette = palette,
                                accent = accent,
                                size = VolaThemesTokens.tileSize,
                                shape = VolaThemesTokens.tileShape,
                            )
                        }
                        Text(
                            palette.displayName(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** One theme as a small gradient tile, the same in the light and the dark theme. */
@Composable
internal fun ThemeTile(
    palette: BrowserColorPalette,
    accent: WorkspaceAccent,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Brush.linearGradient(themeTileColors(palette, accent)), shape),
    )
}

/** What each palette looks like: the accent's containers, the wallpaper's, or the board's tile. */
@Composable
private fun themeTileColors(palette: BrowserColorPalette, accent: WorkspaceAccent): List<Color> =
    when (palette) {
        BrowserColorPalette.Vola -> VolaSchemes.forAccent(accent).light.let { tokens ->
            listOf(Color(tokens.primaryContainer), Color(tokens.tertiaryContainer))
        }
        BrowserColorPalette.Ice -> VolaThemesTokens.iceTile
        BrowserColorPalette.Dusk -> VolaThemesTokens.duskTile
        BrowserColorPalette.Paper -> VolaThemesTokens.paperTile
        BrowserColorPalette.Mono -> VolaThemesTokens.monoTile
        BrowserColorPalette.Dynamic -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wallpaper = dynamicLightColorScheme(LocalContext.current)
            listOf(wallpaper.primaryContainer, wallpaper.tertiaryContainer)
        } else {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
    }

/** «Same as the space» and the eight accents, five to a row with 48 dp targets. */
@Composable
private fun AccentChoices(
    selected: WorkspaceAccent?,
    onSelect: (WorkspaceAccent?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        ThemesSettingsRules.accentChoices.chunked(VolaThemesTokens.ACCENTS_PER_ROW).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { accent ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AccentChoice(
                            accent = accent,
                            selected = accent == selected,
                            onClick = { onSelect(accent) },
                        )
                    }
                }
                repeat(VolaThemesTokens.ACCENTS_PER_ROW - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One accent dot; null is the space's own accent, drawn as a wheel of accents. */
@Composable
private fun AccentChoice(
    accent: WorkspaceAccent?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = accent?.color() ?: MaterialTheme.colorScheme.primary
    val label = accent?.displayName() ?: stringResource(R.string.settings_themes_accent_workspace)
    val ring by animateColorAsState(
        if (selected) color else Color.Transparent,
        label = "accent ring",
    )
    val dotSize by animateDpAsState(
        if (selected) VolaThemesTokens.accentDotSelected else VolaThemesTokens.accentDot,
        label = "accent dot",
    )
    Box(
        modifier = Modifier
            .size(VolaThemesTokens.accentTarget)
            .clip(CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label }
            .testTag(
                accent?.let(ThemesSettingsTestTags::accent)
                    ?: ThemesSettingsTestTags.AccentWorkspace,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(VolaThemesTokens.accentDot + VolaThemesTokens.accentRing * 2)
                .border(VolaThemesTokens.accentRing, ring, CircleShape),
        )
        if (accent == null) {
            AccentWheel(Modifier.size(dotSize))
        } else {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(color, CircleShape),
            )
        }
        if (selected) {
            Icon(
                VolaIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(VolaThemesTokens.accentCheckSize),
                tint = if (
                    accent == null || color.luminance() <= VolaThemesTokens.ACCENT_LIGHT_LUMINANCE
                ) {
                    VolaThemesTokens.checkOnDeep
                } else {
                    VolaThemesTokens.checkOnLight
                },
            )
        }
    }
}

/** Four accents around a circle: every space keeps its own color. */
@Composable
private fun AccentWheel(modifier: Modifier) {
    val colors = listOf(
        WorkspaceAccent.Violet.color(),
        WorkspaceAccent.Teal.color(),
        WorkspaceAccent.Amber.color(),
        WorkspaceAccent.Rose.color(),
    )
    Canvas(modifier) {
        val sweep = 360f / colors.size
        colors.forEachIndexed { index, color ->
            drawArc(
                color = color,
                startAngle = -90f + sweep * index,
                sweepAngle = sweep,
                useCenter = true,
            )
        }
    }
}
