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
    val lastModel: String
)

