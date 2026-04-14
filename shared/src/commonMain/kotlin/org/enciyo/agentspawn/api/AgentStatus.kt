package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
enum class AgentStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isActive: Boolean
        get() = this == PENDING || this == RUNNING
}
