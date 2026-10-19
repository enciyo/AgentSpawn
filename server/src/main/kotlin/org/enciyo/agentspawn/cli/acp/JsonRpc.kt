package org.enciyo.agentspawn.cli.acp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON-RPC 2.0 envelopes: https://www.jsonrpc.org/specification
 *
 * Every line from the agent is read as a [JsonRpcMessage]; which fields are set tells whether it is
 * a request (`method` + `id`), a notification (`method` only) or a response (`id` only).
 */

@Serializable
internal data class JsonRpcMessage(
    val id: JsonElement? = null,
    val method: String? = null,
    val params: JsonObject? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class JsonRpcRequest(
    val id: Long,
    val method: String,
    val params: JsonObject,
    @EncodeDefault val jsonrpc: String = JSON_RPC_VERSION,
)

/** Response to an agent request; exactly one of [result] and [error] must be set. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class JsonRpcResponse(
    val id: JsonElement,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
    @EncodeDefault val jsonrpc: String = JSON_RPC_VERSION,
)

@Serializable
internal data class JsonRpcError(
    val code: Int,
    val message: String = "",
    val data: JsonElement? = null,
) {
    fun describe(): String {
        val detail = (data as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
        return listOfNotNull(message, detail).joinToString(": ")
    }

    companion object {
        const val METHOD_NOT_FOUND = -32601
    }
}

private const val JSON_RPC_VERSION = "2.0"
