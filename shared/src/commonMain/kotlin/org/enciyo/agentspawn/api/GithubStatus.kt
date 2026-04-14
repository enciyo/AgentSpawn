package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable


@Serializable
data class GithubStatus(
    val name: String,
    val active: Boolean,
    val scopes: List<String>,
)


@Serializable
data class GithubLogin(
    val code: String,
    val openUrl: String
)