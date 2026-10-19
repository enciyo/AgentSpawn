package org.enciyo.agentspawn.cli.agent

import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.cli.providers.ClaudeCli
import org.enciyo.agentspawn.cli.providers.CursorCli




interface AgentProviderFactory {
    fun provide(provider: AgentProvider): AgentCli
}

class DefaultAgentProviderFactory(
    cursorCli: CursorCli,
    claudeCli: ClaudeCli,
) : AgentProviderFactory {
    private val providers = mutableMapOf<AgentProvider, AgentCli>(
        AgentProvider.Cursor to cursorCli,
        AgentProvider.Claude to claudeCli,
    )

    override fun provide(provider: AgentProvider): AgentCli {
        return providers[provider] ?: throw IllegalArgumentException(
            "Provider not supported: ${provider::class.simpleName}"
        )
    }
}


context(ctx: AgentProviderFactory)
fun String.provide() = ctx.provide(AgentProvider.fromString(this))

