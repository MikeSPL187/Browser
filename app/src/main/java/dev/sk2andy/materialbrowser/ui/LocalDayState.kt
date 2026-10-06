package dev.sk2andy.materialbrowser.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDate

/**
 * Today in the device time zone, kept current while the screen stays composed: it is read again
 * when the screen resumes and when the system says the date, the time or the time zone changed.
 * A screen left open overnight, or brought back the next morning, moves on to the new day
 * without polling. Recomposes only when the day itself changes.
 */
@Composable
internal fun rememberLocalToday(): LocalDate {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var today by remember { mutableStateOf(LocalDate.now()) }
    DisposableEffect(context, lifecycleOwner) {
        today = LocalDate.now()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                today = LocalDate.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // System broadcasts still arrive at a receiver that other apps cannot reach.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return today
}
