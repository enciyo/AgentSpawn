package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class AgentDto(
    val id: String,
    val status: AgentStatus,
    val title: String,
    val branchName: String,
    val ref: String,
    val repository: String,
    val provider: String,
    val lastModel: String,
    /** ACP session id; null for pre-ACP agents (whose session id is then [id]). */
    val sessionId: String? = null,
)

