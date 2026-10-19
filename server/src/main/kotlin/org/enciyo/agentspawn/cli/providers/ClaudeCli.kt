package org.enciyo.agentspawn.cli.providers

import arrow.core.Either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.McpConfigDto
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.cli.Cli
import org.enciyo.agentspawn.cli.Command
import org.enciyo.agentspawn.cli.FileCli
import org.enciyo.agentspawn.cli.Folders
import org.enciyo.agentspawn.cli.ShellRunner
import org.enciyo.agentspawn.cli.acp.AcpAgent
import org.enciyo.agentspawn.cli.agent.AgentCli
import java.io.File


private val json = Json { ignoreUnknownKeys = true }


class ClaudeCli(
    shellRunner: ShellRunner,
    val fileCli: FileCli,
) : Cli(shellRunner), AgentCli {

    override val program = Command.default("claude")

    // Claude Code has no built-in ACP mode; the official adapter uses the same session files as the Claude Agent SDK
    override val acp = AcpAgent(command = ACP_COMMAND, mcpServers = ::mcpServers)

    private object Commands {
        object Auth : Command("auth")
        object Login : Command("login")
        object Logout : Command("logout")
        object Status : Command("status")
        object JsonOutput : Command("--json")
    }

    companion object {
        const val ACP_COMMAND = "npx -y @agentclientprotocol/claude-agent-acp@0.93.0"

        private val HOME = System.getProperty("user.home")
        private val PATH = "$HOME/.claude/projects"

        // Claude: ~/.claude/projects/{cwd with non-alphanumeric characters replaced by '-'}/{sessionId}.jsonl
        private fun String.expandHome() = replaceFirst("~", HOME)
        private fun projectKey(path: String) = path.expandHome().replace(Regex("[^A-Za-z0-9]"), "-")

        // Only sessions opened by AgentSpawn (~/projects/*)
        private val AGENTSPAWN_SESSIONS = "$PATH/${projectKey(Folders.ROOT_PROJECTS)}-*"
    }

    override suspend fun login(): Either<AppError, ShellResult> =
        run(
            Commands.Auth,
            Commands.Login,
        )

    override suspend fun logout(): Either<AppError, ShellResult> =
        run(
            Commands.Auth,
            Commands.Logout,
        )

    override suspend fun status(): Either<AppError, ShellResult> =
        run(
            Commands.Auth,
            Commands.Status,
            Commands.JsonOutput,
            timeout = 10,
        ).map { result ->
            val status = runCatching { Json.parseToJsonElement(result.output).jsonObject }.getOrNull()
            val email = status
                ?.takeIf { it["loggedIn"]?.jsonPrimitive?.booleanOrNull == true }
                ?.get("email")?.jsonPrimitive?.contentOrNull
            // AgentService expects Cursor's "Logged in as <user>" format
            if (email == null) result else result.copy(output = "Logged in as $email")
        }

    override suspend fun listChats(): Either<AppError, List<String>> =
        fileCli.list("-1 $AGENTSPAWN_SESSIONS/*.jsonl")
            .map { lines ->
                lines.map { it.trim().substringAfterLast('/').removeSuffix(".jsonl") }
                    .filter { it.isNotBlank() && !it.contains(':') }
                    .distinct()
            }

    override suspend fun clear() {
        fileCli.delete(AGENTSPAWN_SESSIONS)
    }

    /** Converts ~/.agentspawn/mcp.json into an ACP McpServer list (stdio or http). */
    private fun mcpServers(): JsonArray {
        val file = File(Folders.MCP_CONFIG.expandHome())
        val config = runCatching { json.decodeFromString<McpConfigDto>(file.readText()) }
            .getOrNull() ?: return JsonArray(emptyList())
        return buildJsonArray {
            config.mcpServers.forEach { (name, server) ->
                addJsonObject {
                    put("name", name)
                    if (server.url != null) {
                        put("type", "http")
                        put("url", server.url)
                        putJsonArray("headers") {}
                    } else {
                        put("command", server.command.orEmpty())
                        putJsonArray("args") { server.args.forEach { add(it) } }
                        putJsonArray("env") {
                            server.env.forEach { (key, value) ->
                                addJsonObject {
                                    put("name", key)
                                    put("value", value)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
