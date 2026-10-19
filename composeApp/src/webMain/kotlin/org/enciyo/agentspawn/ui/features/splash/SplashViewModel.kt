package org.enciyo.agentspawn.ui.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.data.Client
import kotlin.time.Duration.Companion.milliseconds

data class SplashUiState(
    val providerStates: Map<AgentProvider, ProviderState> = AgentProvider.entries.associateWith { ProviderState() },
    val errorMessage: String? = null,
    val githubState: GithubState = GithubState(),
) {
    val isLoading: Boolean get() = providerStates.values.any { it.isLoading }
    val isLoggedIn: Boolean get() = providerStates.values.any { it.isLoggedIn }
    val isGithubConnected: Boolean get() = githubState.connected

    fun provider(provider: AgentProvider): ProviderState = providerStates[provider] ?: ProviderState()
}

data class ProviderState(
    val isLoading: Boolean = true,
    val loggedMail: String? = null,
) {
    val isLoggedIn: Boolean get() = !loggedMail.isNullOrEmpty()
}

data class GithubState(
    val apiKey: String = "",
    val code: String = "",
    val redirect: String = "",
    val isLoading: Boolean = false,
    val connected: Boolean = false,
)

sealed interface SplashEvent {
    data class OpenLoginUrl(val url: String) : SplashEvent
    data object NavigateToChat : SplashEvent
}

class SplashViewModel(
    private val client: Client,
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SplashEvent>()
    val events: SharedFlow<SplashEvent> = _events.asSharedFlow()

    private var githubPollJob: Job? = null
    private val agentPollJobs = mutableMapOf<AgentProvider, Job>()

    init {
        AgentProvider.entries.forEach(::fetchStatus)
        fetchGithubToken()
    }

    fun onProviderToggle(provider: AgentProvider) {
        if (state.value.provider(provider).isLoggedIn) logoutAgent(provider) else loginAgent(provider)
    }

    fun onNavigateToChatClick() {
        viewModelScope.launch { _events.emit(SplashEvent.NavigateToChat) }
    }

    private fun fetchStatus(provider: AgentProvider) {
        execute(
            call = { client.statusAgent(provider) },
            onSuccess = { result ->
                updateProvider(provider) { copy(loggedMail = result.output, isLoading = false) }
            },
            // Not being logged in isn't an error; it's just shown as not connected
            onError = { updateProvider(provider) { copy(loggedMail = null, isLoading = false) } },
        )
    }

    private fun loginAgent(provider: AgentProvider) {
        updateProvider(provider) { copy(isLoading = true) }
        _state.update { it.copy(errorMessage = null) }
        execute(
            call = { client.loginAgent(provider) },
            onSuccess = {
                agentPollJobs[provider]?.cancel()
                pollUntil(timeoutMessage = "${provider.name} login timed out.") {
                    client.statusAgent(provider).getOrNull()?.output.orEmpty().isNotEmpty()
                }
                    .also { agentPollJobs[provider] = it }
                    .invokeOnCompletion { fetchStatus(provider) }
            },
            onError = { e ->
                updateProvider(provider) { copy(isLoading = false) }
                _state.update { it.copy(errorMessage = e.message) }
            },
        )
    }

    private fun logoutAgent(provider: AgentProvider) {
        execute(
            call = { client.logoutAgent(provider) },
            onSuccess = {
                updateProvider(provider) { copy(loggedMail = null) }
                _state.update { it.copy(errorMessage = null) }
            },
        )
    }

    private fun updateProvider(provider: AgentProvider, block: ProviderState.() -> ProviderState) {
        _state.update { it.copy(providerStates = it.providerStates + (provider to it.provider(provider).block())) }
    }

    fun onGithubApiKeyChange(value: String) {
        updateGithub { copy(apiKey = value) }
    }

    fun onConnectGithub() {
        updateGithub { copy(isLoading = true) }
        if (state.value.isGithubConnected) logoutGithub() else loginGithub()
    }

    fun onOpenGithubLoginPage(isOpen: Boolean) {
        val redirect = state.value.githubState.redirect
        if (isOpen) {
            viewModelScope.launch { _events.emit(SplashEvent.OpenLoginUrl(redirect)) }
        }
    }

    private fun fetchGithubToken() {
        updateGithub { copy(isLoading = true) }
        execute(
            call = { client.tokenGithub() },
            onSuccess = { result -> updateGithub { copy(isLoading = false, apiKey = result, connected = true) } },
            onError = { e ->
                updateGithub { copy(isLoading = false) }
                val msg = e.message.orEmpty()
                if (!msg.contains("HTTP 404")) {
                    _state.update { it.copy(errorMessage = msg) }
                }
            },
        )
    }

    private fun loginGithub() {
        execute(
            call = { client.loginGithub() },
            onSuccess = { result ->
                updateGithub { copy(code = result.code, redirect = result.openUrl, isLoading = true) }
                pollUntil(timeoutMessage = "GitHub login timed out.") {
                    client.tokenGithub().getOrNull().orEmpty().isNotEmpty()
                }
                    .also { githubPollJob?.cancel(); githubPollJob = it }
                    .invokeOnCompletion { cause ->
                        if (cause == null) fetchGithubToken()
                    }
            },
            onError = { e ->
                updateGithub { copy(isLoading = false) }
                _state.update { it.copy(errorMessage = e.message) }
            },
        )
    }

    private fun logoutGithub() {
        execute(
            call = { client.logoutGithub() },
            onSuccess = {
                _state.update { it.copy(githubState = GithubState(), errorMessage = null) }
            },
            onError = { e ->
                updateGithub { copy(isLoading = false) }
                _state.update { it.copy(errorMessage = e.message) }
            },
        )
    }

    private fun updateGithub(block: GithubState.() -> GithubState) {
        _state.update { it.copy(githubState = it.githubState.block()) }
    }

    private fun <T> execute(
        call: suspend () -> Either<Throwable, T>,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit = { e -> _state.update { it.copy(errorMessage = e.message) } },
    ) {
        viewModelScope.launch { call().fold(onError, onSuccess) }
    }

    // Each poll starts a CLI process (status); 2 s × 45 = 90 s wait
    private fun pollUntil(
        intervalMs: Long = 2000,
        maxAttempts: Int = 45,
        timeoutMessage: String,
        condition: suspend () -> Boolean,
    ): Job {
        return viewModelScope.launch {
            repeat(maxAttempts) {
                if (!isActive) return@launch
                if (condition()) return@launch
                delay(intervalMs.milliseconds)
            }
            updateGithub { copy(isLoading = false) }
            _state.update { it.copy(errorMessage = timeoutMessage) }
        }
    }
}
