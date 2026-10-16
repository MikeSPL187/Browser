package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 find capsule as canvas screen «с1»: found, counting and empty, light and dark. Not localized. */

private const val PREVIEW_WIDTH_DP = 390

private val Labels = V5FindInPageLabels(
    placeholder = "Найти на странице",
    counting = "Идёт подсчёт совпадений",
    previous = "Предыдущее совпадение",
    next = "Следующее совпадение",
    done = "Отмена",
)

@Composable
private fun FindPreview(query: String, matchText: String, counting: Boolean, dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Box(Modifier.background(V5Theme.colors.background).padding(vertical = V5Spacing.space2)) {
            V5FindInPage(
                query = query,
                onQueryChange = {},
                matchText = matchText,
                isCounting = counting,
                canNavigate = matchText.isNotEmpty(),
                labels = Labels,
                onPrevious = {},
                onNext = {},
                onDone = {},
            )
        }
    }
}

@Preview(name = "Find in page v5 · 2 of 7", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageFoundPreview() = FindPreview("сияни", "2/7", counting = false, dark = false)

@Preview(name = "Find in page v5 · 2 of 7, dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageFoundDarkPreview() = FindPreview("сияни", "2/7", counting = false, dark = true)

@Preview(name = "Find in page v5 · counting", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageCountingPreview() = FindPreview("север", "", counting = true, dark = false)

@Preview(name = "Find in page v5 · empty", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageEmptyPreview() = FindPreview("", "", counting = false, dark = false)
