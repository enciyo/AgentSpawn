package org.enciyo.agentspawn.ui.features.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.ui.design.DrawerTokens
import org.enciyo.agentspawn.ui.features.components.DisplayScreen
import org.koin.compose.viewmodel.koinViewModel


data class DrawerActions(
    val onAgent: (AgentDto) -> Unit,
    val onHome: () -> Unit,
    val onNewAgent: () -> Unit,
)

data class InputActions(
    val onInputChange: (String) -> Unit,
    val onSendMessage: () -> Unit,
    val onSelectModel: (String) -> Unit,
    val onSearchModel: (String) -> Unit,
)

data class HeaderActions(
    val onSelectRepo: (String) -> Unit,
    val onSearchRepo: (String) -> Unit,
    val onSelectBranch: (String) -> Unit,
    val onSearchBranch: (String) -> Unit,
    val onChangeTargetBranch: (String) -> Unit,
)

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = koinViewModel(),
    onSnackMessage: (String) -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ChatEvent.NavigateToHome -> onHome()
                is ChatEvent.ShowError -> onSnackMessage(event.message)
            }
        }
    }

    val drawerActions = remember(viewModel) {
        DrawerActions(
            onAgent = viewModel::onAgent,
            onHome = viewModel::onHome,
            onNewAgent = viewModel::onNewAgent,
        )
    }
    val inputActions = remember(viewModel) {
        InputActions(
            onInputChange = viewModel::onInputChange,
            onSendMessage = viewModel::onSendMessage,
            onSelectModel = viewModel::onSelectModel,
            onSearchModel = viewModel::onSearchModel,
        )
    }
    val headerActions = remember(viewModel) {
        HeaderActions(
            onSelectRepo = viewModel::onSelectRepo,
            onSearchRepo = viewModel::onSearchRepo,
            onSelectBranch = viewModel::onSelectBranch,
            onSearchBranch = viewModel::onSearchBranch,
            onChangeTargetBranch = viewModel::onChangeTargetBranch,
        )
    }

    ChatScreen(
        state = state,
        drawerActions = drawerActions,
        inputActions = inputActions,
        headerActions = headerActions,
    )
}

@Composable
internal fun Modifier.border(
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant
) = this
    .clip(shape = shape)
    .background(color = backgroundColor, shape = shape)
    .border(width = 1.dp, color = borderColor, shape = shape)

@Composable
fun ChatScreen(
    state: ChatUiState,
    drawerActions: DrawerActions,
    inputActions: InputActions,
    headerActions: HeaderActions,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(true) }

    val alignmentBias by animateFloatAsState(
        targetValue = if (state.isChatStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "layout_move"
    )

    DisplayScreen { isDesktop ->

        val animStartPadding by animateDpAsState(
            targetValue = if (isDesktop) if (isExpanded) DrawerTokens.Width else DrawerTokens.MinWidth else 0.dp
        )
        val animEndPadding by animateDpAsState(
            targetValue = if (isDesktop) if (isExpanded) 0.dp else DrawerTokens.MinWidth else 0.dp
        )

        Row(
            modifier = modifier
                .padding(start = animStartPadding, end = animEndPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp, horizontal = 32.dp),
                contentAlignment = BiasAlignment(0f, alignmentBias)
            ) {
                Column(
                    modifier = Modifier.widthIn(min = 200.dp, max = 720.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    MessageScreenAnimatedVisibility(state)
                    Column(modifier = Modifier.border()) {
                        AnimatedVisibility(visible = state.message.status.isActive) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        InputHeader(
                            state = state.input,
                            actions = headerActions,
                        )
                        InputScreen(
                            state = state.input,
                            actions = inputActions,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxHeight().border()
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .widthIn(
                        min = DrawerTokens.MinWidth,
                        max = DrawerTokens.Width
                    )
            ) {
                Icon(
                    contentDescription = "",
                    imageVector = Icons.Default.Menu,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { isExpanded = !isExpanded }
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInHorizontally() + fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = slideOutHorizontally() + fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                Surface(modifier = Modifier.fillMaxHeight()) {
                    DrawerContent(
                        state = state.drawer,
                        actions = drawerActions,
                    )
                }
            }
        }
    }
}
