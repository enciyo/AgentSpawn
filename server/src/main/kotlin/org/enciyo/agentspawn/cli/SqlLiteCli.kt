package org.enciyo.agentspawn.cli

class SqlLiteCli(shellRunner: ShellRunner) : Cli(shellRunner) {

    companion object {
        const val TAG = "sqlite3"
    }


    override val program get() = Command.default(TAG)

    private object SqlLiteCommand {
        data class Path(val path: String) : Command(path)
        data class Mode(val mode: String = "json") : Command("'.mode $mode'")
        data class Query(val table: String, val where: String? = null) : Command(
            "\"SELECT * FROM $table" + (where?.let { " WHERE $it" } ?: "") + "\""
        )
    }

    suspend fun getTables(
        path: String,
        table: String,
        where: String
    ) =
        run(
            SqlLiteCommand.Path(path),
            SqlLiteCommand.Mode(),
            SqlLiteCommand.Query(table, where),
        ).map { listOf(it.output.ifEmpty { it.error }) }
}