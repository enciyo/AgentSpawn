package org.enciyo.agentspawn.ui.features.chat

import agentspawn.composeapp.generated.resources.Res
import agentspawn.composeapp.generated.resources.input_place_holder
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import org.enciyo.agentspawn.ui.features.components.AppTextField
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InputScreen(
    state: InputState,
    actions: InputActions,
    modifier: Modifier = Modifier,
) {
    AppTextField(
        value = state.text,
        isEnabled = state.isEnabled,
        onValueChange = actions.onInputChange,
        placeholder = stringResource(Res.string.input_place_holder),
        line = 3,
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.isMetaPressed && event.key == Key.Enter && state.isVisibleSendButton) {
                    actions.onSendMessage()
                    true
                } else false
            },
    )
}
