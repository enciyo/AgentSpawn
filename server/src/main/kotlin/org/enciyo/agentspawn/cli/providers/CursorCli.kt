package org.enciyo.agentspawn.cli.providers

import arrow.core.Either
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.cli.Cli
import org.enciyo.agentspawn.cli.Command
import org.enciyo.agentspawn.cli.FileCli
import org.enciyo.agentspawn.cli.ShellRunner
import org.enciyo.agentspawn.cli.acp.AcpAgent
import org.enciyo.agentspawn.cli.agent.AgentCli

class CursorCli(
    shellRunner: ShellRunner,
    val fileCli: FileCli,
) : Cli(shellRunner), AgentCli {

    override val program = Command.default("agent")

    // MCP servers are read from Cursor's own ~/.cursor/mcp.json (McpService writes there too)
    override val acp = AcpAgent(command = "agent acp")

    private object Commands {
        object Login : Command("login")
        object Logout : Command("logout")
        object Status : Command("status")
        object Print : Command("--print")
    }

    companion object {
        const val PATH = "~/.cursor"
    }

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

    override suspend fun listChats(): Either<AppError, List<String>> =
        // ACP: ~/.cursor/acp-sessions/{sessionId}/, legacy CLI chats: ~/.cursor/chats/{projectHash}/{chatId}/
        fileCli.list("-1d $PATH/acp-sessions/*/ $PATH/chats/*/*/")
            .map { lines ->
                lines.map { it.trim().trimEnd('/') }
                    .map { it.substringAfterLast('/') }
                    .filter { it.isNotBlank() && !it.contains(':') }
                    .distinct()
            }

    override suspend fun clear() {
        fileCli.delete("$PATH/chats/")
        fileCli.delete("$PATH/acp-sessions/")
    }
}
