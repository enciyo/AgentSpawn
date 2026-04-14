package org.enciyo.agentspawn.features.agent.mapper

import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentStatus
import org.enciyo.agentspawn.features.agent.dao.AgentDao

fun AgentDao.toAgentDto() = AgentDto(
    id = this.chatId,
    title = this.text,
    status = AgentStatus.valueOf(this.status),
    provider = this.provider,
    branchName = this.branchName,
    ref = this.ref,
    repository = this.repository,
    lastModel = this.lastModel.orEmpty()
)
