package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalDarkTheme = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = TrustPrimaryDark,
    secondary = TrustSecondaryDark,
    tertiary = TrustTertiaryDark,
    background = TrustBackgroundDark,
    surface = TrustSurfaceDark,
    onBackground = TrustOnBackgroundDark,
    onSurface = TrustOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = TrustPrimaryLight,
    secondary = TrustSecondaryLight,
    tertiary = TrustTertiaryLight,
    background = TrustBackgroundLight,
    surface = TrustSurfaceLight,
    onBackground = TrustOnBackgroundLight,
    onSurface = TrustOnBackgroundLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Set to false to prioritize our beautiful custom colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
