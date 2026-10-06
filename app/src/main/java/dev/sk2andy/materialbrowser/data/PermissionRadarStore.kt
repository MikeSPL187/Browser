package dev.sk2andy.materialbrowser.data

import android.content.Context
import dev.sk2andy.materialbrowser.browser.permissions.PermissionDecisionPersistence
import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarRepository
import dev.sk2andy.materialbrowser.browser.permissions.PermissionSiteKey
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision

class PermissionRadarStore(context: Context) : PermissionDecisionPersistence {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun load(): Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>> {
        val raw = preferences.getString(KEY_DECISIONS, null) ?: return emptyMap()
        return PermissionRadarCodec.decode(raw, MAX_SITES) ?: run {
            // The next save replaces what could not be read; keep the original to recover from.
            preferences.edit().putString(KEY_UNREADABLE_BACKUP, raw).apply()
            emptyMap()
        }
    }

    @Synchronized
    override fun save(
        decisions: Map<PermissionSiteKey, Map<SitePermission, SitePermissionDecision>>,
    ) {
        preferences.edit()
            .putString(KEY_DECISIONS, PermissionRadarCodec.encode(decisions, MAX_SITES))
            .apply()
    }

    fun flush(): Boolean = preferences.edit().commit()

    internal companion object {
        const val PREFERENCES_NAME = "permission_radar_v1"
        const val KEY_DECISIONS = "decisions"
        const val KEY_UNREADABLE_BACKUP = "decisions_unreadable_backup"
        const val MAX_SITES = PermissionRadarRepository.MAX_SITES
    }
}
