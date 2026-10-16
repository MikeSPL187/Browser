package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Under ui.v5 on a phone the site information sheet (V28, B8) draws its shared pieces (cards, rows,
 * switches, dividers, the header and page bar) the v5 way; what each row does and announces stays
 * its own, so every page built from them changes at once.
 */
internal val LocalSiteInfoV5 = staticCompositionLocalOf { false }
