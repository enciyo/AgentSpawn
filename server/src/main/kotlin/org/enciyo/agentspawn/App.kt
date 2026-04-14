package org.enciyo.agentspawn

import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.enciyo.agentspawn.api.ErrorResponse

@Serializable
sealed interface AppError {
    val message: String

    @Serializable
    data class NotFound(override val message: String) : AppError

    @Serializable
    data class BadRequest(override val message: String) : AppError

    @Serializable
    data class Conflict(override val message: String) : AppError

    @Serializable
    data class Validation(override val message: String, val code: String? = null) : AppError

    @Serializable
    data class Internal(override val message: String) : AppError

    @Serializable
    data class Unauthorized(override val message: String) : AppError
}

fun AppError.toHttpStatus(): HttpStatusCode = when (this) {
    is AppError.NotFound -> HttpStatusCode.NotFound
    is AppError.BadRequest -> HttpStatusCode.BadRequest
    is AppError.Conflict -> HttpStatusCode.Conflict
    is AppError.Validation -> HttpStatusCode.UnprocessableEntity
    is AppError.Internal -> HttpStatusCode.InternalServerError
    is AppError.Unauthorized -> HttpStatusCode.Unauthorized
}

fun AppError.toErrorResponse(): ErrorResponse = when (this) {
    is AppError.Validation -> ErrorResponse(message = message, code = code)
    else -> ErrorResponse(message = message)
}
