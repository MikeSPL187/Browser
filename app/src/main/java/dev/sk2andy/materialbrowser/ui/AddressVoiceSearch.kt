package dev.sk2andy.materialbrowser.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.voice.VoiceInput
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputError
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputRules
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputStatus
import dev.sk2andy.materialbrowser.ui.theme.VolaMotion
import dev.sk2andy.materialbrowser.ui.theme.VolaVoice
import dev.sk2andy.materialbrowser.voice.LocalVoiceInputFactory
import kotlinx.coroutines.flow.collectLatest

internal object AddressVoiceSearchTestTags {
    const val Microphone = "address_voice_search"
    const val Listening = "address_voice_listening"
    const val Cancel = "address_voice_cancel"
    const val PermissionDialog = "address_voice_permission"
}

/** The dialog in front of the system microphone prompt, or after a refusal that sticks. */
internal enum class AddressVoiceDialog {
    /** Why Vola asks for the microphone, before the system prompt. */
    Explain,

    /** The permission was refused for good: only the app settings can turn it on. */
    OpenSettings,
}

/** What the address field gives voice search; refreshed on every composition. */
internal class AddressVoiceHost(
    val editValue: TextFieldValue,
    val onEditValueChange: (TextFieldValue) -> Unit,
    val onSubmit: (String) -> Unit,
    val languageTag: String,
    val hasPermission: () -> Boolean,
    val requestPermission: () -> Unit,
    val onListening: () -> Unit,
    val showMessage: (Int) -> Unit,
)

/**
 * Voice search for the address editor (stage S7): the microphone, its permission, and the words
 * heard so far going into the field. A final phrase is submitted like a confirmed typed one.
 */
@Stable
internal class AddressVoiceSearch internal constructor(
    private val voice: VoiceInput,
    private val host: State<AddressVoiceHost>,
) {
    /** False without an on-device recognizer: the microphone button is not shown at all. */
    val available: Boolean get() = voice.isAvailable

    // The voice level changes many times a second; only the indicator's drawing reads it, so
    // the address bar recomposes when the status or the error changes, not with every level.
    private val statusState = derivedStateOf { voice.state.status }
    private val errorState = derivedStateOf { voice.state.error }

    val status: VoiceInputStatus get() = statusState.value
    val error: VoiceInputError? get() = errorState.value
    val active: Boolean get() = status != VoiceInputStatus.Idle

    /** Loudness of the voice, 0..1; read it where it is drawn. */
    fun level(): Float = voice.state.level

    var dialog by mutableStateOf<AddressVoiceDialog?>(null)
        private set

    /** The field before the first words replaced it, restored on cancel or failure. */
    private var textBefore: TextFieldValue? = null

    fun onMicrophone() {
        if (host.value.hasPermission()) listen() else dialog = AddressVoiceDialog.Explain
    }

    fun onDialogConfirmed(openSettings: () -> Unit) {
        val shown = dialog
        dialog = null
        when (shown) {
            AddressVoiceDialog.Explain -> host.value.requestPermission()
            AddressVoiceDialog.OpenSettings -> openSettings()
            null -> Unit
        }
    }

    fun onDialogDismissed() {
        dialog = null
    }

    fun onPermissionResult(granted: Boolean, refusedForGood: Boolean) {
        when {
            granted -> listen()
            refusedForGood -> dialog = AddressVoiceDialog.OpenSettings
            else -> host.value.showMessage(R.string.voice_search_permission_needed)
        }
    }

    /** Ends listening now and searches for what was heard. */
    fun stop() = voice.stop()

    fun cancel() {
        if (!active) return
        voice.cancel()
        restoreField()
    }

    fun onFailed(error: VoiceInputError) {
        restoreField()
        host.value.showMessage(voiceErrorMessage(error))
    }

    private fun listen() {
        val current = host.value
        textBefore = current.editValue
        current.onListening()
        voice.start(current.languageTag) { text, final ->
            val field = TextFieldValue(text, TextRange(text.length))
            host.value.onEditValueChange(field)
            if (final) {
                textBefore = null
                VoiceInputRules.submission(text)?.let(host.value.onSubmit)
            }
        }
    }

    private fun restoreField() {
        textBefore?.let(host.value.onEditValueChange)
        textBefore = null
    }
}

@Composable
internal fun rememberAddressVoiceSearch(
    editing: Boolean,
    editValue: TextFieldValue,
    onEditValueChange: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
): AddressVoiceSearch {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current
    val voiceFactory = LocalVoiceInputFactory.current
    val voice = remember(context, voiceFactory) { voiceFactory(context) }
    var search: AddressVoiceSearch? = null
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Right after a refusal, no rationale means «don't ask again»: the system prompt will not
        // come back, so the next step is the app settings.
        val refusedForGood = !granted && context.findActivity()
            ?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == false
        search?.onPermissionResult(granted, refusedForGood)
    }
    val host = rememberUpdatedState(
        AddressVoiceHost(
            editValue = editValue,
            onEditValueChange = onEditValueChange,
            onSubmit = onSubmit,
            languageTag = VoiceInputRules.languageTag(LocalConfiguration.current.locales[0].language),
            hasPermission = {
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            },
            requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onListening = {
                keyboard?.hide()
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            },
            showMessage = { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() },
        ),
    )
    val voiceSearch = remember(voice) { AddressVoiceSearch(voice, host) }
    search = voiceSearch
    val error = voiceSearch.error
    LaunchedEffect(error) {
        if (error != null) voiceSearch.onFailed(error)
    }
    // Closing the editor ends the session; so does leaving the screen.
    LaunchedEffect(editing) {
        if (!editing) voiceSearch.cancel()
    }
    DisposableEffect(voiceSearch) {
        onDispose { voiceSearch.cancel() }
    }
    return voiceSearch
}

