package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Address bar command answers above the v5 bar (V28, B6b). Preview text is not localized. */

@Composable
private fun CommandNoticesPreview(dark: Boolean) {
    VolaV5Theme(dark = dark) {
        Column(Modifier.background(V5Theme.colors.chrome)) {
            V5CommandNoticeRow(V5CommandNotice("Кэш очищен, страница перезагружена", rejected = false))
            V5CommandNoticeRow(V5CommandNotice("Закрыто 3 одинаковые вкладки", rejected = false))
            V5CommandNoticeRow(V5CommandNotice("Такой команды нет", rejected = true))
        }
    }
}

@Preview(name = "Command answers · light")
@Composable
private fun V5CommandNoticeLightPreview() = CommandNoticesPreview(dark = false)

@Preview(name = "Command answers · dark")
@Composable
private fun V5CommandNoticeDarkPreview() = CommandNoticesPreview(dark = true)
