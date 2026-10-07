package dev.sk2andy.materialbrowser.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import dev.sk2andy.materialbrowser.shared.voice.VoiceInput
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputError
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputEvent
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputRules
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputState
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputStatus

/**
 * Creates the voice input for a screen. The app uses [OnDeviceVoiceInput]; previews and the
 * debug tour activity provide a scripted one.
 */
internal val LocalVoiceInputFactory = staticCompositionLocalOf<(Context) -> VoiceInput> {
    { context -> OnDeviceVoiceInput(context) }
}

/**
 * [VoiceInput] on the system's on-device recognizer
 * ([SpeechRecognizer.createOnDeviceSpeechRecognizer], API 31): audio is processed on the phone
 * and never leaves it. There is deliberately no fallback to the default recognizer, which may
 * send audio to a server; without an on-device one the microphone button is not shown.
 *
 * Must be used from the main thread, as [SpeechRecognizer] requires.
 */
internal class OnDeviceVoiceInput(context: Context) : VoiceInput {
    private val context = context.applicationContext

    override val isAvailable: Boolean = SpeechRecognizer.isOnDeviceRecognitionAvailable(this.context)

    override var state by mutableStateOf(VoiceInputState())
        private set

    private var recognizer: SpeechRecognizer? = null
    private var onTranscript: ((String, Boolean) -> Unit)? = null

    override fun start(languageTag: String, onTranscript: (text: String, final: Boolean) -> Unit) {
        release()
        dispatch(VoiceInputEvent.Start)
        if (!isAvailable) {
            dispatch(VoiceInputEvent.Failed(VoiceInputError.Failed))
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            dispatch(VoiceInputEvent.Failed(VoiceInputError.PermissionDenied))
            return
        }
        val created = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }
            .getOrNull()
        if (created == null) {
            dispatch(VoiceInputEvent.Failed(VoiceInputError.Failed))
            return
        }
        recognizer = created
        this.onTranscript = onTranscript
        val intent = recognizerIntent(languageTag)
        created.setRecognitionListener(Listener(created, intent))
        runCatching { created.startListening(intent) }.onFailure {
            release()
            dispatch(VoiceInputEvent.Failed(VoiceInputError.Failed))
        }
    }

    override fun stop() {
        val current = recognizer ?: return
        if (state.status == VoiceInputStatus.Recognizing) return
        runCatching { current.stopListening() }
        dispatch(VoiceInputEvent.SpeechEnded)
    }

    override fun cancel() {
        recognizer?.let { runCatching { it.cancel() } }
        release()
        dispatch(VoiceInputEvent.Cancel)
    }

    private fun recognizerIntent(languageTag: String) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

    private fun release() {
        recognizer?.let { runCatching { it.destroy() } }
        recognizer = null
        onTranscript = null
    }

    private fun dispatch(event: VoiceInputEvent) {
        state = VoiceInputRules.reduce(state, event)
    }

    private inner class Listener(
        private val owner: SpeechRecognizer,
        private val intent: Intent,
    ) : RecognitionListener {
        /** Callbacks of a recognizer that was cancelled or replaced are dropped. */
        private val current: Boolean get() = recognizer === owner

        override fun onReadyForSpeech(params: Bundle?) {
            if (current) dispatch(VoiceInputEvent.Ready)
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            if (current) dispatch(VoiceInputEvent.Level(rmsdB))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            if (current) dispatch(VoiceInputEvent.SpeechEnded)
        }

        override fun onError(error: Int) {
            if (!current) return
            val reason = RecognizerErrors.reason(error)
            if (reason == VoiceInputError.LanguageUnavailable &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                // The system recognizer offers to download its own language pack; Vola sends
                // nothing itself.
                try {
                    owner.triggerModelDownload(intent)
                } catch (_: RuntimeException) {
                    // Not every recognizer supports downloads; the message still explains.
                }
            }
            release()
            dispatch(VoiceInputEvent.Failed(reason))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!current) return
            val before = state.transcript
            dispatch(VoiceInputEvent.Partial(firstResult(partialResults)))
            if (state.transcript != before) onTranscript?.invoke(state.transcript, false)
        }

        override fun onResults(results: Bundle?) {
            if (!current) return
            val callback = onTranscript
            dispatch(VoiceInputEvent.Result(firstResult(results)))
            release()
            val text = VoiceInputRules.submission(state.transcript)
            if (text == null) {
                state = VoiceInputState(error = VoiceInputError.NoMatch)
            } else {
                callback?.invoke(text, true)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}

private fun firstResult(bundle: Bundle?): String =
    bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

internal object RecognizerErrors {
    /** The port's reason for a [SpeechRecognizer] error code. */
    fun reason(code: Int): VoiceInputError = when (code) {
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceInputError.NoSpeech
        SpeechRecognizer.ERROR_NO_MATCH -> VoiceInputError.NoMatch
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        -> VoiceInputError.LanguageUnavailable
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputError.PermissionDenied
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
        -> VoiceInputError.Busy
        else -> VoiceInputError.Failed
    }
}
