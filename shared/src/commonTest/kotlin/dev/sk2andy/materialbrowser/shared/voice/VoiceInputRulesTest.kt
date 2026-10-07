package dev.sk2andy.materialbrowser.shared.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceInputRulesTest {
    private fun reduceAll(vararg events: VoiceInputEvent): VoiceInputState =
        events.fold(VoiceInputState()) { state, event -> VoiceInputRules.reduce(state, event) }

    @Test
    fun aSessionGoesFromStartThroughListeningToTheResult() {
        val listening = reduceAll(VoiceInputEvent.Start, VoiceInputEvent.Ready)
        assertEquals(VoiceInputStatus.Listening, listening.status)
        assertTrue(listening.active)

        val heard = VoiceInputRules.reduce(listening, VoiceInputEvent.Partial("weather  in\nMoscow "))
        assertEquals("weather in Moscow", heard.transcript)

        val recognizing = VoiceInputRules.reduce(heard, VoiceInputEvent.SpeechEnded)
        assertEquals(VoiceInputStatus.Recognizing, recognizing.status)
        assertEquals(0f, recognizing.level)

        val done = VoiceInputRules.reduce(recognizing, VoiceInputEvent.Result("weather in Moscow today"))
        assertEquals(VoiceInputStatus.Idle, done.status)
        assertFalse(done.active)
        assertEquals("weather in Moscow today", done.transcript)
        assertNull(done.error)
    }

    @Test
    fun anEmptyResultKeepsTheLastPartialTranscript() {
        val done = reduceAll(
            VoiceInputEvent.Start,
            VoiceInputEvent.Ready,
            VoiceInputEvent.Partial("vola browser"),
            VoiceInputEvent.Result("  "),
        )
        assertEquals("vola browser", done.transcript)
    }

    @Test
    fun anEmptyPartialDoesNotEraseWhatWasHeard() {
        val state = reduceAll(
            VoiceInputEvent.Start,
            VoiceInputEvent.Ready,
            VoiceInputEvent.Partial("zen"),
            VoiceInputEvent.Partial(""),
        )
        assertEquals("zen", state.transcript)
    }

    @Test
    fun aNewSessionStartsClean() {
        val failed = reduceAll(
            VoiceInputEvent.Start,
            VoiceInputEvent.Ready,
            VoiceInputEvent.Partial("old words"),
            VoiceInputEvent.Failed(VoiceInputError.NoMatch),
        )
        assertEquals(VoiceInputError.NoMatch, failed.error)

        val restarted = VoiceInputRules.reduce(failed, VoiceInputEvent.Start)
        assertEquals(VoiceInputState(status = VoiceInputStatus.Starting), restarted)
    }

    @Test
    fun eventsAfterCancelAreIgnored() {
        val cancelled = reduceAll(
            VoiceInputEvent.Start,
            VoiceInputEvent.Ready,
            VoiceInputEvent.Partial("never mind"),
            VoiceInputEvent.Cancel,
        )
        assertEquals(VoiceInputState(), cancelled)
        listOf(
            VoiceInputEvent.Ready,
            VoiceInputEvent.Level(8f),
            VoiceInputEvent.Partial("late words"),
            VoiceInputEvent.SpeechEnded,
            VoiceInputEvent.Result("late result"),
            VoiceInputEvent.Failed(VoiceInputError.Failed),
        ).forEach { event ->
            assertEquals(VoiceInputState(), VoiceInputRules.reduce(cancelled, event), event.toString())
        }
    }

    @Test
    fun levelMovesOnlyWhileTheMicrophoneIsOpen() {
        val starting = reduceAll(VoiceInputEvent.Start, VoiceInputEvent.Level(10f))
        assertEquals(0f, starting.level)

        val listening = reduceAll(VoiceInputEvent.Start, VoiceInputEvent.Ready, VoiceInputEvent.Level(4f))
        assertEquals(0.5f, listening.level)
    }

    @Test
    fun levelMapsTheRecognizerRangeToZeroToOne() {
        assertEquals(0f, VoiceInputRules.level(VoiceInputRules.SILENT_RMS_DB))
        assertEquals(1f, VoiceInputRules.level(VoiceInputRules.LOUD_RMS_DB))
        assertEquals(0f, VoiceInputRules.level(-40f))
        assertEquals(1f, VoiceInputRules.level(25f))
        assertEquals(0f, VoiceInputRules.level(Float.NaN))
    }

    @Test
    fun theRecognitionLanguageFollowsTheInterface() {
        assertEquals("ru-RU", VoiceInputRules.languageTag("ru"))
        assertEquals("ru-RU", VoiceInputRules.languageTag(" RU "))
        assertEquals("en-US", VoiceInputRules.languageTag("en"))
        assertEquals("en-US", VoiceInputRules.languageTag("de"))
        assertEquals("en-US", VoiceInputRules.languageTag(""))
    }

    @Test
    fun onlyRealWordsAreSubmitted() {
        assertEquals("open wikipedia", VoiceInputRules.submission("  open\twikipedia "))
        assertNull(VoiceInputRules.submission(""))
        assertNull(VoiceInputRules.submission(" \n "))
    }
}
