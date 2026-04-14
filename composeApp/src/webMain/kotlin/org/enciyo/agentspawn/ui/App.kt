package org.enciyo.agentspawn.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.enciyo.agentspawn.ui.design.AgentSpawnWrappedTheme
import org.enciyo.agentspawn.ui.features.chat.ChatRoute
import org.enciyo.agentspawn.ui.features.splash.SplashRoute

@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {},
) {
    val navController = rememberNavController()
    AgentSpawnWrappedTheme {
        AppContent(navController = navController)
    }
    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}

@Composable
private fun AppContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppNavHost(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = StartScreen,
        modifier = modifier,
    ) {
        composable<StartScreen> {
            SplashRoute {
                navController.navigate(ChatScreen) {
                    popUpTo(StartScreen) { inclusive = true }
                }
            }
        }
        composable<ChatScreen> {
            ChatRoute(
                onSnackMessage = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
                onHome = {
                    navController.navigate(StartScreen) {
                        popUpTo(StartScreen) { inclusive = true }
                    }
                }
            )
        }
    }
}