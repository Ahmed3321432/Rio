package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val StadiumDarkColorScheme = darkColorScheme(
    primary = TurfEmerald,
    onPrimary = Color(0xFF042111),
    primaryContainer = TurfDeep,
    onPrimaryContainer = TextPrimary,
    secondary = ChampionshipGold,
    onSecondary = Color(0xFF231900),
    secondaryContainer = Color(0xFF3E2E00),
    onSecondaryContainer = ChampionshipGold,
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF002026),
    background = StadiumObsidian,
    onBackground = TextPrimary,
    surface = StadiumSurface,
    onSurface = TextPrimary,
    surfaceVariant = StadiumCard,
    onSurfaceVariant = TextSecondary,
    outline = DividerSubtle,
    error = CrimsonAlert,
    onError = Color.White
)

private val StadiumShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KickChampionsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StadiumDarkColorScheme,
        typography = KickChampionsTypography,
        shapes = StadiumShapes,
        content = content
    )
}
