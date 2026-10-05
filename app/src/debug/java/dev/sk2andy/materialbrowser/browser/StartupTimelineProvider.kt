package dev.sk2andy.materialbrowser.browser

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Printer
import android.view.ViewTreeObserver
import dev.sk2andy.materialbrowser.MainActivity

/**
 * Debug builds only: starts with the process, before any activity, and writes the cold-start
 * timeline the emulator tour reads (S3a). Marks the main activity's create, resume and first frame,
 * and every main-thread message that held the thread for [SLOW_MESSAGE_MILLIS] or more during the
 * first [WATCH_MILLIS]: the dispatch line names the handler and callback class, never page data.
 */
class StartupTimelineProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        StartupTimeline.mark("ProviderCreated")
        watchSlowMainMessages()
        (context?.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (activity is MainActivity) StartupTimeline.mark("ActivityCreateStart")
                }

                override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (activity is MainActivity) StartupTimeline.mark("ActivityCreateEnd")
                }

                override fun onActivityPostResumed(activity: Activity) {
                    if (activity !is MainActivity) return
                    StartupTimeline.mark("ActivityResumed")
                    val decor = activity.window.decorView
                    decor.viewTreeObserver.addOnDrawListener(
                        object : ViewTreeObserver.OnDrawListener {
                            override fun onDraw() {
                                StartupTimeline.mark("FirstFrame")
                                decor.post { decor.viewTreeObserver.removeOnDrawListener(this) }
                            }
                        },
                    )
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
        return true
    }

    private fun watchSlowMainMessages() {
        val looper = Looper.getMainLooper()
        var started = 0L
        var dispatching = ""
        looper.setMessageLogging(
            Printer { line ->
                val now = SystemClock.uptimeMillis()
                if (line.startsWith(">>>>> Dispatching")) {
                    started = now
                    dispatching = line.removePrefix(">>>>> Dispatching to ")
                } else if (line.startsWith("<<<<< Finished")) {
                    val took = now - started
                    if (took >= SLOW_MESSAGE_MILLIS) {
                        Log.i(StartupTimeline.LOG_TAG, "slow $took at ${StartupTimeline.sinceProcessStart()} $dispatching")
                    }
                    if (StartupTimeline.sinceProcessStart() > WATCH_MILLIS) looper.setMessageLogging(null)
                }
            },
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        const val SLOW_MESSAGE_MILLIS = 48L
        const val WATCH_MILLIS = 15_000L
    }
}
