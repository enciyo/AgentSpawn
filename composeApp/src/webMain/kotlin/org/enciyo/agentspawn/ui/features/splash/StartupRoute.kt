package org.enciyo.agentspawn.ui.features.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onGetStarted: () -> Unit,
    onNavigateChat: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isCheckingAuthentication = state.isLoading || state.githubState.isLoading
    val shouldOpenChat = !isCheckingAuthentication && state.isGithubConnected && state.isLoggedIn

    LaunchedEffect(isCheckingAuthentication, state.isGithubConnected, state.isLoggedIn) {
        if (shouldOpenChat) {
            onNavigateChat()
        }
    }

    if (isCheckingAuthentication || shouldOpenChat) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        SplashRoute(onGetStarted = onGetStarted)
    }
}
