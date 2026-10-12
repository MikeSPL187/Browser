package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* The v5 private new tab as canvas screen «h1»: GeckoView, and WebView sharing cookies. Not localized. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val GeckoState = V5PrivateNewTabState(
    body = "Vola не запомнит историю, cookie и данные форм. Закройте частные вкладки — и всё исчезнет.",
    facts = listOf(
        V5PrivateFact(R.drawable.ic_history, "Без истории", "Адреса и запросы не попадают в историю"),
        V5PrivateFact(R.drawable.ic_symbol_cookie, "Ничего на диске", "Cookie, кэш и данные сайтов — только в памяти"),
        V5PrivateFact(R.drawable.ic_symbol_shield, "Та же защита", "Трекеры блокируются, но не попадают в отчёт"),
    ),
    lock = V5PrivateLock(checked = true, available = true, caption = "Открыть — только по биометрии"),
    privateTabCount = 2,
)

@Composable
private fun PrivatePreview(state: V5PrivateNewTabState) {
    VolaV5Theme(privateBrowsing = true) {
        V5PrivateNewTab(state = state, onLockChange = {}, onCloseAll = {})
    }
}

@Preview(name = "Private new tab v5 · GeckoView", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivateNewTabPreview() {
    PrivatePreview(GeckoState)
}

@Preview(name = "Private new tab v5 · WebView, shared cookies", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5PrivateNewTabSharedPreview() {
    PrivatePreview(
        GeckoState.copy(
            body = "Vola не сохраняет историю того, что вы здесь делаете.",
            facts = GeckoState.facts.toMutableList().apply {
                set(1, V5PrivateFact(R.drawable.ic_symbol_cookie, "Общие cookie", "Сайты видят ваши входы из обычных вкладок", warning = true))
            },
            lock = V5PrivateLock(checked = false, available = false, caption = "Недоступно: сначала настройте биометрию"),
            privateTabCount = 0,
        ),
    )
}
