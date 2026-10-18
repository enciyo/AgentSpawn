package org.enciyo.agentspawn.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.enciyo.agentspawn.API_BASE_URL
import org.enciyo.agentspawn.data.Client
import org.enciyo.agentspawn.ui.features.chat.ChatViewModel
import org.enciyo.agentspawn.ui.features.customize.CustomizeViewModel
import org.enciyo.agentspawn.ui.features.splash.SplashViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { provideHttpClient() }
    singleOf(::Client)
    viewModelOf(::SplashViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::CustomizeViewModel)
}


private fun provideHttpClient() = HttpClient(Js) {
    defaultRequest {
        url(API_BASE_URL)
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}
