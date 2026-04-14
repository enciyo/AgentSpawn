package org.enciyo.agentspawn.ui.features.chat

import agentspawn.composeapp.generated.resources.*
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.api.ContentPart
import org.jetbrains.compose.resources.stringResource


@Composable
internal fun ColumnScope.MessageScreenAnimatedVisibility(
    state: ChatUiState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isChatStarted || state.message.isLoading,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 }
        ) + fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = ExitTransition.None,
        modifier = modifier
            .weight(1f)
    ) {
        if (state.message.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MessageScreen(state = state.message)
        }
    }
}

@Composable
private fun MessageScreen(
    state: MessageState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        reverseLayout = true,
        contentPadding = PaddingValues(top = 32.dp, bottom = 64.dp)
    ) {
        items(state.messages) { message ->
            Column(
                modifier = Modifier.animateItem(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                message.parts.forEach { part ->
                    when (part) {
                        is ContentPart.UserQuery -> UserQueryPart(part.query)
                        is ContentPart.Thinking -> ThinkingPart(part.text)
                        is ContentPart.ToolCall -> ToolPart(title = part.name, text = part.args)
                        is ContentPart.ToolResult -> ToolPart(
                            title = part.toolName,
                            text = part.result,
                            isResult = true
                        )

                        is ContentPart.Text -> TextPart(part.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolPart(
    isResult: Boolean = false,
    title: String,
    text: String,
) {
    val prefix = if (isResult) "Result:" else "Call:"
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clickable { isExpanded = isExpanded.not() }
            .border(
                shape = MaterialTheme.shapes.small,
                backgroundColor = Color.Transparent
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "$prefix $title",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        HorizontalDivider()
        AnimatedVisibility(isExpanded) {
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TextPart(text: String) {
    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun UserQueryPart(
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .border()
            .padding(16.dp)
    ) {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ThinkingPart(text: String) {
    Column {
        Text(stringResource(Res.string.thinking))
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}


