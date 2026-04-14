package org.enciyo.agentspawn.ui.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.enciyo.agentspawn.api.*
import org.enciyo.agentspawn.data.Client

class ChatViewModel(
    private val client: Client,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ChatEvent>()
    val event = _event.asSharedFlow()

    private val agentProvider = AgentProvider.Cursor
    private val jobs = mutableMapOf<String, Job>()

    private val repoSearchFlow = MutableStateFlow("")
    private val branchSearchFlow = MutableStateFlow("")


    init {
        loadInitialData()
        observeSearchQueries()
    }

    private fun loadInitialData() {
        pollAgents()
        loadModels()
        loadRepositories()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQueries() {
        viewModelScope.launch {
            repoSearchFlow
                .debounce(150)
                .drop(1)
                .collectLatest { query -> loadRepositories(query, autoSelect = false) }
        }
        viewModelScope.launch {
            branchSearchFlow
                .debounce(150)
                .drop(1)
                .collectLatest { query -> loadBranches(query, autoSelect = false) }
        }
    }

    // endregion

    // region Data Loading

    private fun loadModels() {
        viewModelScope.launch {
            client.agentModels(agentProvider)
                .onRight { updateInput { copy(models = it, selectedModel = it.first()) } }
                .onLeft { emitError(it) }
        }
    }

    private fun loadRepositories(repo: String = "", autoSelect: Boolean = true) {
        relaunch(KEY_REPOS) {
            updateInput { copy(isRepoLoading = true) }
            client.searchRepo(repo)
                .onRight {
                    updateInput {
                        copy(
                            isRepoLoading = false,
                            repositories = it,
                            selectedRepository = if (autoSelect) it.firstOrNull().orEmpty() else selectedRepository,
                            branches = if (autoSelect) emptyList() else branches,
                            searchBranch = if (autoSelect) "" else searchBranch,
                            selectedBranch = if (autoSelect) "" else selectedBranch,
                            targetBranch = if (autoSelect) "" else targetBranch
                        )
                    }
                    if (autoSelect) {
                        loadBranches()
                    }
                }
                .onLeft {
                    updateInput { copy(isRepoLoading = false) }
                    emitError(it)
                }
        }
    }

    private fun loadBranches(branch: String = "", autoSelect: Boolean = true) {
        relaunch(KEY_BRANCHES) {
            updateInput { copy(isBranchLoading = true) }
            client.searchBranch(branch, state.value.input.selectedRepository)
                .onRight {
                    updateInput {
                        copy(
                            isBranchLoading = false,
                            branches = it,
                            selectedBranch = if (autoSelect) it.firstOrNull().orEmpty() else selectedBranch
                        )
                    }
                }
                .onLeft {
                    updateInput { copy(isBranchLoading = false) }
                    emitError(it)
                }
        }
    }

    private fun pollAgents() {
        poll(KEY_AGENTS, 3000) {
            updateDrawer { copy(isLoading = agents.isEmpty()) }
            client.listAgents(agentProvider)
                .onRight { chats -> updateDrawer { copy(agents = chats, isLoading = false) } }
                .onLeft {
                    updateDrawer {
                        copy(
                            isLoading = false,
                            errorMessage = if (agents.isEmpty()) it.message else null
                        )
                    }
                }
        }
    }

    private fun pollAgent(id: String) {
        poll(KEY_AGENT) {
            client.getAgent(id)
                .onRight {
                    updateMessage { copy(messages = it.messages.reversed(), status = it.status, isLoading = false) }
                    updateInput {
                        val updated = copy(
                            selectedRepository = it.repository,
                            selectedBranch = it.branch,
                            repositories = emptyList(),
                            branches = emptyList(),
                            isEnabled = it.status.isActive.not()
                        )
                        if (isPending && it.status.isActive.not()) {
                            updated.copy(isEnabled = true, text = "", isPending = false)
                        } else {
                            updated
                        }
                    }

                }
                .onLeft {
                    updateMessage { copy(errorMessage = it.message, status = AgentStatus.FAILED, isLoading = false) }
                }
        }
    }

    fun onAgent(agent: AgentDto) {
        if (agent.id == state.value.drawer.selectedAgentId) return
        _state.update {
            it.copy(
                drawer = it.drawer.copy(selectedAgentId = agent.id),
                input = it.input.copy(text = ""),
                message = it.message.copy(status = agent.status, isLoading = true, messages = emptyList()),
            )
        }
        cancel(KEY_REPOS)
        cancel(KEY_BRANCHES)
        pollAgent(agent.id)
    }

    fun onHome() {
        viewModelScope.launch {
            _event.emit(ChatEvent.NavigateToHome)
        }
    }

    fun onNewAgent() {
        jobs.values.forEach { it.cancel() }
        _state.value = ChatUiState()
        loadInitialData()
    }

    fun onInputChange(text: String) {
        updateInput { copy(text = text) }
    }

    fun onSendMessage() {
        if (state.value.drawer.selectedAgentId != null) resume() else create()
    }

    fun onSelectModel(model: String) {
        updateInput { copy(selectedModel = model) }
    }

    fun onSearchModel(model: String) {
        updateInput { copy(searchModel = model) }
    }

    fun onSearchBranch(branch: String) {
        updateInput { copy(searchBranch = branch) }
        branchSearchFlow.value = branch
    }

    fun onSearchRepo(repo: String) {
        updateInput { copy(searchRepo = repo) }
        repoSearchFlow.value = repo
    }

    fun onSelectRepo(repo: String) {
        updateInput { copy(selectedRepository = repo) }
        loadBranches()
    }

    fun onSelectBranch(branch: String) {
        updateInput { copy(selectedBranch = branch) }
    }

    fun onChangeTargetBranch(targetBranch: String) {
        updateInput { copy(targetBranch = targetBranch) }
    }

    // endregion

    // region Agent Operations

    private fun resume() {
        viewModelScope.launch {
            val input = state.value.input
            val isActive = state.value.message.status.isActive
            val agentId = state.value.drawer.selectedAgentId.orEmpty()

            validate(
                listOf(
                    isActive.not() to "Agent is running",
                    input.text.isNotEmpty() to "Please enter prompt",
                    agentId.isNotEmpty() to "Please select an agent",
                )
            ) ?: return@launch

            val req = ResumeAgentRequest(prompt = input.text, id = agentId, model = input.selectedModel.lowercase())
            postPendingMessage()
            client.resumeAgent(req).handleAgentResult()
        }
    }

    private fun create() {
        viewModelScope.launch {
            val input = state.value.input

            validate(
                listOf(
                    input.text.isNotEmpty() to "Please enter prompt",
                    input.selectedRepository.isNotEmpty() to "Please select repository",
                    input.selectedBranch.isNotEmpty() to "Please enter ref",
                    input.targetBranch.isNotEmpty() to "Please enter branch name",
                )
            ) ?: return@launch

            val req = CreateAgentRequest(
                prompt = input.text,
                repository = input.selectedRepository,
                ref = input.selectedBranch,
                branchName = input.targetBranch,
                provider = agentProvider.name,
                model = input.selectedModel.lowercase()
            )
            postPendingMessage()
            client.createAgent(req).handleAgentResult()
        }
    }

    private fun postPendingMessage() {
        updateMessage { copy(status = AgentStatus.PENDING) }
        updateInput { copy(isEnabled = false, isPending = true) }
    }

    private suspend fun Either<Throwable, AgentDto>.handleAgentResult() {
        onRight { agent ->
            updateDrawer {
                copy(
                    selectedAgentId = agent.id,
                    agents = listOf(agent) + agents.filterNot { it.id == agent.id }
                )
            }
            pollAgent(agent.id)
        }
        onLeft {
            emitError(it)
        }
    }

    // endregion

    // region Helpers

    private suspend fun validate(rules: List<Pair<Boolean, String>>): Unit? {
        val failed = rules.firstOrNull { !it.first }
        if (failed != null) {
            _event.emit(ChatEvent.ShowError(failed.second))
            return null
        }
        return Unit
    }

    private suspend fun emitError(throwable: Throwable) {
        _event.emit(ChatEvent.ShowError(throwable.message.orEmpty()))
    }

    private fun relaunch(key: String, block: suspend CoroutineScope.() -> Unit) {
        jobs[key]?.cancel()
        jobs[key] = viewModelScope.launch(block = block)
    }

    private fun poll(key: String, interval: Long = 5000L, action: suspend () -> Unit) {
        relaunch(key) {
            while (isActive) {
                action()
                delay(interval)
            }
        }
    }

    private fun cancel(key: String) {
        jobs[key]?.cancel()
        jobs.remove(key)
    }

    private inline fun updateDrawer(block: DrawerState.() -> DrawerState) {
        _state.update { it.copy(drawer = it.drawer.block()) }
    }

    private inline fun updateMessage(block: MessageState.() -> MessageState) {
        _state.update { it.copy(message = it.message.block()) }
    }

    private inline fun updateInput(block: InputState.() -> InputState) {
        _state.update { it.copy(input = it.input.block()) }
    }

    // endregion

    companion object {
        private const val KEY_REPOS = "repos"
        private const val KEY_BRANCHES = "branches"
        private const val KEY_AGENTS = "agents"
        private const val KEY_AGENT = "agent"
    }
}
