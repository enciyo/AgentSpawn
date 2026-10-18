package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class McpServerConfig(
    val command: String? = null,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val url: String? = null,
)

@Serializable
data class McpConfigDto(
    val mcpServers: Map<String, McpServerConfig> = emptyMap(),
)

@Serializable
data class UpsertMcpServerRequest(
    val name: String,
    val command: String? = null,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val url: String? = null,
)
