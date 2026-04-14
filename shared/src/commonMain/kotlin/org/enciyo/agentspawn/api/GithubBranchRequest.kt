package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable


@Serializable
data class GithubBranchRequest(val repository: String, val query: String)
