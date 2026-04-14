package org.enciyo.agentspawn.ui.features.chat

import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentMessageDto
import org.enciyo.agentspawn.api.AgentStatus

data class ChatUiState(
    val drawer: DrawerState = DrawerState(),
    val message: MessageState = MessageState(),
    val input: InputState = InputState(),
) {
    val isChatStarted: Boolean
        get() = message.messages.isNotEmpty()
}

data class DrawerState(
    val agents: List<AgentDto> = emptyList(),
    val selectedAgentId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class InputState(
    val text: String = "",
    val models: List<String> = emptyList(),
    val selectedModel: String = "",
    val searchModel: String = "",
    val repositories: List<String> = emptyList(),
    val selectedRepository: String = "",
    val searchRepo: String = "",
    val isRepoLoading: Boolean = true,
    val branches: List<String> = emptyList(),
    val selectedBranch: String = "",
    val searchBranch: String = "",
    val isBranchLoading: Boolean = true,
    val targetBranch: String = "",
    val isEnabled: Boolean = true,
    val isPending: Boolean = false,
) {
    val isVisibleSendButton
        get() = text.isNotBlank()

    val filteredModels: List<String>
        get() = if (searchModel.isBlank()) models else
            models.filter { it.contains(searchModel, ignoreCase = true) }

    val isVisibleRepositorySelection
        get() = repositories.isNotEmpty() || searchRepo.isNotBlank() || selectedRepository.isNotBlank()

    val isVisibleBranchSelection
        get() = branches.isNotEmpty() || searchBranch.isNotBlank() || selectedBranch.isNotBlank()

    val isVisibleTargetBranch
        get() = branches.isNotEmpty()
}

data class MessageState(
    val messages: List<AgentMessageDto> = emptyList(),
    val status: AgentStatus = AgentStatus.COMPLETED,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

sealed interface ChatEvent {
    data object NavigateToHome : ChatEvent
    data class ShowError(val message: String) : ChatEvent
}
