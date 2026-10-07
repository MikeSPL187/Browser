package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.ui.AddressBarFieldContent
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputState
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputStatus
import dev.sk2andy.materialbrowser.ui.theme.BrowserChromeSurfaceRole
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush
import dev.sk2andy.materialbrowser.ui.theme.browserChromeSurfaceTokens

/**
 * The address editor field as voice search shows it: idle with the microphone, or listening
 * with the words heard so far. Shared by the previews and the debug tour activity.
 */
@Composable
internal fun AddressVoiceSearchPreviewField(state: VoiceInputState?) {
    val tokens = browserChromeSurfaceTokens(BrowserChromeSurfaceRole.AddressBar)
    val transcript = state?.transcript.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.fieldContainerColor, RoundedCornerShape(tokens.cornerRadius)),
    ) {
        AddressBarFieldContent(
            editing = true,
            editValue = TextFieldValue(transcript, TextRange(transcript.length)),
            onEditValueChange = {},
            ghostCompletion = null,
            placeholder = when (state?.status) {
                VoiceInputStatus.Starting, VoiceInputStatus.Listening ->
                    stringResource(R.string.voice_search_listening)
                VoiceInputStatus.Recognizing -> stringResource(R.string.voice_search_recognizing)
                VoiceInputStatus.Idle, null -> stringResource(R.string.search_or_enter_url)
            },
            displayText = "",
            onSubmitAddress = {},
            submissionText = { text, _ -> text },
            editorTrailingContent = {
                if (state != null && state.active) {
                    AddressVoiceListeningControls(
                        status = state.status,
                        level = { state.level },
                        accentColor = tokens.accentColor,
                        onAccentColor = tokens.onAccentColor,
                        onStop = {},
                        onCancel = {},
                    )
                } else {
                    AddressVoiceMicrophoneButton(onClick = {})
                    IconButton(onClick = {}) {
                        Icon(VolaIcons.Close, contentDescription = stringResource(R.string.cd_close_address_input))
                    }
                }
            },
            contentColor = tokens.fieldContentColor,
            secondaryContentColor = tokens.fieldSecondaryContentColor,
            cursorColor = tokens.accentColor,
        )
    }
}

@Composable
private fun VoicePreviewTheme(content: @Composable () -> Unit) {
    MaterialBrowserTheme(
        settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
    ) {
        Column(
            modifier = Modifier
                .background(VolaTheme.auraBrush)
                .padding(VolaSpacing.x4),
            verticalArrangement = Arrangement.spacedBy(VolaSpacing.x3),
        ) {
            content()
        }
    }
}

@VolaPreviews
@Composable
private fun AddressVoiceSearchStatesPreview() {
    VoicePreviewTheme {
        AddressVoiceSearchPreviewField(state = null)
        AddressVoiceSearchPreviewField(
            state = VoiceInputState(status = VoiceInputStatus.Listening, level = 0.2f),
        )
        AddressVoiceSearchPreviewField(
            state = VoiceInputState(
                status = VoiceInputStatus.Listening,
                transcript = "погода в москве на выходные",
                level = 0.9f,
            ),
        )
        AddressVoiceSearchPreviewField(
            state = VoiceInputState(
                status = VoiceInputStatus.Recognizing,
                transcript = "погода в москве на выходные",
            ),
        )
    }
}

@VolaPreviews
@Composable
private fun AddressVoicePermissionExplainPreview() {
    VoicePreviewTheme {
        AddressVoicePermissionDialog(dialog = AddressVoiceDialog.Explain, onConfirm = {}, onDismiss = {})
    }
}

@VolaPreviews
@Composable
private fun AddressVoicePermissionSettingsPreview() {
    VoicePreviewTheme {
        AddressVoicePermissionDialog(dialog = AddressVoiceDialog.OpenSettings, onConfirm = {}, onDismiss = {})
    }
}
