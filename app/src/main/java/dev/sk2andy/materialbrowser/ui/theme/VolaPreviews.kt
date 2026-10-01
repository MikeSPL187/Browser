package dev.sk2andy.materialbrowser.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * The preview matrix every new Vola screen is checked against (docs/vola/ROADMAP.md, section 4):
 * light and dark, the largest system font, a narrow phone and a tablet-wide window. Annotate one
 * preview function with it instead of writing the five previews by hand.
 *
 * The previewed content should follow the system appearance ([dev.sk2andy.materialbrowser.data
 * .BrowserAppearanceMode.System]), so the dark entry renders in the dark theme.
 */
@Preview(name = "Light", group = "Vola", widthDp = 412, showBackground = true)
@Preview(
    name = "Dark",
    group = "Vola",
    widthDp = 412,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Font 200 %", group = "Vola", widthDp = 412, showBackground = true, fontScale = 2f)
@Preview(name = "Narrow phone", group = "Vola", widthDp = 320, showBackground = true)
@Preview(name = "Tablet", group = "Vola", widthDp = 840, showBackground = true)
annotation class VolaPreviews