/** The field's placeholder while listening, or null when voice search is not running. */
@Composable
internal fun AddressVoiceSearch.placeholder(): String? = when (status) {
    VoiceInputStatus.Idle -> null
    VoiceInputStatus.Starting, VoiceInputStatus.Listening -> stringResource(R.string.voice_search_listening)
    VoiceInputStatus.Recognizing -> stringResource(R.string.voice_search_recognizing)
}

/** The microphone in the address editor, with the permission dialogs it leads to. */
@Composable
internal fun AddressVoiceSearchButton(search: AddressVoiceSearch) {
    if (!search.available) return
    val context = LocalContext.current
    AddressVoiceMicrophoneButton(onClick = search::onMicrophone)
    search.dialog?.let { dialog ->
        AddressVoicePermissionDialog(
            dialog = dialog,
            onConfirm = { search.onDialogConfirmed { context.openAppSettings() } },
            onDismiss = search::onDialogDismissed,
        )
    }
}

@Composable
internal fun AddressVoiceMicrophoneButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(VolaVoice.buttonSize)
            .testTag(AddressVoiceSearchTestTags.Microphone),
    ) {
        Icon(
            VolaIcons.Mic,
            contentDescription = stringResource(R.string.voice_search_start),
            modifier = Modifier.size(VolaVoice.iconSize),
        )
    }
}

/**
 * Instead of the editor's buttons while listening: the accent microphone with a halo that grows
 * with the voice (a tap ends listening and searches), and a cancel that brings the field back.
 */
@Composable
internal fun AddressVoiceListeningControls(
    status: VoiceInputStatus,
    level: () -> Float,
    accentColor: Color,
    onAccentColor: Color,
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AddressVoiceLevelButton(
            status = status,
            level = level,
            accentColor = accentColor,
            onAccentColor = onAccentColor,
            onClick = onStop,
        )
        IconButton(
            onClick = onCancel,
            modifier = Modifier.testTag(AddressVoiceSearchTestTags.Cancel),
        ) {
            Icon(VolaIcons.Close, contentDescription = stringResource(R.string.voice_search_cancel))
        }
    }
}

@Composable
private fun AddressVoiceLevelButton(
    status: VoiceInputStatus,
    level: () -> Float,
    accentColor: Color,
    onAccentColor: Color,
    onClick: () -> Unit,
) {
    val recognizing = status == VoiceInputStatus.Recognizing
    val currentLevel by rememberUpdatedState(level)
    val animatedLevel = remember { Animatable(0f) }
    LaunchedEffect(animatedLevel) {
        snapshotFlow { currentLevel() }.collectLatest { target ->
            animatedLevel.animateTo(target, VolaMotion.fast())
        }
    }
    val breath = rememberInfiniteTransition(label = "Voice listening").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(VolaVoice.HALO_IDLE_PERIOD_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Voice idle breath",
    )
    val statusLabel = stringResource(
        if (recognizing) R.string.voice_search_recognizing else R.string.voice_search_listening,
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(VolaVoice.buttonSize)
            .testTag(AddressVoiceSearchTestTags.Listening)
            .semantics {
                stateDescription = statusLabel
                liveRegion = LiveRegionMode.Polite
            }
            .drawBehind {
                if (recognizing) return@drawBehind
                val growth = VolaVoice.HALO_IDLE_GROWTH * breath.value +
                    VolaVoice.HALO_MAX_GROWTH * animatedLevel.value
                drawCircle(
                    color = accentColor.copy(alpha = VolaVoice.HALO_ALPHA),
                    radius = VolaVoice.coreSize.toPx() / 2f * (1f + growth),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .size(VolaVoice.coreSize)
                .background(
                    color = accentColor.copy(alpha = if (recognizing) VolaVoice.RECOGNIZING_ALPHA else 1f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.Mic,
                contentDescription = stringResource(R.string.voice_search_stop),
                tint = onAccentColor,
                modifier = Modifier.size(VolaVoice.coreIconSize),
            )
        }
    }
}

@Composable
internal fun AddressVoicePermissionDialog(
    dialog: AddressVoiceDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(AddressVoiceSearchTestTags.PermissionDialog),
        icon = { Icon(VolaIcons.Mic, contentDescription = null) },
        title = { Text(stringResource(R.string.voice_search_permission_title)) },
        text = {
            Text(
                stringResource(
                    when (dialog) {
                        AddressVoiceDialog.Explain -> R.string.voice_search_permission_message
                        AddressVoiceDialog.OpenSettings -> R.string.voice_search_permission_blocked
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(
                        when (dialog) {
                            AddressVoiceDialog.Explain -> R.string.voice_search_permission_continue
                            AddressVoiceDialog.OpenSettings -> R.string.voice_search_permission_settings
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.voice_search_permission_not_now))
            }
        },
    )
}

@StringRes
internal fun voiceErrorMessage(error: VoiceInputError): Int = when (error) {
    VoiceInputError.NoSpeech -> R.string.voice_search_error_no_speech
    VoiceInputError.NoMatch -> R.string.voice_search_error_no_match
    VoiceInputError.LanguageUnavailable -> R.string.voice_search_error_language
    VoiceInputError.PermissionDenied -> R.string.voice_search_permission_needed
    VoiceInputError.Busy -> R.string.voice_search_error_busy
    VoiceInputError.Failed -> R.string.voice_search_error_failed
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
