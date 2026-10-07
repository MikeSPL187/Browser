package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.BrowserController
import dev.sk2andy.materialbrowser.browser.SiteDataDeletion

/** Every «Undo» snackbar of the browser screen: closed tab, deleted site data, removed Essential. */
@Composable
internal fun BrowserUndoSnackbarEffects(
    controller: BrowserController,
    hostState: SnackbarHostState,
) {
    ClosedTabUndoSnackbarEffect(controller, hostState)
    SiteDataDeletionSnackbarEffect(controller.siteDataDeletion, hostState)
    EssentialRemovalSnackbarEffect(controller.essentials, hostState)
}

@Composable
internal fun ClosedTabUndoSnackbarEffect(
    controller: BrowserController,
    hostState: SnackbarHostState,
) {
    UndoSnackbarEffect(
        offer = controller.closedTabUndoOffer,
        hostState = hostState,
        message = stringResource(R.string.tab_closed),
        onUndo = { token -> controller.undoClosedTab(token) },
        onGone = { token -> controller.dismissClosedTabUndo(token) },
    )
}

/**
 * «Data of example.com deleted · Undo» while the deletion waits out its window, then
 * «Couldn't delete data for example.com · Retry» if the engine fails to delete it.
 */
@Composable
private fun SiteDataDeletionSnackbarEffect(
    deletion: SiteDataDeletion,
    hostState: SnackbarHostState,
) {
    val pending = deletion.pending
    UndoSnackbarEffect(
        offer = pending,
        hostState = hostState,
        message = pending?.let { stringResource(R.string.site_data_deleted, it.baseDomain) }.orEmpty(),
        onUndo = deletion::undo,
    )
    val failed = deletion.failed
    UndoSnackbarEffect(
        offer = failed,
        hostState = hostState,
        message = failed?.let { stringResource(R.string.site_data_delete_failed, it.baseDomain) }.orEmpty(),
        onUndo = deletion::retry,
        onGone = deletion::dismissFailure,
        actionLabel = stringResource(R.string.action_retry),
        duration = SnackbarDuration.Long,
    )
}

/**
 * An «Undo» snackbar for as long as [offer] stands: the owner of the offer keeps its time, and the
 * snackbar goes when the offer does. [actionLabel] and [duration] serve offers like «Retry».
 */
@Composable
private fun <T : Any> UndoSnackbarEffect(
    offer: T?,
    hostState: SnackbarHostState,
    message: String,
    onUndo: (T) -> Unit,
    onGone: (T) -> Unit = {},
    actionLabel: String = stringResource(R.string.action_undo),
    duration: SnackbarDuration = SnackbarDuration.Indefinite,
) {
    LaunchedEffect(offer) {
        val token = offer ?: return@LaunchedEffect
        hostState.currentSnackbarData?.dismiss()
        try {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo(token)
        } finally {
            onGone(token)
        }
    }
}
