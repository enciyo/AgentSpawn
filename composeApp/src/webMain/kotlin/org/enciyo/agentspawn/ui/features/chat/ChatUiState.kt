package org.enciyo.agentspawn.ui.features.chat

import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentMessageDto
import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.api.AgentStatus

data class ChatUiState(
    val drawer: DrawerState = DrawerState(),
    val message: MessageState = MessageState(),
    val input: InputState = InputState(),
) {
    val isChatStarted: Boolean
        get() = message.messages.isNotEmpty()
}

data class ProviderGroup(
    val provider: ProviderInfo,
    val isLoggedIn: Boolean,
    val agents: List<AgentDto> = emptyList(),
)

data class DrawerState(
    val providerGroups: List<ProviderGroup> = emptyList(),
    val selectedAgentId: String? = null,
    val selectedProviderId: String = AgentProvider.Cursor.name,
    val isGrouped: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val agents: List<AgentDto>
        get() = providerGroups.flatMap { it.agents }

    val selectedProvider: ProviderInfo
        get() = ProviderCatalog.byId(selectedProviderId)
            ?: ProviderCatalog.byAgentProvider(AgentProvider.Cursor)

    val loggedInProviders: List<ProviderInfo>
        get() = providerGroups.filter { it.isLoggedIn }.map { it.provider }

    /** New agent StatusLine için seçilebilir (login + wired) provider isimleri. */
    val selectableProviderNames: List<String>
        get() {
            val loggedIn = providerGroups
                .filter { it.isLoggedIn && it.provider.isWired }
                .map { it.provider.displayName }
            if (loggedIn.isNotEmpty()) return loggedIn.distinct()
            return ProviderCatalog.all.filter { it.isWired }.map { it.displayName }
        }
}

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

    /** Repo yoksa gizle; selected veya liste varsa göster. */
    val isVisibleRepositorySelection
        get() = selectedRepository.isNotBlank() || repositories.isNotEmpty()

    /** Branch yoksa gizle; selected veya liste varsa göster. */
    val isVisibleBranchSelection
        get() = selectedBranch.isNotBlank() || branches.isNotEmpty()

    /**
     * Chat öncesi: branch listesi veya değer varsa göster (set edilmeli).
     * Chat sonrası: sadece set edilmişse göster (read-only).
     */
    fun isVisibleTargetBranch(isChatStarted: Boolean): Boolean =
        if (isChatStarted) targetBranch.isNotBlank()
        else branches.isNotEmpty() || targetBranch.isNotBlank()
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
