package dev.sk2andy.materialbrowser.browser.gecko.webpush

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.AppDataTransferContract
import dev.sk2andy.materialbrowser.browser.gecko.GeckoRuntimeOwner
import dev.sk2andy.materialbrowser.data.AppDataArchiveRules
import dev.sk2andy.materialbrowser.data.AppDataTransferLock
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeckoWebPushCoordinatorInstrumentedTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val contextWithoutPreferences = object : ContextWrapper(appContext) {
        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
            error("Reconnect opened preferences while app data was unavailable")
    }

    @Test
    fun activeTransferDefersReconnectBeforeOpeningStoresOrGecko() {
        val token = requireNotNull(AppDataTransferLock.activate(appContext, Process.myPid()))
        try {
            assertReconnectDeferred()
        } finally {
            AppDataTransferLock.release(appContext, token)
        }
    }

    @Test
    fun interruptedRestoreDefersReconnectBeforeOpeningStoresOrGecko() {
        val stateDirectory = File(
            appContext.applicationInfo.dataDir,
            AppDataArchiveRules.TRANSFER_STATE_DIRECTORY_NAME,
        )
        stateDirectory.mkdirs()
        val marker = File(stateDirectory, AppDataTransferContract.RESTORE_MARKER_FILE_NAME)
        check(marker.createNewFile())
        try {
            assertReconnectDeferred()
        } finally {
            marker.delete()
        }
    }

    private fun assertReconnectDeferred() = runBlocking {
        assertFalse(GeckoRuntimeOwner.hasRuntimeForTesting())
        assertFalse(GeckoWebPushCoordinator.reconnectOnce(contextWithoutPreferences))
        assertFalse(GeckoRuntimeOwner.hasRuntimeForTesting())
    }
}
