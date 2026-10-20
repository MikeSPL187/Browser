package dev.sk2andy.materialbrowser.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The history's own file (N-02): moved from the preferences once, never emptied by a failed read. */
@RunWith(AndroidJUnit4::class)
class HistoryFileInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences("history_file_test", Context.MODE_PRIVATE)
    private val path = File(context.cacheDir, "history_file_test.json")

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
        path.delete()
    }

    @After
    fun tearDown() = setUp()

    @Test
    fun theHistoryMovesOutOfThePreferencesOnce() {
        preferences.edit().putString(KEY, "[1]").commit()
        val file = HistoryFile.forTesting(path)

        assertEquals("[1]", file.read(preferences, KEY))
        assertFalse(preferences.contains(KEY))
        assertTrue(path.exists())
        assertEquals("[1]", file.read(preferences, KEY))
    }

    @Test
    fun nothingSavedReadsAsNothing() {
        assertNull(HistoryFile.forTesting(path).read(preferences, KEY))
    }

    @Test
    fun aWriteChangesTheStamp() {
        val file = HistoryFile.forTesting(path)
        val before = file.stamp()

        assertTrue(file.write("[2]", clearing = false))
        assertNotEquals(before, file.stamp())
        assertEquals("[2]", file.read(preferences, KEY))
    }

    @Test
    fun aFileThatCannotBeReadRefusesEverythingButAClear() {
        assertTrue(path.mkdirs()) // a directory where the file should be: reading it fails
        val file = HistoryFile.forTesting(path)

        assertNull(file.read(preferences, KEY))
        assertFalse(file.write("[3]", clearing = false))
        assertTrue(path.delete())
        assertTrue(file.write("[]", clearing = true))
        assertTrue(file.write("[4]", clearing = false))
    }

    private companion object {
        const val KEY = "history"
    }
}
