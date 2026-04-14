package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateAgentRequest(
    val prompt: String,
    val repository: String,
    val ref: String,
    val branchName: String,
    val provider: String,
    val model: String
)
