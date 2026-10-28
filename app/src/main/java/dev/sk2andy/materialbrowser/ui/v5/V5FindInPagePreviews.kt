package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 find capsule as canvas screen «с1»: found, not found, counting and empty, light and dark. Not localized. */

private const val PREVIEW_WIDTH_DP = 390

private val Labels = V5FindInPageLabels(
    placeholder = "Найти на странице",
    counting = "Идёт подсчёт совпадений",
    previous = "Предыдущее совпадение",
    next = "Следующее совпадение",
    done = "Готово",
)

@Composable
private fun FindPreview(
    query: String,
    matchText: String,
    counting: Boolean,
    dark: Boolean,
    found: Boolean = matchText.isNotEmpty(),
    accent: V5Accent = V5Accent.Default,
) {
    VolaV5Theme(dark = dark, accent = accent) {
        Box(Modifier.background(V5Theme.colors.background).padding(vertical = V5Spacing.space2)) {
            V5FindInPage(
                query = query,
                onQueryChange = {},
                matchText = matchText,
                isCounting = counting,
                canNavigate = found,
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
private fun V5FindInPageFoundPreview() = FindPreview("сияни", "2 из 7", counting = false, dark = false)

@Preview(name = "Find in page v5 · 2 of 7, dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageFoundDarkPreview() = FindPreview("сияни", "2 из 7", counting = false, dark = true)

@Preview(name = "Find in page v5 · counting", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageCountingPreview() = FindPreview("север", "", counting = true, dark = false)

@Preview(name = "Find in page v5 · empty", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageEmptyPreview() = FindPreview("", "", counting = false, dark = false)

@Preview(name = "Find in page v5 · not found", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageNotFoundPreview() = FindPreview("сиянье", "Не найдено", counting = false, dark = false, found = false)

@Preview(name = "Find in page v5 · not found, dark", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageNotFoundDarkPreview() = FindPreview("сиянье", "Не найдено", counting = false, dark = true, found = false)

@Preview(name = "Find in page v5 · violet accent", widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun V5FindInPageAccentPreview() = FindPreview("сияни", "2 из 7", counting = false, dark = false, accent = V5Accent.Purple)

@Preview(name = "Find in page v5 · largest font", widthDp = PREVIEW_WIDTH_DP, fontScale = 2f)
@Composable
private fun V5FindInPageLargeFontPreview() = FindPreview("сияни", "2 из 7", counting = false, dark = false)

/** At the largest font «Not found» stands over the bar, not cut inside the capsule. */
@Preview(name = "Find in page v5 · not found, largest font, dark", widthDp = PREVIEW_WIDTH_DP, fontScale = 2f)
@Composable
private fun V5FindInPageNotFoundLargeFontPreview() = FindPreview("териберк", "Не найдено", counting = false, dark = true, found = false)
