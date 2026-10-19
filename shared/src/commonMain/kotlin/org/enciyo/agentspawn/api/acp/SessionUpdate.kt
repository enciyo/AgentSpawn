package org.enciyo.agentspawn.api.acp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

/**
 * The subset of Agent Client Protocol `session/update` notifications used for chat.
 * Field names match the ACP schema exactly: https://agentclientprotocol.com/protocol/schema#sessionupdate
 * Unsupported types (usage_update, available_commands_update...) are filtered out on the server.
 */
@Serializable
@JsonClassDiscriminator("sessionUpdate")
sealed interface SessionUpdate {

    @Serializable
    @SerialName("user_message_chunk")
    data class UserMessageChunk(val content: ContentBlock) : SessionUpdate

    @Serializable
    @SerialName("agent_message_chunk")
    data class AgentMessageChunk(val content: ContentBlock) : SessionUpdate

    @Serializable
    @SerialName("agent_thought_chunk")
    data class AgentThoughtChunk(val content: ContentBlock) : SessionUpdate

    @Serializable
    @SerialName("tool_call")
    data class ToolCall(
        val toolCallId: String,
        val title: String = "",
        val kind: String? = null,
        val status: ToolCallStatus? = null,
        val rawInput: JsonElement? = null,
        val rawOutput: JsonElement? = null,
        val content: List<ToolCallContent> = emptyList(),
        val locations: List<ToolCallLocation> = emptyList(),
    ) : SessionUpdate

    /** Only changed fields are sent; null ones keep their previous value. */
    @Serializable
    @SerialName("tool_call_update")
    data class ToolCallUpdate(
        val toolCallId: String,
        val title: String? = null,
        val kind: String? = null,
        val status: ToolCallStatus? = null,
        val rawInput: JsonElement? = null,
        val rawOutput: JsonElement? = null,
        val content: List<ToolCallContent>? = null,
        val locations: List<ToolCallLocation>? = null,
    ) : SessionUpdate

    @Serializable
    @SerialName("plan")
    data class Plan(val entries: List<PlanEntry>) : SessionUpdate

    companion object {
        /** The `sessionUpdate` values this model carries. */
        val SUPPORTED = setOf(
            "user_message_chunk",
            "agent_message_chunk",
            "agent_thought_chunk",
            "tool_call",
            "tool_call_update",
            "plan",
        )
    }
}

/** ACP ContentBlock; for types other than `text` (image, resource...) only the type is carried. */
@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null,
)

@Serializable
enum class ToolCallStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("in_progress")
    IN_PROGRESS,

    @SerialName("completed")
    COMPLETED,

    @SerialName("failed")
    FAILED,
}

/** ACP ToolCallContent: `content` | `diff` | `terminal`. */
@Serializable
data class ToolCallContent(
    val type: String,
    val content: ContentBlock? = null,
    val path: String? = null,
    val oldText: String? = null,
    val newText: String? = null,
    val terminalId: String? = null,
)

@Serializable
data class ToolCallLocation(
    val path: String,
    val line: Int? = null,
)

@Serializable
data class PlanEntry(
    val content: String,
    val priority: String = "medium",
    val status: String = "pending",
)
