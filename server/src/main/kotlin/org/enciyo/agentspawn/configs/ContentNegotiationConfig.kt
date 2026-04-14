package org.enciyo.agentspawn.configs

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig
import kotlinx.serialization.json.Json


fun ContentNegotiationConfig.handle() {
    json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    })
}