package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class RunShellRequest(
    val command: String,
    val timeout: Long = 2L
)
