package org.enciyo.agentspawn.ui.features.chat

import org.enciyo.agentspawn.api.AgentProvider

/**
 * Drawer / chat için bilinen provider kataloğu.
 * [agentProvider] null ise henüz API'ye bağlı değil; UI section olarak yine gösterilir.
 */
data class ProviderInfo(
    val id: String,
    val displayName: String,
    val agentProvider: AgentProvider?,
) {
    val isWired: Boolean get() = agentProvider != null
}

object ProviderCatalog {
    val all: List<ProviderInfo> = listOf(
        ProviderInfo(
            id = AgentProvider.Cursor.name,
            displayName = "Cursor",
            agentProvider = AgentProvider.Cursor,
        ),
        ProviderInfo(
            id = "ChatGpt",
            displayName = "ChatGPT",
            agentProvider = null,
        ),
        ProviderInfo(
            id = "ClaudeCode",
            displayName = "Claude Code",
            agentProvider = null,
        ),
        ProviderInfo(
            id = "Gemini",
            displayName = "Gemini",
            agentProvider = null,
        ),
    )

    fun byId(id: String): ProviderInfo? =
        all.find { it.id.equals(id, ignoreCase = true) }

    fun byAgentProvider(provider: AgentProvider): ProviderInfo =
        all.find { it.agentProvider == provider }
            ?: ProviderInfo(provider.name, provider.name, provider)
}
