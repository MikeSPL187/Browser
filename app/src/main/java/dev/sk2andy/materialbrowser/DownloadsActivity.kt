package dev.sk2andy.materialbrowser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.sk2andy.materialbrowser.browser.DownloadsController
import dev.sk2andy.materialbrowser.data.AppDataTransferLock
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.DownloadEntry
import dev.sk2andy.materialbrowser.data.DownloadRepository
import dev.sk2andy.materialbrowser.ui.DownloadsLibraryV5
import dev.sk2andy.materialbrowser.ui.openDownloadPage
import dev.sk2andy.materialbrowser.ui.theme.CandyTheme
import dev.sk2andy.materialbrowser.ui.theme.setCandyContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsActivity : ComponentActivity() {
    private val repository by lazy { DownloadRepository(this) }
    private val controller by lazy {
        DownloadsController(
            store = object : DownloadsController.DownloadStore {
                override fun snapshot() = repository.snapshot()
                override fun clear(entries: Collection<DownloadEntry>) = repository.clear(entries)
                override fun cancel(entry: DownloadEntry) = repository.cancel(entry)
                override fun togglePause(entry: DownloadEntry) = repository.togglePause(entry)
                override fun retry(entry: DownloadEntry) = repository.retry(entry)
            },
            worker = object : DownloadsController.Worker {
                override fun <T> run(work: () -> T, onResult: (T) -> Unit) {
                    lifecycleScope.launch { onResult(withContext(Dispatchers.IO) { work() }) }
                }
            },
        )
    }
    private var isFullImmersiveModeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppDataTransferLock.isActive(this)) {
            finish()
            return
        }
        enableEdgeToEdge()
        val store = BrowserSessionStore(this)
        isFullImmersiveModeEnabled = store.loadFullImmersiveModeEnabled()
        applyFullImmersiveMode(isFullImmersiveModeEnabled)
        val appearanceSettings = store.loadAppearanceSettings()
        val workspaceAccent = store.loadActiveWorkspaceAccent()

        setCandyContent(animationsEnabled = appearanceSettings.animationsEnabled) {
            val appearanceDark = appearanceSettings.usesDarkColors(isSystemInDarkTheme())
            SideEffect { applyAppearanceSystemBars(appearanceDark) }
            CandyTheme(
                settings = appearanceSettings,
                workspaceAccent = workspaceAccent,
            ) {
                DownloadsLibraryV5(
                    downloads = controller.downloads,
                    isClearing = controller.isClearing,
                    onOpen = ::openDownload,
                    onTogglePause = { entry -> controller.togglePause(entry, ::reportControl) },
                    onCancel = { entry -> controller.cancel(entry, ::reportControl) },
                    onRetry = { entry -> controller.retry(entry, ::reportControl) },
                    onOpenPage = { url -> openDownloadPage(this, url) },
                    onClearFinished = ::clearFinished,
                    onClose = ::finish,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    controller.refresh()
                    delay(controller.pollDelayMillis)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyFullImmersiveMode(isFullImmersiveModeEnabled)
    }

    private fun openDownload(entry: DownloadEntry) {
        val uri = runCatching { repository.contentUri(entry) }.getOrNull() ?: run {
            toast(R.string.downloads_open_failed)
            return
        }
        val mime = entry.mime.takeIf(String::isNotBlank)
            ?: contentResolver.getType(uri)
            ?: "*/*"
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }.onFailure { toast(R.string.downloads_open_failed) }
    }

    private fun clearFinished(entries: List<DownloadEntry>) {
        controller.clearFinished(entries) { cleared ->
            if (!cleared) toast(R.string.downloads_clear_failed)
        }
    }

    private fun reportControl(changed: Boolean) {
        if (!changed) toast(R.string.downloads_control_failed)
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun launchIntent(context: Context): Intent = Intent(context, DownloadsActivity::class.java)
    }
}
