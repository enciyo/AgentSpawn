package org.enciyo.agentspawn.cli.providers

import arrow.core.Either
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.cli.Cli
import org.enciyo.agentspawn.cli.Command
import org.enciyo.agentspawn.cli.FileCli
import org.enciyo.agentspawn.cli.Folders
import org.enciyo.agentspawn.cli.ShellRunner
import org.enciyo.agentspawn.cli.SqlLiteCli
import org.enciyo.agentspawn.cli.agent.AgentCli

class CursorCli(
    shellRunner: ShellRunner,
    val fileCli: FileCli,
    val sqlLiteCli: SqlLiteCli
) : Cli(shellRunner), AgentCli {

    override val program = Command.default("agent")

    private object Commands {
        object Login : Command("login")
        object Logout : Command("logout")
        object Status : Command("status")
        object CreateChat : Command("create-chat")
        object Print : Command("--print")
        data class Resume(val id: String) : Command("--resume ${userInput(id)}")
        data class Model(val model: String = "auto") : Command("--model ${userInput(model)}")
        data object Force : Command("--force")
        data object ApproveMcps : Command("--approve-mcps")
        data object Trust : Command("--trust")
        data class Prompt(val prompt: String) : Command(userInput(prompt))
        data class Workspace(val path: String) : Command("--workspace $path")
        data object Models : Command("models")
    }

    companion object {
        const val PATH = "~/.cursor"
    }

    override suspend fun models(): Either<AppError, List<String>> =
        run(Commands.Models)
            .map {
                it.output.lines()
                    .filter { it.isNotBlank() }
                    .map { it.substringBefore(" - ").trim() }
                    .drop(2)
                    .dropLast(1)
            }
            .mapLeft { AppError.Internal("Failed to get models: ${it.message}") }


    override suspend fun login(): Either<AppError, ShellResult> =
        run(
            Commands.Login
        )

    override suspend fun logout(): Either<AppError, ShellResult> =
        run(
            Commands.Logout,
        )

    override suspend fun status(): Either<AppError, ShellResult> =
        run(
            Commands.Status,
            Commands.Print,
            timeout = 2,
        )

    override suspend fun createChat(): Either<AppError, ShellResult> =
        run(
            Commands.CreateChat,
            Commands.Force,
            Commands.ApproveMcps,
            Commands.Trust,
            Commands.Print
        )

    override suspend fun resumeChat(
        id: String,
        prompt: String,
        model: String
    ): Either<AppError, ShellResult> =
        run(
            Commands.Prompt(prompt),
            Commands.Resume(id),
            Commands.Force,
            Commands.Print,
            Commands.ApproveMcps,
            Commands.Trust,
            Commands.Model(model),
            Commands.Workspace(Folders.getProjectFolder(id))
        )

    override suspend fun listChats(): Either<AppError, List<String>> =
        fileCli.list("$PATH/chats/*/")

    override suspend fun getChatContent(id: String): Either<AppError, String> =
        sqlLiteCli.getTables(
            "$PATH/chats/*/$id/store.db",
            "blobs",
            "data LIKE '%role%'"
        )
            .map { it.joinToString() }

    override suspend fun clear() {
        fileCli.delete("$PATH/chats/")
    }

    override suspend fun prompt(
        id: String,
        prompt: String,
    ): Either<AppError, ShellResult> =
        run(
            Commands.Prompt(prompt),
            Commands.Force,
            Commands.Print,
            Commands.ApproveMcps,
            Commands.Trust,
            Commands.Model(),
            Commands.Workspace(Folders.getProjectFolder(id))
        )
}