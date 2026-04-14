package org.enciyo.agentspawn.cli

import arrow.core.raise.either

class GitCli(shellRunner: ShellRunner) : Cli(shellRunner) {
    override val program: Command get() = Command.default("git")

    companion object {
        const val GIT_TIMEOUT = 60L
    }

    private object GitCommand {
        data class Workspace(val path: String) : Command("-C $path")
        data class Add(val path: String = ".") : Command("add $path")
        data class Commit(val message: String) : Command("commit -m '$message'")
        data class Checkout(val branch: String, val base: String) : Command("checkout -b $branch origin/$base")
        data class Push(val branch: String) : Command("push origin $branch ")
        data object HasChangelist : Command("status --porcelain | wc -l")
        data class HasBranch(val branch: String) :
            Command("branch -a | grep -q '$branch' && echo 'true' || echo 'false'")

        data class Pull(val branch: String) : Command("pull origin $branch")

    }

    suspend fun add(
        path: String = ".", workSpace: String
    ) = run(
        GitCommand.Workspace(workSpace),
        GitCommand.Add(path),
        timeout = GIT_TIMEOUT
    )

    suspend fun commit(branch: String, workSpace: String) = run(
        GitCommand.Workspace(workSpace),
        GitCommand.Commit(branch),
        timeout = GIT_TIMEOUT
    )

    suspend fun checkout(
        branch: String,
        base: String,
        workSpace: String
    ) = run(
        GitCommand.Workspace(workSpace),
        GitCommand.Checkout(branch, base),
        timeout = GIT_TIMEOUT
    )

    suspend fun push(branch: String, workSpace: String) = run(
        GitCommand.Workspace(workSpace),
        GitCommand.Push(branch),
        timeout = GIT_TIMEOUT
    )

    suspend fun hasBranch(branch: String, workSpace: String) = run(
        GitCommand.Workspace(workSpace),
        GitCommand.HasBranch(branch),
        timeout = GIT_TIMEOUT
    )
        .map { it.output == "true" }

    suspend fun pull(branch: String, workSpace: String) = run(
        GitCommand.Workspace(workSpace),
        GitCommand.Pull(branch),
        timeout = GIT_TIMEOUT
    )

    suspend fun hasChangeList(workSpace: String) = either {
        val status = run(
            GitCommand.Workspace(workSpace),
            GitCommand.HasChangelist,
            timeout = GIT_TIMEOUT
        ).bind()
        status.output.toIntOrNull()?.let { it > 0 } ?: false
    }

}