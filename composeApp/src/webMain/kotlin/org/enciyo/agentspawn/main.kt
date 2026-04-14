package org.enciyo.agentspawn

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import org.enciyo.agentspawn.di.appModule
import org.enciyo.agentspawn.ui.App
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@ExperimentalBrowserHistoryApi
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        KoinApplication(configuration = koinConfiguration { modules(appModule) }) {
            App {
                it.bindToBrowserNavigation()
            }
        }
    }
}
