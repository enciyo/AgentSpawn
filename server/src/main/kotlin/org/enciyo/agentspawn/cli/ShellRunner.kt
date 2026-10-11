package org.enciyo.agentspawn.cli

import arrow.core.Either
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.ShellResult

interface ShellRunner {
    suspend fun runSync(
        vararg args: Command,
        timeoutSeconds: Long = -1,
        workingDir: String = "",
        interruptible: Boolean = true,
        env: Map<String, String> = emptyMap(),
    ): Either<AppError, ShellResult>

    suspend fun runSyncCommand(
        command: String,
        timeoutSeconds: Long = -1,
        workingDir: String = "",
    ): Either<AppError, ShellResult>
}