package org.enciyo.agentspawn.ui.features.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


sealed interface AppItemLeading {
    data class Image(val vector: ImageVector, val tint: Color = Color.Unspecified) : AppItemLeading
    data class Loading(val vector: ImageVector) : AppItemLeading
    data object None : AppItemLeading
}

data class AppItemState(
    val leading: AppItemLeading = AppItemLeading.None,
    val text: String,
    val isSelected: Boolean = false,
) {
    val hasLeading: Boolean get() = leading !is AppItemLeading.None

    companion object {
        fun withIcon(
            vector: ImageVector,
            text: String,
            tint: Color = Color.Unspecified,
            isSelected: Boolean = false
        ) =
            AppItemState(leading = AppItemLeading.Image(vector, tint), text = text, isSelected = isSelected)


        fun textOnly(text: String, isSelected: Boolean = false) =
            AppItemState(leading = AppItemLeading.None, text = text, isSelected = isSelected)
    }
}

@Composable
fun AppItem(
    state: AppItemState,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .background(
                color = if (state.isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                shape = shape,
            )
            .clip(shape = shape)
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (state.hasLeading) Arrangement.spacedBy(8.dp) else Arrangement.Center,
    ) {
        AppItemLeadingContent(state.leading)
        Text(
            text = state.text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppItemLeadingContent(leading: AppItemLeading) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val animatedProgress by infiniteTransition.animateFloat(
        label = "progress",
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse,
        )
    )
    when (leading) {
        AppItemLeading.None -> Unit
        is AppItemLeading.Image ->
            LeadingIcon(vector = leading.vector, tint = leading.tint)

        is AppItemLeading.Loading -> {
            LeadingIcon(vector = leading.vector, modifier = Modifier.alpha(animatedProgress))
        }
    }
}

@Composable
private fun LeadingIcon(
    vector: ImageVector,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = if (tint == Color.Unspecified) LocalContentColor.current else tint,
        modifier = modifier.size(18.dp),
    )
}
