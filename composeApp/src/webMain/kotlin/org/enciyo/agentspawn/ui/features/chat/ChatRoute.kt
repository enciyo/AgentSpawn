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
import org.enciyo.agentspawn.ui.features.customize.CustomizeScreen
import androidx.lifecycle.compose.LifecycleStartEffect
import org.koin.compose.viewmodel.koinViewModel


data class DrawerActions(
    val onAgent: (AgentDto) -> Unit,
    val onHome: () -> Unit,
    val onCustomize: () -> Unit,
    val onNewAgent: () -> Unit,
    val onSelectProvider: (String) -> Unit,
    val onToggleGrouping: () -> Unit,
)

data class InputActions(
    val onInputChange: (String) -> Unit,
    val onSendMessage: () -> Unit,
)

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = koinViewModel(),
    onSnackMessage: (String) -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Agent list polling stops when the tab is hidden (lifecycle STOP)
    LifecycleStartEffect(viewModel) {
        viewModel.onVisibilityChanged(true)
        onStopOrDispose { viewModel.onVisibilityChanged(false) }
    }

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
            onCustomize = viewModel::onCustomize,
            onNewAgent = viewModel::onNewAgent,
            onSelectProvider = viewModel::onSelectProvider,
            onToggleGrouping = viewModel::onToggleGrouping,
        )
    }
    val inputActions = remember(viewModel) {
        InputActions(
            onInputChange = viewModel::onInputChange,
            onSendMessage = viewModel::onSendMessage,
        )
    }
    val statusLineActions = remember(viewModel) {
        StatusLineActions(
            onSelectRepo = viewModel::onSelectRepo,
            onSearchRepo = viewModel::onSearchRepo,
            onExpandRepo = viewModel::onExpandRepo,
            onSelectBranch = viewModel::onSelectBranch,
            onSearchBranch = viewModel::onSearchBranch,
            onExpandBranch = viewModel::onExpandBranch,
            onChangeTargetBranch = viewModel::onChangeTargetBranch,
            onSelectProvider = viewModel::onSelectProviderByDisplayName,
            onSelectModel = viewModel::onSelectModel,
            onSearchModel = viewModel::onSearchModel,
            onSendMessage = viewModel::onSendMessage,
        )
    }

    ChatScreen(
        state = state,
        drawerActions = drawerActions,
        inputActions = inputActions,
        statusLineActions = statusLineActions,
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
    statusLineActions: StatusLineActions,
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
                contentAlignment = when (state.mainPane) {
                    MainPane.Chat -> BiasAlignment(0f, alignmentBias)
                    MainPane.Customize -> Alignment.TopCenter
                },
            ) {
                when (state.mainPane) {
                    MainPane.Customize -> CustomizeScreen()
                    MainPane.Chat -> {
                        Column(
                            modifier = Modifier.widthIn(min = 200.dp, max = 720.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MessageScreenAnimatedVisibility(state)
                            Column(modifier = Modifier.border()) {
                                AnimatedVisibility(visible = state.message.status.isActive) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                InputScreen(
                                    state = state.input,
                                    actions = inputActions,
                                )
                                StatusLine(
                                    state = state.input,
                                    actions = statusLineActions,
                                    selectedProvider = state.drawer.selectedProvider.displayName,
                                    providers = state.drawer.selectableProviderNames,
                                    isChatStarted = state.isChatStarted,
                                    isLoading = state.message.isLoading ||
                                        state.message.status.isActive ||
                                        state.input.isPending ||
                                        !state.input.isEnabled,
                                )
                            }
                        }
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
                        isCustomizeSelected = state.mainPane == MainPane.Customize,
                    )
                }
            }
        }
    }
}
