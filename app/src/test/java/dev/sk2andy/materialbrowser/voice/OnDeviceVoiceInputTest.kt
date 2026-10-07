package dev.sk2andy.materialbrowser.voice

import android.speech.SpeechRecognizer
import dev.sk2andy.materialbrowser.shared.voice.VoiceInputError
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceVoiceInputTest {
    @Test
    fun `recognizer errors map to the reasons the address bar explains`() {
        assertEquals(VoiceInputError.NoSpeech, RecognizerErrors.reason(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(VoiceInputError.NoMatch, RecognizerErrors.reason(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(
            VoiceInputError.LanguageUnavailable,
            RecognizerErrors.reason(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED),
        )
        assertEquals(
            VoiceInputError.LanguageUnavailable,
            RecognizerErrors.reason(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE),
        )
        assertEquals(
            VoiceInputError.PermissionDenied,
            RecognizerErrors.reason(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS),
        )
        assertEquals(VoiceInputError.Busy, RecognizerErrors.reason(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(VoiceInputError.Busy, RecognizerErrors.reason(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS))
        assertEquals(VoiceInputError.Failed, RecognizerErrors.reason(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(VoiceInputError.Failed, RecognizerErrors.reason(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(VoiceInputError.Failed, RecognizerErrors.reason(-1))
    }

    @Test
    fun `only the on-device recognizer is ever created`() {
        // Privacy (CLAUDE.md rule 5): the default recognizer may stream audio to a server.
        val source = File("src/main/java/dev/sk2andy/materialbrowser/voice/OnDeviceVoiceInput.kt").readText()
        assertTrue(source.contains("SpeechRecognizer.createOnDeviceSpeechRecognizer("))
        assertFalse(source.contains("SpeechRecognizer.createSpeechRecognizer("))
        assertTrue(source.contains("SpeechRecognizer.isOnDeviceRecognitionAvailable("))
        assertFalse(source.contains("SpeechRecognizer.isRecognitionAvailable("))
    }
}
