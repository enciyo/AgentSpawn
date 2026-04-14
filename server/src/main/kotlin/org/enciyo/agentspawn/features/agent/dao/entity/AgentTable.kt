package org.enciyo.agentspawn.features.agent.dao.entity

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable


object AgentTable : IntIdTable("agent") {
    val name = varchar("name", 50)
    val chatId = varchar("chatId", 255)
    val text = varchar("text", 255)
    val repository = varchar("repository", 255)
    val ref = varchar("ref", 255)
    val branchName = varchar("branchName", 255)
    val prUri = varchar("prUri", 255).nullable()
    val provider = varchar("provider", 255)
    val status = varchar("status", 50).default("PENDING")
    val lastModel = varchar("lastModel", 255).nullable()
}