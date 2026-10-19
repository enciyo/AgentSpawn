package org.enciyo.agentspawn.features.agent

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import org.enciyo.agentspawn.api.AgentStreamEvent
import kotlin.time.Duration.Companion.seconds
import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.api.CreateAgentRequest
import org.enciyo.agentspawn.api.ResumeAgentRequest
import org.enciyo.agentspawn.getOrRespondError
import org.enciyo.agentspawn.respondError
import org.koin.ktor.ext.get

private const val PATH = "agents"

@ExperimentalKtorApi
fun Route.agentRoute() = route(PATH) {
    val service = get<AgentService>()
    get("status") { status(service) }
    get("login") { login(service) }
    get("logout") { logout(service) }
    get("list") { list(service) }
    get("{id}") { get(service) }
    sse("{id}/stream") { stream(service) }
    post("create") { create(service) }
    post("resume") { resume(service) }
    get("models") { models(service) }
    get("clear") { clear(service) }
}.describe { tag(PATH) }

private suspend fun RoutingContext.status(service: AgentService) {
    val provider = requiredProvider()
    val status = service.status(provider).getOrRespondError() ?: return
    call.respond(status)
}

private suspend fun RoutingContext.logout(service: AgentService) {
    val provider = requiredProvider()
    val result = service.logout(provider).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.login(service: AgentService) {
    val provider = requiredProvider()
    val result = service.login(provider).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.list(service: AgentService) {
    val result = service.chats().getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.get(service: AgentService) {
    val id = requiredId() ?: return
    val result = service.agentById(id).getOrRespondError() ?: return
    call.respond(result)
}

/** Streams agent run events as SSE; each event's `data` field is an [AgentStreamEvent] as JSON. */
private suspend fun ServerSSESession.stream(service: AgentService) {
    heartbeat {
        period = 15.seconds
        event = ServerSentEvent(comments = "ping")
    }
    val id = call.parameters["id"].orEmpty()
    service.stream(id).fold(
        ifLeft = { send(AgentStreamEvent.Error(it.message)) },
        ifRight = { events -> events.collect { send(it) } },
    )
}

private suspend fun ServerSSESession.send(event: AgentStreamEvent) =
    send(ServerSentEvent(data = Json.encodeToString(AgentStreamEvent.serializer(), event)))

private suspend fun RoutingContext.create(service: AgentService) {
    val request = call.receive<CreateAgentRequest>()
    val result = service.create(request).getOrRespondError() ?: return
    call.respond(HttpStatusCode.Accepted, result)
}

private suspend fun RoutingContext.resume(service: AgentService) {
    val request = call.receive<ResumeAgentRequest>()
    val result = service.resume(request).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.models(service: AgentService) {
    val provider = requiredProvider()
    val result = service.models(provider).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.clear(service: AgentService) {
    val result = service.clear()
    call.respond(HttpStatusCode.OK)
}


private suspend fun RoutingContext.requiredId(): String? = call.parameters["id"] ?: run {
    call.respondError(HttpStatusCode.BadRequest, "Missing job ID")
    null
}

private fun RoutingContext.requiredProvider(): String =
    call.request.queryParameters["provider"] ?: AgentProvider.Cursor.name