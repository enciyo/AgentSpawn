package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class AgentLoginResponse(
    val url: String
)
