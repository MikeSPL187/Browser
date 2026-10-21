package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.credentials.PasswordGeneratorRules
import dev.sk2andy.materialbrowser.shared.credentials.PasswordStrength
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavAddressCheck
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncAddressRules
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

/*
 * Sync through your own WebDAV or Nextcloud server (V27, canvas «х»). Only the screens: the binding
 * gives the state and does the work through the engine's port. Tabs and bookmarks live on the
 * server encrypted with a phrase only the person knows; sync runs on opening and leaving Vola and
 * on «Sync now», never in the background (owner's decisions 17.10).
 */

object V5WebDavTestTags {
    const val Connect = "v5_webdav_connect"
    const val Address = "v5_webdav_address"
    const val Login = "v5_webdav_login"
    const val Password = "v5_webdav_password"
    const val Phrase = "v5_webdav_phrase"
    const val PhraseRepeat = "v5_webdav_phrase_repeat"
    const val ConnectButton = "v5_webdav_connect_button"
    const val Status = "v5_webdav_status"
    const val SyncNow = "v5_webdav_sync_now"
    const val DeviceTabs = "v5_webdav_device_tabs"
    const val Disconnect = "v5_webdav_disconnect"
    const val PhraseMismatch = "v5_webdav_phrase_mismatch"

    fun device(id: String) = "v5_webdav_device_$id"
}

/** What went wrong with the server, as the person can act on it. */
enum class V5WebDavError {
    /** The address answers no HTTPS: a typo, the server down or no network. */
    Unreachable,

    /** The server refused the login or the app password. */
    LoginRejected,

    /** An `http://` address: the password and the files would travel in the clear. */
    NotHttps,

    /** Something answers there, but not as WebDAV. */
    NotWebDav,

    /** The server holds Vola's data under another phrase, as after a «Start over» on another device. */
    PhraseMismatch,

    /** A newer Vola wrote the data: update, never «Start over», which would delete it. */
    NewerFormat,

    /** The address's folder is not on the server. */
    FolderNotFound,

    /** No room on the server, or a file over its limit. */
    ServerFull,

    /** A locked file, too many requests or maintenance: later. */
    ServerBusy,

    /** The server failed on its side. */
    ServerError,

    /** Not an address the sync can use: a login, a space, `?` or `#` in it, or a wrong port. */
    InvalidAddress,

    /** An address typed without `https://`, as most are: the line says to start with it (ЯС-1). */
    MissingScheme,
}

/** «Own server» (canvas «х1»): what is typed, and whether the connection is being made. */
@Immutable
data class V5WebDavConnectState(
    val address: String = "",
    val login: String = "",
    val password: String = "",
    val phrase: String = "",
    val phraseRepeat: String = "",
    val connecting: Boolean = false,
    val error: V5WebDavError? = null,
)

class V5WebDavConnectActions(
    val onBack: () -> Unit,
    val onAddress: (String) -> Unit,
    val onLogin: (String) -> Unit,
    val onPassword: (String) -> Unit,
    val onPhrase: (String) -> Unit,
    val onPhraseRepeat: (String) -> Unit,
    val onConnect: () -> Unit,
)

/** Another device's tabs on the server: its name, how many, and when it last synced (as the binding words it). */
@Immutable
data class V5WebDavDevice(val id: String, val name: String, val tabs: Int, val updated: String)

/** One tab of another device. */
@Immutable
data class V5WebDavTab(val url: String, val title: String, val host: String)

/** The server part of Settings → Sync once connected (canvas «х2»). */
@Immutable
data class V5WebDavStatusState(
    val host: String,
    /** When it last synced, as the binding words it («Today at 13:42»); null before the first time. */
    val lastSynced: String?,
    val syncing: Boolean,
    val error: V5WebDavError?,
    val devices: List<V5WebDavDevice>,
)

/**
 * «Own server» (canvas «х1»). One layer: the server, then the phrase with the plain warning that a
 * forgotten phrase cannot be recovered. «Connect» is there once everything is filled in, the
 * address is HTTPS and the two phrases match.
 */
