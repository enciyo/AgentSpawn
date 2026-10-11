package org.enciyo.agentspawn.features.github

import arrow.core.raise.context.bind
import arrow.core.raise.either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.enciyo.agentspawn.AppError
import org.enciyo.agentspawn.api.GithubLogin
import org.enciyo.agentspawn.api.GithubStatus
import org.enciyo.agentspawn.cli.Folders
import org.enciyo.agentspawn.cli.GitCli
import org.enciyo.agentspawn.cli.GitHubCli


class GithubService(
    private val gitCli: GitCli,
    private val gitHubCli: GitHubCli,
) {

    companion object {
        private val ONE_TIME_CODE_REGEX = Regex("""one-time code:\s*([A-Z0-9-]+)""", RegexOption.IGNORE_CASE)
    }


    suspend fun clone(
        id: String,
        repoUrl: String,
    ) = either {
        val path = Folders.getProjectFolder(id)
        gitHubCli.clone(repoUrl, path).bind()
    }

    suspend fun createPullRequest(
        repoUrl: String,
        title: String,
        body: String,
        head: String,
        base: String
    ) =
        gitHubCli.createPullRequest(
            repoUrl = repoUrl,
            title = title,
            body = body,
            headBranch = head,
            baseBranch = base
        )

    suspend fun push(
        id: String,
        message: String,
        branch: String,
    ) = either {
        val folder = Folders.getProjectFolder(id)
        gitCli.add(workSpace = folder).bind()
        gitCli.commit(message, workSpace = folder).bind()
        gitCli.push(branch = branch, workSpace = folder).bind()
    }

    suspend fun hasChangelist(id: String) = either {
        val folder = Folders.getProjectFolder(id)
        gitCli.hasChangeList(workSpace = folder).bind()
    }


    suspend fun checkout(id: String, branch: String, ref: String) = either {
        val folder = Folders.getProjectFolder(id)
        val hasBranch = gitCli.hasBranch(branch, folder).bind()
        if (hasBranch)
            gitCli.pull(branch = branch, workSpace = folder).bind()
        else
            gitCli.checkout(branch = branch, base = ref, workSpace = folder).bind()
    }

    suspend fun status() = either {
        val status = gitHubCli.status().bind()
        status.output.split("\n\n")
            .mapNotNull { account ->
                val lines = account.lines()
                val nameLine = lines.firstOrNull { it.contains("Logged in to") } ?: return@mapNotNull null
                val name = nameLine.substringAfter("account ").substringBefore(" (").trim()
                val activeLine = lines.firstOrNull { it.contains("Active account") } ?: return@mapNotNull null
                val active = activeLine.substringAfter("Active account: ").trim() == "true"
                val scopeLine = lines.firstOrNull { it.contains("Token scopes") } ?: return@mapNotNull null
                val scopes = scopeLine.substringAfter("Token scopes: ").trim().removeSurrounding("'").split("', '")
                    .map { it.trim() }
                GithubStatus(
                    name = name,
                    active = active,
                    scopes = scopes,
                )
            }
    }

    suspend fun login() = either {
        val login = gitHubCli.login().bind()
        val output = listOf(login.output, login.error).filter { it.isNotBlank() }.joinToString("\n")
        val code = ONE_TIME_CODE_REGEX.find(output)?.groupValues?.get(1).orEmpty()
            .ifBlank { output.substringAfter("code: ", "").substringBefore("\n").trim() }
        if (code.isBlank()) {
            raise(AppError.Internal("GitHub device code was not printed. Output:\n${output.take(400)}"))
        }
        GithubLogin(
            code = code,
            openUrl = "https://github.com/login/device",
        )
    }

    suspend fun logout() = either {
        val status = status().bind()
        status.forEach {
            gitHubCli.logout(it.name).bind()
        }
        true
    }

    suspend fun repositories() = either {
        val status = gitHubCli.status().bind()
        val repositories = gitHubCli.repositories().bind()
        parseJson(repositories.output, GitHubCli.Json.NAME_WITH_OWNER)
    }

    suspend fun searchRepos(key: String, limit: Int) = either {
        val status = gitHubCli.status().bind()
        val repositories = gitHubCli.searchRepos(key, limit).bind()
        parseJson(repositories.output, GitHubCli.Json.FULL_NAME)
    }

    suspend fun searchBranches(
        key: String,
        repo: String,
    ) = either {
        val status = gitHubCli.status().bind()
        val branches = gitHubCli.searchBranch(key, repo).bind()
        branches.output.lines()
    }

    suspend fun token() = either {
        val result = gitHubCli.token().bind()
        val key = "no oauth token found"
        if (result.output.contains(key) || result.output.isEmpty())
            raise(AppError.NotFound(result.output.trim()))
        result.output.substringAfter(key).trim().let {
            "${it.take(4)}...${it.takeLast(2)}"
        }
    }


    private fun parseJson(json: String, objectKey: String): List<String> =
        runCatching {
            Json.parseToJsonElement(json).jsonArray.map {
                it.jsonObject[objectKey]?.jsonPrimitive?.content ?: ""
            }
        }
            .onFailure { print(it) }
            .getOrElse { emptyList() }
}


