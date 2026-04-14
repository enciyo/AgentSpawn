package org.enciyo.agentspawn.features.agent

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.*
import org.enciyo.agentspawn.cli.Prompt
import org.enciyo.agentspawn.cli.agent.AgentCli
import org.enciyo.agentspawn.cli.agent.AgentProviderFactory
import org.enciyo.agentspawn.cli.agent.provide
import org.enciyo.agentspawn.features.agent.mapper.toAgentContentDto
import org.enciyo.agentspawn.features.agent.model.Agent
import org.enciyo.agentspawn.features.github.GithubService
import org.enciyo.agentspawn.ifNullOrEmpty
import java.util.*


class AgentService(
    providerFactory: AgentProviderFactory,
    scope: CoroutineScope,
    private val agentRepository: AgentRepository,
    private val githubService: GithubService,
) : AgentProviderFactory by providerFactory, CoroutineScope by scope {

    init {
        sync()
    }

    fun sync() {
        launch {
            val allAgents = agentRepository.allAgents()
            val cursor = AgentProvider.Cursor.name.provide()
            val agentsChats = cursor.listChats().getOrElse { emptyList() }
            val toRemove = allAgents.filterNot { agentsChats.contains(it.id) }
            agentRepository.removeAgens(toRemove.map { it.id })
        }
    }

    fun clear() {
        launch {
            val cursor = AgentProvider.Cursor.name.provide()
            cursor.clear()
            sync()
        }
    }

    suspend fun chats() = either {
        runCatching {
            agentRepository.allAgents()
        }.getOrElse {
            raise(AppError.Internal(""))
        }
    }

    suspend fun agentById(id: String): Either<AppError, AgentContentDto> = either {
        val agent = agentRepository.agentById(id) ?: raise(AppError.NotFound("Agent not found: $id"))
        val provider = agent.provider.provide()
        val messages = provider.getChatContent(id).getOrNull()?.toAgentContentDto()
        AgentContentDto(
            chatId = agent.id,
            messages = messages ?: emptyList(),
            status = agent.status,
            repository = agent.repository,
            branch = agent.branchName
        )
    }

    suspend fun create(request: CreateAgentRequest): Either<AppError, AgentDto> = either {
        val agent = Agent(
            name = request.branchName,
            chatId = UUID.randomUUID().toString(),
            text = request.prompt.trim(),
            repository = request.repository,
            ref = request.ref,
            branchName = request.branchName,
            provider = request.provider,
            status = AgentStatus.PENDING,
            lastModel = request.model
        )
        val saved = agentRepository.addAgent(agent)
        launch {
            agentRepository.updateStatus(saved.id, AgentStatus.RUNNING)
            provision(saved.id, request)
            agentRepository.updateStatus(saved.id, AgentStatus.COMPLETED)
        }
        saved
    }

    private suspend fun provision(chatId: String, request: CreateAgentRequest) = either {
        // Cloning
        githubService.clone(id = chatId, repoUrl = request.repository).bind()
        // Agent resume chat
        val provider = request.provider.provide()
        val isPushed = resumeProvision(
            provider = provider,
            chatId = chatId,
            prompt = request.prompt,
            branchName = request.branchName,
            ref = request.ref,
            model = request.model,
        ).bind()
        if (isPushed.not()) return@either null
        val pushBody = provider.prompt(id = chatId, prompt = Prompt.generatePullRequestBodyPrompt).bind()
        val title = provider.prompt(id = chatId, prompt = Prompt.generatePullRequestTitlePrompt).bind()
        githubService.createPullRequest(
            repoUrl = request.repository,
            title = title.output,
            body = pushBody.output,
            head = request.branchName,
            base = request.ref
        )
    }.onLeft {
        agentRepository.updateStatus(chatId, AgentStatus.FAILED)
    }

    private suspend fun resumeProvision(
        provider: AgentCli,
        chatId: String,
        prompt: String,
        branchName: String,
        ref: String,
        model: String
    ) = either {
        githubService.checkout(id = chatId, branch = branchName, ref = ref).bind()
        provider.resumeChat(chatId, prompt, model).bind()
        val hasChangeList = githubService.hasChangelist(chatId).bind()
        if (!hasChangeList) return@either false
        val commitMessage = provider.prompt(chatId, Prompt.generateCommitMessagePrompt).bind()
        githubService.push(id = chatId, message = commitMessage.output, branch = branchName).bind()
        true
    }
        .onLeft {
            agentRepository.updateStatus(chatId, AgentStatus.FAILED)
        }


    suspend fun resume(request: ResumeAgentRequest) = either {
        val agent = agentRepository.agentById(request.id) ?: raise(AppError.NotFound("Agent not found: ${request.id}"))
        if (agent.status.isActive) raise(AppError.Conflict("Agent is already running: ${request.id}"))
        launch {
            agentRepository.updateStatus(agent.id, AgentStatus.RUNNING)
            resumeProvision(
                provider = agent.provider.provide(),
                chatId = agent.id,
                prompt = request.prompt,
                branchName = agent.branchName,
                ref = agent.ref,
                model = request.model.ifEmpty { agent.lastModel },
            )
            agentRepository.updateStatus(agent.id, AgentStatus.COMPLETED)
        }
        agent.copy(status = AgentStatus.RUNNING)
    }

    suspend fun logout(provider: String) = provider.provide().logout()

    suspend fun status(provider: String) = either {
        val status = provider.provide().status().bind()
        val delimiter = "Logged in as "
        if (!status.output.contains(delimiter)) raise(AppError.Unauthorized("Not logged in"))
        status.copy(
            output = status.output.substringAfter("Logged in as ").substringBefore("\n")
        )
    }

    suspend fun login(provider: String) = provider.provide().login()

    suspend fun models(provider: String) = provider.provide().models()

}
