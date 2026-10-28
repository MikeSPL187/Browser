package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5SnapshotBackdropRulesTest {
    @Test
    fun `the glass shows the page only from Android 13, with a snapshot, when transparency is allowed`() {
        assertTrue(V5SnapshotBackdropRules.showsSnapshot(hasSnapshot = true, blurSupported = true, opaque = false))
        assertFalse(V5SnapshotBackdropRules.showsSnapshot(hasSnapshot = false, blurSupported = true, opaque = false))
        assertFalse(V5SnapshotBackdropRules.showsSnapshot(hasSnapshot = true, blurSupported = false, opaque = false))
        assertFalse(V5SnapshotBackdropRules.showsSnapshot(hasSnapshot = true, blurSupported = true, opaque = true))
    }

    @Test
    fun `the snapshot shrinks to a few dozen pixels and keeps its shape`() {
        assertEquals(32 to 64, V5SnapshotBackdropRules.blurredSize(540, 1080))
        assertEquals(32 to 18, V5SnapshotBackdropRules.blurredSize(1920, 1080))
    }

    @Test
    fun `a tiny snapshot is not stretched, and a sliver keeps one row`() {
        assertEquals(20 to 40, V5SnapshotBackdropRules.blurredSize(20, 40))
        assertEquals(32 to 1, V5SnapshotBackdropRules.blurredSize(4000, 10))
    }
}
