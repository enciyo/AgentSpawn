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

    private val jobs = mutableMapOf<String, Job>()

    private val repoSearchFlow = MutableStateFlow("")
    private val branchSearchFlow = MutableStateFlow("")

    private val selectedAgentProvider: AgentProvider
        get() = state.value.drawer.selectedProvider.agentProvider ?: AgentProvider.Cursor

    init {
        loadInitialData()
        observeSearchQueries()
    }

    private fun loadInitialData() {
        loadModels(selectedAgentProvider)
        loadRepositories()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQueries() {
        viewModelScope.launch {
            repoSearchFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .drop(1)
                .collectLatest { query -> loadRepositories(query, autoSelect = false) }
        }
        viewModelScope.launch {
            branchSearchFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .drop(1)
                .collectLatest { query -> loadBranches(query, autoSelect = false) }
        }
    }

    // endregion

    // region Data Loading

    private fun loadModels(provider: AgentProvider) {
        viewModelScope.launch {
            client.agentModels(provider)
                .onRight { models ->
                    updateInput {
                        copy(
                            models = models,
                            selectedModel = models.firstOrNull().orEmpty(),
                        )
                    }
                }
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

    /** Polls the agent list while the tab is visible; no requests are made while it is hidden. */
    fun onVisibilityChanged(isVisible: Boolean) {
        if (isVisible) pollAgents() else cancel(KEY_AGENTS)
    }

    private fun pollAgents() {
        poll(KEY_AGENTS, AGENTS_POLL_MS) {
            updateDrawer { copy(isLoading = providerGroups.isEmpty() && agents.isEmpty()) }
            refreshProviderGroups()
        }
    }

    /**
     * Login status is checked once per session since each check starts a CLI process;
     * later rounds only fetch the agent list (from the DB).
     */
    private var loggedInIds: Set<String>? = null

    /**
     * Groups chats by provider using a single list.
     * Catalog providers that aren't wired up yet also come through as sections.
     */
    private suspend fun refreshProviderGroups() {
        val ids = loggedInIds ?: fetchLoggedInProviders().also { loggedInIds = it }
        listAgentsInto(ids)
    }

    private suspend fun fetchLoggedInProviders(): Set<String> {
        val wiredProviders = ProviderCatalog.all.mapNotNull { it.agentProvider }
        return coroutineScope {
            wiredProviders
                .map { provider ->
                    async {
                        val loggedIn = client.statusAgent(provider)
                            .getOrNull()
                            ?.output
                            .orEmpty()
                            .isNotBlank()
                        provider.name to loggedIn
                    }
                }
                .awaitAll()
                .filter { it.second }
                .map { it.first }
                .toSet()
        }
    }

    private suspend fun listAgentsInto(loggedInIds: Set<String>) {
        client.listAgents(selectedAgentProvider)
            .onRight { chats ->
                val groups = ProviderCatalog.all.map { info ->
                    val agentsForProvider = chats.filter {
                        it.provider.equals(info.id, ignoreCase = true)
                    }
                    val isLoggedIn = info.agentProvider?.name in loggedInIds ||
                        agentsForProvider.isNotEmpty()
                    ProviderGroup(
                        provider = info,
                        isLoggedIn = isLoggedIn && info.isWired,
                        agents = agentsForProvider,
                    )
                }
                val selectedStillValid = groups.any { it.provider.id == state.value.drawer.selectedProviderId }
                updateDrawer {
                    copy(
                        providerGroups = groups,
                        isLoading = false,
                        errorMessage = null,
                        selectedProviderId = if (selectedStillValid) {
                            selectedProviderId
                        } else {
                            groups.firstOrNull { it.isLoggedIn }?.provider?.id
                                ?: AgentProvider.Cursor.name
                        },
                    )
                }
            }
            .onLeft {
                updateDrawer {
                    copy(
                        isLoading = false,
                        errorMessage = if (agents.isEmpty()) it.message else null,
                    )
                }
            }
    }

    /** Fetches history once; connects to the live stream if the agent is still running. */
    private fun loadAgent(id: String) {
        relaunch(KEY_AGENT) {
            val content = fetchAgent(id) ?: return@relaunch
            if (content.status.isActive) streamAgent(id)
        }
    }

    /** Streams the run live after create/resume. */
    private fun startStream(id: String) {
        relaunch(KEY_AGENT) {
            streamAgent(id)
        }
    }

    /**
     * The run stream starts by replaying the full history via `session/load`. The replay is collected silently;
     * from [AgentStreamEvent.TurnStarted] on, the screen updates live, so existing messages aren't cleared and refilled.
     */
    private suspend fun streamAgent(id: String) {
        var transcript = ChatTranscript()
        var isLive = false
        client.streamAgent(id)
            .catch { emitError(it) }
            .collect { event ->
                when (event) {
                    is AgentStreamEvent.Update -> transcript = transcript.reduce(event.update)
                    is AgentStreamEvent.TurnStarted -> isLive = true
                    is AgentStreamEvent.Error -> _event.emit(ChatEvent.ShowError(event.message))
                    is AgentStreamEvent.TurnEnded,
                    is AgentStreamEvent.Done -> Unit
                }
                if (!isLive) return@collect
                updateMessage {
                    copy(
                        messages = transcript.messages.reversed(),
                        status = (event as? AgentStreamEvent.Done)?.status ?: AgentStatus.RUNNING,
                        isLoading = false,
                    )
                }
            }
        // Run finished (or connection dropped): sync with the authoritative history and status
        fetchAgent(id)
    }

    private suspend fun fetchAgent(id: String): AgentContentDto? =
        client.getAgent(id)
            .onRight {
                // A running agent's history comes from the stream; show loading until then
                if (it.status.isActive) {
                    updateMessage { copy(status = it.status, isLoading = messages.isEmpty()) }
                } else {
                    updateMessage {
                        copy(messages = it.updates.toTranscript().messages.reversed(), status = it.status, isLoading = false)
                    }
                }
                updateInput {
                    val updated = copy(
                        selectedRepository = it.repository,
                        selectedBranch = it.branch,
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
            .getOrNull()

    fun onAgent(agent: AgentDto) {
        if (agent.id == state.value.drawer.selectedAgentId) return
        val previousProviderId = state.value.drawer.selectedProviderId
        val providerId = ProviderCatalog.byId(agent.provider)?.id
            ?: agent.provider.ifBlank { AgentProvider.Cursor.name }
        _state.update {
            it.copy(
                drawer = it.drawer.copy(
                    selectedAgentId = agent.id,
                    selectedProviderId = providerId,
                ),
                input = it.input.copy(text = ""),
                message = it.message.copy(status = agent.status, isLoading = true, messages = emptyList()),
                mainPane = MainPane.Chat,
            )
        }
        if (providerId != previousProviderId) {
            ProviderCatalog.byId(providerId)?.agentProvider?.let { loadModels(it) }
        }
        cancel(KEY_REPOS)
        cancel(KEY_BRANCHES)
        loadAgent(agent.id)
    }

    fun onSelectProvider(providerId: String) {
        val info = ProviderCatalog.byId(providerId) ?: return
        if (!info.isWired) {
            viewModelScope.launch {
                _event.emit(ChatEvent.ShowError("${info.displayName} is not connected yet."))
            }
            return
        }
        val group = state.value.drawer.providerGroups.find { it.provider.id == providerId }
        if (group != null && !group.isLoggedIn) {
            viewModelScope.launch {
                _event.emit(ChatEvent.ShowError("${info.displayName}: not logged in."))
            }
            return
        }
        val previous = state.value.drawer.selectedProviderId
        updateDrawer {
            copy(
                selectedProviderId = providerId,
                selectedAgentId = null,
            )
        }
        _state.update { it.copy(mainPane = MainPane.Chat) }
        updateMessage { copy(messages = emptyList(), status = AgentStatus.COMPLETED, isLoading = false) }
        cancel(KEY_AGENT)
        if (providerId != previous) {
            updateInput {
                copy(
                    text = "",
                    isEnabled = true,
                    isPending = false,
                    models = emptyList(),
                    selectedModel = "",
                    searchModel = "",
                )
            }
            info.agentProvider?.let { loadModels(it) }
        } else {
            updateInput { copy(text = "", isEnabled = true, isPending = false) }
        }
    }

    fun onSelectProviderByDisplayName(displayName: String) {
        if (state.value.isChatStarted || isStatusLoading()) return
        val info = ProviderCatalog.all.find { it.displayName == displayName } ?: return
        onSelectProvider(info.id)
    }

    fun onHome() {
        viewModelScope.launch {
            _event.emit(ChatEvent.NavigateToHome)
        }
    }

    fun onCustomize() {
        _state.update { it.copy(mainPane = MainPane.Customize) }
    }

    fun onToggleGrouping() {
        updateDrawer { copy(isGrouped = !isGrouped) }
    }

    fun onNewAgent() {
        cancel(KEY_AGENT)
        val drawer = state.value.drawer
        val input = state.value.input
        _state.update {
            it.copy(
                drawer = drawer.copy(selectedAgentId = null),
                message = MessageState(),
                input = InputState(
                    models = input.models,
                    selectedModel = input.selectedModel,
                ),
                mainPane = MainPane.Chat,
            )
        }
        loadRepositories()
    }

    fun onInputChange(text: String) {
        updateInput { copy(text = text) }
    }

    fun onSendMessage() {
        if (state.value.drawer.selectedAgentId != null) resume() else create()
    }

    fun onSelectModel(model: String) {
        if (isStatusLoading()) return
        updateInput { copy(selectedModel = model) }
    }

    fun onSearchModel(model: String) {
        if (isStatusLoading()) return
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
        if (state.value.isChatStarted || isStatusLoading()) return
        updateInput { copy(selectedRepository = repo) }
        loadBranches()
    }

    fun onExpandRepo() {
        if (state.value.isChatStarted || isStatusLoading()) return
        if (state.value.input.repositories.isEmpty() && !state.value.input.isRepoLoading) {
            loadRepositories(autoSelect = false)
        }
    }

    fun onSelectBranch(branch: String) {
        if (state.value.isChatStarted || isStatusLoading()) return
        updateInput { copy(selectedBranch = branch) }
    }

    fun onExpandBranch() {
        if (state.value.isChatStarted || isStatusLoading()) return
        val input = state.value.input
        if (input.selectedRepository.isBlank()) return
        if (input.branches.isEmpty() && !input.isBranchLoading) {
            loadBranches(autoSelect = false)
        }
    }

    fun onChangeTargetBranch(targetBranch: String) {
        if (state.value.isChatStarted || isStatusLoading()) return
        updateInput { copy(targetBranch = targetBranch) }
    }

    private fun isStatusLoading(): Boolean {
        val current = state.value
        return current.message.isLoading ||
            current.message.status.isActive ||
            current.input.isPending ||
            !current.input.isEnabled
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

            val provider = state.value.drawer.selectedProvider
            val apiProvider = provider.agentProvider
            if (apiProvider == null) {
                _event.emit(ChatEvent.ShowError("${provider.displayName} is not connected yet."))
                return@launch
            }

            val req = CreateAgentRequest(
                prompt = input.text,
                repository = input.selectedRepository,
                ref = input.selectedBranch,
                branchName = input.targetBranch,
                provider = apiProvider.name,
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
            val providerId = ProviderCatalog.byId(agent.provider)?.id
                ?: agent.provider.ifBlank { AgentProvider.Cursor.name }
            updateDrawer {
                val updatedGroups = providerGroups.map { group ->
                    if (!group.provider.id.equals(providerId, ignoreCase = true)) group
                    else group.copy(
                        isLoggedIn = true,
                        agents = listOf(agent) + group.agents.filterNot { it.id == agent.id },
                    )
                }.let { groups ->
                    if (groups.any { it.provider.id.equals(providerId, ignoreCase = true) }) groups
                    else groups + ProviderGroup(
                        provider = ProviderCatalog.byId(providerId)
                            ?: ProviderInfo(providerId, providerId, null),
                        isLoggedIn = true,
                        agents = listOf(agent),
                    )
                }
                copy(
                    selectedAgentId = agent.id,
                    selectedProviderId = providerId,
                    providerGroups = updatedGroups,
                )
            }
            startStream(agent.id)
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
        private const val AGENTS_POLL_MS = 15_000L
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
