@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.util.Locale

internal fun formatCount(count: Int, locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(count)
