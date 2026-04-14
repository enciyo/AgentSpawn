package org.enciyo.agentspawn.ui.features.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


@Composable
fun DisplayScreen(
    content: @Composable (isDesktop: Boolean) -> Unit
) {
    BoxWithConstraints {
        content(maxWidth > 720.dp)
    }
}