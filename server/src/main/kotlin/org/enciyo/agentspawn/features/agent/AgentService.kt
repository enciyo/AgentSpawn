package org.enciyo.agentspawn.features.agent

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.TtlCache
import org.enciyo.agentspawn.api.AgentContentDto
import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.api.AgentStatus
import org.enciyo.agentspawn.api.AgentStreamEvent
import org.enciyo.agentspawn.api.CreateAgentRequest
import org.enciyo.agentspawn.api.ResumeAgentRequest
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.api.acp.SessionUpdate
import org.enciyo.agentspawn.cli.Folders
import org.enciyo.agentspawn.cli.Prompt
import org.enciyo.agentspawn.cli.agent.AgentCli
import org.enciyo.agentspawn.cli.agent.AgentProviderFactory
import org.enciyo.agentspawn.cli.agent.provide
import org.enciyo.agentspawn.features.agent.model.Agent
import org.enciyo.agentspawn.features.github.GithubService
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class AgentService(
    providerFactory: AgentProviderFactory,
    scope: CoroutineScope,
    private val agentRepository: AgentRepository,
    private val githubService: GithubService,
    private val streamHub: AgentStreamHub,
) : AgentProviderFactory by providerFactory, CoroutineScope by scope {

    // Each one starts a CLI/ACP process; cached for a while since the frontend asks often
    private val statusCache = TtlCache<String, ShellResult>(1.minutes)
    private val modelsCache = TtlCache<String, List<String>>(30.minutes)
    private val historyCache = TtlCache<String, List<SessionUpdate>>(1.hours)

    init {
        sync()
    }

    /** Removes agents whose session was deleted on the provider from the DB; each provider is compared against its own session list. */
    fun sync() {
        launch {
            agentRepository.allAgents()
                .filterNot { it.status.isActive }
                .groupBy { it.provider }
                .forEach { (provider, agents) ->
                    val cli = runCatching { provider.provide() }.getOrNull() ?: return@forEach
                    // list failed or empty → don't wipe
                    val sessions = cli.listChats().getOrElse { return@forEach }
                    if (sessions.isEmpty()) return@forEach
                    val toRemove = agents.filterNot { it.sessionKey in sessions }
                    if (toRemove.isNotEmpty()) {
                        agentRepository.removeAgens(toRemove.map { it.id })
                    }
                }
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

    /** Returns empty history while the agent is running; the run's stream already carries it via the `session/load` replay. */
    suspend fun agentById(id: String): Either<AppError, AgentContentDto> = either {
        val agent = agentRepository.agentById(id) ?: raise(AppError.NotFound("Agent not found: $id"))
        val updates = when {
            agent.status.isActive -> emptyList()
            else -> streamHub.lastRun(id)
                ?: historyCache.getOrLoad(id) { acp { agent.provider.provide().acp.history(projectDir(id), agent.sessionKey) } }
                    .onLeft { println("[ACP] history unavailable for $id: ${it.message}") }
                    .getOrElse { emptyList() }
        }
        AgentContentDto(
            chatId = agent.id,
            updates = updates,
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
        launchRun(saved.id) {
            provision(saved.id, request)
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
            sessionId = null,
            prompt = request.prompt,
            branchName = request.branchName,
            ref = request.ref,
            model = request.model,
        ).bind()
        if (isPushed.not()) return@either null
        val pushBody = acp { provider.acp.ask(projectDir(chatId), Prompt.generatePullRequestBodyPrompt, ONE_OFF_MODEL) }.bind()
        val title = acp { provider.acp.ask(projectDir(chatId), Prompt.generatePullRequestTitlePrompt, ONE_OFF_MODEL) }.bind()
        githubService.createPullRequest(
            repoUrl = request.repository,
            title = title,
            body = pushBody,
            head = request.branchName,
            base = request.ref
        )
    }

    private suspend fun resumeProvision(
        provider: AgentCli,
        chatId: String,
        sessionId: String?,
        prompt: String,
        branchName: String,
        ref: String,
        model: String
    ) = either {
        githubService.checkout(id = chatId, branch = branchName, ref = ref).bind()
        runAgent(provider, chatId, sessionId, prompt, model).bind()
        val hasChangeList = githubService.hasChangelist(chatId).bind()
        if (!hasChangeList) return@either false
        val commitMessage = acp { provider.acp.ask(projectDir(chatId), Prompt.generateCommitMessagePrompt, ONE_OFF_MODEL) }.bind()
        githubService.push(id = chatId, message = commitMessage, branch = branchName).bind()
        true
    }

    suspend fun resume(request: ResumeAgentRequest) = either {
        val agent = agentRepository.agentById(request.id) ?: raise(AppError.NotFound("Agent not found: ${request.id}"))
        if (agent.status.isActive) raise(AppError.Conflict("Agent is already running: ${request.id}"))
        launchRun(agent.id) {
            resumeProvision(
                provider = agent.provider.provide(),
                chatId = agent.id,
                sessionId = agent.sessionKey,
                prompt = request.prompt,
                branchName = agent.branchName,
                ref = agent.ref,
                model = request.model.ifEmpty { agent.lastModel },
            )
        }
        agent.copy(status = AgentStatus.RUNNING)
    }

    /** Emits the active run's events if there is one, otherwise only the latest status. */
    suspend fun stream(id: String): Either<AppError, Flow<AgentStreamEvent>> = either {
        streamHub.subscribe(id)?.let { return@either it }
        val agent = agentRepository.agentById(id) ?: raise(AppError.NotFound("Agent not found: $id"))
        flowOf(AgentStreamEvent.Done(agent.status))
    }

    /**
     * Starts the run in the background and publishes its events to [streamHub].
     * The hub is opened before the request returns so the client can connect to the stream right after.
     */
    private fun launchRun(chatId: String, block: suspend () -> Either<AppError, *>) {
        historyCache.invalidate(chatId)
        streamHub.start(chatId)
        launch {
            var status = AgentStatus.FAILED
            try {
                agentRepository.updateStatus(chatId, AgentStatus.RUNNING)
                block()
                    .onLeft { streamHub.emit(chatId, AgentStreamEvent.Error(it.message)) }
                    .onRight { status = AgentStatus.COMPLETED }
            } finally {
                agentRepository.updateStatus(chatId, status)
                streamHub.finish(chatId, status)
            }
        }
    }

    private suspend fun runAgent(
        provider: AgentCli,
        chatId: String,
        sessionId: String?,
        prompt: String,
        model: String,
    ): Either<AppError, Unit> = either {
        var failure: String? = null
        provider.acp.prompt(
            cwd = projectDir(chatId),
            sessionId = sessionId,
            prompt = prompt,
            model = model,
            onSession = { if (it != sessionId) agentRepository.updateSessionId(chatId, it) },
        )
            .catch { failure = it.message ?: "Agent failed" }
            .collect { event ->
                streamHub.emit(chatId, event)
                if (event is AgentStreamEvent.TurnEnded && event.stopReason == "refusal") failure = "Agent refused the prompt"
            }
        failure?.let { raise(AppError.Internal(it)) }
    }

    suspend fun logout(provider: String) = provider.provide().logout()
        .also { statusCache.invalidate(provider) }

    /** Only a logged-in result is cached; while waiting for login, every poll asks the CLI. */
    suspend fun status(provider: String): Either<AppError, ShellResult> = statusCache.getOrLoad(provider) {
        either {
            val status = provider.provide().status().bind()
            val delimiter = "Logged in as "
            if (!status.output.contains(delimiter)) raise(AppError.Unauthorized("Not logged in"))
            status.copy(
                output = status.output.substringAfter("Logged in as ").substringBefore("\n")
            )
        }
    }

    suspend fun login(provider: String) = provider.provide().login()
        .also { statusCache.invalidate(provider) }

    // In Cursor every call leaves an empty session under ~/.cursor/acp-sessions; the cache limits that too
    suspend fun models(provider: String) = modelsCache.getOrLoad(provider) { acp { provider.provide().acp.models(Folders.HOME) } }

    private fun projectDir(chatId: String) = Folders.getProjectDir(chatId)

    private suspend fun <T> acp(block: suspend () -> T): Either<AppError, T> =
        Either.catch { block() }.mapLeft { AppError.Internal(it.message ?: "ACP request failed") }

    private companion object {
        // One-off questions such as commit messages / PR text; if the agent can't match the model it uses its own default
        const val ONE_OFF_MODEL = "auto"
    }
}

/** For pre-ACP agents the session id was the same as the chat id (`claude --session-id`, `agent --resume`). */
private val AgentDto.sessionKey: String
    get() = sessionId ?: id
