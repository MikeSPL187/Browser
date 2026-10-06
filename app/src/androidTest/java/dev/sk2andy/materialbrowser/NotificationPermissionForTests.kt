package dev.sk2andy.materialbrowser

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Grants the notification permission before MainActivity starts. The first private tab asks for
 * it, and the system dialog would cover the activity the test drives. The CI run clears app
 * data, and with it runtime permissions, before every test.
 */
internal fun grantNotificationPermissionForTests() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    runCatching {
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }
}
