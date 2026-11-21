package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = HarvestAmber,
    onTertiary = Color.White,
    tertiaryContainer = HarvestContainer,
    onTertiaryContainer = OnHarvestContainer,
    background = WarmCream,
    onBackground = EspressoInk,
    surface = PureCard,
    onSurface = EspressoInk,
    surfaceVariant = WarmLinen,
    onSurfaceVariant = WarmMutedInk,
    surfaceContainerLowest = PureCard,
    surfaceContainerLow = WarmCream,
    surfaceContainer = WarmLinen,
    surfaceContainerHigh = WarmSand,
    outline = SubtleOutline,
    outlineVariant = WarmSand,
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = OnTerracottaContainer,
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = TerracottaContainer,
    secondary = SageLight,
    onSecondary = OnSageContainer,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = SageContainer,
    tertiary = HarvestLight,
    onTertiary = OnHarvestContainer,
    tertiaryContainer = HarvestContainerDark,
    onTertiaryContainer = HarvestContainer,
    background = DarkCharcoal,
    onBackground = WarmPaperText,
    surface = DarkSurface,
    onSurface = WarmPaperText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkMutedText,
    surfaceContainerLowest = DarkCharcoal,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = Color(0xFF352F2A),
    outline = DarkOutline,
    outlineVariant = Color(0xFF352F2A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val DailyGoodsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DailyGoodsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DailyGoodsShapes,
        content = content
    )
}
