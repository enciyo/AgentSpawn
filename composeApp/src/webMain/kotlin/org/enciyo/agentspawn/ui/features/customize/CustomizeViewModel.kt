package org.enciyo.agentspawn.ui.features.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.enciyo.agentspawn.api.McpConfigDto
import org.enciyo.agentspawn.data.Client

data class CustomizeUiState(
    val selectedTabIndex: Int = 0,
    val mcpJson: String = "",
    val savedMcpJson: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isMcpJsonValid: Boolean = true,
    val errorMessage: String? = null,
) {
    val isDirty: Boolean get() = mcpJson != savedMcpJson
    val canSave: Boolean get() = isDirty && isMcpJsonValid && !isSaving && !isLoading
}

class CustomizeViewModel(
    private val client: Client,
) : ViewModel() {

    private val prettyJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _state = MutableStateFlow(CustomizeUiState())
    val state: StateFlow<CustomizeUiState> = _state.asStateFlow()

    init {
        loadMcp()
    }

    fun onTabSelected(index: Int) {
        _state.update { it.copy(selectedTabIndex = index) }
    }

    fun onMcpJsonChange(value: String) {
        val validationError = mcpJsonValidationError(value)
        _state.update {
            it.copy(
                mcpJson = value,
                isMcpJsonValid = validationError == null,
                errorMessage = validationError,
            )
        }
    }

    fun loadMcp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            client.getMcp().fold(
                ifLeft = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load MCP config",
                        )
                    }
                },
                ifRight = { config ->
                    val text = prettyJson.encodeToString(config)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            mcpJson = text,
                            savedMcpJson = text,
                            isMcpJsonValid = true,
                            errorMessage = null,
                        )
                    }
                },
            )
        }
    }

    fun saveMcp() {
        viewModelScope.launch {
            if (!_state.value.canSave) return@launch
            val raw = _state.value.mcpJson
            val parsed = runCatching { prettyJson.decodeFromString<McpConfigDto>(raw) }
                .getOrElse { e ->
                    _state.update {
                        it.copy(
                            isMcpJsonValid = false,
                            errorMessage = "Invalid JSON: ${e.message}",
                        )
                    }
                    return@launch
                }

            _state.update { it.copy(isSaving = true, errorMessage = null) }
            client.putMcp(parsed).fold(
                ifLeft = { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = e.message ?: "Failed to save MCP config",
                        )
                    }
                },
                ifRight = { config ->
                    val text = prettyJson.encodeToString(config)
                    _state.update {
                        it.copy(
                            isSaving = false,
                            mcpJson = text,
                            savedMcpJson = text,
                        )
                    }
                },
            )
        }
    }

    private fun mcpJsonValidationError(raw: String): String? {
        if (raw.isBlank()) return "JSON cannot be blank"
        return runCatching { prettyJson.decodeFromString<McpConfigDto>(raw) }
            .exceptionOrNull()
            ?.let { "Invalid JSON: ${it.message}" }
    }
}
