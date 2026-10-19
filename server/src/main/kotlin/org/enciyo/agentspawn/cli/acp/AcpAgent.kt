package org.enciyo.agentspawn.cli.acp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.enciyo.agentspawn.api.AgentStreamEvent
import org.enciyo.agentspawn.api.acp.ContentBlock
import org.enciyo.agentspawn.api.acp.SessionUpdate

class AcpAgent(
    private val command: String,
    private val mcpServers: () -> JsonArray = { JsonArray(emptyList()) },
) {

    fun prompt(
        cwd: String,
        sessionId: String?,
        prompt: String,
        model: String,
        onSession: suspend (String) -> Unit = {},
    ): Flow<AgentStreamEvent> = channelFlow {
        connect(cwd, onUpdate = { send(AgentStreamEvent.Update(it)) }) { connection ->
            val session = connection.openSession(cwd, sessionId)
            onSession(session.id)
            connection.selectModel(session, model)
            send(AgentStreamEvent.TurnStarted)
            send(AgentStreamEvent.Update(SessionUpdate.UserMessageChunk(textBlock(prompt))))
            val result: PromptResponse = connection.call(AcpMethod.SessionPrompt, PromptRequest(session.id, listOf(textBlock(prompt))))
            send(AgentStreamEvent.TurnEnded(result.stopReason))
        }
    }

    suspend fun history(cwd: String, sessionId: String): List<SessionUpdate> {
        val updates = mutableListOf<SessionUpdate>()
        connect(cwd, onUpdate = { updates += it }) { connection ->
            connection.send(AcpMethod.SessionLoad, loadRequest(cwd, sessionId))
        }
        return updates
    }

    suspend fun ask(cwd: String, prompt: String, model: String = ""): String {
        val answer = StringBuilder()
        connect(cwd, onUpdate = { update ->
            when (update) {
                is SessionUpdate.AgentMessageChunk -> answer.append(update.content.text.orEmpty())
                is SessionUpdate.ToolCall -> answer.clear()
                else -> Unit
            }
        }) { connection ->
            val session = connection.openSession(cwd, sessionId = null)
            connection.selectModel(session, model)
            connection.send(AcpMethod.SessionPrompt, PromptRequest(session.id, listOf(textBlock(prompt))))
        }
        return answer.toString().trim()
    }

    suspend fun models(cwd: String): List<String> =
        connect(cwd) { connection ->
            val session = connection.openSession(cwd, sessionId = null)
            session.models().map { it.name }.ifEmpty { session.modelOptions().map { it.value } }
        }

    private suspend fun <T> connect(
        cwd: String,
        onUpdate: suspend (SessionUpdate) -> Unit = {},
        block: suspend (AcpConnection) -> T,
    ): T = AcpConnection.start(command, cwd, onUpdate).use { connection ->
        connection.send(
            AcpMethod.Initialize,
            InitializeRequest(
                protocolVersion = PROTOCOL_VERSION,
                clientCapabilities = ClientCapabilities(
                    fs = FileSystemCapability(readTextFile = false, writeTextFile = false),
                    terminal = false,
                ),
            ),
        )
        block(connection)
    }

    private suspend fun AcpConnection.openSession(cwd: String, sessionId: String?): Session {
        if (sessionId != null) {
            runCatching { call<LoadSessionRequest, SessionResponse>(AcpMethod.SessionLoad, loadRequest(cwd, sessionId)) }
                .onSuccess { return Session(sessionId, it) }
                .onFailure { println("[ACP] session/load failed for $sessionId: ${it.message}") }
        }
        val result: SessionResponse = call(AcpMethod.SessionNew, NewSessionRequest(cwd, mcpServers()))
        return Session(checkNotNull(result.sessionId) { "${AcpMethod.SessionNew.value} returned no sessionId" }, result)
    }

    private suspend fun AcpConnection.selectModel(session: Session, model: String) {
        if (model.isBlank()) return
        val selection = runCatching {
            session.models().firstOrNull { it.matches(model) }?.let {
                send(AcpMethod.SessionSetModel, SetModelRequest(session.id, it.id))
                return@runCatching
            }
            session.modelOptions().firstOrNull { it.matches(model) }?.let {
                send(AcpMethod.SessionSetConfigOption, SetConfigOptionRequest(session.id, it.configId, it.value))
                return@runCatching
            }
            println("[ACP] model not offered by agent: $model")
        }
        selection.onFailure { println("[ACP] model selection failed ($model): ${it.message}") }
    }

    private fun loadRequest(cwd: String, sessionId: String) = LoadSessionRequest(sessionId, cwd, mcpServers())

    private fun textBlock(text: String) = ContentBlock(type = "text", text = text)

    /** Encodes [params] to JSON and sends the request; returns the raw `result`. */
    private suspend inline fun <reified P> AcpConnection.send(method: AcpMethod, params: P): JsonObject =
        request(method, AcpConnection.json.encodeToJsonElement(params).jsonObject)

    /** Like [send], but decodes `result` into [R]. */
    private suspend inline fun <reified P, reified R> AcpConnection.call(method: AcpMethod, params: P): R =
        AcpConnection.json.decodeFromJsonElement(send(method, params))

    private class Session(val id: String, val response: SessionResponse) {

        fun models(): List<Model> =
            response.models?.availableModels.orEmpty().map { Model(id = it.modelId, name = it.name) }

        fun modelOptions(): List<ModelOption> =
            response.configOptions
                .filter { it.category == "model" }
                .flatMap { config ->
                    config.options.mapNotNull { option ->
                        ModelOption(configId = config.id, value = option.value ?: return@mapNotNull null, name = option.name)
                    }
                }
    }

    private data class Model(val id: String, val name: String) {
        fun matches(model: String) = id == model ||
            name.equals(model, ignoreCase = true) ||
            id.substringBefore('[').equals(model, ignoreCase = true)
    }

    private data class ModelOption(val configId: String, val value: String, val name: String) {
        fun matches(model: String) = value.equals(model, ignoreCase = true) || name.equals(model, ignoreCase = true)
    }

    private companion object {
        const val PROTOCOL_VERSION = 1
    }
}
