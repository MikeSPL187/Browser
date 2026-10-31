package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal object PasswordsTestTags {
    const val Start = "passwords_start"
    const val PhraseDone = "passwords_phrase_done"
    const val ConfirmCheck = "passwords_confirm_check"
    const val Unlock = "passwords_unlock"
    const val Export = "passwords_export"
    const val OpenSite = "passwords_open_site"
    const val Recover = "passwords_recover"
    const val RecoverField = "passwords_recover_field"
    const val RecoverOpen = "passwords_recover_open"
    const val List = "passwords_list"
    const val SystemFillNote = "passwords_system_fill_note"
    const val PasskeyNote = "passwords_passkey_note"
    const val Generate = "passwords_generate"
    const val Health = "passwords_health"
    const val Import = "passwords_import"
    const val AddTotp = "passwords_totp_add"
    const val RemoveTotp = "passwords_totp_remove"
    const val TotpField = "passwords_totp_field"
    const val TotpSave = "passwords_totp_save"
    const val TotpValue = "passwords_totp_value"
    const val Search = "passwords_search"
    const val Add = "passwords_add"
    const val Lock = "passwords_lock"
    const val Reveal = "passwords_reveal"
    const val PasswordValue = "passwords_password_value"
    const val Delete = "passwords_delete"
    const val Erase = "passwords_erase"
    const val EraseConfirm = "passwords_erase_confirm"
    const val Save = "passwords_save"

    fun confirmWord(position: Int) = "passwords_confirm_word:$position"

    fun login(id: String) = "passwords_login:$id"
}