@Composable
fun V5WebDavConnect(
    state: V5WebDavConnectState,
    actions: V5WebDavConnectActions,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val canConnect = V5WebDavRules.canConnect(state)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5WebDavTestTags.Connect),
    ) {
        V5PageBar(
            title = stringResource(R.string.v5_webdav_title),
            onBack = actions.onBack,
            actions = {
                if (state.connecting) {
                    val connecting = stringResource(R.string.v5_webdav_connecting)
                    CircularProgressIndicator(
                        color = colors.accent,
                        strokeWidth = V5Sizes.barProgress,
                        modifier = Modifier
                            .padding(end = V5Spacing.space4)
                            .size(V5Sizes.iconSmall)
                            .semantics { contentDescription = connecting },
                    )
                } else {
                    TextButton(
                        onClick = actions.onConnect,
                        enabled = canConnect,
                        modifier = Modifier.testTag(V5WebDavTestTags.ConnectButton),
                    ) {
                        Text(
                            stringResource(R.string.v5_webdav_connect),
                            style = V5Theme.type.headline,
                            color = if (canConnect) colors.accent else colors.labelTertiary,
                        )
                    }
                }
            },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            SectionHeader(stringResource(R.string.v5_webdav_server_header))
            V5NameField(
                value = state.address,
                placeholder = stringResource(R.string.v5_webdav_address),
                onValueChange = actions.onAddress,
                onDone = {},
                maxLength = V5WebDavRules.MAX_FIELD,
                capitalize = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                enabled = !state.connecting,
                modifier = Modifier.testTag(V5WebDavTestTags.Address),
            )
            V5NameField(
                value = state.login,
                placeholder = stringResource(R.string.v5_webdav_login),
                onValueChange = actions.onLogin,
                onDone = {},
                maxLength = V5WebDavRules.MAX_FIELD,
                capitalize = false,
                imeAction = ImeAction.Next,
                enabled = !state.connecting,
                modifier = Modifier.testTag(V5WebDavTestTags.Login),
            )
            V5NameField(
                value = state.password,
                placeholder = stringResource(R.string.v5_webdav_password),
                onValueChange = actions.onPassword,
                onDone = {},
                maxLength = V5WebDavRules.MAX_FIELD,
                password = true,
                imeAction = ImeAction.Next,
                enabled = !state.connecting,
                modifier = Modifier.testTag(V5WebDavTestTags.Password),
            )
            val problem = state.error ?: V5WebDavRules.addressProblem(state.address)
            if (problem != null) {
                Problem(stringResource(V5WebDavRules.errorText(problem)))
            } else {
                Footnote(stringResource(R.string.v5_webdav_server_footer))
            }
            SectionHeader(stringResource(R.string.v5_webdav_phrase_header), Modifier.padding(top = V5Spacing.space4))
            V5NameField(
                value = state.phrase,
                placeholder = stringResource(R.string.v5_webdav_phrase),
                onValueChange = actions.onPhrase,
                onDone = {},
                maxLength = V5WebDavRules.MAX_FIELD,
                password = true,
                imeAction = ImeAction.Next,
                enabled = !state.connecting,
                modifier = Modifier.testTag(V5WebDavTestTags.Phrase),
            )
            V5NameField(
                value = state.phraseRepeat,
                placeholder = stringResource(R.string.v5_webdav_phrase_repeat),
                onValueChange = actions.onPhraseRepeat,
                onDone = { if (canConnect) actions.onConnect() },
                maxLength = V5WebDavRules.MAX_FIELD,
                password = true,
                enabled = !state.connecting,
                modifier = Modifier.testTag(V5WebDavTestTags.PhraseRepeat),
            )
            if (V5WebDavRules.phrasesDiffer(state)) {
                Problem(stringResource(R.string.v5_webdav_phrase_differs))
            } else if (V5WebDavRules.phraseTooSimple(state.phrase)) {
                Problem(stringResource(R.string.v5_webdav_phrase_simple))
            } else {
                Footnote(stringResource(R.string.v5_webdav_phrase_footer, V5WebDavRules.MIN_PHRASE))
            }
            PhraseWarning()
        }
    }
}

/**
 * The server part of Settings → Sync once connected (canvas «х2»): how it synced, «Sync now»,
 * and the tabs of the person's other devices. The page around it holds the QR transfer and
 * «Disconnect» ([V5WebDavDisconnectRow]).
 */
