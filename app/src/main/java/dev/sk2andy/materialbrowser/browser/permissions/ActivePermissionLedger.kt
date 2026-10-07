package dev.sk2andy.materialbrowser.browser.permissions

import java.util.IdentityHashMap

internal data class ActivePermissionGrant(
    val tabId: String,
    val site: PermissionSiteKey,
    val permissions: Set<SitePermission>,
)

internal data class PermissionRevocationPlan(
    val reloadNow: Set<String>,
    val reloadAfterNotificationSync: Set<String>,
) {
    val tabs: Set<String> get() = reloadNow + reloadAfterNotificationSync
}

internal class ActivePermissionLedger {
    private val grants = IdentityHashMap<Any, ActivePermissionGrant>()

    fun record(token: Any, grant: ActivePermissionGrant) {
        grants[token] = grant
    }

    fun permissions(tabId: String, site: PermissionSiteKey): Set<SitePermission> =
        grants.values
            .asSequence()
            .filter { grant -> grant.tabId == tabId && grant.site == site }
            .flatMap { grant -> grant.permissions.asSequence() }
            .toSet()

    fun has(tabId: String, site: PermissionSiteKey, permission: SitePermission): Boolean =
        grants.values.any { grant ->
            grant.tabId == tabId && grant.site == site && permission in grant.permissions
        }

    fun hasSite(tabId: String, site: PermissionSiteKey): Boolean =
        grants.values.any { grant -> grant.tabId == tabId && grant.site == site }

    fun hasTab(tabId: String): Boolean = grants.values.any { grant -> grant.tabId == tabId }

    /** Tabs holding access for [site]: to [permission] only, or to anything when it is null. */
    fun tabsFor(site: PermissionSiteKey, permission: SitePermission? = null): Set<String> =
        grants.values
            .asSequence()
            .filter { grant ->
                grant.site == site && (permission == null || permission in grant.permissions)
            }
            .mapTo(linkedSetOf(), ActivePermissionGrant::tabId)

    /**
     * Which tabs a changed decision for [site] must reload to end access already granted:
     * [permission] alone, or everything when the site is reset (null). Tabs whose revoked access
     * is notifications only wait until the engine has stored the new notification decision;
     * everything else (camera, microphone, location) reloads right away.
     */
    fun revocationPlan(
        site: PermissionSiteKey,
        permission: SitePermission?,
        includeTab: (String) -> Boolean,
    ): PermissionRevocationPlan {
        val reloadNow = linkedSetOf<String>()
        val reloadAfterNotificationSync = linkedSetOf<String>()
        tabsFor(site, permission).filter(includeTab).forEach { tabId ->
            val revoked = permission?.let(::setOf) ?: permissions(tabId, site)
            if (revoked == setOf(SitePermission.Notifications)) reloadAfterNotificationSync += tabId
            else reloadNow += tabId
        }
        return PermissionRevocationPlan(reloadNow, reloadAfterNotificationSync)
    }

    fun drop(token: Any): Boolean = grants.remove(token) != null

    fun dropTab(tabId: String): Boolean {
        val tokens = grants.entries
            .asSequence()
            .filter { (_, grant) -> grant.tabId == tabId }
            .map { (token, _) -> token }
            .toList()
        tokens.forEach(grants::remove)
        return tokens.isNotEmpty()
    }

    fun clear() = grants.clear()
}
