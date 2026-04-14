package org.enciyo.agentspawn

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.enciyo.agentspawn.api.ErrorResponse


fun <T> Result<T>.toEither(mapError: (Throwable) -> AppError = { defaultErrorMapper(it) }): Either<AppError, T> =
    fold(
        onSuccess = { it.right() },
        onFailure = { mapError(it).left() }
    )


context(ctx: RoutingContext)
suspend inline fun <reified T : Any> Either<AppError, T>.getOrRespondError(): T? {
    return fold(
        ifLeft = { error ->
            ctx.call.respond(error.toHttpStatus(), error.toErrorResponse())
            null
        },
        ifRight = { it }
    )
}

suspend fun RoutingCall.respondError(
    status: HttpStatusCode,
    message: String,
    code: String? = null
) {
    respond(status, ErrorResponse(message = message, code = code))
}

private fun defaultErrorMapper(e: Throwable): AppError = when (e) {
    is IllegalArgumentException -> AppError.BadRequest(e.message ?: "Bad request")
    is NoSuchElementException -> AppError.NotFound(e.message ?: "Not found")
    is IllegalStateException -> AppError.Conflict(e.message ?: "Conflict")
    else -> AppError.Internal(e.message ?: "Unknown error")
}

