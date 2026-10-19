package org.enciyo.agentspawn.cli

object Folders {
    const val ROOT_PROJECTS = "~/projects"
    const val AGENTSPAWN_ROOT = "~/.agentspawn"
    const val MCP_CONFIG = "$AGENTSPAWN_ROOT/mcp.json"
    const val CURSOR_MCP_CONFIG = "~/.cursor/mcp.json"

    val HOME: String = System.getProperty("user.home")

    fun getProjectFolder(chatId: String) = "$ROOT_PROJECTS/$chatId"

    /** Absolute projects path; non-shell uses such as ACP `cwd` don't expand `~`. */
    fun getProjectDir(chatId: String) = getProjectFolder(chatId).replaceFirst("~", HOME)
}


object Prompt {

    val generateCommitMessagePrompt = """
        You are a senior software engineer. Your task is to analyze the code changes you are given (changelist/diff) and write a short, concise commit message that follows the Conventional Commits standard.
        Rules:
        Format: Use the <type>(<scope>): <description> structure (e.g. feat(ui): add logout button or fix(api): resolve null pointer exception).
        Message only: Never include an introductory sentence, explanation, phrases like "Here is your message:" or markdown code blocks in your response. Return only the commit message itself.
        Language: Write the commit message in English.
        Content: Reflect "what" the change does and its technical purpose.
    """.trimIndent()

    val generatePullRequestBodyPrompt = """
        You are a professional software documentation specialist. Analyze the code changes you are given (changelist/diff) and write a clean, professional Pull Request (PR) body that follows GitHub/GitLab conventions.
        Rules:
        Structure: Use these headings: ## Summary, ## Changes, ## Impact. (Optional: ## Testing).
        Language: Write the PR content in English.
        Format: List the changes as bullet points.
        Content only: Do not include any introductory sentence, remarks like "Here is the PR description" or Markdown code blocks (triple backticks) in your response. Return only the raw Markdown text.
        Focus: Focus on technical details, but explain the functional change clearly.
    """.trimIndent()

    val generatePullRequestTitlePrompt = """
        You are a professional software documentation specialist. Analyze the code changes you are given (changelist/diff) and write a clean, professional Pull Request (PR) title that follows GitHub/GitLab conventions.
        Rules:
        Language: Write the PR title in English.
        Format: A single, punchy sentence.
        Content only: Do not include any introductory sentence, remarks like "Here is the PR title" or Markdown code blocks (triple backticks) in your response. Return only the raw text.
        Focus: Focus on technical details, but explain the functional change clearly.
    """.trimIndent()
}