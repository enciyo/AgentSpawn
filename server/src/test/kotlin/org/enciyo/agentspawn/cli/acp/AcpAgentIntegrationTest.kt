package org.enciyo.agentspawn.cli.acp

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.enciyo.agentspawn.api.AgentStreamEvent
import org.enciyo.agentspawn.api.acp.SessionUpdate
import org.enciyo.agentspawn.api.acp.ToolCallStatus
import org.enciyo.agentspawn.cli.providers.ClaudeCli
import org.junit.Assume.assumeTrue
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end ACP test against real agents; requires an installed, logged-in CLI and consumes quota.
 * To run: `ACP_IT=1 ./gradlew :server:test --tests '*AcpAgentIntegrationTest*'`
 */
class AcpAgentIntegrationTest {

    @Test
    fun cursor() = verify(AcpAgent(command = "agent acp"))

    @Test
    fun claude() = verify(AcpAgent(command = ClaudeCli.ACP_COMMAND))

    private fun verify(agent: AcpAgent) = runBlocking {
        assumeTrue("Run with ACP_IT=1", System.getenv("ACP_IT") == "1")
        val cwd = Files.createTempDirectory("acp-it").toFile().canonicalPath
        File(cwd, "a.txt").writeText("hello\n")

        // Turn 1: new session; writing a file also exercises the permission request/auto-approve path
        var sessionId = ""
        val first = agent.prompt(
            cwd = cwd,
            sessionId = null,
            prompt = "Create a file named b.txt containing exactly the word world, then reply with one short sentence.",
            model = "",
            onSession = { sessionId = it },
        ).toList()

        assertTrue(sessionId.isNotBlank())
        assertEquals(AgentStreamEvent.TurnStarted, first.first())
        assertEquals(AgentStreamEvent.TurnEnded("end_turn"), first.last())
        val firstUpdates = first.updates()
        assertTrue(firstUpdates.first() is SessionUpdate.UserMessageChunk)
        assertTrue(firstUpdates.any { it is SessionUpdate.ToolCall })
        assertTrue(firstUpdates.any { it is SessionUpdate.ToolCallUpdate && it.status == ToolCallStatus.COMPLETED })
        assertTrue(firstUpdates.any { it is SessionUpdate.AgentMessageChunk })
        assertEquals("world", File(cwd, "b.txt").readText().trim())

        // Turn 2: session/load replays the history before TurnStarted
        val second = agent.prompt(cwd, sessionId, "Now reply with only the word done.", model = "").toList()
        val replay = second.takeWhile { it != AgentStreamEvent.TurnStarted }.updates()
        assertTrue(replay.any { it is SessionUpdate.UserMessageChunk })
        assertTrue(replay.any { it is SessionUpdate.AgentMessageChunk })
        assertEquals(AgentStreamEvent.TurnEnded("end_turn"), second.last())

        // History: the user messages of both turns
        val history = agent.history(cwd, sessionId)
        val userTexts = history.filterIsInstance<SessionUpdate.UserMessageChunk>().joinToString("") { it.content.text.orEmpty() }
        assertTrue(userTexts.contains("b.txt") && userTexts.contains("done"))

        assertTrue(agent.ask(cwd, "Reply with only the word pong.", model = "auto").contains("pong", ignoreCase = true))
        assertTrue(agent.models(cwd).isNotEmpty())
    }

    private fun List<AgentStreamEvent>.updates() = filterIsInstance<AgentStreamEvent.Update>().map { it.update }
}
