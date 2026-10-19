package org.enciyo.agentspawn.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.enciyo.agentspawn.api.acp.SessionUpdate

@Serializable
data class AgentContentDto(
    val chatId: String,
    /** Conversation history replayed via ACP `session/load`; empty while the agent is running, since history then comes from the stream. */
    val updates: List<SessionUpdate>,
    val status: AgentStatus,
    val repository: String,
    val branch: String,
)

@Serializable
data class AgentMessageDto(
    val role: Role,
    val parts: List<ContentPart>
)

@Serializable
@JsonClassDiscriminator("type")
sealed class ContentPart {

    @Serializable
    @SerialName("text")
    data class Text(val text: String) : ContentPart()

    @Serializable
    @SerialName("reasoning")
    data class Thinking(val text: String) : ContentPart()

    @Serializable
    @SerialName("tool_call")
    data class ToolCall(val name: String, val args: String) : ContentPart()

    @Serializable
    @SerialName("tool_result")
    data class ToolResult(val toolName: String, val result: String) : ContentPart()

    @Serializable
    @SerialName("user_query")
    data class UserQuery(val query: String) : ContentPart()
}

@Serializable
enum class Role {
    USER,
    ASSISTANT,
    TOOL,
    UNKNOWN
}