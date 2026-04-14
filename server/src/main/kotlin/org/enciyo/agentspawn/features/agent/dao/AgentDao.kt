package org.enciyo.agentspawn.features.agent.dao

import org.enciyo.agentspawn.features.agent.dao.entity.AgentTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class AgentDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AgentDao>(AgentTable)
    var name by AgentTable.name
    var chatId by AgentTable.chatId
    var text by AgentTable.text
    var repository by AgentTable.repository
    var ref by AgentTable.ref
    var branchName by AgentTable.branchName
    var pUri by AgentTable.prUri
    var provider by AgentTable.provider
    var status by AgentTable.status
    var lastModel by AgentTable.lastModel

}