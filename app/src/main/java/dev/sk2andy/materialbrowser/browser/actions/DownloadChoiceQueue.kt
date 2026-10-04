package dev.sk2andy.materialbrowser.browser.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** «Download with…» choices, shown one at a time in the order the downloads came. */
class DownloadChoiceQueue {
    var pending by mutableStateOf<PendingDownloadChoice?>(null)
        private set
    private val queue = ArrayDeque<PendingDownloadChoice>()

    fun enqueue(choice: PendingDownloadChoice) {
        if (pending == null) pending = choice else queue.addLast(choice)
    }

    /** Takes the shown choice off the screen and brings up the next one. */
    fun take(): PendingDownloadChoice? {
        val choice = pending ?: return null
        pending = queue.removeFirstOrNull()
        return choice
    }

    /** Drops every choice and releases what the engine held for it. */
    fun releaseAll() {
        val choices = listOfNotNull(pending) + queue
        pending = null
        queue.clear()
        choices.forEach { choice -> choice.releaseResponse?.invoke() }
    }
}
