@file:OptIn(
    io.ktor.utils.io.ExperimentalKtorApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class,
)

package org.enciyo.agentspawn

import io.github.smiley4.ktorredoc.redoc
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.apikey.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.swagger.codegen.v3.generators.html.StaticHtmlCodegen
import org.enciyo.agentspawn.configs.AppConfig
import org.enciyo.agentspawn.configs.handle
import org.enciyo.agentspawn.di.appModule
import org.enciyo.agentspawn.features.agent.agentRoute
import org.enciyo.agentspawn.features.github.githubRoute
import org.enciyo.agentspawn.features.mcp.mcpRoute
import org.enciyo.agentspawn.features.shell.shellRoute
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.KoinKtorApplication
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    EngineMain.main(args)
}

suspend fun Application.module() {
    install(Koin) { handle() }
    val appConfig by inject<AppConfig>()
    configurePlugins(appConfig)
    configureDatabases(appConfig)
    configureRouting(appConfig)
}

private fun Application.configurePlugins(appConfig: AppConfig) {
    install(ContentNegotiation) { handle() }
    install(CORS) { handle() }
    install(RequestValidation) { handle() }
    install(StatusPages) { handle() }
    if (appConfig.isAuthEnable) {
        install(Authentication) { handle(appConfig) }
    }
}

context(application: Application)
private fun KoinKtorApplication.handle() {
    slf4jLogger()
    modules(application.appModule)
}

private fun AuthenticationConfig.handle(appConfig: AppConfig) {
    apiKey {
        validate { it.takeIf { it == appConfig.apiKey } }
        challenge { call ->
            call.respond(
                HttpStatusCode.Unauthorized,
                AppError.Unauthorized("You are not authorized to perform this action")
            )
        }
    }
}

private fun Routing.authenticateIfEnabled(
    config: AppConfig,
    block: Route.() -> Unit
) {
    when {
        config.isAuthEnable -> authenticate { block() }
        else -> block()
    }
}


private fun Application.configureRouting(appConfig: AppConfig) {
    routing {
        get("/") { call.respondRedirect("/swagger", permanent = true) }
        authenticateIfEnabled(appConfig) {
            shellRoute()
            agentRoute()
            githubRoute()
            mcpRoute()
            swaggerUI(path = "swagger") { handle() }
            openAPI(path = "openapi") { handle() }
            route("redoc") {
                redoc(openApiUrl = "openapi")
            }
        }
    }
}

private fun SwaggerConfig.handle() {
    version = "5.31.0"
    source = OpenApiDocSource.Routing(ContentType.Application.Json) { routingRoot.descendants() }
}

private fun OpenAPIConfig.handle() {
    openapiVersion = "3.1.0"
    codegen = StaticHtmlCodegen()
    source = OpenApiDocSource.Routing(ContentType.Application.Json) { routingRoot.descendants() }
}


private fun Application.configureDatabases(appConfig: AppConfig) {
    val jdbcUrl = appConfig.database.jdbcUrl
    val user = appConfig.database.user
    val password = appConfig.database.password

    Flyway.configure()
        .dataSource(jdbcUrl, user, password)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(
        url = jdbcUrl,
        user = user,
        password = password
    )
}