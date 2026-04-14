package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable

@Serializable
data class AuthStatusResponse(
    val isAuthenticated: Boolean,
    val email: String
)

