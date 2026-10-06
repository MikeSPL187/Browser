package dev.sk2andy.materialbrowser.ui

import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/**
 * Text fields in [content] ask the keyboard not to learn from what is typed while [isPrivate] holds:
 * a private tab's addresses and searches must not land in the keyboard's personal dictionary on
 * disk (#123, screen 4). The flag is read when the keyboard starts, so the field stays the same.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PrivateKeyboard(isPrivate: Boolean, content: @Composable () -> Unit) {
    val privateNow by rememberUpdatedState(isPrivate)
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            if (!privateNow) nextHandler.startInputMethod(request)
            nextHandler.startInputMethod(
                PlatformTextInputMethodRequest { outAttributes ->
                    request.createInputConnection(outAttributes).also {
                        outAttributes.imeOptions =
                            outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    }
                },
            )
        },
        content = content,
    )
}
