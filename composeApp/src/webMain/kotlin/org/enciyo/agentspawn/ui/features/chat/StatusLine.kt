package org.enciyo.agentspawn.ui.features.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.ui.design.AgentSpawnWrappedTheme
import org.enciyo.agentspawn.ui.features.components.AppTextField

data class StatusLineActions(
    val onSelectRepo: (String) -> Unit,
    val onSearchRepo: (String) -> Unit,
    val onExpandRepo: () -> Unit,
    val onSelectBranch: (String) -> Unit,
    val onSearchBranch: (String) -> Unit,
    val onExpandBranch: () -> Unit,
    val onChangeTargetBranch: (String) -> Unit,
    val onSelectProvider: (String) -> Unit,
    val onSelectModel: (String) -> Unit,
    val onSearchModel: (String) -> Unit,
    val onSendMessage: () -> Unit,
)

@Composable
internal fun StatusLine(
    state: InputState,
    actions: StatusLineActions,
    selectedProvider: String,
    providers: List<String>,
    isChatStarted: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val canEditRepoBranch = !isChatStarted && !isLoading
    val canEditProvider = !isChatStarted && !isLoading
    val canInteract = !isLoading
    val hasProvider = selectedProvider.isNotBlank()
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        ) {
            AnimatedVisibility(state.isVisibleRepositorySelection) {
                DropdownSelector(
                    items = state.repositories,
                    selectedItem = state.selectedRepository,
                    isEnable = canEditRepoBranch,
                    onItemSelected = actions.onSelectRepo,
                    searchQuery = state.searchRepo,
                    onSearch = actions.onSearchRepo,
                    onExpand = actions.onExpandRepo,
                    isLoading = state.isRepoLoading,
                )
            }
            AnimatedVisibility(state.isVisibleBranchSelection) {
                DropdownSelector(
                    items = state.branches,
                    selectedItem = state.selectedBranch,
                    isEnable = canEditRepoBranch,
                    onItemSelected = actions.onSelectBranch,
                    searchQuery = state.searchBranch,
                    onSearch = actions.onSearchBranch,
                    onExpand = actions.onExpandBranch,
                    isLoading = state.isBranchLoading,
                )
            }
            AnimatedVisibility(state.isVisibleTargetBranch(isChatStarted)) {
                TargetBranch(
                    value = state.targetBranch,
                    onValueChange = actions.onChangeTargetBranch,
                    isEnabled = canEditRepoBranch,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProviderSelector(
                selectedProvider = selectedProvider,
                providers = providers,
                onSelectProvider = actions.onSelectProvider,
                isEnable = canEditProvider,
            )
            ModelSelector(
                selectedModel = state.selectedModel,
                models = state.filteredModels,
                onSelectModel = actions.onSelectModel,
                onSearchModel = actions.onSearchModel,
                searchQuery = state.searchModel,
                isEnable = canInteract && hasProvider,
                visible = hasProvider && (state.filteredModels.isNotEmpty() || state.searchModel.isNotBlank()),
            )
            AnimatedVisibility(state.isVisibleSendButton && canInteract) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = actions.onSendMessage),
                    contentDescription = "Command + Enter",
                )
            }
        }
    }
}

@Composable
private fun ProviderSelector(
    selectedProvider: String,
    providers: List<String>,
    onSelectProvider: (String) -> Unit,
    isEnable: Boolean,
) {
    val items = remember(providers, selectedProvider) {
        if (selectedProvider.isNotBlank() && selectedProvider !in providers) {
            listOf(selectedProvider) + providers
        } else {
            providers
        }
    }
    AnimatedVisibility(
        visible = items.isNotEmpty() || selectedProvider.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        DropdownSelector(
            items = items,
            selectedItem = selectedProvider.ifBlank { "Provider" },
            isEnable = isEnable && items.isNotEmpty(),
            onItemSelected = onSelectProvider,
            onSearch = {},
            searchQuery = "",
            isLoading = false,
        )
    }
}

@Composable
private fun ModelSelector(
    selectedModel: String,
    searchQuery: String,
    models: List<String>,
    onSelectModel: (String) -> Unit,
    onSearchModel: (String) -> Unit,
    isEnable: Boolean,
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        DropdownSelector(
            items = models,
            selectedItem = selectedModel,
            isEnable = isEnable,
            onItemSelected = onSelectModel,
            onSearch = onSearchModel,
            searchQuery = searchQuery,
            isLoading = false,
        )
    }
}

@Composable
private fun TargetBranch(
    value: String,
    onValueChange: (String) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var fieldWidthDp by remember { mutableStateOf(120.dp) }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Select Target Branch",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp),
        )

        AppTextField(
            value = value,
            onValueChange = onValueChange,
            isEnabled = isEnabled,
            placeholder = "Enter target branch",
            textStyle = MaterialTheme.typography.bodySmall,
            contentPadding = PaddingValues(vertical = 0.dp),
            line = 1,
            modifier = Modifier
                .widthIn(min = fieldWidthDp, max = 200.dp)
                .onSizeChanged { size ->
                    val measured = with(density) { size.width.toDp() }
                    if (measured > fieldWidthDp) {
                        fieldWidthDp = minOf(measured, 200.dp)
                    }
                }
                .onPreviewKeyEvent {
                    if (isEnabled && it.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        focusRequester.freeFocus()
                    } else false
                }
                .then(if (isEnabled) Modifier.focusRequester(focusRequester) else Modifier),
        )
    }
}

private val PreviewStatusLineActions = StatusLineActions(
    onSelectRepo = {},
    onSearchRepo = {},
    onExpandRepo = {},
    onSelectBranch = {},
    onSearchBranch = {},
    onExpandBranch = {},
    onChangeTargetBranch = {},
    onSelectProvider = {},
    onSelectModel = {},
    onSearchModel = {},
    onSendMessage = {},
)

private val PreviewInputState = InputState(
    text = "Ask something",
    repositories = listOf("Mavra-Android/NasaAssignment", "enciyo/AgentSpawn"),
    selectedRepository = "Mavra-Android/NasaAssignment",
    isRepoLoading = false,
    branches = listOf("develop", "main"),
    selectedBranch = "develop",
    isBranchLoading = false,
    targetBranch = "feature/status-line",
    models = listOf("gpt-5.3-codex-low", "gpt-5.3-codex-high"),
    selectedModel = "gpt-5.3-codex-low",
)

@Preview(showBackground = true, widthDp = 720, name = "StatusLine — chat before")
@Composable
private fun StatusLinePreviewBeforeChat() {
    AgentSpawnWrappedTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 8.dp),
        ) {
            StatusLine(
                state = PreviewInputState,
                actions = PreviewStatusLineActions,
                selectedProvider = "Cursor",
                providers = listOf("Cursor", "ChatGPT"),
                isChatStarted = false,
                isLoading = false,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 720, name = "StatusLine — chat started")
@Composable
private fun StatusLinePreviewChatStarted() {
    AgentSpawnWrappedTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 8.dp),
        ) {
            StatusLine(
                state = PreviewInputState,
                actions = PreviewStatusLineActions,
                selectedProvider = "Cursor",
                providers = listOf("Cursor"),
                isChatStarted = true,
                isLoading = false,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 720, name = "StatusLine — loading")
@Composable
private fun StatusLinePreviewLoading() {
    AgentSpawnWrappedTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 8.dp),
        ) {
            StatusLine(
                state = PreviewInputState,
                actions = PreviewStatusLineActions,
                selectedProvider = "Cursor",
                providers = listOf("Cursor"),
                isChatStarted = true,
                isLoading = true,
            )
        }
    }
}
