package org.enciyo.agentspawn.cli.agent

import arrow.core.Either
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.ShellResult

interface AgentCli {
    suspend fun login(): Either<AppError, ShellResult>
    suspend fun models(): Either<AppError, List<String>>
    suspend fun logout(): Either<AppError, ShellResult>
    suspend fun status(): Either<AppError, ShellResult>
    suspend fun createChat(): Either<AppError, ShellResult>
    suspend fun resumeChat(id: String, prompt: String, model: String): Either<AppError, ShellResult>
    suspend fun listChats(): Either<AppError, List<String>>
    suspend fun getChatContent(id: String): Either<AppError, String>
    suspend fun prompt(id: String, prompt: String): Either<AppError, ShellResult>
    suspend fun getMcpConfigs(): Either<AppError, ShellResult>
    suspend fun clear()
}


