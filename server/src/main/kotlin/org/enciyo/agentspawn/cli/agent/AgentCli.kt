package org.enciyo.agentspawn.cli.agent

import arrow.core.Either
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.cli.acp.AcpAgent

interface AgentCli {
    /** Chat, history and model operations: every provider speaks the Agent Client Protocol. */
    val acp: AcpAgent

    // Account/file operations not covered by ACP go through the provider CLI
    suspend fun login(): Either<AppError, ShellResult>
    suspend fun logout(): Either<AppError, ShellResult>
    suspend fun status(): Either<AppError, ShellResult>
    suspend fun listChats(): Either<AppError, List<String>>
    suspend fun clear()
}
