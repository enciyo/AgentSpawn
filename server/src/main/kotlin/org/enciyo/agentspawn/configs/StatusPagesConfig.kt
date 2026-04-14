package org.enciyo.agentspawn.configs

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import org.enciyo.agentspawn.AppError

@ExperimentalSerializationApi
fun StatusPagesConfig.handle() {
    exception<RequestValidationException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            AppError.Validation(message = cause.message ?: "Validation error"),
        )
    }
    exception<BadRequestException> { call, cause ->
        call.respond(
            HttpStatusCode.UnprocessableEntity,
            AppError.Validation(message = cause.resolveMessage()),
        )
    }
}

@ExperimentalSerializationApi
private fun BadRequestException.resolveMessage(): String {
    val chain = generateSequence(this as Throwable) { it.cause }
    val missing = chain.filterIsInstance<MissingFieldException>().firstOrNull()
    val serialization = chain.filterIsInstance<SerializationException>().firstOrNull()
    return when {
        missing != null -> "Missing required field(s): ${missing.missingFields.joinToString()}"
        serialization != null -> serialization.toReadableMessage()
        else -> "Invalid request body: $message"
    }
}

private fun SerializationException.toReadableMessage(): String {
    val raw = message ?: return "Invalid field value"
    val field = Regex("""at path: \$\.(\w+)""").find(raw)?.groupValues?.get(1)
    return when {
        "Expected quotation mark" in raw && field != null -> "Field \"$field\" must be a string"
        "Expected int literal" in raw && field != null -> "Field \"$field\" must be an integer"
        "Expected boolean literal" in raw && field != null -> "Field \"$field\" must be a boolean"
        field != null -> "Invalid value for field \"$field\""
        else -> "Invalid field value"
    }
}

