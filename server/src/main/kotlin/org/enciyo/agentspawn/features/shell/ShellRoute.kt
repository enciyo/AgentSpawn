@file:OptIn(ExperimentalKtorApi::class)

package org.enciyo.agentspawn.features.shell

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import org.enciyo.agentspawn.api.RunShellRequest
import org.enciyo.agentspawn.getOrRespondError
import org.koin.ktor.ext.inject


private const val PATH = "shell"

fun Route.shellRoute() = route(PATH) {
    val service by inject<ShellService>()
    post("create") { create(service) }
}.describe {
    tag(PATH)
}

private suspend fun RoutingContext.create(service: ShellService) {
    val request = call.receive<RunShellRequest>()
    val result = service.runSyncCommand(request.command, request.timeout).getOrRespondError() ?: return
    call.respond(result)
}
