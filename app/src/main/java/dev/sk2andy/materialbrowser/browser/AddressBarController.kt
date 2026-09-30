package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.gecko.AndroidBrowserEngineSessionPort
import dev.sk2andy.materialbrowser.browser.integration.BrowserUriPolicy
import dev.sk2andy.materialbrowser.data.AddressBarActionLayout
import dev.sk2andy.materialbrowser.data.AddressBarActionLayoutRules
import dev.sk2andy.materialbrowser.data.AddressBarDockEdge
import dev.sk2andy.materialbrowser.data.AddressBarDockPlacement
import dev.sk2andy.materialbrowser.shared.browser.AddressBarLongPressAction

/** The address bar preferences [AddressBarController] reads at startup and saves on change. */
interface AddressBarPreferenceStore {
    fun loadAddressBarDockPlacement(): AddressBarDockPlacement?
    fun loadLastAddressBarDockPlacement(): AddressBarDockPlacement?
    fun saveAddressBarDockPlacement(placement: AddressBarDockPlacement?)
    fun loadAddressBarDockingEnabled(): Boolean
    fun saveAddressBarDockingEnabled(enabled: Boolean)
    fun loadAddressBarLongPressAction(): AddressBarLongPressAction
    fun saveAddressBarLongPressAction(action: AddressBarLongPressAction)
    fun loadAddressBarActionLayout(): AddressBarActionLayout
    fun saveAddressBarActionLayout(layout: AddressBarActionLayout)
    fun loadStartupAddressFocusMode(): StartupAddressFocusMode
    fun saveStartupAddressFocusMode(mode: StartupAddressFocusMode)
}

/**
 * The address bar's placement and preferences, plus the probe that parks the bar on the right
 * edge when it would cover the text field a page is editing. [BrowserController] owns one and
 * answers the [Host] questions about tabs, engine sessions and the keyboard.
 */
