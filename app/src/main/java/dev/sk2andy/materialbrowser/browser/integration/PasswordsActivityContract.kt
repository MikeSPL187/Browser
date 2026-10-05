package dev.sk2andy.materialbrowser.browser.integration

import android.content.Context
import android.content.Intent
import dev.sk2andy.materialbrowser.PasswordsActivity

internal object PasswordsActivityContract {
    fun launchIntent(context: Context): Intent = Intent(context, PasswordsActivity::class.java)
}
