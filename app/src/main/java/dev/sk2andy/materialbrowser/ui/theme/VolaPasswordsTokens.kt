package dev.sk2andy.materialbrowser.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** The Passwords screens (boards W-Passwords, W-PasswordDetail, W-Locked). List rows reuse [VolaLibrary]. */
internal object VolaPasswords {
    val sidePadding = VolaSpacing.x4
    val sectionGap = VolaSpacing.x3

    /** The key or lock above a setup or locked screen's title. */
    val heroSize = 72.dp
    val heroShape = RoundedCornerShape(24.dp)
    val heroIconSize = 36.dp
    val heroGap = VolaSpacing.x4

    /** Text blocks on setup screens are kept readable on wide screens. */
    val textMaxWidth = 420.dp
    val pointGap = VolaSpacing.x3
    val pointIconSize = 22.dp

    /** The 12 words, numbered, two to a row. */
    val phraseCardShape = RoundedCornerShape(24.dp)
    val phraseCardPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    val phraseWordGap = 10.dp
    val phraseNumberWidth = 24.dp
    const val PHRASE_COLUMNS = 2

    /** A login's header on its page: the site's tile and name. */
    val detailHeaderGap = 14.dp

    /** A field row inside a detail card: small label over the value, actions on the right. */
    val fieldPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 4.dp, bottom = 10.dp)
    val fieldLabelGap = 2.dp

    val buttonHeight = 52.dp
    val buttonIconGap = VolaSpacing.x2
    val formGap = VolaSpacing.x3

    /** Sheets over a page: «Save password?» and «Sign in to …» (board W-Autofill). */
    val sheetPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    val sheetGap = VolaSpacing.x3
    val sheetHeaderPadding = PaddingValues(horizontal = 4.dp)
    val sheetHeaderGap = VolaSpacing.x3
    val sheetIconSize = 40.dp
    val sheetIconShape = RoundedCornerShape(14.dp)
    val sheetIconGlyph = 22.dp
    val sheetVerifiedIcon = 15.dp
    val sheetVerifiedGap = VolaSpacing.x1
    val sheetCardShape = RoundedCornerShape(24.dp)
    val sheetActionSize = 40.dp
    val sheetActionIcon = 22.dp
    val sheetButtonHeight = 48.dp
    val sheetButtonGap = VolaSpacing.x2

    /** The generator (board W-Generator): the value on a card, then length and switches. */
    val generatorValuePadding = PaddingValues(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
    val generatorOptionsPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val generatorSwitchHeight = 48.dp

    /** The password check (board W-PasswordHealth). */
    val healthSummaryGap = VolaSpacing.x2
}
