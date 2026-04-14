package org.enciyo.agentspawn.features.agent.mapper

import kotlinx.serialization.json.*
import org.enciyo.agentspawn.api.AgentContentDto
import org.enciyo.agentspawn.api.AgentMessageDto
import org.enciyo.agentspawn.api.AgentStatus
import org.enciyo.agentspawn.api.ContentPart
import org.enciyo.agentspawn.api.Role


fun String.toAgentContentDto(): List<AgentMessageDto> {
    val rawMessages = runCatching { Json.parseToJsonElement(this).jsonArray }.getOrNull() ?: return emptyList()
    val messages = rawMessages.mapNotNull { it.jsonObject["data"]?.toAgentMessageDto() }
    return messages
}

private fun JsonElement.toAgentMessageDto(): AgentMessageDto? {
    val obj = runCatching {
        Json.parseToJsonElement((this as JsonPrimitive).content).jsonObject
    }.getOrNull() ?: return null
    val role = when (obj["role"]?.jsonPrimitive?.content) {
        "user" -> Role.USER
        "assistant" -> Role.ASSISTANT
        "tool" -> Role.TOOL
        else -> return null
    }
    val content = obj["content"] ?: return null
    val parts = when (content) {
        is JsonArray -> content.mapNotNull { it.toContentPart() }
        is JsonPrimitive -> listOf(ContentPart.Text(content.content.clearTags()))
        else -> emptyList()
    }
    if (parts.isEmpty()) return null
    return AgentMessageDto(role = role, parts = parts)
}

private fun JsonElement.toContentPart(): ContentPart? {
    val obj = jsonObject
    return when (obj["type"]?.jsonPrimitive?.content) {
        "text" -> {
            val text = obj["text"]?.jsonPrimitive?.content
            if (text.isNullOrBlank()) return null
            val query = "user_query"
            if (text.contains(query)) {
                ContentPart.UserQuery(text.clearTags())
            } else {
                ContentPart.Text(text.clearTags())
            }
        }

        "reasoning" -> {
            val text = obj["text"]?.jsonPrimitive?.content
            if (text.isNullOrBlank()) return null
            ContentPart.Thinking(text.clearTags())
        }

        "tool-call" -> ContentPart.ToolCall(
            name = obj["toolName"]?.jsonPrimitive?.content ?: "",
            args = obj["args"]?.toString() ?: "{}"
        )

        "tool-result" -> ContentPart.ToolResult(
            toolName = obj["toolName"]?.jsonPrimitive?.content ?: "",
            result = obj["result"]?.toString() ?: ""
        )

        else -> null
    }
}


private fun String.clearTags(): String {
    return this.replace(Regex("<[^>]*>"), "")
        .trim()
}

