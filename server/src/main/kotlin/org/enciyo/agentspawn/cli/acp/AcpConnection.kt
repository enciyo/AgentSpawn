package org.enciyo.agentspawn.cli.acp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.enciyo.agentspawn.api.acp.SessionUpdate
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AcpException(message: String) : Exception(message)

class AcpConnection private constructor(
    private val process: Process,
    private val onUpdate: suspend (SessionUpdate) -> Unit,
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writer = process.outputStream.bufferedWriter()
    private val writeLock = Mutex()
    private val nextId = AtomicLong()
    private val pending = ConcurrentHashMap<Long, CompletableDeferred<JsonObject>>()
    private val stderrTail = ArrayDeque<String>()

    private fun startReading() {
        scope.launch {
            runInterruptible {
                process.errorStream.bufferedReader().forEachLine { line ->
                    synchronized(stderrTail) {
                        stderrTail.addLast(line)
                        if (stderrTail.size > STDERR_TAIL) stderrTail.removeFirst()
                    }
                }
            }
        }
        scope.launch {
            val reader = process.inputStream.bufferedReader()
            while (true) {
                val line = runInterruptible { reader.readLine() } ?: break
                val message = runCatching { json.decodeFromString(JsonRpcMessage.serializer(), line) }.getOrNull() ?: continue
                runCatching { dispatch(message) }
                    .onFailure { println("[ACP] dispatch failed: ${it.message}") }
            }
            val reason = AcpException("ACP agent exited: ${stderr().ifBlank { "no output" }}")
            pending.values.forEach { it.completeExceptionally(reason) }
            pending.clear()
        }
    }

    private suspend fun dispatch(message: JsonRpcMessage) {
        val method = message.method
        val id = message.id
        when {
            method != null && id != null -> respondToAgent(id, method, message.params)
            method == AcpMethod.SessionUpdate.value -> {
                val update = json.decodeFromJsonElement(SessionNotification.serializer(), message.params ?: return).update
                val type = update["sessionUpdate"]?.jsonPrimitive?.contentOrNull
                if (type in SessionUpdate.SUPPORTED) onUpdate(json.decodeFromJsonElement(SessionUpdate.serializer(), update))
            }

            method == null && id != null -> {
                val deferred = pending.remove((id as? JsonPrimitive)?.longOrNull ?: return) ?: return
                val error = message.error
                if (error != null) deferred.completeExceptionally(AcpException(error.describe()))
                else deferred.complete(message.result as? JsonObject ?: JsonObject(emptyMap()))
            }
        }
    }

    private suspend fun respondToAgent(id: JsonElement, method: String, params: JsonObject?) {
        val response = when (method) {
            AcpMethod.SessionRequestPermission.value -> {
                val options = params?.let { json.decodeFromJsonElement(RequestPermissionRequest.serializer(), it).options }.orEmpty()
                val option = PERMISSION_PREFERENCE.firstNotNullOfOrNull { kind ->
                    options.firstOrNull { it.kind == kind }
                } ?: options.firstOrNull()
                val outcome = option?.let { PermissionOutcome.Selected(it.optionId) } ?: PermissionOutcome.Cancelled
                JsonRpcResponse(
                    id = id,
                    result = json.encodeToJsonElement(RequestPermissionResponse.serializer(), RequestPermissionResponse(outcome)),
                )
            }

            else -> JsonRpcResponse(
                id = id,
                error = JsonRpcError(JsonRpcError.METHOD_NOT_FOUND, "Method not supported by client: $method"),
            )
        }
        write(JsonRpcResponse.serializer(), response)
    }

    suspend fun request(method: AcpMethod, params: JsonObject): JsonObject {
        val id = nextId.incrementAndGet()
        val deferred = CompletableDeferred<JsonObject>()
        pending[id] = deferred
        write(JsonRpcRequest.serializer(), JsonRpcRequest(id = id, method = method.value, params = params))
        return deferred.await()
    }

    private suspend fun <T> write(serializer: KSerializer<T>, message: T) = writeLock.withLock {
        withContext(Dispatchers.IO) {
            writer.write(json.encodeToString(serializer, message))
            writer.newLine()
            writer.flush()
        }
    }

    private fun stderr() = synchronized(stderrTail) { stderrTail.joinToString("\n") }

    override fun close() {
        runCatching { writer.close() }
        process.destroy()
        if (process.isAlive) process.destroyForcibly()
        scope.cancel()
    }

    companion object {
        private const val STDERR_TAIL = 20
        private val PERMISSION_PREFERENCE = listOf("allow_always", "allow_once")

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun start(
            command: String,
            workingDir: String,
            onUpdate: suspend (SessionUpdate) -> Unit = {},
        ): AcpConnection {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(File(workingDir))
                .start()
            println("[ACP] started: $command (cwd=$workingDir, pid=${process.pid()})")
            return AcpConnection(process, onUpdate).also { it.startReading() }
        }
    }
}
