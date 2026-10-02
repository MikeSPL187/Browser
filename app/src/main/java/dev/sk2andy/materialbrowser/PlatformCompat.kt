package dev.sk2andy.materialbrowser

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.view.WindowManager

/*
 * Android 12 (API 31–32) fallbacks for platform APIs that only exist from Android 13.
 * Callers use these instead of branching on the SDK level themselves.
 */

internal fun Context.hasPostNotificationsPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

internal fun PackageManager.queryIntentActivitiesCompat(
    intent: Intent,
    flags: Int = 0,
): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, flags)
    }

internal fun PackageManager.queryIntentServicesCompat(
    intent: Intent,
    flags: Int = 0,
): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentServices(intent, flags)
    }

internal fun PackageManager.getApplicationInfoCompat(
    packageName: String,
    flags: Int = 0,
): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, flags)
    }

internal fun PackageManager.getActivityInfoCompat(
    component: ComponentName,
    flags: Int = 0,
): ActivityInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getActivityInfo(component, PackageManager.ComponentInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getActivityInfo(component, flags)
    }

/**
 * Hides the window preview in Recents while a protected profile or a private tab is visible:
 * Android keeps that preview on disk.
 *
 * Android 12 has no Recents-only switch, so it falls back to FLAG_SECURE, which also blocks
 * screenshots of the protected profile and of private tabs. Nothing else in the app sets FLAG_SECURE.
 */
internal fun Activity.setRecentsPreviewEnabled(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        setRecentsScreenshotEnabled(enabled)
        return
    }
    val secure = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    if (enabled && secure) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else if (!enabled && !secure) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
