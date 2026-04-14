package org.enciyo.agentspawn


fun String?.ifNullOrEmpty(action: () -> String): String = if (this.isNullOrEmpty()) action() else this
