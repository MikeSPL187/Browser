package dev.sk2andy.materialbrowser.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode

/*
 * Theme previews: every workspace accent (and private mode) in the light and the pure black dark
 * theme, with the aura, the main color roles, buttons and the v4 type scale. Preview-only
 * sample text is not localized.
 */

private const val PREVIEW_WIDTH_DP = 760
private const val PREVIEW_HEIGHT_DP = 1420
private val SwatchWidth = 70.dp
private val SwatchHeight = 36.dp
private val AuraHeight = 64.dp

@Composable
private fun WorkspaceGridPreviewContent(mode: BrowserAppearanceMode) {
    val settings = AppearanceSettings(appearanceMode = mode)
    MaterialBrowserTheme(settings = settings) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(VolaSpacing.x4),
                verticalArrangement = Arrangement.spacedBy(VolaSpacing.x4),
            ) {
                WorkspaceAccent.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x4)) {
                        pair.forEach { accent ->
                            Box(modifier = Modifier.weight(1f)) {
                                MaterialBrowserTheme(
                                    settings = settings,
                                    workspaceAccent = accent,
                                ) {
                                    WorkspaceSample(name = accent.name)
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x4)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MaterialBrowserTheme(settings = settings, privateMode = true) {
                            WorkspaceSample(name = "Private")
                        }
                    }
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSample(name: String) {
    val colors = MaterialTheme.colorScheme
    val extended = VolaTheme.extendedColors
    Surface(
        shape = VolaShapes.card,
        color = extended.card,
        shadowElevation = VolaElevation.level1,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AuraHeight)
                    .background(Brush.verticalGradient(extended.aura)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = VolaSpacing.x5),
                    horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(VolaSpacing.x8)
                            .clip(MaterialTheme.shapes.small)
                            .background(colors.primary),
                    )
                    Text(
                        text = name,
                        style = VolaTypeScale.titleLarge,
                        color = colors.onSurface,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(VolaSpacing.x5),
                verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
            ) {
                SwatchRow(
                    "primary" to colors.primary,
                    "primaryC" to colors.primaryContainer,
                    "secondaryC" to colors.secondaryContainer,
                    "tertiaryC" to colors.tertiaryContainer,
                )
                SwatchRow(
                    "highest" to colors.surfaceContainerHighest,
                    "ok" to extended.ok,
                    "warn" to extended.warn,
                    "error" to colors.error,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2)) {
                    Button(onClick = {}) { Text("Open") }
                    FilledTonalButton(onClick = {}) { Text("Group") }
                    OutlinedButton(onClick = {}) { Text("Close") }
                }
                Text(
                    text = "north-guide.ru",
                    style = VolaTypeScale.title,
                    color = colors.onSurface,
                )
                Text(
                    text = "Own cookies, sign-ins and site data",
                    style = VolaTypeScale.body,
                    color = colors.onSurface,
                )
                Text(
                    text = "Updated 5 minutes ago",
                    style = VolaTypeScale.caption,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SwatchRow(vararg swatches: Pair<String, Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(VolaSpacing.x2)) {
        swatches.forEach { (label, color) ->
            Column(
                modifier = Modifier.width(SwatchWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SwatchHeight)
                        .clip(MaterialTheme.shapes.small)
                        .background(color),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TypeScalePreviewContent(mode: BrowserAppearanceMode) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = mode)) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(VolaSpacing.x4),
                verticalArrangement = Arrangement.spacedBy(VolaSpacing.x2),
            ) {
                with(VolaTypeScale) {
                    Text("Workspaces", style = display)
                    Text("Tab overview", style = headline)
                    Text("Workspace settings", style = titleLarge)
                    Text("north-guide.ru", style = title)
                    Text("Own cookies, sign-ins and site data", style = body)
                    Text("Open in group", style = label)
                    Text(
                        text = "Updated 5 minutes ago",
                        style = caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Essentials".uppercase(),
                        style = overline,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Ice on Lake Baikal grows strong by mid-February.",
                        style = reading,
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Workspaces · light",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Composable
private fun WorkspacesLightPreview() {
    WorkspaceGridPreviewContent(BrowserAppearanceMode.Light)
}

@Preview(
    name = "Workspaces · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WorkspacesDarkPreview() {
    WorkspaceGridPreviewContent(BrowserAppearanceMode.Dark)
}

@Preview(name = "Type scale · light", widthDp = 360)
@Composable
private fun TypeScaleLightPreview() {
    TypeScalePreviewContent(BrowserAppearanceMode.Light)
}

@Preview(
    name = "Type scale · dark · 200 %",
    widthDp = 360,
    fontScale = 2f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TypeScaleDarkLargeFontPreview() {
    TypeScalePreviewContent(BrowserAppearanceMode.Dark)
}
