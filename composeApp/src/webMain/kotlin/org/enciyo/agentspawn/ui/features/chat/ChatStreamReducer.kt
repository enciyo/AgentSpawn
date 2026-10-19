package org.enciyo.agentspawn.ui.features.chat

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.enciyo.agentspawn.api.AgentMessageDto
import org.enciyo.agentspawn.api.ContentPart
import org.enciyo.agentspawn.api.Role
import org.enciyo.agentspawn.api.acp.SessionUpdate
import org.enciyo.agentspawn.api.acp.ToolCallContent
import org.enciyo.agentspawn.api.acp.ToolCallStatus

/**
 * Folds ACP `session/update`s into the [AgentMessageDto] list rendered by the UI.
 * History (the `session/load` replay) and the live stream go through the same path. Messages are chronological (oldest first).
 */
internal data class ChatTranscript(
    val messages: List<AgentMessageDto> = emptyList(),
    private val tools: Map<String, ToolState> = emptyMap(),
) {
    internal data class ToolState(
        val title: String,
        val args: String,
        /** Location of a ToolCall part; replaced in place when its title/input is updated later. */
        val message: Int,
        val part: Int,
        val finished: Boolean = false,
    )

    fun reduce(update: SessionUpdate): ChatTranscript = when (update) {
        is SessionUpdate.UserMessageChunk -> appendText(Role.USER, update.content.text) { part, text ->
            (part as? ContentPart.UserQuery)?.let { ContentPart.UserQuery(it.query + text) }
        } ?: plusPart(Role.USER, ContentPart.UserQuery(update.content.text.orEmpty()))

        is SessionUpdate.AgentMessageChunk -> appendText(Role.ASSISTANT, update.content.text) { part, text ->
            (part as? ContentPart.Text)?.let { ContentPart.Text(it.text + text) }
        } ?: plusPart(Role.ASSISTANT, ContentPart.Text(update.content.text.orEmpty()))

        is SessionUpdate.AgentThoughtChunk -> appendText(Role.ASSISTANT, update.content.text) { part, text ->
            (part as? ContentPart.Thinking)?.let { ContentPart.Thinking(it.text + text) }
        } ?: plusPart(Role.ASSISTANT, ContentPart.Thinking(update.content.text.orEmpty()))

        is SessionUpdate.ToolCall -> {
            val args = update.rawInput.toArgs()
            val added = plusPart(Role.ASSISTANT, ContentPart.ToolCall(update.title, args))
            val last = added.messages.lastIndex
            added.copy(tools = tools + (update.toolCallId to ToolState(update.title, args, last, added.messages[last].parts.lastIndex)))
                .finishIfDone(update.toolCallId, update.status, update.content, update.rawOutput)
        }

        is SessionUpdate.ToolCallUpdate -> {
            val tool = tools[update.toolCallId]
            if (tool == null) this
            else {
                val updated = tool.copy(
                    title = update.title ?: tool.title,
                    args = update.rawInput?.toArgs()?.takeIf { it != "{}" } ?: tool.args,
                )
                replacePart(updated.message, updated.part, ContentPart.ToolCall(updated.title, updated.args))
                    .copy(tools = tools + (update.toolCallId to updated))
                    .finishIfDone(update.toolCallId, update.status, update.content, update.rawOutput)
            }
        }

        is SessionUpdate.Plan -> plusPart(
            Role.ASSISTANT,
            ContentPart.Text(update.entries.joinToString("\n") { entry ->
                val mark = if (entry.status == "completed") "x" else " "
                "- [$mark] ${entry.content}"
            }),
        )
    }

    /** Once a tool has completed or failed, appends its result once as a TOOL message. */
    private fun finishIfDone(
        id: String,
        status: ToolCallStatus?,
        content: List<ToolCallContent>?,
        rawOutput: JsonElement?,
    ): ChatTranscript {
        val tool = tools[id] ?: return this
        if (tool.finished || (status != ToolCallStatus.COMPLETED && status != ToolCallStatus.FAILED)) return this
        val result = content.orEmpty().mapNotNull { it.toText() }.joinToString("\n")
            .ifBlank { rawOutput.toResult() }
        return copy(
            messages = messages + AgentMessageDto(Role.TOOL, listOf(ContentPart.ToolResult(tool.title, result))),
            tools = tools + (id to tool.copy(finished = true)),
        )
    }

    /** Appends the text to the last message if it has the same role and its last part is the same type; otherwise null. */
    private inline fun appendText(
        role: Role,
        text: String?,
        merge: (ContentPart, String) -> ContentPart?,
    ): ChatTranscript? {
        if (text.isNullOrEmpty()) return this
        val last = messages.lastOrNull()?.takeIf { it.role == role } ?: return null
        val merged = last.parts.lastOrNull()?.let { merge(it, text) } ?: return null
        return copy(messages = messages.dropLast(1) + last.copy(parts = last.parts.dropLast(1) + merged))
    }

    private fun plusPart(role: Role, part: ContentPart): ChatTranscript {
        val last = messages.lastOrNull()
        return if (last?.role == role) copy(messages = messages.dropLast(1) + last.copy(parts = last.parts + part))
        else copy(messages = messages + AgentMessageDto(role, listOf(part)))
    }

    private fun replacePart(message: Int, part: Int, value: ContentPart): ChatTranscript {
        val target = messages.getOrNull(message) ?: return this
        if (part !in target.parts.indices) return this
        val parts = target.parts.toMutableList().also { it[part] = value }
        return copy(messages = messages.toMutableList().also { it[message] = target.copy(parts = parts) })
    }
}

internal fun List<SessionUpdate>.toTranscript(): ChatTranscript =
    fold(ChatTranscript()) { transcript, update -> transcript.reduce(update) }

private fun JsonElement?.toArgs(): String = when (this) {
    null, is JsonNull -> "{}"
    else -> toString()
}

private fun JsonElement?.toResult(): String = when (this) {
    null, is JsonNull -> ""
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonObject -> (this["content"] as? JsonPrimitive)?.contentOrNull ?: toString()
    else -> toString()
}

private fun ToolCallContent.toText(): String? = when (type) {
    "content" -> content?.text
    "diff" -> "$path\n${newText.orEmpty()}"
    else -> null
}
