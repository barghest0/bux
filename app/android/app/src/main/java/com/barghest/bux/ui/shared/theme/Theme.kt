package com.barghest.bux.ui.shared.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class BuxColors(
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val divider: Color,
    val isDark: Boolean
)

val LocalBuxColors = staticCompositionLocalOf {
    BuxColors(
        positive = Positive,
        negative = Negative,
        warning = Warning,
        divider = DividerLight,
        isDark = false
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    background = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    background = SurfaceLight
)

private val DarkBuxColors = BuxColors(
    positive = PositiveDark,
    negative = NegativeDark,
    warning = WarningDark,
    divider = DividerDark,
    isDark = true
)

private val LightBuxColors = BuxColors(
    positive = Positive,
    negative = Negative,
    warning = Warning,
    divider = DividerLight,
    isDark = false
)

@Composable
fun BuxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val buxColors = if (darkTheme) DarkBuxColors else LightBuxColors

    CompositionLocalProvider(LocalBuxColors provides buxColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