@Composable
fun V5WebDavStatus(
    state: V5WebDavStatusState,
    onSyncNow: () -> Unit,
    onOpenDevice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().testTag(V5WebDavTestTags.Status),
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space6),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space2)) {
            StatusCard(state)
            V5ListGroup {
                V5ListRow(
                    title = stringResource(R.string.v5_webdav_sync_now),
                    icon = V5RowIcon(VolaIcons.Sync, V5Tile.Sky),
                    titleColor = V5Theme.colors.accent,
                    enabled = !state.syncing,
                    onClick = onSyncNow,
                    modifier = Modifier.testTag(V5WebDavTestTags.SyncNow),
                )
            }
        }
        if (state.devices.isEmpty()) {
            Footnote(stringResource(R.string.v5_webdav_no_devices))
        } else {
            V5ListGroup(
                header = stringResource(R.string.v5_webdav_devices_header),
                footer = stringResource(R.string.v5_webdav_devices_footer),
            ) {
                state.devices.forEach { device ->
                    V5ListRow(
                        title = device.name,
                        subtitle = stringResource(
                            R.string.v5_webdav_device_detail,
                            pluralStringResource(R.plurals.v5_webdav_tabs, device.tabs, device.tabs),
                            device.updated,
                        ),
                        icon = V5RowIcon(VolaIcons.Tab, V5Tile.Indigo),
                        trailing = V5RowTrailing.Chevron,
                        onClick = { onOpenDevice(device.id) },
                        modifier = Modifier.testTag(V5WebDavTestTags.device(device.id)),
                    )
                }
            }
        }
    }
}

/** «Disconnect» at the end of the sync page; what it keeps is asked in [V5WebDavDisconnectSheet]. */
@Composable
fun V5WebDavDisconnectRow(onDisconnect: () -> Unit) {
    V5ListGroup {
        V5ListRow(
            title = stringResource(R.string.v5_webdav_disconnect),
            titleColor = V5Theme.colors.danger,
            onClick = onDisconnect,
            modifier = Modifier.testTag(V5WebDavTestTags.Disconnect),
        )
    }
}

