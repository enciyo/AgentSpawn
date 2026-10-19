package org.enciyo.agentspawn.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.enciyo.agentspawn.api.acp.SessionUpdate

/**
 * `agents/{id}/stream` SSE event.
 * Conversation content is carried verbatim as ACP [SessionUpdate] for every provider;
 * the other events are AgentSpawn's run lifecycle.
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface AgentStreamEvent {

    /** ACP `session/update`. History loaded via `session/load` at the start of a run is also replayed through these events. */
    @Serializable
    @SerialName("update")
    data class Update(val update: SessionUpdate) : AgentStreamEvent

    /** History replay is done and a new turn starts; the prompt follows as a `user_message_chunk`. */
    @Serializable
    @SerialName("turn_started")
    data object TurnStarted : AgentStreamEvent

    /** ACP `session/prompt` response (end_turn, max_tokens, refusal, cancelled...). */
    @Serializable
    @SerialName("turn_ended")
    data class TurnEnded(val stopReason: String) : AgentStreamEvent

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : AgentStreamEvent

    /** The whole run (including commit, push and PR) is done; the stream closes. */
    @Serializable
    @SerialName("done")
    data class Done(val status: AgentStatus) : AgentStreamEvent
}
