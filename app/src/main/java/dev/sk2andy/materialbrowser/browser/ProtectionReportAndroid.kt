package dev.sk2andy.materialbrowser.browser

import android.content.Context
import android.os.Handler
import dev.sk2andy.materialbrowser.data.ProtectionReportStore
import java.time.LocalDate
import java.util.concurrent.Executors

/**
 * The report on the device preferences and the main thread [handler], with the local calendar; the
 * store is read and written on a thread of its own.
 */
internal fun androidProtectionReportController(
    context: Context,
    handler: Handler,
): ProtectionReportController = ProtectionReportController(
    store = ProtectionReportStore(context),
    host = object : ProtectionReportController.Host {
        private val storeThread = Executors.newSingleThreadExecutor { task ->
            Thread(task, "ProtectionReport").apply { isDaemon = true }
        }

        override fun runInBackground(task: () -> Unit) = storeThread.execute { task() }

        override fun post(action: () -> Unit) {
            handler.post { action() }
        }

        override fun today(): Long = LocalDate.now().toEpochDay()

        override fun postDelayed(action: Runnable, delayMillis: Long) {
            handler.postDelayed(action, delayMillis)
        }

        override fun removeCallbacks(action: Runnable) = handler.removeCallbacks(action)
    },
)
