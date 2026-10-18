package org.enciyo.agentspawn.features.mcp

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.json.Json
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.McpConfigDto
import org.enciyo.agentspawn.api.McpServerConfig
import org.enciyo.agentspawn.api.UpsertMcpServerRequest
import org.enciyo.agentspawn.cli.FileCli
import org.enciyo.agentspawn.cli.Folders

class McpService(
    private val fileCli: FileCli,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    suspend fun get(): Either<AppError, McpConfigDto> = either {
        readCanonical().bind()
    }

    suspend fun put(config: McpConfigDto): Either<AppError, McpConfigDto> = either {
        persist(config).bind()
        config
    }

    suspend fun upsert(request: UpsertMcpServerRequest): Either<AppError, McpConfigDto> = either {
        ensure(request.name.isNotBlank()) {
            AppError.Validation("MCP server name cannot be blank")
        }
        val current = readCanonical().bind()
        val server = McpServerConfig(
            command = request.command,
            args = request.args,
            env = request.env,
            url = request.url,
        )
        val updated = current.copy(
            mcpServers = current.mcpServers + (request.name to server)
        )
        persist(updated).bind()
        updated
    }

    suspend fun delete(name: String): Either<AppError, McpConfigDto> = either {
        ensure(name.isNotBlank()) {
            AppError.Validation("MCP server name cannot be blank")
        }
        val current = readCanonical().bind()
        ensure(current.mcpServers.containsKey(name)) {
            AppError.NotFound("MCP server '$name' not found")
        }
        val updated = current.copy(mcpServers = current.mcpServers - name)
        persist(updated).bind()
        updated
    }

    private suspend fun readCanonical(): Either<AppError, McpConfigDto> = either {
        val shell = fileCli.read(Folders.MCP_CONFIG).getOrElse { null }
            ?: return@either McpConfigDto()
        val output = shell.output.trim()
        if (output.isEmpty()) return@either McpConfigDto()
        runCatching { json.decodeFromString<McpConfigDto>(output) }
            .fold(
                onSuccess = { it },
                onFailure = { raise(AppError.Internal("Failed to parse MCP config: ${it.message}")) }
            )
    }

    private suspend fun persist(config: McpConfigDto): Either<AppError, Unit> = either {
        fileCli.make(Folders.AGENTSPAWN_ROOT).bind()
        val content = json.encodeToString(McpConfigDto.serializer(), config)
        fileCli.write(Folders.MCP_CONFIG, content).bind()
        // Write-through sync to Cursor (only wired provider)
        fileCli.write(Folders.CURSOR_MCP_CONFIG, content).bind()
    }
}
