package dev.sk2andy.materialbrowser.browser

import android.content.Context
import android.os.Handler
import dev.sk2andy.materialbrowser.data.ProtectionReportStore
import java.time.LocalDate

/** The report on the device preferences and the main thread [handler], with the local calendar. */
internal fun androidProtectionReportController(
    context: Context,
    handler: Handler,
): ProtectionReportController = ProtectionReportController(
    store = ProtectionReportStore(context),
    host = object : ProtectionReportController.Host {
        override fun today(): Long = LocalDate.now().toEpochDay()

        override fun postDelayed(action: Runnable, delayMillis: Long) {
            handler.postDelayed(action, delayMillis)
        }

        override fun removeCallbacks(action: Runnable) = handler.removeCallbacks(action)
    },
)
