package dev.sk2andy.materialbrowser.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons

/* Gallery of the Material Symbols Rounded icons in VolaIcons. Preview-only labels are the
 * icon names and are not localized. */

private val IconCellWidth = 76.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolaIconsGallery(mode: BrowserAppearanceMode) {
    MaterialBrowserTheme(settings = AppearanceSettings(appearanceMode = mode)) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VolaIcons.all.forEach { icon ->
                    Column(
                        modifier = Modifier.width(IconCellWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = icon.name.removePrefix("VolaIcons."),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "VolaIcons · light", widthDp = 400)
@Composable
private fun VolaIconsLightPreview() {
    VolaIconsGallery(BrowserAppearanceMode.Light)
}

@Preview(
    name = "VolaIcons · dark",
    widthDp = 400,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun VolaIconsDarkPreview() {
    VolaIconsGallery(BrowserAppearanceMode.Dark)
}
