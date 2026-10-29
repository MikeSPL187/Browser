package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Why a hand-made or edited login was not saved. */
internal enum class PasswordEditError { SiteInvalid, PasswordRequired, Duplicate, Failed }
