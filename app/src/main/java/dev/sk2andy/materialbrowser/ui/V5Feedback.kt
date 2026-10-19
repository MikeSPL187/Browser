package dev.sk2andy.materialbrowser.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.filterNotNull

/**
 * A short message from a screen of the browser, shown as the line above the bar (V5FeedbackNotice
 * under v5) instead of a toast (LG-02, principle 1). The browser screen shows it through
 * [V5FeedbackHostEffect]; a screen of another activity, such as Downloads, still gets a toast.
 * One per process, as there is one browser screen.
 */
internal object V5Feedback {
    internal data class Message(val id: Long, val text: String)

    internal var pending by mutableStateOf<Message?>(null)
        private set

    private var host: WeakReference<Activity>? = null
    private var nextId = 0L

    fun show(context: Context, text: String) {
        val activity = context.findActivity()
        if (activity != null && activity === host?.get()) {
            pending = Message(++nextId, text)
        } else {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun show(context: Context, @StringRes text: Int) = show(context, context.getString(text))

    internal fun attach(activity: Activity?) {
        host = activity?.let(::WeakReference)
    }

    internal fun detach(activity: Activity?) {
        if (host?.get() === activity) host = null
    }

    internal fun take(id: Long) {
        if (pending?.id == id) pending = null
    }
}

/** Shows [V5Feedback]'s messages on [hostState] while the browser screen is composed. */
@Composable
internal fun V5FeedbackHostEffect(hostState: SnackbarHostState) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        V5Feedback.attach(activity)
        onDispose { V5Feedback.detach(activity) }
    }
    LaunchedEffect(hostState) {
        snapshotFlow { V5Feedback.pending }.filterNotNull().collect { message ->
            V5Feedback.take(message.id)
            hostState.showSnackbar(message.text, duration = SnackbarDuration.Short)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
