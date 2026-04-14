package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class ResumeAgentRequest(
    val prompt: String,
    val model: String,
    val id: String,
)
