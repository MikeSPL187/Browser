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

/** Where listening to a page from the menu stands before the voice takes over (V19c). */
sealed interface ListenPreparation {
    /** The page's text is being taken out of the page. */
    data object Preparing : ListenPreparation

    /** The page gave no article to read. */
    data class Failed(val reason: ReaderExtractionFailure) : ListenPreparation
}

/** When the mini player shows (V19b): while there is something to pause, resume or explain. */
object ListenRules {
    fun showsPlayer(
        item: ListenItem?,
        status: ReaderSpeechStatus,
        preparation: ListenPreparation? = null,
    ): Boolean = item != null && (preparation != null || when (status) {
        ReaderSpeechStatus.Initializing,
        ReaderSpeechStatus.Speaking,
        ReaderSpeechStatus.Paused,
        ReaderSpeechStatus.Unavailable,
        -> true
        ReaderSpeechStatus.Ready,
        ReaderSpeechStatus.Closed,
        -> false
    })

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

    /** Set while a page from the menu is being prepared, or when it could not be (V19c). */
    var preparation by mutableStateOf<ListenPreparation?>(null)
        private set

    val state: ReaderSpeechState
        get() = speech?.state ?: ReaderSpeechState(ReaderSpeechStatus.Closed)

    /** Bumped by every [prepare], so a late answer for an earlier page is ignored. */
    private var preparationId = 0

    /**
     * Listening to a page from the menu: the article is taken out of the page first. Returns the id
     * that [prepared] or [failed] answers with.
     */
    fun prepare(title: String, sourceUrl: String, isPrivate: Boolean): Int {
        speech?.close()
        speech = null
        item = ListenItem(title, sourceUrl, text = "", isPrivate = isPrivate)
        preparation = ListenPreparation.Preparing
        return ++preparationId
    }

    /** The prepared page's article, if [id] is still the page being prepared. */
    fun prepared(id: Int, document: ReaderDocument) {
        val current = item ?: return
        if (id != preparationId || preparation != ListenPreparation.Preparing) return
        listen(
            current.copy(
                title = document.title.ifBlank { current.title },
                sourceUrl = document.sourceUrl,
                text = document.speechText,
            ),
        )
    }

    fun failed(id: Int, reason: ReaderExtractionFailure) {
        if (id != preparationId || preparation != ListenPreparation.Preparing) return
        preparation = ListenPreparation.Failed(reason)
    }

    fun listen(next: ListenItem) {
        preparation = null
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
        preparation = null
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
