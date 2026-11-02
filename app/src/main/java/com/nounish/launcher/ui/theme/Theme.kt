package com.nounish.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class LauncherColors(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val surface: Color,
    val border: Color
)

val LocalLauncherColors = staticCompositionLocalOf {
    LauncherColors(
        background = AmoledBackground,
        textPrimary = AmoledTextPrimary,
        textSecondary = AmoledTextSecondary,
        surface = AmoledSurface,
        border = AmoledBorder
    )
}

val LocalLauncherTypography = staticCompositionLocalOf {
    getLauncherTypography(FontFamily.Default)
}

object LauncherTheme {
    val colors: LauncherColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLauncherColors.current

    val typography: LauncherTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalLauncherTypography.current
}

@Composable
fun NounishLauncherTheme(
    isLightTheme: Boolean = false,
    useMonospace: Boolean = false,
    fontSize: String = "medium",
    content: @Composable () -> Unit
) {
    val colors = if (isLightTheme) {
        LauncherColors(
            background = LightBackground,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            surface = LightSurface,
            border = LightBorder
        )
    } else {
        LauncherColors(
            background = AmoledBackground,
            textPrimary = AmoledTextPrimary,
            textSecondary = AmoledTextSecondary,
            surface = AmoledSurface,
            border = AmoledBorder
        )
    }

    val fontScale = getFontScale(fontSize)
    val fontFamily = if (useMonospace) FontFamily.Monospace else FontFamily.Default
    val typography = getLauncherTypography(fontFamily, fontScale)

    val materialColors = if (isLightTheme) {
        lightColorScheme(
            background = LightBackground,
            surface = LightSurface,
            onBackground = LightTextPrimary,
            onSurface = LightTextPrimary
        )
    } else {
        darkColorScheme(
            background = AmoledBackground,
            surface = AmoledSurface,
            onBackground = AmoledTextPrimary,
            onSurface = AmoledTextPrimary
        )
    }

    CompositionLocalProvider(
        LocalLauncherColors provides colors,
        LocalLauncherTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}
