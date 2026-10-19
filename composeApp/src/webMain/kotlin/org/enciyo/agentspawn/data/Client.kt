package org.enciyo.agentspawn.data

import arrow.core.raise.either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.enciyo.agentspawn.api.AgentContentDto
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.api.AgentStreamEvent
import org.enciyo.agentspawn.api.CreateAgentRequest
import org.enciyo.agentspawn.api.GithubBranchRequest
import org.enciyo.agentspawn.api.GithubLogin
import org.enciyo.agentspawn.api.GithubStatus
import org.enciyo.agentspawn.api.McpConfigDto
import org.enciyo.agentspawn.api.ResumeAgentRequest

class Client(private val httpClient: HttpClient) {

    private val streamJson = Json { ignoreUnknownKeys = true }

    suspend fun listAgents(provider: AgentProvider) = fetch<List<AgentDto>> {
        httpClient.get("agents/list") {
            parameter("provider", provider.name)
        }
    }

    suspend fun createAgent(request: CreateAgentRequest) = fetch<AgentDto> {
        httpClient.post("agents/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getAgent(id: String) = fetch<AgentContentDto> {
        httpClient.get("agents/$id")
    }

    fun streamAgent(id: String): Flow<AgentStreamEvent> = channelFlow {
        httpClient.sse("agents/$id/stream") {
            incoming.collect { event ->
                val data = event.data ?: return@collect
                send(streamJson.decodeFromString(AgentStreamEvent.serializer(), data))
            }
        }
    }

    suspend fun resumeAgent(request: ResumeAgentRequest) = fetch<AgentDto> {
        httpClient.post("agents/resume") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun statusAgent(provider: AgentProvider) = fetch<ShellResult> {
        httpClient.get("agents/status") {
            parameter("provider", provider.name)
        }
    }


    suspend fun loginAgent(provider: AgentProvider) = fetch<ShellResult> {
        httpClient.get("agents/login") {
            parameter("provider", provider.name)
        }
    }

    suspend fun logoutAgent(provider: AgentProvider) = fetch<ShellResult> {
        httpClient.get("agents/logout") {
            parameter("provider", provider.name)
        }
    }

    suspend fun agentModels(provider: AgentProvider) = fetch<List<String>> {
        httpClient.get("agents/models") {
            parameter("provider", provider.name)
        }
    }

    suspend fun statusGithub() = fetch<List<GithubStatus>> {
        httpClient.get("github/status")
    }

    suspend fun loginGithub() = fetch<GithubLogin> {
        httpClient.get("github/login")
    }

    suspend fun logoutGithub() = fetch<Boolean> {
        httpClient.get("github/logout")
    }

    suspend fun repositories() = fetch<List<String>> {
        httpClient.get("github/repo")
    }

    suspend fun searchRepo(q: String) = fetch<List<String>> {
        httpClient.get("github/repo/search") {
            parameter("q", q)
        }
    }

    suspend fun tokenGithub() = fetch<String> {
        httpClient.get("github/token")
    }

    suspend fun searchBranch(q: String, repo: String) = fetch<List<String>> {
        httpClient.post("github/branch") {
            contentType(ContentType.Application.Json)
            setBody(GithubBranchRequest(repo, q))
        }
    }

    suspend fun getMcp() = fetch<McpConfigDto> {
        httpClient.get("mcp")
    }

    suspend fun putMcp(config: McpConfigDto) = fetch<McpConfigDto> {
        httpClient.put("mcp") {
            contentType(ContentType.Application.Json)
            setBody(config)
        }
    }

    private suspend inline fun <reified T> fetch(block: suspend () -> HttpResponse) = either {
        runCatching {
            val response = block()
            when (response.status.value) {
                in 200..299 -> response.body<T>()
                else -> {
                    val error = response.bodyAsText()
                    val message =
                        runCatching { Json.parseToJsonElement(error).jsonObject["message"]?.jsonPrimitive?.content }
                            .getOrElse { error }

                    throw Exception("HTTP ${response.status.value}: $message")
                }
            }
        }
            .getOrElse { e -> raise(e) }
    }

}