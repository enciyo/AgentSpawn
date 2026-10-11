@file:OptIn(ExperimentalKtorApi::class)

package org.enciyo.agentspawn.features.github

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.enciyo.agentspawn.api.GithubBranchRequest
import org.enciyo.agentspawn.getOrRespondError
import org.koin.ktor.ext.inject

private const val PATH = "/github"

fun Route.githubRoute() = route(PATH) {
    val service by inject<GithubService>()
    get("status") { status(service) }
    get("login") { login(service) }
    get("logout") { logout(service) }
    get("repo") { repositories(service) }
    get("repo/search") { search(service) }
    post("branch") { branch(service) }
    get("token") { token(service) }
}
    .describe { tag(PATH) }

private suspend fun RoutingContext.status(service: GithubService) {
    val result = service.status().getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.login(service: GithubService) {
    val result = service.login().getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.logout(service: GithubService) {
    val result = service.logout().getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.repositories(service: GithubService) {
    val result = service.repositories().getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.search(service: GithubService) {
    val q = call.request.queryParameters["q"] ?: ""
    val limit = call.request.queryParameters["limit"]?.toInt() ?: 100
    val result = service.searchRepos(q, limit).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.branch(service: GithubService) {
    val request = call.receive<GithubBranchRequest>()
    val result = service.searchBranches(request.query, request.repository).getOrRespondError() ?: return
    call.respond(result)
}

private suspend fun RoutingContext.token(service: GithubService) {
    val result = service.token().getOrRespondError() ?: return
    call.respond(result)
}
