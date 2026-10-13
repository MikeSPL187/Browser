package dev.sk2andy.materialbrowser.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** An article being listened to: what the mini player shows and the voice reads. */
data class ListenItem(
    val title: String,
    val sourceUrl: String,
    val text: String,
    val isPrivate: Boolean,
)

/** When the mini player shows (V19b): while there is something to pause, resume or explain. */
object ListenRules {
    fun showsPlayer(item: ListenItem?, status: ReaderSpeechStatus): Boolean = item != null && when (status) {
        ReaderSpeechStatus.Initializing,
        ReaderSpeechStatus.Speaking,
        ReaderSpeechStatus.Paused,
        ReaderSpeechStatus.Unavailable,
        -> true
        ReaderSpeechStatus.Ready,
        ReaderSpeechStatus.Closed,
        -> false
    }

    /** A private page's title stays out of a normal tab's bar. */
    fun showsTitle(item: ListenItem, selectedTabPrivate: Boolean): Boolean = !item.isPrivate || selectedTabPrivate
}

/**
 * Listening that outlives the reader (V19b, system-v5.md section 10): one voice for the whole
 * browser, so an article goes on after the reader closes and the mini player above the bar
 * pauses or stops it. Nothing is written to disk; the voice is released when listening ends.
 */
class ListenSession private constructor(private val context: Context) {
    var item by mutableStateOf<ListenItem?>(null)
        private set
    private var speech by mutableStateOf<ReaderSpeech?>(null)

    val state: ReaderSpeechState
        get() = speech?.state ?: ReaderSpeechState(ReaderSpeechStatus.Closed)

    fun listen(next: ListenItem) {
        val current = speech
        if (current != null && item == next) {
            current.play(next.text)
            return
        }
        current?.close()
        item = next
        speech = ReaderSpeechController(context).also { it.play(next.text) }
    }

    fun pause() {
        speech?.pause()
    }

    fun resume() {
        val current = item ?: return
        speech?.play(current.text)
    }

    /** Stops listening and lets the voice go. */
    fun close() {
        speech?.close()
        speech = null
        item = null
    }

    /**
     * The reader's voice for [document], which controls this session. With [keepsSpeaking] (a mini
     * player is there to stop it) the article goes on after the reader closes; otherwise closing
     * the reader stops it, as before V19b.
     */
    fun speechFor(document: ReaderDocument, isPrivate: Boolean, keepsSpeaking: Boolean): ReaderSpeech = object : ReaderSpeech {
        private fun isCurrent() = item?.sourceUrl == document.sourceUrl

        override val state: ReaderSpeechState
            get() = if (isCurrent()) this@ListenSession.state else ReaderSpeechState(ReaderSpeechStatus.Ready)

        override fun play(content: String) =
            listen(ListenItem(document.title, document.sourceUrl, content, isPrivate))

        override fun pause() {
            if (isCurrent()) this@ListenSession.pause()
        }

        override fun stop() {
            if (isCurrent()) this@ListenSession.close()
        }

        override fun close() {
            if (!keepsSpeaking && isCurrent()) this@ListenSession.close()
        }
    }

    companion object {
        @Volatile
        private var instance: ListenSession? = null

        fun get(context: Context): ListenSession = instance ?: synchronized(this) {
            instance ?: ListenSession(context.applicationContext).also { instance = it }
        }
    }
}
