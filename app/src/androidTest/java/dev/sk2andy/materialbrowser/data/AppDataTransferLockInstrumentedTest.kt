package dev.sk2andy.materialbrowser.data

import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDataTransferLockInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val stateDirectory by lazy {
        File(context.applicationInfo.dataDir, AppDataArchiveRules.TRANSFER_STATE_DIRECTORY_NAME)
    }

    @Before
    @After
    fun clearTransferState() {
        stateDirectory.deleteRecursively()
    }

    @Test
    fun activeOwnerCannotBeReplacedOrReleasedByAnotherToken() {
        val token = AppDataTransferLock.activate(context, Process.myPid())

        assertNotNull(token)
        assertTrue(AppDataTransferLock.isActive(context))
        assertNull(AppDataTransferLock.activate(context, Process.myPid()))
        AppDataTransferLock.release(context, "different-token")
        assertTrue(AppDataTransferLock.isActive(context))
        AppDataTransferLock.release(context, requireNotNull(token))
        assertFalse(AppDataTransferLock.isActive(context))
    }

    @Test
    fun sameTransferProcessCanReclaimItsLockAfterRecreation() {
        val originalProcess = ProcessBuilder("sh", "-c", "echo \$\$; exec sleep 30").start()
        try {
            val mainPid = originalProcess.inputStream.bufferedReader().use { it.readLine().toInt() }
            val transferPid = Process.myPid()
            val token = requireNotNull(AppDataTransferLock.activate(context, mainPid))
            assertTrue(AppDataTransferLock.claim(context, token, mainPid, transferPid))

            // On recreation, the intent still carries the original main-process PID.
            assertTrue(AppDataTransferLock.claim(context, token, mainPid, transferPid))
            assertFalse(AppDataTransferLock.claim(context, "different-token", mainPid, transferPid))
            assertFalse(AppDataTransferLock.claim(context, token, mainPid, mainPid))
        } finally {
            originalProcess.destroy()
        }
    }
}
