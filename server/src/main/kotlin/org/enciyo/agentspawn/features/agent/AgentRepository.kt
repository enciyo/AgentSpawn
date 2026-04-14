package org.enciyo.agentspawn.features.agent

import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentStatus
import org.enciyo.agentspawn.features.agent.dao.AgentDao

import org.enciyo.agentspawn.features.agent.dao.entity.AgentTable
import org.enciyo.agentspawn.features.agent.mapper.toAgentDto
import org.enciyo.agentspawn.features.agent.model.Agent

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction


interface AgentRepository {
    suspend fun allAgents(): List<AgentDto>
    suspend fun agentById(id: String): AgentDto?
    suspend fun addAgent(agent: Agent): AgentDto
    suspend fun removeAgens(ids: List<String>)
    suspend fun update(copy: AgentDto): AgentDto
    suspend fun updateStatus(chatId: String, status: AgentStatus): AgentDto
}

class AgentRepositoryImpl : AgentRepository {
    override suspend fun allAgents(): List<AgentDto> =
        suspendTransaction {
            AgentDao.all()
                .orderBy(AgentTable.id to SortOrder.DESC)
                .map(AgentDao::toAgentDto)
        }

    override suspend fun agentById(id: String): AgentDto? = suspendTransaction {
        AgentDao.find { (AgentTable.chatId eq id) }
            .limit(1)
            .firstOrNull()
            ?.let(AgentDao::toAgentDto)
    }

    override suspend fun addAgent(agent: Agent) = suspendTransaction {
        AgentDao.new {
            name = agent.name
            chatId = agent.chatId
            text = agent.text
            repository = agent.repository
            ref = agent.ref
            branchName = agent.branchName
            pUri = agent.prUri
            provider = agent.provider
            status = agent.status.name
            lastModel = agent.lastModel
        }
            .toAgentDto()
    }

    override suspend fun removeAgens(ids: List<String>) {
        suspendTransaction {
            AgentDao.find { (AgentTable.chatId inList ids) }
                .forEach(AgentDao::delete)
        }
    }

    override suspend fun update(copy: AgentDto) = suspendTransaction {
        val agent = AgentDao.find { (AgentTable.chatId eq copy.id) }
            .limit(1)
            .firstOrNull() ?: return@suspendTransaction copy
        return@suspendTransaction agent.toAgentDto()
    }

    override suspend fun updateStatus(chatId: String, status: AgentStatus): AgentDto =
        suspendTransaction {
            AgentDao.findSingleByAndUpdate(AgentTable.chatId eq chatId) {
                it.status = status.name
            }
                ?.toAgentDto()
                ?: throw IllegalArgumentException("Agent with chatId $chatId not found")
        }
}
