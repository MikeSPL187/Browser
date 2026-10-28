package dev.sk2andy.materialbrowser.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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

    /** The speeds the mini player's capsule steps through, from the usual one up and back to slower. */
    val SPEEDS = listOf(1f, 1.2f, 1.5f, 2f, 0.8f)

    /** Words a voice says in a minute at 1×: a little faster than the reading pace of the library. */
    const val SPOKEN_WORDS_PER_MINUTE = 160

    /** The speed after [current]; an unknown speed starts the list again. */
    fun nextSpeed(current: Float): Float {
        val index = SPEEDS.indexOfFirst { abs(it - current) < SPEED_TOLERANCE }
        return SPEEDS[(index + 1) % SPEEDS.size]
    }

    /** About how long [text] takes to say at [speed], in whole seconds; zero for no text. */
    fun durationSeconds(text: String, speed: Float): Int {
        val words = text.split(WHITESPACE).count(String::isNotBlank)
        if (words == 0) return 0
        return (words * SECONDS_PER_MINUTE / (SPOKEN_WORDS_PER_MINUTE * speed.coerceAtLeast(SPEEDS.min())))
            .roundToInt()
            .coerceAtLeast(1)
    }

    /** How much of the article is said once the voice is at [characterOffset], from 0 to 1. */
    fun progress(text: String, characterOffset: Int): Float =
        if (text.isEmpty()) 0f else (characterOffset.toFloat() / text.length).coerceIn(0f, 1f)

    /** «3:12»: minutes and two-digit seconds, as on a player. */
    fun clock(seconds: Int): String {
        val whole = seconds.coerceAtLeast(0)
        return "%d:%02d".format(Locale.ROOT, whole / SECONDS_IN_MINUTE, whole % SECONDS_IN_MINUTE)
    }

    private const val SPEED_TOLERANCE = 0.01f
    private const val SECONDS_PER_MINUTE = 60f
    private const val SECONDS_IN_MINUTE = 60
    private val WHITESPACE = Regex("\\s+")
}

/**
 * Listening that outlives the reader (V19b, system-v5.md section 10): one voice for the whole
 * browser, so an article goes on after the reader closes and the mini player above the bar
 * pauses or stops it. Nothing is written to disk; the voice is released when listening ends.
 */
class ListenSession private constructor(private val context: Context) {
    var item by mutableStateOf<ListenItem?>(null)
        private set
    private var speech by mutableStateOf<ReaderSpeechController?>(null)

    /** The voice's speed, kept for the next article while the app runs; never written to disk. */
    var speed by mutableStateOf(ListenRules.SPEEDS.first())
        private set

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
        speech = ReaderSpeechController(context).also {
            it.setSpeed(speed)
            it.play(next.text)
        }
    }

    /** The mini player's speed capsule: the next speed, at once if the voice is speaking. */
    fun nextSpeed() {
        speed = ListenRules.nextSpeed(speed)
        speech?.setSpeed(speed)
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
