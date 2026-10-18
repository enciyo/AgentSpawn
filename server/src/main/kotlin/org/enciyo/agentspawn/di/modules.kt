package org.enciyo.agentspawn.di

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.config.HoconApplicationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.enciyo.agentspawn.cli.providers.CursorCli
import org.enciyo.agentspawn.cli.FileCli
import org.enciyo.agentspawn.cli.GitCli
import org.enciyo.agentspawn.cli.GitHubCli
import org.enciyo.agentspawn.cli.ShellRunner
import org.enciyo.agentspawn.cli.SqlLiteCli
import org.enciyo.agentspawn.cli.agent.AgentProviderFactory
import org.enciyo.agentspawn.cli.agent.DefaultAgentProviderFactory
import org.enciyo.agentspawn.configs.AppConfig
import org.enciyo.agentspawn.features.agent.AgentRepository
import org.enciyo.agentspawn.features.agent.AgentRepositoryImpl
import org.enciyo.agentspawn.features.agent.AgentService
import org.enciyo.agentspawn.features.github.GithubService
import org.enciyo.agentspawn.features.mcp.McpService
import org.enciyo.agentspawn.features.shell.ShellService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module


val Application.appModule
    get() =
        module {
            single { CoroutineScope(SupervisorJob()) }

            //Cli
            singleOf(::SqlLiteCli)
            singleOf(::GitCli)
            singleOf(::FileCli)
            singleOf(::GitHubCli)
            singleOf(::CursorCli)

            //Services
            singleOf(::ShellService) bind ShellRunner::class
            singleOf(::AgentService)
            singleOf(::GithubService)
            singleOf(::McpService)

            singleOf(::AgentRepositoryImpl) bind AgentRepository::class
            singleOf(::DefaultAgentProviderFactory) bind AgentProviderFactory::class

            single {
                val env = System.getenv("KTOR_ENV")
                    ?: this@appModule.environment.config.propertyOrNull("ktor.environment")?.getString()
                    ?: "development"
                val config = HoconApplicationConfig(ConfigFactory.load("application-$env.conf"))
                AppConfig.from(config)
            }

        }