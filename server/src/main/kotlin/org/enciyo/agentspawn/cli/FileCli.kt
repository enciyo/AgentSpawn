package org.enciyo.agentspawn.cli

import arrow.core.Either
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.ShellResult

class FileCli(shellRunner: ShellRunner) : Cli(shellRunner) {

    override val program: Command get() = Command.default("")

    private object FileCommand {
        data class List(val path: String? = null) : Command("ls $path")
        data class MakeDirectory(val path: String) : Command("mkdir -p $path")
        data class Delete(val path: String) : Command("rm -rf $path")
        data class Read(val path: String) : Command("cat $path")
    }

    suspend fun list(
        path: String,
    ): Either<AppError, List<String>> =
        run(FileCommand.List(path))
            .map { it.output.trim().lines() }

    suspend fun make(path: String): Either<AppError, Boolean> =
        run(FileCommand.MakeDirectory(path))
            .map { it.output.trim().lines() }
            .map { it.isEmpty() }

    suspend fun delete(string: String): Either<AppError, ShellResult> =
        run(FileCommand.Delete(string))

    suspend fun read(string: String): Either<AppError, ShellResult> =
        run(FileCommand.Read(string))

}