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

data class SplashUiState(
    val isLoading: Boolean = true,
    val selectedProvider: AgentProvider = AgentProvider.Cursor,
    val providers: List<AgentProvider> = AgentProvider.entries,
    val loggedMail: String? = null,
    val errorMessage: String? = null,
    val githubState: GithubState = GithubState(),
) {
    val isLoggedIn: Boolean get() = !loggedMail.isNullOrEmpty()
    val isGithubConnected: Boolean get() = githubState.apiKey.isNotEmpty()
}

data class GithubState(
    val apiKey: String = "",
    val code: String = "",
    val redirect: String = "",
    val isLoading: Boolean = false,
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
    private var cursorPollJob: Job? = null

    private val selectedProvider get() = state.value.selectedProvider

    init {
        fetchStatus()
        fetchGithubToken()
    }

    fun onSelectProvider(provider: AgentProvider) {
        _state.update { it.copy(selectedProvider = provider, isLoading = true) }
        fetchStatus()
    }

    fun onProviderToggle() {
        if (state.value.isLoggedIn) logoutAgent() else loginAgent()
    }

    fun onNavigateToChatClick() {
        viewModelScope.launch { _events.emit(SplashEvent.NavigateToChat) }
    }

    private fun fetchStatus() {
        execute(
            call = { client.statusAgent(selectedProvider) },
            onSuccess = { result ->
                _state.update { it.copy(loggedMail = result.output, errorMessage = null, isLoading = false) }
            },
            onError = { e ->
                _state.update { it.copy(loggedMail = null, errorMessage = e.message.orEmpty(), isLoading = false) }
            },
        )
    }

    private fun loginAgent() {
        execute(
            call = { client.loginAgent(selectedProvider) },
            onSuccess = {
                pollUntil { client.statusAgent(selectedProvider).getOrNull()?.output.orEmpty().isNotEmpty() }
                    .also { cursorPollJob = it }
                    .invokeOnCompletion { fetchStatus() }
            },
        )
    }

    private fun logoutAgent() {
        execute(
            call = { client.logoutAgent(selectedProvider) },
            onSuccess = { _state.update { it.copy(loggedMail = null, errorMessage = null) } },
        )
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
        updateGithub { copy(redirect = "", code = "") }
        if (isOpen) {
            viewModelScope.launch { _events.emit(SplashEvent.OpenLoginUrl(redirect)) }
        }
    }

    private fun fetchGithubToken() {
        updateGithub { copy(isLoading = true) }
        execute(
            call = { client.tokenGithub() },
            onSuccess = { result -> updateGithub { copy(isLoading = false, apiKey = result) } },
            onError = { e ->
                updateGithub { copy(isLoading = false) }
                _state.update { it.copy(errorMessage = e.message.orEmpty()) }
            },
        )
    }

    private fun loginGithub() {
        execute(
            call = { client.loginGithub() },
            onSuccess = { result ->
                updateGithub { copy(code = result.code, redirect = result.openUrl, isLoading = false) }
                pollUntil { client.tokenGithub().getOrNull().orEmpty().isNotEmpty() }
                    .also { githubPollJob = it }
                    .invokeOnCompletion { fetchGithubToken() }
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
                _state.update { it.copy(githubState = GithubState(), errorMessage = null, isLoading = false) }
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

    private fun pollUntil(
        intervalMs: Long = 1000,
        condition: suspend () -> Boolean,
    ): Job {
        githubPollJob?.cancel()
        cursorPollJob?.cancel()
        return viewModelScope.launch {
            while (isActive) {
                if (condition()) break
                delay(intervalMs)
            }
        }
    }
}
