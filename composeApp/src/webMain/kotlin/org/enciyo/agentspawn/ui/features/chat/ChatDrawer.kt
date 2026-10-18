package org.enciyo.agentspawn.ui.features.chat

import agentspawn.composeapp.generated.resources.Res
import agentspawn.composeapp.generated.resources.customize
import agentspawn.composeapp.generated.resources.home
import agentspawn.composeapp.generated.resources.new_agent
import agentspawn.composeapp.generated.resources.no_agents
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.enciyo.agentspawn.AppVersion
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
    isCustomizeSelected: Boolean = false,
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
            onCustomize = actions.onCustomize,
            onNewAgent = actions.onNewAgent,
            isCustomizeSelected = isCustomizeSelected,
        )
        GroupingToggle(
            isGrouped = state.isGrouped,
            onToggle = actions.onToggleGrouping,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        if (state.isGrouped) {
            ProviderGroupedAgentList(
                groups = state.providerGroups.filter { it.agents.isNotEmpty() },
                selectedAgentId = state.selectedAgentId,
                selectedProviderId = state.selectedProviderId,
                onAgent = actions.onAgent,
                onSelectProvider = actions.onSelectProvider,
                modifier = Modifier.weight(1f),
            )
        } else {
            FlatAgentList(
                agents = state.agents,
                selectedAgentId = state.selectedAgentId,
                onAgent = actions.onAgent,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = AppVersion.NAME,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun DrawerActions(
    onHome: () -> Unit,
    onCustomize: () -> Unit,
    onNewAgent: () -> Unit,
    isCustomizeSelected: Boolean,
) {
    AppItem(
        onClick = onHome,
        state = AppItemState.withIcon(
            vector = Icons.AutoMirrored.Outlined.ExitToApp,
            text = stringResource(Res.string.home)
        )
    )
    AppItem(
        onClick = onCustomize,
        state = AppItemState.withIcon(
            vector = Icons.Outlined.Tune,
            text = stringResource(Res.string.customize),
            isSelected = isCustomizeSelected,
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
private fun GroupingToggle(
    isGrouped: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Group by provider",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            checked = isGrouped,
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(0.75f),
        )
    }
}

@Composable
private fun FlatAgentList(
    agents: List<AgentDto>,
    selectedAgentId: String?,
    onAgent: (AgentDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (agents.isEmpty()) {
            item {
                AppItem(state = AppItemState.textOnly(stringResource(Res.string.no_agents)))
            }
        }
        items(agents, key = { it.id }) { agent ->
            AgentItem(
                agent = agent,
                isSelected = selectedAgentId == agent.id,
                onAgent = onAgent,
            )
        }
    }
}

@Composable
private fun ProviderGroupedAgentList(
    groups: List<ProviderGroup>,
    selectedAgentId: String?,
    selectedProviderId: String,
    onAgent: (AgentDto) -> Unit,
    onSelectProvider: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (groups.isEmpty()) {
            item {
                AppItem(state = AppItemState.textOnly(stringResource(Res.string.no_agents)))
            }
        }

        groups.forEach { group ->
            item(key = "header-${group.provider.id}") {
                ProviderSectionHeader(
                    group = group,
                    isSelected = group.provider.id.equals(selectedProviderId, ignoreCase = true),
                    onClick = { onSelectProvider(group.provider.id) },
                )
            }

            items(
                items = group.agents,
                key = { it.id },
            ) { agent ->
                AgentItem(
                    agent = agent,
                    isSelected = selectedAgentId == agent.id,
                    onAgent = onAgent,
                )
            }

            item(key = "spacer-${group.provider.id}") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProviderSectionHeader(
    group: ProviderGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = "${group.provider.displayName} (${group.agents.size})",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
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
