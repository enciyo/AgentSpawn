package org.enciyo.agentspawn.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Black = Color(0xFF000000)
private val White = Color(0xFFD4D4D4)
private val MediumLightGray = Color(0xFFE0E0E0)
private val DarkGray = Color(0xFF424242)
private val DarkSurface = Color(0xFF121212)
private val DarkSurfaceVariant = Color(0xFF2C2C2C)
private val DarkOutline = Color(0xFF757575)
private val DarkRed = Color(0xFFB00020)

object DrawerTokens {
    val Width = 260.dp
    val MinWidth = 24.dp
}


private val BlackWhiteDarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = White,
    inversePrimary = DarkGray,
    secondary = MediumLightGray,
    onSecondary = Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = White,
    tertiary = MediumLightGray,
    onTertiary = Black,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = White,
    background = DarkSurface,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = White,
    surfaceTint = White,
    inverseSurface = White,
    inverseOnSurface = Black,
    error = DarkRed,
    onError = Black,
    errorContainer = DarkGray,
    onErrorContainer = White,
    outline = DarkOutline,
    outlineVariant = DarkGray,
    scrim = Black,
    surfaceBright = DarkSurfaceVariant,
    surfaceDim = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkGray,
    surfaceContainerHighest = DarkOutline,
    surfaceContainerLow = DarkSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceVariant,
    primaryFixed = DarkSurfaceVariant,
    primaryFixedDim = DarkGray,
    onPrimaryFixed = White,
    onPrimaryFixedVariant = MediumLightGray,
    secondaryFixed = DarkSurfaceVariant,
    secondaryFixedDim = DarkGray,
    onSecondaryFixed = White,
    onSecondaryFixedVariant = MediumLightGray,
    tertiaryFixed = DarkSurfaceVariant,
    tertiaryFixedDim = DarkGray,
    onTertiaryFixed = White,
    onTertiaryFixedVariant = MediumLightGray,
)


val textFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )

@Composable
fun AgentSpawnWrappedTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BlackWhiteDarkColorScheme,
        content = content,
    )
}
