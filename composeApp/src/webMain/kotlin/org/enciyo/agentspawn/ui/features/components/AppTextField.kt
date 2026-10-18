package org.enciyo.agentspawn.ui.features.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.ui.design.textFieldColors


@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String,
    isEnabled: Boolean = true,
    line: Int = 1,
    singleLine: Boolean = line == 1,
    contentPadding: PaddingValues = TextFieldDefaults.contentPaddingWithoutLabel(top = 8.dp, bottom = 8.dp),
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        minLines = line,
        maxLines = line,
        enabled = isEnabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer),
        textStyle = textStyle.copy(
            color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
    ) {
        TextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = it,
            enabled = true,
            interactionSource = remember { MutableInteractionSource() },
            colors = textFieldColors,
            singleLine = false,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            contentPadding = contentPadding,
        )
    }
}