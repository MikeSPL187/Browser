package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme

/* Sync through your own server as canvas screens «х1»–«х4», light and dark. */

private const val PREVIEW_WIDTH_DP = 390
private const val PREVIEW_HEIGHT_DP = 844

private val NoConnectActions = V5WebDavConnectActions(
    onBack = {}, onAddress = {}, onLogin = {}, onPassword = {}, onPhrase = {}, onPhraseRepeat = {}, onConnect = {},
)
private val Filled = V5WebDavConnectState(
    address = "https://cloud.anna.ru",
    login = "anna",
    password = "app-password-1234",
    phrase = "лиса-кедр-зима-окно",
    phraseRepeat = "лиса-кедр-зима-окно",
)
private val Devices = listOf(
    V5WebDavDevice("tablet", "Планшет Анны", tabs = 8, updated = "сегодня в 12:10"),
    V5WebDavDevice("pixel", "Pixel 7", tabs = 3, updated = "вчера"),
)
private val Status = V5WebDavStatusState("cloud.anna.ru", "Сегодня в 13:42", syncing = false, error = null, devices = Devices)

@Preview(name = "Connect · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavConnectLightPreview() {
    VolaV5Theme(dark = false) { V5WebDavConnect(Filled, NoConnectActions) }
}

@Preview(name = "Connect, http and phrases differ · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavConnectProblemsDarkPreview() {
    VolaV5Theme(dark = true) {
        V5WebDavConnect(Filled.copy(address = "http://cloud.anna.ru", phraseRepeat = "лиса-кедр"), NoConnectActions)
    }
}

@Composable
private fun StatusPage(state: V5WebDavStatusState) {
    Column(
        Modifier.fillMaxSize().background(V5Theme.colors.background).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
    ) {
        V5PageBar(title = stringResource(R.string.v5_sync_title), onBack = {})
        V5WebDavStatus(state, onSyncNow = {}, onOpenDevice = {}, modifier = Modifier.padding(top = V5Spacing.space2))
        V5WebDavDisconnectRow(onDisconnect = {})
    }
}

@Preview(name = "Status · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavStatusLightPreview() {
    VolaV5Theme(dark = false) { StatusPage(Status) }
}

@Preview(name = "Status, unreachable, no devices · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavStatusErrorDarkPreview() {
    VolaV5Theme(dark = true) { StatusPage(Status.copy(error = V5WebDavError.Unreachable, devices = emptyList())) }
}

@Preview(name = "Device tabs · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavDeviceTabsPreview() {
    VolaV5Theme(dark = false) {
        V5WebDavDeviceTabs(
            device = Devices.first(),
            tabs = listOf(
                V5WebDavTab("https://developer.android.com/", "Как устроены пружины в Compose", "developer.android.com"),
                V5WebDavTab("https://docs.nextcloud.com/", "Nextcloud — пароли приложений", "docs.nextcloud.com"),
                V5WebDavTab("https://eda.ru/", "", "eda.ru"),
            ),
            onBack = {},
            onOpen = {},
        )
    }
}

@Preview(name = "Phrase mismatch · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavPhraseMismatchPreview() {
    VolaV5Theme(dark = true) { V5WebDavPhraseMismatch("cloud.anna.ru", onRetry = {}, onStartOver = {}) }
}

@Preview(name = "Disconnect sheet · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavDisconnectSheetPreview() {
    VolaV5Theme(dark = false) {
        V5WebDavDisconnectSheet(visible = true, host = "cloud.anna.ru", onDisconnect = {}, onDisconnectAndDelete = {}, onDismiss = {})
    }
}

@Preview(name = "Status, server full · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavStatusFullLightPreview() {
    VolaV5Theme(dark = false) { StatusPage(Status.copy(error = V5WebDavError.ServerFull)) }
}

@Preview(name = "Status, newer format · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavStatusNewerDarkPreview() {
    VolaV5Theme(dark = true) { StatusPage(Status.copy(error = V5WebDavError.NewerFormat)) }
}

@Preview(name = "Delete failed sheet · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavDeleteFailedLightPreview() {
    VolaV5Theme(dark = false) { V5WebDavDeleteFailedSheet(visible = true, host = "cloud.anna.ru", onRetry = {}, onDismiss = {}) }
}

@Preview(name = "Delete failed sheet · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavDeleteFailedDarkPreview() {
    VolaV5Theme(dark = true) { V5WebDavDeleteFailedSheet(visible = true, host = "cloud.anna.ru", onRetry = {}, onDismiss = {}) }
}

@Preview(name = "Connect, address with a login and a simple phrase · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavConnectInvalidLightPreview() {
    VolaV5Theme(dark = false) {
        V5WebDavConnect(
            Filled.copy(address = "https://anna@cloud.anna.ru", phrase = "aaaaaaaaaa", phraseRepeat = "aaaaaaaaaa"),
            NoConnectActions,
        )
    }
}

@Preview(name = "Connect, connecting · dark", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Composable
private fun V5WebDavConnectConnectingDarkPreview() {
    VolaV5Theme(dark = true) { V5WebDavConnect(Filled.copy(connecting = true), NoConnectActions) }
}
