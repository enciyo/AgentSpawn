package org.enciyo.agentspawn.api

enum class AgentProvider {
    Cursor,
    Claude;


    companion object {
        fun fromString(string: String): AgentProvider {
            return entries.find { it.name.equals(string, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown provider: $string")
        }
    }
}
