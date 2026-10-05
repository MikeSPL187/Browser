package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.VolaAppearance
import dev.sk2andy.materialbrowser.ui.theme.VolaColorRules
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaThemesTokens
import dev.sk2andy.materialbrowser.ui.theme.auraBrush

internal object AppearanceMainTestTags {
    fun chromeStyle(style: BrowserChromeStyle) =
        "appearance_settings_chrome_style:${style.stableId}"
    fun mode(mode: BrowserAppearanceMode) = "appearance_settings_mode:${mode.stableId}"
    const val Themes = "appearance_settings_themes"
}

/** The board's order for the theme: light, dark, then following the system («Auto»). */
private val MODE_ORDER = listOf(
    BrowserAppearanceMode.Light,
    BrowserAppearanceMode.Dark,
    BrowserAppearanceMode.System,
)

/**
 * The main part of «Appearance» (board W-SetAppearance): the shell's style as two cards with a
 * small phone each, the light or dark theme as a row of choices, then the way into «Themes».
 */
@Composable
internal fun ColumnScope.AppearanceMainSections(
    settings: AppearanceSettings,
    workspaceAccent: WorkspaceAccent,
    onSettingsChanged: (AppearanceSettings) -> Unit,
    onOpenThemes: () -> Unit,
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
        AppearanceGroupTitle(stringResource(R.string.settings_appearance_mode))
        AppearanceChoiceRow(
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
    ThemesEntry(settings, workspaceAccent, onOpenThemes)
}

/** The way into «Themes»: the current theme's tile, its name, the accent and the corners. */
@Composable
private fun ThemesEntry(
    settings: AppearanceSettings,
    workspaceAccent: WorkspaceAccent,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VolaThemesTokens.entryMinHeight)
            .testTag(AppearanceMainTestTags.Themes),
        shape = VolaAppearance.groupShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(VolaAppearance.groupPadding),
            horizontalArrangement = Arrangement.spacedBy(VolaThemesTokens.entryGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeTile(
                palette = settings.colorPalette,
                accent = VolaColorRules.accent(workspaceAccent, settings.accentOverride),
                size = VolaThemesTokens.entryTileSize,
                shape = VolaThemesTokens.entryTileShape,
            )
            Column(modifier = Modifier.weight(1f)) {
                AppearanceGroupTitle(stringResource(R.string.settings_themes_title))
                Text(
                    SettingsHomeSummaryRules.join(
                        listOf(
                            settings.colorPalette.displayName(),
                            settings.accentOverride?.displayName(),
                            settings.shapeStyle.displayName(),
                        ),
                    ).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                VolaIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
internal fun AppearanceGroupTitle(text: String, modifier: Modifier = Modifier) {
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
internal fun <T> AppearanceChoiceRow(
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
