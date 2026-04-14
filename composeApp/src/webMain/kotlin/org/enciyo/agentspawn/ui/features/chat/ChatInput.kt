package org.enciyo.agentspawn.ui.features.chat

import agentspawn.composeapp.generated.resources.Res
import agentspawn.composeapp.generated.resources.input_place_holder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.ui.features.components.AppTextField
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InputHeader(
    state: InputState,
    actions: HeaderActions,
) {
    val scrollState = rememberScrollState()
    AnimatedVisibility(
        state.isVisibleRepositorySelection || state.isVisibleBranchSelection,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 28.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            AnimatedVisibility(state.isVisibleRepositorySelection) {
                DropdownSelector(
                    items = state.repositories,
                    selectedItem = state.selectedRepository,
                    isEnable = state.repositories.isNotEmpty(),
                    onItemSelected = actions.onSelectRepo,
                    searchQuery = state.searchRepo,
                    onSearch = actions.onSearchRepo,
                    isLoading = state.isRepoLoading
                )
            }
            AnimatedVisibility(state.isVisibleBranchSelection) {
                DropdownSelector(
                    items = state.branches,
                    selectedItem = state.selectedBranch,
                    isEnable = state.branches.isNotEmpty(),
                    onItemSelected = actions.onSelectBranch,
                    searchQuery = state.searchBranch,
                    onSearch = actions.onSearchBranch,
                    isLoading = state.isBranchLoading
                )
            }
            AnimatedVisibility(
                state.isVisibleTargetBranch,
            ) {
                TargetBranch(
                    value = state.targetBranch,
                    onValueChange = actions.onChangeTargetBranch,
                )
            }
        }
    }
}

@Composable
internal fun InputScreen(
    state: InputState,
    actions: InputActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.border()) {
        InputArea(
            value = state.text,
            isEnabled = state.isEnabled,
            onInputChange = actions.onInputChange,
            onSendMessage = actions.onSendMessage,
            isVisibleSendButton = state.isVisibleSendButton
        )
        InputToolbar(
            state = state,
            actions = actions,
        )
    }
}

@Composable
private fun InputArea(
    value: String,
    isVisibleSendButton: Boolean,
    isEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTextField(
        value = value,
        isEnabled = isEnabled,
        onValueChange = onInputChange,
        placeholder = stringResource(Res.string.input_place_holder),
        line = 3,
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.isShiftPressed && event.key == Key.Enter && isVisibleSendButton) {
                    onSendMessage()
                    true
                } else false
            }
    )
}

@Composable
private fun InputToolbar(
    state: InputState,
    actions: InputActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModelSelector(
            modifier = Modifier.weight(1f),
            selectedModel = state.selectedModel,
            models = state.filteredModels,
            onSelectModel = actions.onSelectModel,
            onSearchModel = actions.onSearchModel,
            searchQuery = state.searchModel
        )
        AnimatedVisibility(state.isVisibleSendButton) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.Send,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = actions.onSendMessage),
                contentDescription = "Shift + Enter"
            )
        }
    }
}

@Composable
private fun ModelSelector(
    modifier: Modifier = Modifier,
    selectedModel: String,
    searchQuery: String,
    models: List<String>,
    onSelectModel: (String) -> Unit,
    onSearchModel: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = models.isNotEmpty() || searchQuery.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        DropdownSelector(
            items = models,
            selectedItem = selectedModel,
            onItemSelected = onSelectModel,
            onSearch = onSearchModel,
            searchQuery = searchQuery,
            isLoading = false
        )
    }
}

@Composable
private fun TargetBranch(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var minWidth by remember { mutableStateOf(Int.MAX_VALUE) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            modifier = Modifier.size(12.dp)
        )

        AppTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Enter target branch",
            textStyle = MaterialTheme.typography.bodySmall,
            contentPadding = PaddingValues(vertical = 0.dp),
            line = 1,
            modifier = Modifier
                .onPreviewKeyEvent {
                    if (it.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Down)
                        focusRequester.freeFocus()
                    } else false
                }
                .onGloballyPositioned { coordinates ->
                    minWidth = coordinates.size.width
                }
                .focusRequester(focusRequester)
                .widthIn(min = minWidth.dp, max = minWidth.dp)
        )
    }
}
