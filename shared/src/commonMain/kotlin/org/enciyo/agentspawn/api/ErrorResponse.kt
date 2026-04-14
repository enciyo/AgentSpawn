package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String,
    val code: String? = null,
    val details: String? = null
)
