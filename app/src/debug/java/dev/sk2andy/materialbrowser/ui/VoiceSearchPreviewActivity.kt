package dev.sk2andy.materialbrowser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.sk2andy.materialbrowser.data.AppearanceSettings
import dev.sk2andy.materialbrowser.data.BrowserAppearanceMode
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputState
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputStatus
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
import dev.sk2andy.materialbrowser.ui.theme.VolaTheme
import dev.sk2andy.materialbrowser.ui.theme.auraBrush
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent
import kotlinx.coroutines.delay

/**
 * Debug-only: voice search for the emulator tour, which has no on-device speech model and no
 * voice. `--es screen listening` (default) shows the address field listening with a scripted
 * voice level; `--es screen explain` shows the microphone explanation. Back closes it.
 */
internal class VoiceSearchPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val explain = intent.getStringExtra("screen") == "explain"
        val transcript = if (resources.configuration.locales[0].language == "ru") {
            "погода в москве на выходные"
        } else {
            "weather in moscow this weekend"
        }
        setCandyContent(animationsEnabled = false) {
            MaterialBrowserTheme(
                settings = AppearanceSettings(appearanceMode = BrowserAppearanceMode.System),
            ) {
                var level by remember { mutableFloatStateOf(0.6f) }
                LaunchedEffect(Unit) {
                    val script = floatArrayOf(0.6f, 0.9f, 0.4f, 0.75f, 0.2f)
                    var index = 0
                    while (true) {
                        level = script[index++ % script.size]
                        delay(400)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VolaTheme.auraBrush)
                        .safeDrawingPadding()
                        .padding(VolaSpacing.x4),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    AddressVoiceSearchPreviewField(
                        state = if (explain) {
                            null
                        } else {
                            VoiceInputState(
                                status = VoiceInputStatus.Listening,
                                transcript = transcript,
                                level = level,
                            )
                        },
                    )
                }
                if (explain) {
                    AddressVoicePermissionDialog(
                        dialog = AddressVoiceDialog.Explain,
                        onConfirm = { finish() },
                        onDismiss = { finish() },
                    )
                }
            }
        }
    }
}
