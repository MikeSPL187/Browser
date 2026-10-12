package dev.sk2andy.materialbrowser.browser

import android.content.Context
import android.os.Handler
import dev.sk2andy.materialbrowser.data.HiddenDistractionsStore
import java.util.concurrent.Executors

/** The lists in the app's preferences, read and written on one storage thread off [handler]'s. */
internal fun androidHiddenDistractionsController(
    context: Context,
    handler: Handler,
    pageFor: (tabId: String) -> PageDistractionsPort?,
    onChanged: () -> Unit,
): HiddenDistractionsController {
    val storeThread = Executors.newSingleThreadExecutor { task ->
        Thread(task, "HiddenDistractions").apply { isDaemon = true }
    }
    return HiddenDistractionsController(
        storage = HiddenDistractionsStore(context),
        host = object : HiddenDistractionsController.Host {
            override fun runInBackground(task: () -> Unit) = storeThread.execute { task() }

            override fun post(action: () -> Unit) {
                handler.post { action() }
            }
        },
        pageFor = pageFor,
        onChanged = onChanged,
    )
}
