package dev.sk2andy.materialbrowser.browser.permissions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.browser.BrowserTab

/**
 * A site's access to camera, microphone, location and the rest (#500): the one request in flight, its
 * prompt, the Android permissions it waits for, and what each tab holds now. Decisions are kept by
 * [repository]; the controller supplies the tabs and the engine sessions through [Host].
 */
internal class SitePermissionAccess(
    private val repository: PermissionRadarRepository,
    private val host: Host,
) {
    /** The controller state and engine calls a request depends on. */
    interface Host {
        val tabs: List<BrowserTab>
        val selectedTabId: String
        val isActivityResumed: Boolean
        val isActivityStarted: Boolean
        val isDestroyed: Boolean

        fun pageUrl(tabId: String): String?

        fun navigationGeneration(tabId: String): Int?

        fun hasEngineSession(tabId: String): Boolean

        /** Tells the tab's engine; false without a session. [onComplete] runs once it applied. */
        fun setNotificationPermission(
            tabId: String,
            origin: String,
            decision: SitePermissionDecision,
            onComplete: (() -> Unit)? = null,
        ): Boolean

        fun reload(tabId: String)

        fun hasRuntimePermission(permission: String): Boolean

        fun requestRuntimePermissions(permissions: Set<String>)

        fun clearExternalNavigationAuthorization(tabId: String)

        fun scheduleResidentSessionTrim()
    }

    private val activePermissions = ActivePermissionLedger()
    private var pendingAccess: PendingAccess? = null
    private var shownPromptId: Long? = null
    private var promptSequence = 0L

    /** Bumped on every change, so what reads it is drawn again. */
    var revision by mutableIntStateOf(0)
        private set

    var prompt by mutableStateOf<PermissionPrompt?>(null)
        private set

    /** Whether a request is waiting on the person or on Android. */
    val isBusy: Boolean get() = pendingAccess != null

    val pendingTabId: String? get() = pendingAccess?.identity?.tabId

    /** Something about permissions changed outside a request (a profile removed, data cleared). */
    fun touch() {
        revision++
    }

    fun hasActivity(tabId: String): Boolean {
        revision
        return pendingAccess?.identity?.tabId == tabId || activePermissions.hasTab(tabId)
    }

    fun radarSnapshot(tabId: String, requestedOrigin: String?): PermissionRadarSnapshot {
        revision
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return PermissionRadarSnapshot.Empty
        val currentOrigin = PermissionOrigin.normalize(host.pageUrl(tabId) ?: tab.url)
        val selectedOrigin = PermissionOrigin.normalize(requestedOrigin) ?: currentOrigin
        val knownOrigins = buildSet {
            addAll(repository.origins(tab.profileId, tab.isIncognito))
            currentOrigin?.let(::add)
        }.sorted()
        val origin = selectedOrigin ?: return PermissionRadarSnapshot.Empty.copy(
            isPrivate = tab.isIncognito,
            knownOrigins = knownOrigins,
        )
        val site = PermissionSiteKey(tab.profileId, origin)
        val pending = pendingAccess
            ?.takeIf { access -> access.identity.tabId == tabId && access.identity.origin == origin }
            ?.requested
            .orEmpty()
        val active = activePermissions.permissions(tabId, site)
        return PermissionRadarSnapshot(
            site = site,
            isPrivate = tab.isIncognito,
            knownOrigins = knownOrigins,
            entries = SitePermission.entries.map { permission ->
                PermissionRadarEntry(
                    permission = permission,
                    decision = repository.decision(site, permission, tab.isIncognito),
                    allowedForSession = repository.isAllowedForSession(
                        site,
                        permission,
                        tab.isIncognito,
                    ),
                    activity = when (permission) {
                        in pending -> SitePermissionActivity.Pending
                        in active -> SitePermissionActivity.Active
                        else -> SitePermissionActivity.Idle
                    },
                )
            },
        )
    }

    fun setDecision(
        tabId: String,
        origin: String,
        permission: SitePermission,
        decision: SitePermissionDecision,
    ): Boolean {
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return false
        val normalizedOrigin = PermissionOrigin.normalize(origin) ?: return false
        if (normalizedOrigin != origin) return false
        val site = PermissionSiteKey(tab.profileId, normalizedOrigin)
        repository.setDecision(site, permission, decision, tab.isIncognito)
        val reloadAfterSync = if (decision == SitePermissionDecision.Allow) emptySet()
        else revokeAccess(tab, site, permission)
        if (permission == SitePermission.Notifications) {
            syncNotificationPermission(tabId, normalizedOrigin, decision, reloadAfterSync)
        }
        revision++
        if (pendingAccess?.let { it.site == site && permission in it.requested } == true) {
            cancelPending()
        }
        return true
    }

    fun resetSite(tabId: String, origin: String): Boolean {
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return false
        val normalizedOrigin = PermissionOrigin.normalize(origin) ?: return false
        if (normalizedOrigin != origin) return false
        val site = PermissionSiteKey(tab.profileId, normalizedOrigin)
        repository.resetSite(site, tab.isIncognito)
        val reloadAfterSync = revokeAccess(tab, site, permission = null)
        syncNotificationPermission(tabId, normalizedOrigin, SitePermissionDecision.Ask, reloadAfterSync)
        revision++
        if (pendingAccess?.site == site) cancelPending(tabId)
        return true
    }

    /** Ends access [site] holds in every tab like [tab]; returns tabs to reload after notification sync. */
    private fun revokeAccess(
        tab: BrowserTab,
        site: PermissionSiteKey,
        permission: SitePermission?,
    ): Set<String> {
        val plan = activePermissions.revocationPlan(site, permission) { id ->
            host.tabs.any { other -> other.id == id && other.isIncognito == tab.isIncognito }
        }
        plan.tabs.forEach { id ->
            cancelPending(id)
            removeActiveForTab(id)
            host.clearExternalNavigationAuthorization(id)
        }
        plan.reloadNow.forEach(host::reload)
        return plan.reloadAfterNotificationSync
    }

    private fun syncNotificationPermission(
        tabId: String,
        origin: String,
        decision: SitePermissionDecision,
        reloadAfterSync: Set<String>,
    ) {
        val applied = host.setNotificationPermission(tabId, origin, decision) {
            reloadAfterSync.forEach(host::reload)
        }
        if (!applied) reloadAfterSync.forEach(host::reload)
    }

    fun respondToPrompt(promptId: Long, choice: PermissionPromptChoice) {
        val pending = pendingAccess?.takeIf { it.promptId == promptId } ?: return
        if (!isRequestCurrent(pending.identity)) {
            cancelPending(pending.identity.tabId)
            return
        }
        val prompted = pending.prompted
        val effectiveChoice = if (
            SitePermission.Notifications in prompted && choice == PermissionPromptChoice.AllowOnce
        ) PermissionPromptChoice.AllowAlways else choice
        when (effectiveChoice) {
            PermissionPromptChoice.AllowOnce -> repository.allowOnce(
                pending.site,
                prompted,
                pending.identity.isPrivate,
            )
            PermissionPromptChoice.AllowAlways -> prompted.forEach { permission ->
                repository.setDecision(
                    pending.site,
                    permission,
                    SitePermissionDecision.Allow,
                    pending.identity.isPrivate,
                )
            }
            PermissionPromptChoice.Block -> prompted.forEach { permission ->
                repository.setDecision(
                    pending.site,
                    permission,
                    SitePermissionDecision.Block,
                    pending.identity.isPrivate,
                )
            }
        }
        prompt = null
        revision++
        val allowed = if (effectiveChoice == PermissionPromptChoice.Block) {
            pending.allowed
        } else {
            pending.allowed + prompted
        }
        continueAccess(pending.copy(allowed = allowed, prompted = emptySet()))
    }

    fun onPromptShown(promptId: Long) {
        val pending = pendingAccess?.takeIf { it.promptId == promptId } ?: return
        if (shownPromptId == promptId) return
        shownPromptId = promptId
        pending.onPromptShown?.let { callback -> runCatching(callback) }
    }

    /** Android answered the runtime permissions a site request was waiting for. */
    fun onRuntimePermissionResult(results: Map<String, Boolean>) {
        val pending = pendingAccess?.takeIf(PendingAccess::awaitingRuntime) ?: return
        if (!isRequestCurrent(pending.identity, requireResumed = false)) {
            cancelPending(pending.identity.tabId)
            return
        }
        val granted = PermissionRequestRules.afterRuntimeResult(pending.allowed) { permission ->
            when (permission) {
                SitePermission.Location -> permission.runtimePermissions.any { runtimePermission ->
                    results[runtimePermission] == true || host.hasRuntimePermission(runtimePermission)
                }
                else -> permission.runtimePermissions.all { runtimePermission ->
                    results[runtimePermission] == true || host.hasRuntimePermission(runtimePermission)
                }
            }
        }
        finishAccess(pending, granted)
    }

    fun begin(
        identity: PermissionRequestIdentity,
        site: PermissionSiteKey,
        requested: Set<SitePermission>,
        requestToken: Any,
        onPromptShown: (() -> Unit)? = null,
        grant: (Set<SitePermission>) -> Unit,
        deny: () -> Unit,
    ) {
        val matrix = PermissionRequestRules.decisions(
            permissions = requested,
            decisionFor = { permission ->
                repository.decision(site, permission, identity.isPrivate)
            },
            allowedForSession = { permission ->
                repository.isAllowedForSession(site, permission, identity.isPrivate)
            },
        )
        val promptId = if (matrix.pending.isEmpty()) null else ++promptSequence
        val pending = PendingAccess(
            identity = identity,
            site = site,
            requested = requested,
            allowed = matrix.allowed,
            prompted = matrix.pending,
            requestToken = requestToken,
            onPromptShown = onPromptShown,
            promptId = promptId,
            awaitingRuntime = false,
            delivery = PermissionResponseDelivery(grant, deny),
        )
        pendingAccess = pending
        revision++
        if (promptId != null) {
            prompt = PermissionPrompt(
                id = promptId,
                tabId = identity.tabId,
                site = site,
                permissions = matrix.pending,
                isPrivate = identity.isPrivate,
            )
        } else {
            continueAccess(pending)
        }
    }

    private fun continueAccess(pending: PendingAccess) {
        if (!isRequestCurrent(
                pending.identity,
                requireResumed = !pending.awaitingRuntime,
            )
        ) {
            cancelPending(pending.identity.tabId)
            return
        }
        pendingAccess = pending
        val missingRuntimePermissions = pending.allowed.flatMapTo(linkedSetOf()) { permission ->
            if (hasRuntimePermissionFor(permission)) emptySet()
            else permission.runtimePermissions.filterNot(host::hasRuntimePermission)
        }
        if (missingRuntimePermissions.isEmpty()) {
            finishAccess(pending, pending.allowed)
            return
        }
        pendingAccess = pending.copy(awaitingRuntime = true)
        revision++
        runCatching { host.requestRuntimePermissions(missingRuntimePermissions) }
            .onFailure { cancelPending(pending.identity.tabId) }
    }

    private fun finishAccess(
        pending: PendingAccess,
        granted: Set<SitePermission>,
    ) {
        if (pendingAccess?.requestToken !== pending.requestToken) return
        if (!isRequestCurrent(
                pending.identity,
                requireResumed = !pending.awaitingRuntime,
            )
        ) {
            cancelPending(pending.identity.tabId)
            return
        }
        pendingAccess = null
        prompt = null
        if (
            SitePermission.Notifications in pending.allowed &&
            SitePermission.Notifications !in granted
        ) {
            repository.setDecision(
                pending.site,
                SitePermission.Notifications,
                SitePermissionDecision.Block,
                pending.identity.isPrivate,
            )
            host.setNotificationPermission(
                pending.identity.tabId,
                pending.site.origin,
                SitePermissionDecision.Block,
            )
        }
        if (granted.isEmpty()) {
            runCatching { pending.delivery.deny() }
        } else {
            runCatching { pending.delivery.grant(granted) }
                .onSuccess {
                    activePermissions.record(pending.requestToken, ActivePermissionGrant(
                        tabId = pending.identity.tabId,
                        site = pending.site,
                        permissions = granted,
                    ))
                }
                .onFailure { activePermissions.drop(pending.requestToken) }
        }
        revision++
        host.scheduleResidentSessionTrim()
    }

    /** Denies the request in flight, or only [tabId]'s when given. */
    fun cancelPending(tabId: String? = null) {
        val pending = pendingAccess ?: return
        if (tabId != null && pending.identity.tabId != tabId) return
        pendingAccess = null
        prompt = null
        runCatching { pending.delivery.deny() }
        revision++
        host.scheduleResidentSessionTrim()
    }

    /** Cancels the request in flight unless it waits on Android, which answers on its own. */
    fun cancelPendingUnlessAwaitingRuntime() {
        if (pendingAccess?.awaitingRuntime != true) cancelPending()
    }

    /** The engine withdrew the request [requestToken] stood for. */
    fun dropCanceled(requestToken: Any) {
        val pending = pendingAccess ?: return
        if (pending.requestToken !== requestToken) return
        pendingAccess = null
        prompt = null
        pending.delivery.drop()
        revision++
        host.scheduleResidentSessionTrim()
    }

    fun removeActiveForTab(tabId: String) {
        val removed = activePermissions.dropTab(tabId)
        if (removed) {
            revision++
            host.scheduleResidentSessionTrim()
        }
    }

    /** Forgets every grant in this run (data cleared, the controller destroyed). */
    fun clearActive() {
        activePermissions.clear()
    }

    fun requestIdentity(
        tabId: String,
        normalizedOrigin: String?,
    ): PermissionRequestIdentity? {
        val tab = host.tabs.firstOrNull { it.id == tabId } ?: return null
        val origin = normalizedOrigin ?: return null
        val generation = host.navigationGeneration(tabId) ?: return null
        return PermissionRequestIdentity(
            tabId = tabId,
            profileId = tab.profileId,
            origin = origin,
            navigationGeneration = generation,
            isPrivate = tab.isIncognito,
        )
    }

    fun isRequestCurrent(
        identity: PermissionRequestIdentity,
        requireResumed: Boolean = true,
    ): Boolean {
        val tab = host.tabs.firstOrNull { it.id == identity.tabId }
        val currentOrigin = PermissionOrigin.normalize(
            host.pageUrl(identity.tabId) ?: tab?.url,
        )
        return PermissionRequestRules.isCurrent(
            identity,
            PermissionRequestState(
                tabId = identity.tabId,
                profileId = tab?.profileId.orEmpty(),
                topLevelOrigin = currentOrigin,
                navigationGeneration = host.navigationGeneration(identity.tabId),
                isPrivate = tab?.isIncognito ?: false,
                isSelected = host.selectedTabId == identity.tabId,
                isActivityResumed = if (requireResumed) {
                    host.isActivityResumed && !host.isDestroyed
                } else {
                    host.isActivityStarted && !host.isDestroyed
                },
                tabExists = tab != null && host.hasEngineSession(identity.tabId),
            ),
        )
    }

    private fun hasRuntimePermissionFor(permission: SitePermission): Boolean = when (permission) {
        SitePermission.Location -> permission.runtimePermissions.any(host::hasRuntimePermission)
        else -> permission.runtimePermissions.all(host::hasRuntimePermission)
    }

    private data class PendingAccess(
        val identity: PermissionRequestIdentity,
        val site: PermissionSiteKey,
        val requested: Set<SitePermission>,
        val allowed: Set<SitePermission>,
        val prompted: Set<SitePermission>,
        val requestToken: Any,
        val onPromptShown: (() -> Unit)?,
        val promptId: Long?,
        val awaitingRuntime: Boolean,
        val delivery: PermissionResponseDelivery,
    )
}
