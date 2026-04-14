package org.enciyo.agentspawn.features.shell

import arrow.core.Either
import org.enciyo.agentspawn.*
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.cli.Command
import org.enciyo.agentspawn.cli.ShellRunner


class ShellService : ShellRunner {
    override suspend fun runSync(
        vararg args: Command,
        timeoutSeconds: Long,
        workingDir: String,
        interruptible: Boolean
    ): Either<AppError, ShellResult> {
        val command = args.joinToString(" ") { it.str }
        return command.exec(timeout = timeoutSeconds, workingDir = workingDir, interruptible = interruptible).toEither()
    }

    override suspend fun runSyncCommand(
        command: String,
        timeoutSeconds: Long,
        workingDir: String
    ) =
        command.exec(timeout = timeoutSeconds, workingDir = workingDir).toEither()
}
