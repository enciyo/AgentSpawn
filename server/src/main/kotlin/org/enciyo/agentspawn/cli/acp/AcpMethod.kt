package org.enciyo.agentspawn.cli.acp

/** ACP JSON-RPC methods we use: https://agentclientprotocol.com/protocol/schema */
enum class AcpMethod(val value: String) {
    // Client → agent requests
    Initialize("initialize"),
    SessionNew("session/new"),
    SessionLoad("session/load"),
    SessionPrompt("session/prompt"),
    SessionSetModel("session/set_model"),
    SessionSetConfigOption("session/set_config_option"),

    // Agent → client messages
    SessionUpdate("session/update"),
    SessionRequestPermission("session/request_permission"),
}
