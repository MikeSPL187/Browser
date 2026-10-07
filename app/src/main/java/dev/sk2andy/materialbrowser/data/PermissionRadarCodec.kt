package dev.sk2andy.materialbrowser.data

import dev.sk2andy.materialbrowser.browser.permissions.PermissionOrigin
import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision
import org.json.JSONArray
import org.json.JSONObject

/** The JSON form of the persistent Permission Radar decisions, oldest changed site first. */
internal object PermissionRadarCodec {
    /**
     * The decisions in [raw], or `null` when its root is not a JSON array at all. A damaged site
     * entry is skipped on its own, so it never takes the valid ones down with it.
     */
    fun decode(
        raw: String,
        maxSites: Int,
    ): Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>>? {
        val values = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        return buildMap {
            // Sites are written oldest first; the newest maxSites are the ones kept.
            for (index in (values.length() - maxSites).coerceAtLeast(0) until values.length()) {
                val entry = runCatching { decodeSite(values.optJSONObject(index)) }.getOrNull()
                    ?: continue
                remove(entry.first)
                put(entry.first, entry.second)
            }
        }
    }

    fun encode(
        decisions: Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>>,
        maxSites: Int,
    ): String {
        val values = JSONArray()
        decisions.asSequence()
            .filter { (site, permissions) ->
                site.profileId.isNotBlank() &&
                    PermissionOrigin.normalize(site.origin) == site.origin &&
                    permissions.any { it.value != SitePermissionDecision.Ask }
            }
            .toList()
            .takeLast(maxSites)
            .forEach { (site, permissions) ->
                val encodedPermissions = JSONObject()
                permissions.forEach { (permission, decision) ->
                    if (decision != SitePermissionDecision.Ask) {
                        encodedPermissions.put(permission.name, decision.name)
                    }
                }
                values.put(
                    JSONObject()
                        .put("profileId", site.profileId)
                        .put("origin", site.origin)
                        .put("permissions", encodedPermissions),
                )
            }
        return values.toString()
    }

    private fun decodeSite(
        item: JSONObject?,
    ): Pair<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>>? {
        if (item == null) return null
        val profileId = item.optString("profileId").trim()
        val origin = PermissionOrigin.normalize(item.optString("origin"))
        if (profileId.isEmpty() || origin == null) return null
        val permissions = item.optJSONObject("permissions") ?: return null
        val decisions = buildMap {
            SitePermission.entries.forEach { permission ->
                val decision = runCatching {
                    SitePermissionDecision.valueOf(permissions.optString(permission.name))
                }.getOrNull()
                if (decision != null && decision != SitePermissionDecision.Ask) {
                    put(permission, decision)
                }
            }
        }
        return if (decisions.isEmpty()) null else PermissionSiteKey(profileId, origin) to decisions
    }
}
