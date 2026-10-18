package org.enciyo.agentspawn.configs

import io.ktor.server.plugins.requestvalidation.RequestValidationConfig
import io.ktor.server.plugins.requestvalidation.ValidationResult
import org.enciyo.agentspawn.api.CreateAgentRequest
import org.enciyo.agentspawn.api.RunShellRequest
import org.enciyo.agentspawn.api.UpsertMcpServerRequest


fun RequestValidationConfig.handle() {
    validate<RunShellRequest> { request ->
        when {
            request.command.isEmpty() -> ValidationResult.Invalid("Command cannot be blank")
            request.timeout < 0 -> ValidationResult.Invalid("Timeout cannot be negative")
            else -> ValidationResult.Valid
        }
    }
    validate<CreateAgentRequest> { request ->
        when {
            request.prompt.isBlank() -> ValidationResult.Invalid("Prompt cannot be blank")
            request.ref.isBlank() -> ValidationResult.Invalid("Ref cannot be blank")
            request.repository.isBlank() -> ValidationResult.Invalid("Repository cannot be blank")
            request.model.isEmpty() -> ValidationResult.Invalid("Model cannot be blank")
            else -> ValidationResult.Valid
        }
    }
    validate<UpsertMcpServerRequest> { request ->
        when {
            request.name.isBlank() -> ValidationResult.Invalid("MCP server name cannot be blank")
            else -> ValidationResult.Valid
        }
    }
}