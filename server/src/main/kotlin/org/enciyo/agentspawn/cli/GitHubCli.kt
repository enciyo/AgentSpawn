package org.enciyo.agentspawn.cli

class GitHubCli(
    shellRunner: ShellRunner,
) : Cli(shellRunner) {
    override val program: Command get() = Command.default("gh")

    object Json {
        const val FULL_NAME = "fullName"
        const val NAME_WITH_OWNER = "nameWithOwner"
    }

    private object GHCommand {
        data class Clone(
            val url: String,
            val name: String,
        ) :
            Command("repo clone $url $name")

        data object Status :
            Command("auth status")

        data object Login :
            Command("auth login --hostname github.com --git-protocol https --web")

        data class PullRequest(
            val repoPath: String,
            val title: String,
            val body: String,
            val baseBranch: String = "main",
            val headBranch: String = "feature-branch",
        ) :
            Command("pr create --repo $repoPath --title \"$title\" --body \"$body\" --base $baseBranch --head $headBranch")

        data class Logout(val user: String) :
            Command("auth logout --user $user")

        data object Repositories :
            Command("repo list --json ${Json.NAME_WITH_OWNER} --limit 200")

        data class SearchRepository(
            val search: String,
            val limit: Int
        ) :
            Command("search repos \"$search\" user:$(gh api user --template '{{.login}}') $(gh api user/orgs --template '{{range .}}org:{{.login}} {{end}}') --json ${Json.FULL_NAME} --limit $limit")

        //Branches
        data class SearchBranch(val search: String, val repository: String) :
            Command("api repos/$repository/branches --paginate -q '.[] | select(.name | contains(\"$search\")) | .name' ")

        data object Token :
            Command("auth token --hostname github.com")
    }

    suspend fun login() =
        run(GHCommand.Login, timeout = 5, interruptible = false)

    suspend fun logout(user: String) =
        run(GHCommand.Logout(user))

    suspend fun status() = run(GHCommand.Status)

    suspend fun clone(repoUrl: String, path: String) =
        run(GHCommand.Clone(repoUrl, path))

    suspend fun createPullRequest(
        repoUrl: String,
        title: String,
        body: String,
        baseBranch: String,
        headBranch: String,
    ) =
        run(GHCommand.PullRequest(repoUrl, title, body, baseBranch, headBranch))

    suspend fun repositories() =
        run(GHCommand.Repositories)

    suspend fun searchRepos(search: String, limit: Int) =
        run(GHCommand.SearchRepository(search, limit))


    suspend fun searchBranch(
        search: String,
        repo: String,
    ) =
        run(GHCommand.SearchBranch(search, repo))

    suspend fun token() =
        run(GHCommand.Token, timeout = 5, interruptible = true)
}
