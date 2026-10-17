package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 slider (canvas «ц3»): text size in steps of 10 %, and a disabled one. */

@Composable
private fun SliderPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Column(Modifier.background(V5Theme.colors.background).padding(V5Spacing.space4)) {
            V5Slider(value = 130f, onValueChange = {}, valueRange = 50f..250f, steps = 19)
            V5Slider(value = 0.4f, onValueChange = {}, enabled = false)
        }
    }
}

@Preview(name = "V5Slider · light")
@Composable
private fun V5SliderLightPreview() = SliderPreview(dark = false)

@Preview(name = "V5Slider · dark")
@Composable
private fun V5SliderDarkPreview() = SliderPreview(dark = true)
