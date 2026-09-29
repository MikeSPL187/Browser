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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode

@Composable
private fun VolaPalettePreviewContent(mode: BrowserAppearanceMode) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = mode)) {
        val colors = MaterialTheme.colorScheme
        Surface(color = colors.surface) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = VolaBrand.Ink,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launcher_foreground_art),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(48.dp),
                            tint = Color.Unspecified,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(VolaBrand.linearGradient()),
                    )
                }
                PaletteRow(
                    "primary" to colors.primary,
                    "primaryCont." to colors.primaryContainer,
                    "tertiary" to colors.tertiary,
                    "tertiaryCont." to colors.tertiaryContainer,
                )
                PaletteRow(
                    "surface" to colors.surface,
                    "containerLow" to colors.surfaceContainerLow,
                    "container" to colors.surfaceContainer,
                    "containerHigh" to colors.surfaceContainerHigh,
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(vararg swatches: Pair<String, Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.forEach { (name, color) ->
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(color),
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(name = "Vola palette · light", widthDp = 360)
@Composable
private fun VolaPaletteLightPreview() {
    VolaPalettePreviewContent(BrowserAppearanceMode.Light)
}

@Preview(
    name = "Vola palette · dark",
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VolaPaletteDarkPreview() {
    VolaPalettePreviewContent(BrowserAppearanceMode.Dark)
}

@Preview(name = "Vola palette · AMOLED", widthDp = 360)
@Composable
private fun VolaPaletteAmoledPreview() {
    VolaPalettePreviewContent(BrowserAppearanceMode.Amoled)
}
