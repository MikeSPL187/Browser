package dev.sk2andy.materialbrowser.reader

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ReaderSpeechController(context: Context) : ReaderSpeech {
    override var state by mutableStateOf(ReaderSpeechState())
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var text: String = ""

    /** Bumped on every play, pause, stop and close; callbacks of older queues are ignored. */
    private var generation = 0

    /** Read on the TTS binder thread. */
    @Volatile
    private var chunkStarts: Map<String, Int> = emptyMap()

    init {
        engine = TextToSpeech(context.applicationContext) { result ->
            handler.post {
                if (state.status == ReaderSpeechStatus.Closed) return@post
                if (result == TextToSpeech.SUCCESS) {
                    engine?.setOnUtteranceProgressListener(listener)
                    dispatch(ReaderSpeechEvent.Initialized)
                } else {
                    dispatch(ReaderSpeechEvent.InitializationFailed)
                }
            }
        }
    }

    override fun play(content: String) {
        if (state.status != ReaderSpeechStatus.Ready && state.status != ReaderSpeechStatus.Paused) return
        text = content
        val start = state.characterOffset.coerceIn(0, text.length)
        if (start >= text.length) dispatch(ReaderSpeechEvent.Stop)
        if (text.isBlank() || start >= text.length) return
        val tts = engine ?: return
        val playGeneration = ++generation
        val chunks = chunks(
            text,
            start,
            TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3_500),
            playGeneration,
        )
        chunkStarts = chunks.associate { it.first to it.second }
        chunks.forEachIndexed { index, (id, _, chunk) ->
            val result = tts.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                Bundle(),
                id,
            )
            if (result == TextToSpeech.ERROR) {
                dispatch(ReaderSpeechEvent.InitializationFailed)
                return
            }
        }
        dispatch(ReaderSpeechEvent.Play)
    }

    override fun pause() {
        if (state.status != ReaderSpeechStatus.Speaking) return
        generation++
        engine?.stop()
        dispatch(ReaderSpeechEvent.Pause)
    }

    override fun stop() {
        generation++
        engine?.stop()
        dispatch(ReaderSpeechEvent.Stop)
    }

    override fun close() {
        generation++
        engine?.stop()
        engine?.shutdown()
        engine = null
        dispatch(ReaderSpeechEvent.Close)
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            if (utteranceId == null || utteranceId != chunkStarts.keys.lastOrNull()) return
            postIfCurrent(utteranceId) { dispatch(ReaderSpeechEvent.Completed) }
        }

        @Deprecated("Deprecated in Android")
        override fun onError(utteranceId: String?) {
            postIfCurrent(utteranceId) { dispatch(ReaderSpeechEvent.InitializationFailed) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            postIfCurrent(utteranceId) { dispatch(ReaderSpeechEvent.InitializationFailed) }
        }

        override fun onRangeStart(
            utteranceId: String?,
            start: Int,
            end: Int,
            frame: Int,
        ) {
            val base = chunkStarts[utteranceId] ?: return
            postIfCurrent(utteranceId) { dispatch(ReaderSpeechEvent.RangeStarted(base + start)) }
        }
    }

    /**
     * Runs [action] on the main thread only while [utteranceId] belongs to the current queue;
     * the check repeats inside the posted runnable, after any stop or new play queued before it.
     */
    private fun postIfCurrent(utteranceId: String?, action: () -> Unit) {
        val utteranceGeneration = ReaderUtteranceIds.generationOf(utteranceId) ?: return
        handler.post {
            if (ReaderUtteranceIds.isCurrent(utteranceGeneration, generation)) action()
        }
    }

    private fun dispatch(event: ReaderSpeechEvent) {
        state = ReaderSpeechRules.reduce(state, event, text.length)
    }

    private fun chunks(
        content: String,
        startOffset: Int,
        maxLength: Int,
        generation: Int,
    ): List<Triple<String, Int, String>> {
        val result = mutableListOf<Triple<String, Int, String>>()
        var cursor = startOffset
        var ordinal = 0
        while (cursor < content.length) {
            val hardEnd = minOf(cursor + maxLength, content.length)
            val split = if (hardEnd == content.length) {
                hardEnd
            } else {
                content.lastIndexOfAny(charArrayOf('\n', '.', '!', '?', ' '), hardEnd)
                    .takeIf { it > cursor + maxLength / 2 }
                    ?.plus(1)
                    ?: hardEnd
            }
            result += Triple(
                ReaderUtteranceIds.id(generation, ordinal, cursor),
                cursor,
                content.substring(cursor, split),
            )
            cursor = split
            ordinal++
        }
        return result
    }
}

/**
 * Utterance ids carry the playback generation, so a late callback from a stopped or replaced
 * queue (the same chunk ordinal and offset as the new one) cannot change the new playback.
 */
internal object ReaderUtteranceIds {
    private const val PREFIX = "reader"

    fun id(generation: Int, ordinal: Int, cursor: Int): String =
        "$PREFIX-$generation-$ordinal-$cursor"

    fun generationOf(utteranceId: String?): Int? {
        val parts = utteranceId?.split('-') ?: return null
        if (parts.size != 4 || parts[0] != PREFIX) return null
        return parts[1].toIntOrNull()
    }

    fun isCurrent(utteranceGeneration: Int, currentGeneration: Int): Boolean =
        utteranceGeneration == currentGeneration
}
