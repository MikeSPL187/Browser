package dev.sk2andy.materialbrowser.browser

import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import dev.sk2andy.materialbrowser.BuildConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Cold-start marks for the emulator tour (stage S3a): milliseconds since the process started,
 * logged as [LOG_TAG] in debug builds and shown as trace sections in every build. Only fixed
 * names are logged; nothing leaves the device.
 */
internal object StartupTimeline {
    const val LOG_TAG = "VolaStartup"
    private val reported: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** The moment [name] happened, once per process. */
    fun mark(name: String) {
        if (BuildConfig.DEBUG && reported.add(name)) {
            Log.i(LOG_TAG, "mark $name ${sinceProcessStart()}")
        }
    }

    /** How long [block] took, once per process, and on which thread. */
    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection("Vola.Startup.$name")
        val start = SystemClock.uptimeMillis()
        try {
            return block()
        } finally {
            Trace.endSection()
            duration(name, SystemClock.uptimeMillis() - start)
        }
    }

    @PublishedApi
    internal fun duration(name: String, millis: Long) {
        if (BuildConfig.DEBUG && reported.add(name)) {
            val thread = if (Looper.myLooper() == Looper.getMainLooper()) "main" else "background"
            Log.i(LOG_TAG, "took $name $millis $thread")
        }
    }

    fun sinceProcessStart(): Long = SystemClock.uptimeMillis() - Process.getStartUptimeMillis()
}
