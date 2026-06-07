package com.shohan.walktracker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary          = GreenPrimary,
    onPrimary        = BgDark,
    secondary        = BluePrimary,
    onSecondary      = BgDark,
    tertiary         = OrangeAccent,
    background       = BgDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = CardDark,
    onSurfaceVariant = TextSecondary,
    outline          = TextHint,
)

@Composable
fun WalkTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = Typography,
        content     = content
    )
}
