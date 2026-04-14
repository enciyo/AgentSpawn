package org.enciyo.agentspawn.features.agent.model

import org.enciyo.agentspawn.api.AgentStatus

data class Agent(
    val id: Int = 0,
    val name: String,
    val chatId: String,
    val text: String,
    val repository: String,
    val ref: String,
    val branchName: String,
    val prUri: String? = null,
    val jobId: String? = null,
    val provider: String,
    val status: AgentStatus = AgentStatus.PENDING,
    val lastModel: String,
)
