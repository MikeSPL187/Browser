package dev.sk2andy.materialbrowser.data

import android.content.SharedPreferences
import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozedTabStoreTest {
    @Test
    fun `a failed save puts the previous list back in memory`() {
        val preferences = FakePreferences()
        assertTrue(SnoozedTabStore.save(preferences, listOf(snoozed("kept"))))
        val saved = preferences.getString(SnoozedTabStore.KEY_TABS, null)

        preferences.failNextCommit = true
        val committed = SnoozedTabStore.save(preferences, listOf(snoozed("lost")))

        assertFalse(committed)
        assertEquals(saved, preferences.getString(SnoozedTabStore.KEY_TABS, null))
    }

    @Test
    fun `a failed first save leaves no list behind`() {
        val preferences = FakePreferences().apply { failNextCommit = true }

        assertFalse(SnoozedTabStore.save(preferences, listOf(snoozed("lost"))))
        assertFalse(preferences.contains(SnoozedTabStore.KEY_TABS))
    }

    private fun snoozed(id: String) = SnoozedTab(
        tab = BrowserTab(id, 1L, url = "https://example.com/$id"),
        wakeAtMillis = 1_000L,
        createdAtMillis = 1L,
    )

    /** Like the platform: an editor writes memory first, and a failed commit keeps the change there. */
    private class FakePreferences : SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        var failNextCommit = false

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? =
            values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String, defValue: Int) = defValue
        override fun getLong(key: String, defValue: Long) = defValue
        override fun getFloat(key: String, defValue: Float) = defValue
        override fun getBoolean(key: String, defValue: Boolean) = defValue
        override fun contains(key: String) = key in values
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener,
        ) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?) = apply { changes[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = this
            override fun putLong(key: String, value: Long) = this
            override fun putFloat(key: String, value: Float) = this
            override fun putBoolean(key: String, value: Boolean) = this
            override fun remove(key: String) = apply { changes[key] = null }
            override fun clear() = this
            override fun apply() {
                commit()
            }

            override fun commit(): Boolean {
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                if (!failNextCommit) return true
                failNextCommit = false
                return false
            }
        }
    }
}