class AddressBarController internal constructor(
    private val store: AddressBarPreferenceStore,
    private val host: Host,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
) {
    internal interface Host {
        val selectedTabId: String
        val selectedTab: BrowserTab

        fun tab(tabId: String): BrowserTab?

        fun engineSession(tabId: String): AndroidBrowserEngineSessionPort?

        fun navigationGeneration(tabId: String): Int

        /** Whether the keyboard is up for a page field rather than for the browser's own UI. */
        fun isPageImeVisible(): Boolean

        /** Called before the dock placement changes; the browser collapses its bottom bar. */
        fun onDockPlacementChanging()
    }

    var longPressAction by mutableStateOf(AddressBarLongPressAction.Default)
        private set
    var dockPlacement by mutableStateOf<AddressBarDockPlacement?>(null)
        private set
    var isDockingEnabled by mutableStateOf(true)
        private set
    var actionLayout by mutableStateOf(AddressBarActionLayout.Default)
        private set
    var startupFocusMode by mutableStateOf(StartupAddressFocusMode.Default)
        private set

    /** The last placement the bar was docked to; docking again returns there. */
    private var lastDockPlacement = AddressBarDockPlacement.Default
    val lastDockEdge: AddressBarDockEdge
        get() = lastDockPlacement.edge
    val isDocked: Boolean
        get() = dockPlacement != null

    private var viewportRect: BrowserViewportRect? = null
    private var pendingAutoDockProbe: Runnable? = null
    private var pendingAutoDockTabId: String? = null
    private var autoDockProbeGeneration = 0L

    /** Loads the saved preferences. A saved dock placement is dropped if docking is off. */
    internal fun restore() {
        longPressAction = store.loadAddressBarLongPressAction()
        isDockingEnabled = store.loadAddressBarDockingEnabled()
        val storedDockPlacement = store.loadAddressBarDockPlacement()
        lastDockPlacement = store.loadLastAddressBarDockPlacement()
            ?: storedDockPlacement
            ?: AddressBarDockPlacement.Default
        dockPlacement = storedDockPlacement.takeIf { isDockingEnabled }
        if (!isDockingEnabled && storedDockPlacement != null) {
            store.saveAddressBarDockPlacement(null)
        }
        actionLayout = store.loadAddressBarActionLayout()
        startupFocusMode = store.loadStartupAddressFocusMode()
    }

    fun updateDocked(docked: Boolean) {
        if (docked && !isDockingEnabled) return
        val placement = if (docked) dockPlacement ?: lastDockPlacement else null
        updateDockPlacement(placement)
    }

    fun parkOnRight() {
        if (!isDockingEnabled) return
        updateDockPlacement(lastDockPlacement.copy(edge = AddressBarDockEdge.Right))
    }

    fun updateDockPlacement(placement: AddressBarDockPlacement?) {
        val normalized = placement?.normalized()
        if (normalized != null && !isDockingEnabled) return
        if (dockPlacement == normalized) return
        if (normalized != null) cancelAutoDockProbe()
        host.onDockPlacementChanging()
        dockPlacement = normalized
        if (normalized != null) lastDockPlacement = normalized
        store.saveAddressBarDockPlacement(normalized)
    }

    fun updateDockingEnabled(enabled: Boolean) {
        if (isDockingEnabled == enabled) return
        isDockingEnabled = enabled
        store.saveAddressBarDockingEnabled(enabled)
        if (!enabled) {
            cancelAutoDockProbe()
            updateDocked(false)
        }
    }

    fun updateLongPressAction(action: AddressBarLongPressAction) {
        if (longPressAction == action) return
        longPressAction = action
        store.saveAddressBarLongPressAction(action)
    }

    fun updateActionLayout(layout: AddressBarActionLayout) {
        val normalized = AddressBarActionLayoutRules.normalize(layout)
        if (actionLayout == normalized) return
        actionLayout = normalized
        store.saveAddressBarActionLayout(normalized)
    }

    fun updateStartupFocusMode(mode: StartupAddressFocusMode) {
        if (startupFocusMode == mode) return
        startupFocusMode = mode
        store.saveStartupAddressFocusMode(mode)
    }

    /** Where the floating bar sits over the page; the auto-dock probe tests fields against it. */
    fun setBoundsInViewport(
        leftPx: Float,
        topPx: Float,
        rightPx: Float,
        bottomPx: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ) {
        val updatedRect = AddressBarAutoDockRules.viewportRect(
            leftPx = leftPx,
            topPx = topPx,
            rightPx = rightPx,
            bottomPx = bottomPx,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )
        if (viewportRect == updatedRect) return
        viewportRect = updatedRect
        if (host.isPageImeVisible()) {
            val selectedTab = host.selectedTab
            scheduleAutoDockProbe(selectedTab.id, selectedTab.url, requiresPageIme = true)
        }
    }

    fun clearBoundsInViewport() {
        viewportRect = null
    }

    /**
     * Checks, after the page settles, whether the floating bar covers a text field of [url] and
     * parks it on the right if so. With [requiresPageIme], only the focused field counts and the
     * check retries while the keyboard animates in.
     */
    internal fun scheduleAutoDockProbe(
        tabId: String,
        url: String,
        requiresPageIme: Boolean = false,
    ) {
        if (host.selectedTabId != tabId) return
        val expectedUrl = BrowserUriPolicy.normalizeHttpUrl(url) ?: return
        val session = host.engineSession(tabId) ?: return
        val navigationGeneration = host.navigationGeneration(tabId)
        cancelAutoDockProbe()
        scheduleAutoDockProbeAttempt(
            probe = AutoDockProbe(
                tabId = tabId,
                expectedUrl = expectedUrl,
                session = session,
                navigationGeneration = navigationGeneration,
                generation = autoDockProbeGeneration,
            ),
            requiresPageIme = requiresPageIme,
            completedRetryCount = 0,
            delayMillis = AUTO_DOCK_PROBE_DELAY_MILLIS,
        )
    }

    /** Stops a pending probe, for any tab or only for [tabId]. */
    internal fun cancelAutoDockProbe(tabId: String? = null) {
        if (tabId != null && pendingAutoDockTabId != tabId) return
        pendingAutoDockProbe?.let(removeCallbacks)
        pendingAutoDockProbe = null
        pendingAutoDockTabId = null
        autoDockProbeGeneration++
    }

    private class AutoDockProbe(
        val tabId: String,
        val expectedUrl: String,
        val session: AndroidBrowserEngineSessionPort,
        val navigationGeneration: Int,
        val generation: Long,
    )

    private fun scheduleAutoDockProbeAttempt(
        probe: AutoDockProbe,
        requiresPageIme: Boolean,
        completedRetryCount: Int,
        delayMillis: Long,
    ) {
        val attempt = Runnable {
            if (autoDockProbeGeneration != probe.generation) return@Runnable
            pendingAutoDockProbe = null
            val rect = viewportRect
            if (
                (requiresPageIme && !host.isPageImeVisible()) ||
                !AddressBarAutoDockRules.shouldProbe(
                    dockingEnabled = isDockingEnabled,
                    addressBarDocked = isDocked,
                    selectedTabMatches = host.selectedTabId == probe.tabId,
                    isHttpPage = true,
                    isPrivatePage = host.tab(probe.tabId)?.isIncognito != false,
                    hasViewportRect = rect != null,
                ) ||
                !isProbeContextCurrent(probe, rect)
            ) {
                cancelAutoDockProbe()
                return@Runnable
            }
            probe.session.probeTextInputOcclusion(
                viewportRect = requireNotNull(rect),
                mode = if (requiresPageIme) {
                    TextInputOcclusionProbeMode.FocusedTextInput
                } else {
                    TextInputOcclusionProbeMode.AllEditors
                },
            ) { result ->
                if (autoDockProbeGeneration != probe.generation) return@probeTextInputOcclusion
                if (
                    (requiresPageIme && !host.isPageImeVisible()) ||
                    !isProbeContextCurrent(probe, rect)
                ) {
                    cancelAutoDockProbe()
                    return@probeTextInputOcclusion
                }
                if (result == TextInputOcclusionProbeResult.Occluded) {
                    parkOnRight()
                    return@probeTextInputOcclusion
                }
                val retryDelayMillis = if (
                    requiresPageIme &&
                    AddressBarAutoDockRules.shouldRetryFocusedProbe(result)
                ) {
                    AddressBarAutoDockRules.focusedProbeRetryDelayMillis(completedRetryCount)
                } else {
                    null
                }
                if (retryDelayMillis == null) {
                    cancelAutoDockProbe()
                    return@probeTextInputOcclusion
                }
                scheduleAutoDockProbeAttempt(
                    probe = probe,
                    requiresPageIme = true,
                    completedRetryCount = completedRetryCount + 1,
                    delayMillis = retryDelayMillis,
                )
            }
        }
        pendingAutoDockProbe = attempt
        pendingAutoDockTabId = probe.tabId
        postDelayed(attempt, delayMillis)
    }

    /** Whether the tab, session, navigation, URL and bar position still match the probe. */
    private fun isProbeContextCurrent(probe: AutoDockProbe, rect: BrowserViewportRect?): Boolean {
        val tab = host.tab(probe.tabId)
        return AddressBarAutoDockRules.isProbeContextCurrent(
            dockingEnabled = isDockingEnabled,
            addressBarDocked = isDocked,
            selectedTabMatches = host.selectedTabId == probe.tabId,
            sessionMatches = host.engineSession(probe.tabId) === probe.session,
            navigationMatches = host.navigationGeneration(probe.tabId) == probe.navigationGeneration,
            urlMatches = tab?.url?.let(BrowserUriPolicy::normalizeHttpUrl) == probe.expectedUrl,
            viewportRectMatches = viewportRect == rect,
            isPrivatePage = tab?.isIncognito != false,
        )
    }

    internal companion object {
        const val AUTO_DOCK_PROBE_DELAY_MILLIS = 350L
    }
}
