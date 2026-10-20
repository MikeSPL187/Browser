package dev.sk2andy.materialbrowser.browser.downloads

import androidx.compose.runtime.mutableStateListOf
import dev.sk2andy.materialbrowser.browser.actions.DownloadActionResult
import dev.sk2andy.materialbrowser.browser.actions.DownloadChoiceQueue
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadLaunchResult
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadManagerApp
import dev.sk2andy.materialbrowser.browser.actions.PendingDownloadChoice
import dev.sk2andy.materialbrowser.data.BrowserDownloadRequest
import dev.sk2andy.materialbrowser.data.BrowserDownloadSettings
import dev.sk2andy.materialbrowser.data.DownloadManagerMode

/**
 * Where a download goes once a page asked for it (#500): the file check first, then the built-in
 * download, the chosen download manager, or the question which one. The engines' entry points stay
 * in the controller and hand each request here with what starts the built-in download.
 */
internal class DownloadRouter(
    private val host: Host,
    /** Android's side: the system download queue, launching another app, finding the apps. */
    private val enqueueBuiltIn: (BrowserDownloadRequest) -> DownloadActionResult?,
    private val launchExternal: (
        request: BrowserDownloadRequest,
        app: ExternalDownloadManagerApp,
        settings: BrowserDownloadSettings,
        allowSessionData: Boolean,
    ) -> ExternalDownloadLaunchResult,
    private val discover: (BrowserDownloadRequest?) -> List<ExternalDownloadManagerApp>,
) {
    /** The controller state and reporting a route depends on. */
    interface Host {
        val settings: BrowserDownloadSettings

        fun isIncognito(tabId: String): Boolean

        /** Asks once for the notification that shows download progress. */
        fun requestNotificationPermission()

        fun report(result: DownloadActionResult)
    }

    val choices = DownloadChoiceQueue()
    val safety = DownloadSafetyGate()

    /** The download managers installed, for the settings page. */
    val managers = mutableStateListOf<ExternalDownloadManagerApp>()

    fun enqueue(request: BrowserDownloadRequest): DownloadActionResult? = enqueueBuiltIn(request)

    /**
     * Routes [request] from [tabId]. [builtInDownload] starts the built-in download; [releaseResponse]
     * lets go of what the engine held when another app takes it; [isSourceCurrent] says whether the
     * page is still the one that asked. Null when the answer comes later (the file check or a choice).
     */
    fun route(
        request: BrowserDownloadRequest,
        tabId: String,
        builtInDownload: () -> DownloadActionResult? = { enqueueBuiltIn(request) },
        releaseResponse: (() -> Unit)? = null,
        isSourceCurrent: (() -> Boolean)? = null,
        safetyChecked: Boolean = false,
    ): DownloadActionResult? {
        if (!safetyChecked && safety.hold(
                request = request,
                save = {
                    route(
                        request, tabId, builtInDownload, releaseResponse, isSourceCurrent,
                        safetyChecked = true,
                    )?.let(host::report)
                },
                cancel = { releaseResponse?.invoke() },
            )
        ) return null
        // The sheet may have waited while the page moved on: nothing from it goes to any manager.
        if (safetyChecked && isSourceCurrent?.invoke() == false) {
            releaseResponse?.invoke()
            return null
        }
        val startBuiltInDownload = {
            runCatching(host::requestNotificationPermission)
            builtInDownload()
        }
        val settings = host.settings
        val isIncognito = host.isIncognito(tabId)
        return when (DownloadManagerMode.forSource(settings.managerMode, isIncognito)) {
            DownloadManagerMode.BuiltIn -> startBuiltInDownload()
            DownloadManagerMode.AskEveryTime -> {
                val apps = discover(request)
                if (apps.isEmpty()) {
                    startBuiltInDownload()
                } else {
                    choices.enqueue(
                        PendingDownloadChoice(
                            request = request,
                            apps = apps,
                            isIncognito = isIncognito,
                            builtInDownload = startBuiltInDownload,
                            releaseResponse = releaseResponse,
                            isSourceCurrent = isSourceCurrent,
                        ),
                    )
                    null
                }
            }
            DownloadManagerMode.External -> {
                val app = discover(request).firstOrNull { it.id == settings.externalManagerId }
                if (app == null) {
                    startBuiltInDownload()
                } else {
                    launchExternallyOrFallback(
                        request = request,
                        app = app,
                        isIncognito = isIncognito,
                        builtInDownload = startBuiltInDownload,
                        releaseResponse = releaseResponse,
                    )
                }
            }
        }
    }

    /** The person picked [managerId] for the shown choice, or the built-in download for null. */
    fun confirmChoice(managerId: String?) {
        val choice = choices.take() ?: return
        if (choice.isSourceCurrent?.invoke() == false) {
            choice.releaseResponse?.invoke()
            return
        }
        val builtInDownload = choice.builtInDownload ?: { enqueueBuiltIn(choice.request) }
        val result = if (managerId == null) {
            builtInDownload()
        } else {
            val app = choice.apps.firstOrNull { it.id == managerId }
            if (app == null) {
                builtInDownload()
            } else {
                launchExternallyOrFallback(
                    choice.request,
                    app,
                    choice.isIncognito,
                    builtInDownload,
                    choice.releaseResponse,
                )
            }
        }
        result?.let(host::report)
    }

    fun dismissChoice() {
        choices.take()?.releaseResponse?.invoke()
    }

    private fun launchExternallyOrFallback(
        request: BrowserDownloadRequest,
        app: ExternalDownloadManagerApp,
        isIncognito: Boolean,
        builtInDownload: () -> DownloadActionResult?,
        releaseResponse: (() -> Unit)?,
    ): DownloadActionResult? = when (
        val result = launchExternal(request, app, host.settings, !isIncognito)
    ) {
        is ExternalDownloadLaunchResult.Launched -> {
            releaseResponse?.invoke()
            DownloadActionResult.HandedOff(fileName = request.fileName, appName = result.appName)
        }
        ExternalDownloadLaunchResult.Unavailable -> builtInDownload()
    }

    fun refreshManagers() {
        val discovered = discover(null)
        if (managers == discovered) return
        managers.clear()
        managers += discovered
    }

    /** The controller is destroyed: every held choice and file check lets go. */
    fun close() {
        choices.releaseAll()
        safety.cancelAll()
    }
}
