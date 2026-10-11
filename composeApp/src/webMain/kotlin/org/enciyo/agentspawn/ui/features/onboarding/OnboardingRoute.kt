package org.enciyo.agentspawn.ui.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.enciyo.agentspawn.ui.features.components.ToastBanner
import org.enciyo.agentspawn.ui.features.components.ProviderAvailability
import org.enciyo.agentspawn.ui.features.components.ProviderList
import org.enciyo.agentspawn.ui.features.components.ProviderListItem
import org.enciyo.agentspawn.ui.features.components.ToastType
import org.enciyo.agentspawn.ui.features.splash.SplashEvent
import org.enciyo.agentspawn.ui.features.splash.SplashViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OnboardingRoute(
    onNavigateChat: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var showCopiedBanner by remember { mutableStateOf(false) }
    var showingProviders by remember { mutableStateOf(false) }

    LaunchedEffect(showCopiedBanner) {
        if (showCopiedBanner) {
            delay(2200.milliseconds)
            showCopiedBanner = false
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SplashEvent.OpenLoginUrl -> uriHandler.openUri(event.url)
                is SplashEvent.NavigateToChat -> onNavigateChat()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showingProviders) {
            ProvidersOnboardingScreen(
                isLoading = state.isLoading,
                isConnected = state.isLoggedIn,
                account = state.loggedMail,
                errorMessage = state.errorMessage,
                onConnect = viewModel::onProviderToggle,
                onContinue = onNavigateChat
            )
        } else Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState())
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Connect GitHub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Connect your GitHub account so AgentSpawn can work with your repositories.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (state.isGithubConnected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = if (state.isGithubConnected) "GitHub connected"
                                else "Connect your GitHub account",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (state.isGithubConnected) "Your account is ready to use."
                                else "Authorize access using GitHub's secure device sign-in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!state.isGithubConnected && state.githubState.code.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.onOpenGithubLoginPage(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Open GitHub")
                            }
                            Text(
                                text = "Enter this one-time code on GitHub",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SelectionContainer {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = state.githubState.code,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(state.githubState.code))
                                            showCopiedBanner = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy code"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = viewModel::onConnectGithub,
                        enabled = !state.githubState.isLoading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        if (state.githubState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(if (state.isGithubConnected) "Disconnect GitHub" else "Continue with GitHub")
                    }
                    Text(
                        text = "You'll confirm the connection on GitHub with a one-time code. AgentSpawn never asks for your GitHub password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.isGithubConnected) {
                Button(onClick = { showingProviders = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }
        }
        if (showCopiedBanner) {
            ToastBanner(
                message = "Kopyalandı",
                type = ToastType.Success,
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
            )
        }
    }
}

@Composable
private fun ProvidersOnboardingScreen(
    isLoading: Boolean,
    isConnected: Boolean,
    account: String?,
    errorMessage: String?,
    onConnect: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Choose your provider",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Connect an AI coding agent to continue.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ProviderList(
            items = listOf(
                ProviderListItem(
                    id = "cursor",
                    name = "Cursor",
                    actionLabel = if (isConnected) "Disconnect" else "Connect",
                    isLoading = isLoading
                ),
                ProviderListItem(
                    id = "chatgpt",
                    name = "ChatGPT",
                    actionLabel = "Coming Soon",
                    availability = ProviderAvailability.ComingSoon
                ),
                ProviderListItem(
                    id = "claude-code",
                    name = "Claude Code",
                    actionLabel = "Coming Soon",
                    availability = ProviderAvailability.ComingSoon
                ),
                ProviderListItem(
                    id = "gemini",
                    name = "Gemini",
                    actionLabel = "Coming Soon",
                    availability = ProviderAvailability.ComingSoon
                )
            ),
            onAction = { item ->
                if (item.id == "cursor") onConnect()
            }
        )
        if (errorMessage != null) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (isConnected) {
            Text(
                text = account.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continue")
            }
        }
    }
}

