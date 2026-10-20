package dev.sk2andy.materialbrowser.browser.gecko

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.sk2andy.materialbrowser.browser.engine.EngineActivityIntegration

/** Keeps Gecko's activity-result and process-runtime wiring outside the engine-neutral activity. */
internal class GeckoActivityIntegration(
    context: Context,
    launch: (PendingIntent) -> Unit,
    onPendingChanged: (Boolean) -> Unit,
    isPendingRequestCurrent: () -> Boolean,
) : EngineActivityIntegration {
    private val delegate = GeckoWebAuthnActivityDelegate(
        launch = launch,
        onPendingChanged = onPendingChanged,
        isPendingRequestCurrent = isPendingRequestCurrent,
    )

    init {
        GeckoRuntimeOwner.bindWebAuthnActivityDelegate(
            context = context.applicationContext,
            delegate = delegate,
        )
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        delegate.onActivityResult(resultCode, data)
    }

    override fun onHostPaused() {
        delegate.onHostPaused()
    }

    override fun onHostResumed() {
        delegate.onHostResumed()
    }

    override fun close() {
        GeckoRuntimeOwner.unbindWebAuthnActivityDelegate(delegate)
        delegate.close()
    }
}
