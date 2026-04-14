package org.enciyo.agentspawn.cli


abstract class Command(val str: String) {

    companion object {
        fun default(str: String): Command = object : Command(str) {}
        fun empty(): Command = Command.default("")
        fun userInput(raw: String): String = raw.shellEscape()
    }

}

internal fun String.shellEscape(): String = "'${replace("'", "'\\''")}'"

