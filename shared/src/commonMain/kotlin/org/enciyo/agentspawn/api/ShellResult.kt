package org.enciyo.agentspawn.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ShellResult(
    val output: String,
    val error: String,
    val exitCode: Int?,
    val isTimeout: Boolean
){

    override fun toString(): String {
        return Json.encodeToString(output)
    }
}