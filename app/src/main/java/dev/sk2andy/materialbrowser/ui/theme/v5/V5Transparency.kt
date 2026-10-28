package dev.sk2andy.materialbrowser.ui.theme.v5

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Whether materials must be opaque (system-v5.md section 4): battery saver and high-contrast text.
 * Android has no «Reduce transparency» switch of its own; high-contrast text is the setting people
 * who need solid backgrounds turn on. Battery saver is followed live, contrast when the screen opens.
 */
@Composable
fun rememberV5OpaqueMaterials(): Boolean {
    val context = LocalContext.current
    var powerSave by remember { mutableStateOf(context.isPowerSaveMode()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                powerSave = context.isPowerSaveMode()
            }
        }
        // Previews have no system to register with: there the materials simply stay translucent.
        val registered = runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.isSuccess
        onDispose { if (registered) context.unregisterReceiver(receiver) }
    }
    val highContrast = remember(context) { context.isHighTextContrast() }
    return powerSave || highContrast
}

private fun Context.isPowerSaveMode(): Boolean =
    runCatching { getSystemService(PowerManager::class.java)?.isPowerSaveMode == true }.getOrDefault(false)

/** The secure setting behind «High contrast text»; readable by any app, absent on some builds. */
private fun Context.isHighTextContrast(): Boolean =
    runCatching { Settings.Secure.getInt(contentResolver, HIGH_TEXT_CONTRAST, 0) == 1 }.getOrDefault(false)

private const val HIGH_TEXT_CONTRAST = "high_text_contrast_enabled"
