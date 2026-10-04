package dev.sk2andy.materialbrowser.browser.permissions

/** What the permission request sheet asks, by what the site wants (board W-Permission). */
enum class PermissionQuestion { Location, Camera, Microphone, CameraAndMicrophone, Notifications, Other }

object PermissionPromptRules {
    fun question(permissions: Set<SitePermission>): PermissionQuestion = when (permissions) {
        setOf(SitePermission.Location) -> PermissionQuestion.Location
        setOf(SitePermission.Camera) -> PermissionQuestion.Camera
        setOf(SitePermission.Microphone) -> PermissionQuestion.Microphone
        setOf(SitePermission.Camera, SitePermission.Microphone) -> PermissionQuestion.CameraAndMicrophone
        setOf(SitePermission.Notifications) -> PermissionQuestion.Notifications
        else -> PermissionQuestion.Other
    }

    /**
     * The choices, strongest first. Notifications are only ever granted for good: a one-time
     * grant would vanish before the first notification arrives.
     */
    fun choices(permissions: Set<SitePermission>): List<PermissionPromptChoice> =
        if (SitePermission.Notifications in permissions) {
            listOf(PermissionPromptChoice.AllowAlways, PermissionPromptChoice.Block)
        } else {
            listOf(
                PermissionPromptChoice.AllowAlways,
                PermissionPromptChoice.AllowOnce,
                PermissionPromptChoice.Block,
            )
        }

    /** The permission whose icon heads the sheet: the most sensitive one asked for. */
    fun heroPermission(permissions: Set<SitePermission>): SitePermission =
        HERO_ORDER.firstOrNull(permissions::contains) ?: SitePermission.Location

    private val HERO_ORDER = listOf(
        SitePermission.Camera,
        SitePermission.Microphone,
        SitePermission.Location,
        SitePermission.Notifications,
        SitePermission.MidiSysex,
        SitePermission.ProtectedMedia,
    )
}
