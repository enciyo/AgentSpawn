package org.enciyo.agentspawn

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import org.enciyo.agentspawn.api.ShellResult
import org.enciyo.agentspawn.cli.SqlLiteCli
import org.postgresql.core.SqlCommand
import java.io.InputStream
import java.util.concurrent.TimeUnit

private val ignoreOutputCommandsForPrint = setOf(SqlLiteCli.TAG)

suspend fun String.exec(
    timeout: Long = -1,
    interruptible: Boolean = true,
    workingDir: String = ".",
    env: Map<String, String> = emptyMap(),
) = runCatching {
    val process = startShellProcess(trim(), workingDir, env)
    withContext(Dispatchers.IO) {
        collectProcessOutput(process, timeout, interruptible)
    }
}
    .onSuccess {
        // Contains
        if (ignoreOutputCommandsForPrint.any { contains(it) }) return@onSuccess
        printSuccess(
            command = this,
            result = it.output.take(300) + "..."
        )
    }
    .onFailure {
        if (ignoreOutputCommandsForPrint.any { contains(it) }) return@onFailure
        printError(
            command = this,
            result = it.message ?: "Unknown error"
        )
    }


private const val DEFAULT_TIMEOUT_SECONDS = 500L
private const val BACKGROUND_TTL_MS = 5 * 60_000L

@OptIn(DelicateCoroutinesApi::class)
private suspend fun collectProcessOutput(process: Process, timeoutMs: Long, interruptible: Boolean): ShellResult {
    val outCh = Channel<String>(Channel.UNLIMITED)
    val errCh = Channel<String>(Channel.UNLIMITED)
    val readerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val outJob = readerScope.launch { streamToChannel(process.inputStream, outCh) }
    val errJob = readerScope.launch { streamToChannel(process.errorStream, errCh) }
    val effectiveTimeout = if (timeoutMs > 0) timeoutMs else DEFAULT_TIMEOUT_SECONDS
    val (exitCode, isTimeout) = waitWithTimeout(process, TimeUnit.SECONDS.toMillis(effectiveTimeout), interruptible)

    if (isTimeout && !interruptible) {
        println("[BackgroundProcess] PID=${process.pid()} isAlive=${process.isAlive} — detached")
        scheduleBackgroundCleanup(process, readerScope, outJob, errJob)
        return ShellResult(
            output = cleanTerminalOutput(drainChannelSync(outCh))
                .ifEmpty { cleanTerminalOutput(drainChannelSync(errCh)) },
            error = "",
            exitCode = null,
            isTimeout = true,
        )
    }

    closeStreamsAndJoin(process, outJob, errJob)
    readerScope.cancel()
    val rawStdout = cleanTerminalOutput(drainChannel(outCh))
    val rawStderr = cleanTerminalOutput(drainChannel(errCh))
    val failed = (isTimeout && timeoutMs < 0) || (exitCode != null && exitCode != 0)
    return ShellResult(
        output = if (!failed && rawStdout.isEmpty()) rawStderr else rawStdout,
        error = if (failed) rawStderr else "",
        exitCode = exitCode,
        isTimeout = isTimeout,
    )
}

@OptIn(DelicateCoroutinesApi::class)
private fun scheduleBackgroundCleanup(
    process: Process,
    readerScope: CoroutineScope,
    outJob: Job,
    errJob: Job,
) {
    GlobalScope.launch(Dispatchers.IO) {
        val finished = withTimeoutOrNull(BACKGROUND_TTL_MS) {
            runInterruptible { process.waitFor() }
        }
        if (finished != null) {
            println("[BackgroundProcess] PID=${process.pid()} exitCode=${process.exitValue()} — completed")
        } else {
            println("[BackgroundProcess] PID=${process.pid()} — TTL expired, destroying")
            process.destroyForcibly()
        }
        closeStreamsAndJoin(process, outJob, errJob)
        readerScope.cancel()
    }
}

private suspend fun closeStreamsAndJoin(process: Process, outJob: Job, errJob: Job) {
    runCatching { process.inputStream.close() }
    runCatching { process.errorStream.close() }
    joinAll(outJob, errJob)
}

private fun startShellProcess(
    command: String,
    workingDir: String = ".",
    env: Map<String, String> = emptyMap(),
): Process {
    val builder = ProcessBuilder("sh", "-c", command)
        .directory(java.io.File(workingDir))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
    if (env.isNotEmpty()) builder.environment().putAll(env)
    return builder.start()
}

private suspend fun streamToChannel(stream: InputStream, ch: Channel<String>) {
    try {
        withContext(Dispatchers.IO) {
            runInterruptible {
                try {
                    stream.bufferedReader().useLines { lines ->
                        lines.forEach { line -> ch.trySend(line) }
                    }
                } catch (e: Exception) {
                    // Ignore exceptions from stream reading(e.g., if process is killed)
                }
            }
        }
    } finally {
        ch.close()
    }
}

private suspend fun waitWithTimeout(process: Process, timeoutMs: Long, interruptible: Boolean): Pair<Int?, Boolean> {
    val exitCode = withTimeoutOrNull(timeoutMs) {
        withContext(Dispatchers.IO) {
            runInterruptible {
                process.waitFor()
            }
        }
    }
    if (exitCode == null && interruptible) {
        process.destroyForcibly()
    }
    return exitCode to (exitCode == null)
}


private suspend fun drainChannel(ch: Channel<String>): String =
    buildString {
        ch.receiveAsFlow().collect { line ->
            val t = line.trim()
            if (t.isNotEmpty()) {
                append(t); append("\n")
            }
        }
    }.trimEnd()

private fun cleanTerminalOutput(raw: String): String {
    val noAnsi = ANSI_ESCAPE_REGEX.replace(raw, "")
    return noAnsi
        .replace(Regex(" +"), " ")
        .trim()
}

private fun drainChannelSync(ch: Channel<String>): String =
    buildString {
        var result = ch.tryReceive()
        while (result.isSuccess) {
            val t = result.getOrNull()?.trim().orEmpty()
            if (t.isNotEmpty()) {
                append(t); append("\n")
            }
            result = ch.tryReceive()
        }
    }.trimEnd()

private val ANSI_ESCAPE_REGEX = Regex("\u001B\\[[?0-9;]*[a-zA-Z]")


private fun printSuccess(
    command: String,
    result: String
) {
    val greenColor = "\u001b[32m"
    val reset = "\u001b[0m" // to reset color to the default
    println("$greenColor$command$reset\n$result")
}

private fun printError(
    command: String,
    result: String
) {

    val red = "\u001b[31m"
    val reset = "\u001B[0m"
    println("$red$command$reset\n$result")
}