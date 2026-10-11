package org.enciyo.agentspawn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.enciyo.agentspawn.ui.design.AgentSpawnWrappedTheme
import org.enciyo.agentspawn.ui.features.chat.ChatRoute
import org.enciyo.agentspawn.ui.features.onboarding.OnboardingRoute
import org.enciyo.agentspawn.ui.features.splash.StartupRoute

@Composable
fun App(
    onNavControllerReady: suspend (NavController) -> Unit = {},
) {
    val navController = rememberNavController()
    AgentSpawnWrappedTheme {
        AppContent(navController = navController)
    }
    LaunchedEffect(navController) {
        onNavControllerReady(navController)
    }
}

@Composable
private fun AppContent(navController: NavHostController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = StartScreen,
                ) {
                    composable<StartScreen> {
                        StartupRoute(
                            onGetStarted = { navController.navigate(OnboardingScreen) },
                            onNavigateChat = {
                                navController.navigate(ChatScreen) {
                                    popUpTo(StartScreen) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable<OnboardingScreen> {
                        OnboardingRoute(
                            onNavigateChat = {
                                navController.navigate(ChatScreen) {
                                    popUpTo(OnboardingScreen) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable<ChatScreen> {
                        ChatRoute(
                            onSnackMessage = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
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
        }
    }
}