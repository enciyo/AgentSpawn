package org.enciyo.agentspawn.features.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transformWhile
import org.enciyo.agentspawn.api.AgentStatus
import org.enciyo.agentspawn.api.AgentStreamEvent
import org.enciyo.agentspawn.api.acp.SessionUpdate
import java.util.concurrent.ConcurrentHashMap

/**
 * Publishes the events of active agent runs.
 * All events are replayed for the duration of the run; a client that connects later receives the run from the start.
 * Since a run starts with `session/load`, the replay carries the full conversation; it is kept as history once the run ends.
 */
class AgentStreamHub {
    private val runs = ConcurrentHashMap<String, MutableSharedFlow<AgentStreamEvent>>()
    private val lastRuns = ConcurrentHashMap<String, List<SessionUpdate>>()

    fun start(id: String) {
        runs[id] = MutableSharedFlow(replay = MAX_REPLAY)
    }

    fun emit(id: String, event: AgentStreamEvent) {
        runs[id]?.tryEmit(event)
    }

    fun finish(id: String, status: AgentStatus) {
        emit(id, AgentStreamEvent.Done(status))
        runs.remove(id)?.let { run ->
            val updates = run.replayCache.filterIsInstance<AgentStreamEvent.Update>().map { it.update }
            // If the session never opened (e.g. clone failure), the history on disk is more accurate
            if (updates.isNotEmpty()) lastRuns[id] = updates
        }
    }

    /** Full conversation of the last run (history replay + new turn); avoids spawning a process for `session/load`. */
    fun lastRun(id: String): List<SessionUpdate>? = lastRuns[id]

    /** Null if there is no active run; otherwise a flow that ends with [AgentStreamEvent.Done]. */
    fun subscribe(id: String): Flow<AgentStreamEvent>? =
        runs[id]?.transformWhile { event ->
            emit(event)
            event !is AgentStreamEvent.Done
        }

    private companion object {
        const val MAX_REPLAY = 100_000
    }
}
