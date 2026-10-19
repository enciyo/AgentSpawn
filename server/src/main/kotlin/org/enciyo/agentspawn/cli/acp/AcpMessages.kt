package org.enciyo.agentspawn.cli.acp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
import org.enciyo.agentspawn.api.acp.ContentBlock

/**
 * The subset of ACP requests and responses used by AcpAgent.
 * Field names match the ACP schema exactly: https://agentclientprotocol.com/protocol/schema
 *
 * Since [AcpConnection.json] doesn't use `encodeDefaults`, requests have no default values;
 * every field must be set explicitly to be sent.
 */

@Serializable
internal data class InitializeRequest(
    val protocolVersion: Int,
    val clientCapabilities: ClientCapabilities,
)

@Serializable
internal data class ClientCapabilities(
    val fs: FileSystemCapability,
    val terminal: Boolean,
)

@Serializable
internal data class FileSystemCapability(
    val readTextFile: Boolean,
    val writeTextFile: Boolean,
)

@Serializable
internal data class NewSessionRequest(
    val cwd: String,
    val mcpServers: JsonArray,
)

@Serializable
internal data class LoadSessionRequest(
    val sessionId: String,
    val cwd: String,
    val mcpServers: JsonArray,
)

/** Response of `session/new` and `session/load`; `session/load` doesn't return [sessionId]. */
@Serializable
internal data class SessionResponse(
    val sessionId: String? = null,
    val models: SessionModelState? = null,
    val configOptions: List<SessionConfigOption> = emptyList(),
)

@Serializable
internal data class SessionModelState(
    val availableModels: List<ModelInfo> = emptyList(),
)

@Serializable
internal data class ModelInfo(
    val modelId: String,
    val name: String = "",
)

@Serializable
internal data class SessionConfigOption(
    val id: String,
    val category: String? = null,
    val options: List<SessionConfigValue> = emptyList(),
)

/** Grouped options have no [value]; they are skipped. */
@Serializable
internal data class SessionConfigValue(
    val value: String? = null,
    val name: String = "",
)

@Serializable
internal data class SetModelRequest(
    val sessionId: String,
    val modelId: String,
)

@Serializable
internal data class SetConfigOptionRequest(
    val sessionId: String,
    val configId: String,
    val value: String,
)

@Serializable
internal data class PromptRequest(
    val sessionId: String,
    val prompt: List<ContentBlock>,
)

@Serializable
internal data class PromptResponse(
    val stopReason: String = "end_turn",
)

/** Agent → client `session/update` notification; skipped without decoding if the [update] type isn't supported. */
@Serializable
internal data class SessionNotification(
    val update: JsonObject,
)

/** Agent → client `session/request_permission` request. */
@Serializable
internal data class RequestPermissionRequest(
    val options: List<PermissionOption> = emptyList(),
)

@Serializable
internal data class PermissionOption(
    val optionId: String,
    val kind: String,
    val name: String = "",
)

@Serializable
internal data class RequestPermissionResponse(
    val outcome: PermissionOutcome,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("outcome")
internal sealed interface PermissionOutcome {

    @Serializable
    @SerialName("cancelled")
    data object Cancelled : PermissionOutcome

    @Serializable
    @SerialName("selected")
    data class Selected(val optionId: String) : PermissionOutcome
}
