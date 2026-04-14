package org.enciyo.agentspawn.configs

import io.ktor.server.config.*
import io.ktor.util.reflect.*


data class AppConfig(
    val isAuthEnable: Boolean,
    val apiKey: String,
    val database: DatabaseConfig,
) {
    companion object {
        fun from(config: ApplicationConfig) =
            AppConfig(
                isAuthEnable = config["settings.auth.enabled"],
                apiKey = config["settings.api_key"],
                database = DatabaseConfig.from(config)
            )
    }
}


data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
) {
    companion object {
        fun from(config: ApplicationConfig): DatabaseConfig =
            DatabaseConfig(
                host = config["database.host"],
                port = config["database.port"],
                name = config["database.name"],
                user = config["database.user"],
                password = config["database.password"],
            )
    }

    val jdbcUrl = "jdbc:postgresql://$host:$port/$name"

}

inline operator fun <reified T> ApplicationConfig.get(key: String): T {
    val value = property(key)
    return value.getAs(typeInfo<T>()) as T
}
