package org.enciyo.agentspawn.configs

import io.ktor.http.*
import io.ktor.server.plugins.cors.*


fun CORSConfig.handle() {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.CacheControl)
    allowHeader(HttpHeaders.LastEventID)
    anyHost()
}