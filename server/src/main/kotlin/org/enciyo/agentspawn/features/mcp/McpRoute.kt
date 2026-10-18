@file:OptIn(ExperimentalKtorApi::class)

package org.enciyo.agentspawn.features.mcp

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.enciyo.agentspawn.api.McpConfigDto
import org.enciyo.agentspawn.api.UpsertMcpServerRequest
import org.enciyo.agentspawn.getOrRespondError
import org.enciyo.agentspawn.respondError
import io.ktor.http.HttpStatusCode
import org.koin.ktor.ext.inject

private const val PATH = "mcp"

fun Route.mcpRoute() = route(PATH) {
    val service by inject<McpService>()
    get { getConfig(service) }
    put { putConfig(service) }
    post("servers") { upsertServer(service) }
    delete("servers/{name}") { deleteServer(service) }
}.describe { tag(PATH) }

private suspend fun RoutingContext.getConfig(service: McpService) {
    val result = service.get().getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.putConfig(service: McpService) {
    val body = call.receive<McpConfigDto>()
    val result = service.put(body).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.upsertServer(service: McpService) {
    val body = call.receive<UpsertMcpServerRequest>()
    val result = service.upsert(body).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.deleteServer(service: McpService) {
    val name = call.parameters["name"] ?: run {
        call.respondError(HttpStatusCode.BadRequest, "Missing MCP server name")
        return
    }
    val result = service.delete(name).getOrRespondError() ?: return
    call.respond(result)
}
