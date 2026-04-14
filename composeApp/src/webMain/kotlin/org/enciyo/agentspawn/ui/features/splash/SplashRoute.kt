package org.enciyo.agentspawn.ui.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.api.AgentProvider
import org.enciyo.agentspawn.ui.features.chat.border
import org.enciyo.agentspawn.ui.features.components.AppTextField
import org.koin.compose.viewmodel.koinViewModel

data class SplashActions(
    val onGithubToggle: () -> Unit,
    val onProviderToggle: () -> Unit,
    val onNavigateChat: () -> Unit,
    val onOpenGithubLoginPage: (Boolean) -> Unit,
    val onGithubApiKeyChange: (String) -> Unit,
    val onSelectProvider: (AgentProvider) -> Unit,
)


@Composable
fun SplashRoute(
    viewModel: SplashViewModel = koinViewModel(),
    onNavigateChat: () -> Unit,
) {

    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val action = remember(viewModel) {
        SplashActions(
            onGithubToggle = viewModel::onConnectGithub,
            onNavigateChat = viewModel::onNavigateToChatClick,
            onProviderToggle = viewModel::onProviderToggle,
            onOpenGithubLoginPage = viewModel::onOpenGithubLoginPage,
            onGithubApiKeyChange = viewModel::onGithubApiKeyChange,
            onSelectProvider = viewModel::onSelectProvider,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SplashEvent.OpenLoginUrl -> uriHandler.openUri(event.url)
                is SplashEvent.NavigateToChat -> onNavigateChat()
            }
        }
    }

    SplashScreen(uiState = state, action = action)
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    uiState: SplashUiState,
    action: SplashActions,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        if (uiState.githubState.code.isNotEmpty()) {
            AlertDialog(
                text = {
                    SelectionContainer {
                        Text(text = buildAnnotatedString {
                            append("Please enter the code")
                            withStyle(
                                MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            ) {
                                append(" ${uiState.githubState.code} ")
                            }
                            append("in the opened page to complete the Github authentication. Then refresh this page")
                        })
                    }
                },
                onDismissRequest = { action.onOpenGithubLoginPage(false) },
                confirmButton = {
                    TextButton(onClick = { action.onOpenGithubLoginPage(true) }) {
                        Text("Open")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {

            GithubAuth(
                value = uiState.githubState.apiKey,
                isLoading = uiState.githubState.isLoading,
                onValueChange = action.onGithubApiKeyChange,
                onGithubToggle = action.onGithubToggle
            )
            if (uiState.githubState.apiKey.isNotEmpty()) {
                Providers(
                    providers = uiState.providers,
                    selectedProvider = uiState.selectedProvider,
                    onSelectProvider = action.onSelectProvider,
                    isLoggedIn = uiState.isLoggedIn,
                    isLoading = uiState.isLoading,
                    loggedInfo = uiState.loggedMail,
                    onToggleClick = action.onProviderToggle
                )
            }
            if (
                uiState.loggedMail.isNullOrEmpty().not() &&
                uiState.githubState.apiKey.isNotEmpty()
            ) {
                OpenChat(
                    info = uiState.loggedMail,
                    onNavigateChat = action.onNavigateChat
                )
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

        }
    }
}

@Composable
fun GithubAuth(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onGithubToggle: () -> Unit,
    isLoading: Boolean,
) {

    Section(
        modifier = modifier,
        text = "Github Authentication"
    ) {
        AppTextField(
            modifier = Modifier.weight(1f).border(),
            value = value,
            isEnabled = false,
            onValueChange = onValueChange,
            placeholder = "Api Key",
        )
        SectionButton(
            text = if (value.isEmpty()) "Connect" else "Disconnect",
            isLoading = isLoading,
            onClick = onGithubToggle
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Providers(
    modifier: Modifier = Modifier,
    providers: List<AgentProvider>,
    isLoading: Boolean,
    selectedProvider: AgentProvider,
    onSelectProvider: (AgentProvider) -> Unit,
    onToggleClick: () -> Unit,
    isLoggedIn: Boolean,
    loggedInfo: String?,
) {
    var expanded by remember { mutableStateOf(false) }

    val wrappedInfo = if (loggedInfo.orEmpty().isNotEmpty()) " ($loggedInfo)" else ""

    Section(
        modifier = modifier,
        text = "Provider"
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            modifier = Modifier.weight(1f),
            onExpandedChange = { expanded = it },
        ) {
            AppTextField(
                value = selectedProvider.name + wrappedInfo,
                onValueChange = {},
                placeholder = "Provider",
                isEnabled = false,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .border(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                providers.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.name) },
                        onClick = {
                            onSelectProvider(provider)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        SectionButton(
            isLoading = isLoading,
            onClick = onToggleClick,
            text = if (isLoggedIn.not()) "Connect" else "Disconnect"
        )
    }
}


@Composable
fun Section(
    text: String,
    modifier: Modifier = Modifier,
    rowScope: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            rowScope()
        }
    }
}

@Composable
fun OpenChat(
    info: String,
    onNavigateChat: () -> Unit
) {
    Section(text = "Chat") {
        AppTextField(
            modifier = Modifier.weight(1f).border(),
            value = info,
            isEnabled = false,
            onValueChange = {},
            placeholder = "Api Key",
        )
        SectionButton(
            text = "Open Chat",
            isLoading = false,
            onClick = onNavigateChat
        )
    }
}

@Composable
fun SectionButton(
    isLoading: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        enabled = isLoading.not(),
        modifier = Modifier.width(120.dp),
        onClick = onClick
    ) {
        if (isLoading.not()) Text(text) else CircularProgressIndicator(modifier = Modifier.size(16.dp))
    }
}

