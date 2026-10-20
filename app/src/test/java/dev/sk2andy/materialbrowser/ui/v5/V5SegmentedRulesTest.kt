package dev.sk2andy.materialbrowser.ui.v5

import org.junit.Assert.assertEquals
import org.junit.Test

class V5SegmentedRulesTest {
    @Test
    fun labelsThatFitTheirShareGetEqualSegments() {
        assertEquals(listOf(1f, 1f, 1f), V5SegmentedRules.weights(listOf(200, 150, 260), available = 900))
    }

    @Test
    fun aLabelTooWideForItsShareGivesEverySegmentItsLabelsWidth() {
        // «Downloads» at 200 % font: 330 px against a 320 px share.
        assertEquals(listOf(310f, 260f, 330f), V5SegmentedRules.weights(listOf(310, 260, 330), available = 960))
    }

    @Test
    fun noLabelsNoSegments() {
        assertEquals(emptyList<Float>(), V5SegmentedRules.weights(emptyList(), available = 900))
    }
}
