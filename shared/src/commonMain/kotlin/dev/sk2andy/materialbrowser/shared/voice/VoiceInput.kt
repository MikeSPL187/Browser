package dev.sk2andy.materialbrowser.shared.voice

/**
 * Voice input for the address field (research F1, stage S7). Speech is recognised on the
 * device only: an implementation never sends audio to a server, and reports itself unavailable
 * when the device has no on-device recognizer rather than falling back to a cloud one.
 *
 * Not part of the browser engine, but still a port, so the address bar and its previews depend
 * only on this interface.
 */
interface VoiceInput {
    /** Whether speech can be recognised on this device without the network. */
    val isAvailable: Boolean

    /** The current session; observable from Compose in the Android implementation. */
    val state: VoiceInputState

    /**
     * Starts listening in [languageTag] (a BCP 47 tag, see [VoiceInputRules.languageTag]).
     * [onTranscript] receives the words heard so far while the person speaks ([final] false)
     * and once the full phrase with [final] true; it is never called after [cancel].
     */
    fun start(languageTag: String, onTranscript: (text: String, final: Boolean) -> Unit)

    /** Stops listening and recognises what was said so far. */
    fun stop()

    /** Drops the session without a result. */
    fun cancel()
}

enum class VoiceInputStatus {
    Idle,

    /** Started; the microphone is not open yet. */
    Starting,

    /** The microphone is open. */
    Listening,

    /** The person stopped speaking; the final phrase is on its way. */
    Recognizing,
}

enum class VoiceInputError {
    /** Nothing was said before the recognizer gave up. */
    NoSpeech,

    /** Speech was heard but not understood. */
    NoMatch,

    /** The device has no on-device model for the language (or it is still downloading). */
    LanguageUnavailable,

    /** The microphone permission is missing. */
    PermissionDenied,

    /** The recognizer is in use by another app. */
    Busy,

    /** The microphone or the recognizer failed. */
    Failed,
}

data class VoiceInputState(
    val status: VoiceInputStatus = VoiceInputStatus.Idle,
    /** The words heard so far in this session. */
    val transcript: String = "",
    /** Loudness of the voice for the listening indicator, 0 (silence) to 1 (loud). */
    val level: Float = 0f,
    /** Why the last session ended without a result; cleared by the next start. */
    val error: VoiceInputError? = null,
) {
    /** True from the tap on the microphone until the result or the cancel. */
    val active: Boolean get() = status != VoiceInputStatus.Idle
}

sealed interface VoiceInputEvent {
    data object Start : VoiceInputEvent
    data object Ready : VoiceInputEvent
    data class Level(val rmsDb: Float) : VoiceInputEvent
    data class Partial(val text: String) : VoiceInputEvent
    data object SpeechEnded : VoiceInputEvent
    data class Result(val text: String) : VoiceInputEvent
    data class Failed(val error: VoiceInputError) : VoiceInputEvent
    data object Cancel : VoiceInputEvent
}

object VoiceInputRules {
    /**
     * The range Android recognizers report loudness in (`RecognitionListener.onRmsChanged`):
     * about -2 dB in silence, about 10 dB for a raised voice.
     */
    const val SILENT_RMS_DB = -2f
    const val LOUD_RMS_DB = 10f

    private val whitespace = Regex("\\s+")

    fun reduce(state: VoiceInputState, event: VoiceInputEvent): VoiceInputState = when (event) {
        VoiceInputEvent.Start -> VoiceInputState(status = VoiceInputStatus.Starting)
        VoiceInputEvent.Ready -> if (state.status == VoiceInputStatus.Starting) {
            state.copy(status = VoiceInputStatus.Listening)
        } else {
            state
        }
        is VoiceInputEvent.Level -> if (state.status == VoiceInputStatus.Listening) {
            state.copy(level = level(event.rmsDb))
        } else {
            state
        }
        is VoiceInputEvent.Partial -> if (state.active) {
            transcript(event.text)?.let { state.copy(transcript = it) } ?: state
        } else {
            state
        }
        VoiceInputEvent.SpeechEnded -> if (state.active) {
            state.copy(status = VoiceInputStatus.Recognizing, level = 0f)
        } else {
            state
        }
        is VoiceInputEvent.Result -> if (state.active) {
            VoiceInputState(transcript = transcript(event.text) ?: state.transcript)
        } else {
            state
        }
        is VoiceInputEvent.Failed -> if (state.active) {
            VoiceInputState(error = event.error)
        } else {
            state
        }
        VoiceInputEvent.Cancel -> VoiceInputState()
    }

    /** Loudness in dB as reported by the recognizer, mapped to 0..1 for the indicator. */
    fun level(rmsDb: Float): Float {
        if (rmsDb.isNaN()) return 0f
        return ((rmsDb - SILENT_RMS_DB) / (LOUD_RMS_DB - SILENT_RMS_DB)).coerceIn(0f, 1f)
    }

    /**
     * The recognition language for the interface language (the first locale's language code):
     * Russian for a Russian interface, English otherwise — the two languages Vola speaks.
     */
    fun languageTag(interfaceLanguage: String): String =
        if (interfaceLanguage.trim().lowercase() == "ru") "ru-RU" else "en-US"

    /** Recognised words as they go into the address field: one line, no outer spaces. */
    fun transcript(text: String): String? =
        text.replace(whitespace, " ").trim().takeIf { it.isNotEmpty() }

    /**
     * What a final phrase submits, as if typed and confirmed: null when nothing usable was
     * heard. A spoken "example dot com" stays a search; the search engine handles it.
     */
    fun submission(text: String): String? = transcript(text)
}