/** Another device's tabs (canvas «х3»); a tap opens the page in a new tab here. */
@Composable
fun V5WebDavDeviceTabs(
    device: V5WebDavDevice,
    tabs: List<V5WebDavTab>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag(V5WebDavTestTags.DeviceTabs),
    ) {
        V5PageBar(title = device.name, onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = V5Spacing.space4, bottom = V5Spacing.space8),
        ) {
            if (tabs.isEmpty()) {
                Footnote(stringResource(R.string.v5_webdav_device_empty))
            } else {
                V5ListGroup(footer = stringResource(R.string.v5_webdav_device_updated, device.updated)) {
                    tabs.forEach { tab ->
                        V5ListRow(
                            title = tab.title.ifBlank { tab.host },
                            subtitle = tab.host,
                            leading = { SiteTile(tab.host) },
                            onClick = { onOpen(tab.url) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * «The phrase does not fit» (canvas «х4»): the server holds Vola's data under another phrase and
 * nothing was overwritten. Starting over deletes that data, after [V5WebDavStartOverSheet].
 */
@Composable
fun V5WebDavPhraseMismatch(host: String, onRetry: () -> Unit, onStartOver: () -> Unit) {
    V5StatePage(
        icon = VolaIcons.Key,
        title = stringResource(R.string.v5_webdav_mismatch_title),
        message = stringResource(R.string.v5_webdav_mismatch_message, host),
        primary = V5StateAction(label = stringResource(R.string.v5_webdav_mismatch_retry), onClick = onRetry),
        secondary = listOf(
            V5StateAction(label = stringResource(R.string.v5_webdav_start_over), onClick = onStartOver, destructive = true),
        ),
        modifier = Modifier.testTag(V5WebDavTestTags.PhraseMismatch),
    )
}

/** «Disconnect»: sync stops on this phone; the data on the server stays unless the person deletes it too. */
@Composable
fun V5WebDavDisconnectSheet(
    visible: Boolean,
    host: String,
    onDisconnect: () -> Unit,
    onDisconnectAndDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    V5PromptSheet(
        visible = visible,
        title = stringResource(R.string.v5_webdav_disconnect_title),
        message = stringResource(R.string.v5_webdav_disconnect_message, host),
        actions = listOf(
            V5PromptAction(stringResource(R.string.v5_webdav_disconnect), onDisconnect, V5PromptRole.Primary),
            V5PromptAction(stringResource(R.string.v5_webdav_disconnect_delete), onDisconnectAndDelete, V5PromptRole.Destructive),
        ),
        onDismiss = onDismiss,
    )
}

/**
 * «Disconnect and delete data» did not reach the server: the data is still there and the phone is
 * still connected, so the deletion can be asked again from here.
 */
@Composable
fun V5WebDavDeleteFailedSheet(visible: Boolean, host: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    V5PromptSheet(
        visible = visible,
        title = stringResource(R.string.v5_webdav_delete_failed_title),
        message = stringResource(R.string.v5_webdav_delete_failed_message, host),
        actions = listOf(
            V5PromptAction(stringResource(R.string.action_retry), onRetry, V5PromptRole.Destructive),
        ),
        onDismiss = onDismiss,
    )
}

/** «Start over»: deletes Vola's data on the server for every device; this phone's tabs and bookmarks stay. */
@Composable
fun V5WebDavStartOverSheet(visible: Boolean, host: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    V5PromptSheet(
        visible = visible,
        title = stringResource(R.string.v5_webdav_start_over_title),
        message = stringResource(R.string.v5_webdav_start_over_message, host),
        actions = listOf(
            V5PromptAction(stringResource(R.string.v5_webdav_start_over_confirm), onConfirm, V5PromptRole.Destructive),
        ),
        onDismiss = onDismiss,
    )
}

@Composable
private fun StatusCard(state: V5WebDavStatusState) {
    val colors = V5Theme.colors
    val (icon: ImageVector, tint) = when {
        state.syncing -> VolaIcons.Sync to colors.accent
        state.error != null -> VolaIcons.Error to colors.danger
        else -> VolaIcons.CheckCircle to colors.success
    }
    val title = when {
        state.syncing -> stringResource(R.string.v5_webdav_syncing)
        state.error != null -> stringResource(V5WebDavRules.errorTitle(state.error))
        else -> stringResource(R.string.v5_webdav_synced)
    }
    val detail = stringResource(
        R.string.v5_webdav_status_detail,
        state.lastSynced ?: stringResource(R.string.v5_webdav_never_synced),
        state.host,
    )
    Row(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .clip(V5Theme.shapes.m)
            .background(colors.surface)
            .padding(V5Spacing.space4)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(
            Modifier
                .size(V5Sizes.syncStatus)
                .clip(V5Theme.shapes.full)
                .background(tint.copy(alpha = STATUS_WASH_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(V5Sizes.syncStatusIcon))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = V5Theme.type.title, color = colors.label, modifier = Modifier.semantics { heading() })
            // Why it failed and what to do: the title alone («Sync failed») leaves the person guessing.
            if (!state.syncing && state.error != null) {
                Text(stringResource(V5WebDavRules.errorText(state.error)), style = V5Theme.type.footnote, color = colors.label)
            }
            Text(detail, style = V5Theme.type.footnote, color = colors.labelSecondary)
        }
    }
}

@Composable
private fun PhraseWarning() {
    val colors = V5Theme.colors
    Row(
        Modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .padding(top = V5Spacing.space2)
            .fillMaxWidth()
            .clip(V5Theme.shapes.m)
            .background(colors.surface)
            .padding(V5Spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Icon(VolaIcons.Key, contentDescription = null, tint = colors.warning, modifier = Modifier.size(V5Sizes.iconSmall))
        Column(verticalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
            Text(stringResource(R.string.v5_webdav_phrase_warning_title), style = V5Theme.type.headline, color = colors.label)
            Text(stringResource(R.string.v5_webdav_phrase_warning), style = V5Theme.type.callout, color = colors.labelSecondary)
        }
    }
}

@Composable
private fun SiteTile(host: String) {
    val tile = V5NewTabRules.letterTile(host)
    Box(
        Modifier
            .size(V5Sizes.rowTile)
            .clip(V5Theme.shapes.xs)
            .background(tile.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(V5NewTabRules.letter(host), style = V5Theme.type.callout, color = tile.glyph)
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = V5Theme.type.footnote,
        color = V5Theme.colors.labelSecondary,
        modifier = modifier
            .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
            .semantics { heading() },
    )
}

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        style = V5Theme.type.footnote,
        color = V5Theme.colors.labelSecondary,
        modifier = Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
    )
}

@Composable
private fun Problem(text: String) {
    Text(
        text,
        style = V5Theme.type.footnote,
        color = V5Theme.colors.danger,
        modifier = Modifier
            .padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private const val STATUS_WASH_ALPHA = 0.18f

/** Pure decisions of the own-server screens, for unit tests. */
internal object V5WebDavRules {
    const val MAX_FIELD = 512

    /** The shortest phrase: long enough that guessing it from the server's files stays out of reach. */
    const val MIN_PHRASE = 10

    /** Distinct characters a phrase needs: «aaaaaaaaaa» is ten characters, but guessed at once. */
    const val MIN_PHRASE_DISTINCT = 5

    /**
     * What is wrong with a typed address before asking the server; null when nothing is, or
     * while only the scheme is typed. The same check as the sync's, so «Connect» never sends an
     * address the sync would refuse as unreachable.
     */
    fun addressProblem(address: String): V5WebDavError? = when (WebDavSyncAddressRules.check(address)) {
        WebDavAddressCheck.NotHttps -> V5WebDavError.NotHttps
        WebDavAddressCheck.Invalid -> {
            val trimmed = address.trim()
            when {
                // Said once it reads as an address («cloud.example.com»), not at the first letter.
                "://" !in trimmed && '.' in trimmed -> V5WebDavError.MissingScheme
                trimmed.substringAfter("://", missingDelimiterValue = "").isNotEmpty() -> V5WebDavError.InvalidAddress
                else -> null
            }
        }
        is WebDavAddressCheck.Valid -> null
    }

    /** Whether [address] is an address the sync can use. */
    fun isHttpsAddress(address: String): Boolean = WebDavSyncAddressRules.check(address) is WebDavAddressCheck.Valid

    /**
     * Long enough, but guessed at once offline (ЯС-2): too few different characters, a run along
     * the alphabet, the digits or a keyboard row («1234567890», «qwertyuiop», «abcdefghij»), a
     * common password with digits («password12»), or under the generator's «fair» strength.
     */
    fun phraseTooSimple(phrase: String): Boolean {
        if (phrase.length < MIN_PHRASE) return false
        if (phrase.toSet().size < MIN_PHRASE_DISTINCT) return true
        val lower = phrase.lowercase()
        if (SEQUENCES.any { sequence -> sequence.contains(lower) || sequence.reversed().contains(lower) }) return true
        val rest = COMMON_WORDS.fold(lower) { left, word -> left.replace(word, "") }
        if (rest.length < lower.length && rest.all { !it.isLetter() }) return true
        return PasswordGeneratorRules.strength(PasswordGeneratorRules.estimatedBits(phrase)) == PasswordStrength.Weak
    }

    private val SEQUENCES = listOf(
        "abcdefghijklmnopqrstuvwxyz",
        "01234567890",
        "qwertyuiopasdfghjklzxcvbnm",
        "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik9ol0p",
        "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
        "йцукенгшщзхъфывапролджэячсмитьбю",
    )

    private val COMMON_WORDS = listOf("password", "passw0rd", "qwerty", "letmein", "iloveyou", "admin", "welcome", "пароль")

    /** The repeat is typed and differs from the phrase. */
    fun phrasesDiffer(state: V5WebDavConnectState): Boolean =
        state.phraseRepeat.isNotEmpty() && state.phraseRepeat != state.phrase

    fun canConnect(state: V5WebDavConnectState): Boolean =
        !state.connecting &&
            isHttpsAddress(state.address) &&
            state.login.isNotBlank() &&
            state.password.isNotEmpty() &&
            state.phrase.length >= MIN_PHRASE &&
            !phraseTooSimple(state.phrase) &&
            state.phrase == state.phraseRepeat

    /** The line under the server fields. */
    fun errorText(error: V5WebDavError): Int = when (error) {
        V5WebDavError.Unreachable -> R.string.v5_webdav_error_unreachable
        V5WebDavError.LoginRejected -> R.string.v5_webdav_error_login
        V5WebDavError.NotHttps -> R.string.v5_webdav_error_not_https
        V5WebDavError.NotWebDav -> R.string.v5_webdav_error_not_webdav
        V5WebDavError.PhraseMismatch -> R.string.v5_webdav_error_phrase
        V5WebDavError.NewerFormat -> R.string.v5_webdav_error_newer
        V5WebDavError.FolderNotFound -> R.string.v5_webdav_error_folder
        V5WebDavError.ServerFull -> R.string.v5_webdav_error_full
        V5WebDavError.ServerBusy -> R.string.v5_webdav_error_busy
        V5WebDavError.ServerError -> R.string.v5_webdav_error_server
        V5WebDavError.InvalidAddress -> R.string.v5_webdav_error_address
        V5WebDavError.MissingScheme -> R.string.v5_webdav_error_scheme
    }

    /** The status card's title when the last sync failed. */
    fun errorTitle(error: V5WebDavError): Int = when (error) {
        V5WebDavError.Unreachable -> R.string.v5_webdav_status_unreachable
        V5WebDavError.LoginRejected -> R.string.v5_webdav_status_login
        V5WebDavError.PhraseMismatch -> R.string.v5_webdav_mismatch_title
        V5WebDavError.NotHttps, V5WebDavError.NotWebDav, V5WebDavError.InvalidAddress, V5WebDavError.MissingScheme ->
            R.string.v5_webdav_status_failed
        V5WebDavError.NewerFormat -> R.string.v5_webdav_status_newer
        V5WebDavError.FolderNotFound -> R.string.v5_webdav_status_folder
        V5WebDavError.ServerFull -> R.string.v5_webdav_status_full
        V5WebDavError.ServerBusy -> R.string.v5_webdav_status_busy
        V5WebDavError.ServerError -> R.string.v5_webdav_status_server
    }
}
