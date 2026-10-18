package org.enciyo.agentspawn.ui.features.customize

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import agentspawn.composeapp.generated.resources.Res
import agentspawn.composeapp.generated.resources.customize
import agentspawn.composeapp.generated.resources.customize_mcp
import agentspawn.composeapp.generated.resources.customize_mcp_json_placeholder
import agentspawn.composeapp.generated.resources.customize_mcp_save
import agentspawn.composeapp.generated.resources.customize_skills
import agentspawn.composeapp.generated.resources.customize_skills_placeholder
import org.enciyo.agentspawn.ui.features.components.AppTextField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class CustomizeTab {
    Mcp,
    Skills,
}

@Composable
fun CustomizeScreen(
    modifier: Modifier = Modifier,
    viewModel: CustomizeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val tabs = CustomizeTab.entries

    Column(
        modifier = modifier
            .widthIn(min = 200.dp, max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.customize),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        TabRow(
            selectedTabIndex = state.selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = state.selectedTabIndex == index,
                    onClick = { viewModel.onTabSelected(index) },
                    text = {
                        Text(
                            text = when (tab) {
                                CustomizeTab.Mcp -> stringResource(Res.string.customize_mcp)
                                CustomizeTab.Skills -> stringResource(Res.string.customize_skills)
                            },
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (tabs[state.selectedTabIndex]) {
            CustomizeTab.Mcp -> McpJsonEditor(
                state = state,
                onJsonChange = viewModel::onMcpJsonChange,
                onSave = viewModel::saveMcp,
            )
            CustomizeTab.Skills -> CustomizePlaceholder(
                message = stringResource(Res.string.customize_skills_placeholder),
            )
        }
    }
}

@Composable
private fun McpJsonEditor(
    state: CustomizeUiState,
    onJsonChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            AppTextField(
                value = state.mcpJson,
                onValueChange = onJsonChange,
                placeholder = stringResource(Res.string.customize_mcp_json_placeholder),
                line = 18,
                visualTransformation = rememberJsonSyntaxHighlightTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .border(
                        width = 1.dp,
                        color = if (state.isMcpJsonValid) {
                            MaterialTheme.colorScheme.outlineVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            )
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp),
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                ) {
                    Text(stringResource(Res.string.customize_mcp_save))
                }
            }
        }
    }
}

@Composable
private fun CustomizePlaceholder(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
