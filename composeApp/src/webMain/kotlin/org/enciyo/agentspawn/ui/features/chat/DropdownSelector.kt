package org.enciyo.agentspawn.ui.features.chat

import agentspawn.composeapp.generated.resources.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.enciyo.agentspawn.ui.design.DesignTokens
import org.enciyo.agentspawn.ui.features.components.AppTextField
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun DropdownSelector(
    items: List<String>,
    selectedItem: String,
    isEnable: Boolean = true,
    isLoading: Boolean,
    onItemSelected: (String) -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onExpand: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(expanded) {
        if (expanded) {
            onExpand()
            delay(150.milliseconds)
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier) {
        DropdownChip(
            text = selectedItem,
            isEnable = isEnable,
            onClick = {
                expanded = true
            }
        )
        DropdownMenu(
            expanded = expanded,
            modifier = Modifier.width(250.dp).height(320.dp),
            onDismissRequest = {
                expanded = false
                onSearch("")
            }
        ) {
            AppTextField(
                value = searchQuery,
                onValueChange = onSearch,
                placeholder = stringResource(Res.string.search),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            HorizontalDivider()
            items.forEach { item ->
                Text(
                    item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onItemSelected(item)
                            expanded = false
                            onSearch("")
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DropdownChip(
    text: String,
    isEnable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.RadiusSm))
            .clickable(enabled = isEnable, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
        if (isEnable) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select $text",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
