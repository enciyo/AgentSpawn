package org.enciyo.agentspawn.ui.features.chat

import agentspawn.composeapp.generated.resources.Res
import agentspawn.composeapp.generated.resources.home
import agentspawn.composeapp.generated.resources.logout
import agentspawn.composeapp.generated.resources.new_agent
import agentspawn.composeapp.generated.resources.no_agents
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.api.AgentDto
import org.enciyo.agentspawn.api.AgentStatus.*
import org.enciyo.agentspawn.ui.design.DrawerTokens
import org.enciyo.agentspawn.ui.features.components.AppItem
import org.enciyo.agentspawn.ui.features.components.AppItemLeading
import org.enciyo.agentspawn.ui.features.components.AppItemState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DrawerContent(
    state: DrawerState,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(DrawerTokens.Width)
            .fillMaxHeight()
    ) {
        AnimatedVisibility(state.errorMessage.isNullOrBlank().not()) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
        }
        DrawerActions(
            onHome = actions.onHome,
            onNewAgent = actions.onNewAgent,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        AgentList(
            agents = state.agents,
            selectedAgentId = state.selectedAgentId,
            onAgent = actions.onAgent,
        )
    }
}

@Composable
private fun DrawerActions(
    onHome: () -> Unit,
    onNewAgent: () -> Unit,
) {
    AppItem(
        onClick = onHome,
        state = AppItemState.withIcon(
            vector = Icons.AutoMirrored.Outlined.ExitToApp,
            text = stringResource(Res.string.home)
        )
    )
    AppItem(
        onClick = onNewAgent,
        state = AppItemState.withIcon(
            vector = Icons.Outlined.CreateNewFolder,
            text = stringResource(Res.string.new_agent)
        )
    )
}

@Composable
private fun AgentList(
    agents: List<AgentDto>,
    selectedAgentId: String?,
    onAgent: (AgentDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (agents.isEmpty())
            item {
                AnimatedVisibility(agents.isEmpty()) {
                    AppItem(state = AppItemState.textOnly(stringResource(Res.string.no_agents)))
                }
            }
        items(agents) { agent ->
            AgentItem(
                agent = agent,
                isSelected = selectedAgentId == agent.id,
                onAgent = onAgent,
            )
        }
    }
}

@Composable
private fun AgentItem(
    agent: AgentDto,
    isSelected: Boolean,
    onAgent: (AgentDto) -> Unit,
) {
    val leading = when (agent.status) {
        COMPLETED -> AppItemLeading.Image(Icons.Default.CheckCircleOutline)
        PENDING -> AppItemLeading.Loading(Icons.Default.CloudCircle)
        RUNNING -> AppItemLeading.Loading(Icons.Default.Circle)
        FAILED -> AppItemLeading.Image(Icons.Default.BuildCircle, tint = MaterialTheme.colorScheme.error)
        CANCELLED -> AppItemLeading.Image(Icons.Default.Cancel, tint = MaterialTheme.colorScheme.error)
    }
    AppItem(
        state = AppItemState(
            leading = leading,
            text = agent.title,
            isSelected = isSelected
        ),
        onClick = { onAgent(agent) }
    )
}
