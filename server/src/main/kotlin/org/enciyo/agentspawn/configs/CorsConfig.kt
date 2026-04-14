package org.enciyo.agentspawn.configs

import io.ktor.http.*
import io.ktor.server.plugins.cors.*


fun CORSConfig.handle() {
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    anyHost()
}