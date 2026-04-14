package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
enum class ShellJobStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
