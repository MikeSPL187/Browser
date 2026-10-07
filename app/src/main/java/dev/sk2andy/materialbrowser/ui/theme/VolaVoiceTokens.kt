package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Voice search in the address field (stage S7): the microphone button and, while listening, a
 * filled accent core with a halo that breathes with the voice.
 */
internal object VolaVoice {
    /** The button and its touch target. */
    val buttonSize = 48.dp
    val iconSize = 24.dp

    /** The filled accent circle behind the microphone while listening. */
    val coreSize = 32.dp
    val coreIconSize = 20.dp

    /**
     * How far the halo grows past the core at the loudest voice, as a share of the core; with the
     * idle breath it stays about within the 48 dp field.
     */
    const val HALO_MAX_GROWTH = 0.5f

    /** The halo also keeps a slow idle breath, so silence still reads as «listening». */
    const val HALO_IDLE_GROWTH = 0.1f
    const val HALO_IDLE_PERIOD_MILLIS = 1400
    const val HALO_ALPHA = 0.28f

    /** The core dims while the final phrase is being recognised. */
    const val RECOGNIZING_ALPHA = 0.6f
}
