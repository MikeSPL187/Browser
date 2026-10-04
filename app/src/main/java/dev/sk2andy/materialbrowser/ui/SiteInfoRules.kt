package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.permissions.PermissionRadarEntry
import dev.sk2andy.materialbrowser.browser.permissions.SitePermission
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionActivity
import dev.sk2andy.materialbrowser.browser.permissions.SitePermissionDecision

/** The pages of the site information sheet: the overview, then the details it links to. */
internal enum class SiteInfoPage { Overview, PrivacyXRay, Permissions, Certificate }

/** What the overview of the site information sheet (board W-SiteInfo) shows. */
internal object SiteInfoRules {
    /** Always listed, as on the board; the rarer ones only once the site has used them. */
    private val CORE_PERMISSIONS = listOf(
        SitePermission.Location,
        SitePermission.Camera,
        SitePermission.Microphone,
        SitePermission.Notifications,
    )

    /**
     * Permission rows in a stable order. Notifications are left out where they cannot work: in the
     * System WebView build and in private tabs.
     */
    fun visiblePermissions(
        entries: List<PermissionRadarEntry>,
        notificationsSupported: Boolean,
        isPrivate: Boolean,
    ): List<PermissionRadarEntry> = entries
        .filter { entry ->
            when {
                entry.permission == SitePermission.Notifications ->
                    notificationsSupported && !isPrivate
                entry.permission in CORE_PERMISSIONS -> true
                else -> isCustomized(entry)
            }
        }
        .sortedBy { entry ->
            CORE_PERMISSIONS.indexOf(entry.permission).takeIf { it >= 0 } ?: CORE_PERMISSIONS.size
        }

    /** The site has a decision of its own or is using the permission right now. */
    fun isCustomized(entry: PermissionRadarEntry): Boolean =
        entry.decision != SitePermissionDecision.Ask ||
            entry.allowedForSession ||
            entry.activity != SitePermissionActivity.Idle

    /** The letter on the site's gem in the sheet header. */
    fun initial(host: String): String =
        host.removePrefix("www.").firstOrNull { it.isLetterOrDigit() }?.uppercase().orEmpty()
}
