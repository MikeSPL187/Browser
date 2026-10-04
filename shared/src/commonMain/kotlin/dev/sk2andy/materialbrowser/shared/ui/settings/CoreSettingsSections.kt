package dev.sk2andy.materialbrowser.shared.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.browser.PageTranslationProvider

data class TranslationProviderSettingsStrings(
    val title: String,
    val providerNames: Map<PageTranslationProvider, String>,
    val providerSummaries: Map<PageTranslationProvider, String>,
)

@Composable
fun TranslationProviderSettings(
    provider: PageTranslationProvider,
    strings: TranslationProviderSettingsStrings,
    containerColor: Color,
    enabled: Boolean,
    onProviderChanged: (PageTranslationProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        SettingsChoice(
            title = strings.title,
            value = strings.providerNames.getValue(provider),
            expanded = menuExpanded,
            onClick = { menuExpanded = true },
            containerColor = containerColor,
            enabled = enabled,
            modifier = modifier,
        )
        SettingsDropdown(
            expanded = enabled && menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            PageTranslationProvider.entries.forEach { candidate ->
                SettingsDropdownItem(
                    label = strings.providerNames.getValue(candidate),
                    selected = candidate == provider,
                    onClick = {
                        menuExpanded = false
                        onProviderChanged(candidate)
                    },
                )
            }
        }
    }
    Text(
        strings.providerSummaries.getValue(provider),
        modifier = Modifier.padding(start = 18.dp, top = 6.dp, end = 18.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
