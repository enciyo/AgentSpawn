package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class ShellJobDto(
    val id: String,
    val command: String,
    val status: ShellJobStatus,
    val createdAt: Long,
    val finishedAt: Long? = null,
    val output: String? = null,
    val error: String? = null,
    val exitCode: Int? = null,
    val timedOut: Boolean? = null,
    val pid: Long? = null
)
