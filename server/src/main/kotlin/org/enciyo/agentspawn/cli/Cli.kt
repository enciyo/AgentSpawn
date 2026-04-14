package org.enciyo.agentspawn.cli


abstract class Cli(protected val shellRunner: ShellRunner) {
    abstract val program: Command

    suspend fun run(
        vararg args: Command,
        timeout: Long = -1,
        innerProgram: Command = program,
        workingDir: String = ".",
        interruptible: Boolean = false,
    ) =
        shellRunner.runSync(
            timeoutSeconds = timeout,
            args = arrayOf(innerProgram, *args),
            workingDir = workingDir,
            interruptible = interruptible
        )

}
