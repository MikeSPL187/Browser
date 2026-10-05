package dev.sk2andy.materialbrowser.browser.integration

import android.content.Context
import android.content.Intent
import dev.sk2andy.materialbrowser.PasswordsActivity

internal object PasswordsActivityContract {
    /** Opens «Move to Vola» (Q22a): after the vault is set up or opened, the import sources. */
    const val EXTRA_IMPORT = "dev.sk2andy.materialbrowser.passwords.IMPORT"

    fun launchIntent(context: Context): Intent = Intent(context, PasswordsActivity::class.java)

    fun importIntent(context: Context): Intent = launchIntent(context).putExtra(EXTRA_IMPORT, true)
}
