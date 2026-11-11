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
    val border: Color,
    val accent: Color
)

fun getAccentColor(accentKey: String, isLightTheme: Boolean): Color {
    return when (accentKey) {
        "nounish_red" -> NounishRed
        "warm_amber" -> WarmAmber
        "mint_green" -> MintGreen
        "slate_blue" -> if (isLightTheme) SlateBlueDark else SlateBlue
        "lavender" -> if (isLightTheme) SoftLavenderDark else SoftLavender
        else -> if (isLightTheme) LightTextPrimary else AmoledTextPrimary
    }
}

val LocalLauncherColors = staticCompositionLocalOf {
    LauncherColors(
        background = AmoledBackground,
        textPrimary = AmoledTextPrimary,
        textSecondary = AmoledTextSecondary,
        surface = AmoledSurface,
        border = AmoledBorder,
        accent = AmoledTextPrimary
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
    showWallpaper: Boolean = false,
    wallpaperDimLevel: Float = 0.80f,
    accentColor: String = "monochrome",
    content: @Composable () -> Unit
) {
    val dimAlpha = wallpaperDimLevel.coerceIn(0.2f, 0.98f)
    val chosenAccent = getAccentColor(accentColor, isLightTheme)
    val colors = if (isLightTheme) {
        val bg = if (showWallpaper) Color.White.copy(alpha = dimAlpha) else LightBackground
        val surf = if (showWallpaper) Color(0xFFF2F2F2).copy(alpha = 0.85f) else LightSurface
        LauncherColors(
            background = bg,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            surface = surf,
            border = LightBorder,
            accent = chosenAccent
        )
    } else {
        val bg = if (showWallpaper) Color.Black.copy(alpha = dimAlpha) else AmoledBackground
        val surf = if (showWallpaper) Color(0xFF141414).copy(alpha = 0.85f) else AmoledSurface
        LauncherColors(
            background = bg,
            textPrimary = AmoledTextPrimary,
            textSecondary = AmoledTextSecondary,
            surface = surf,
            border = AmoledBorder,
            accent = chosenAccent
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
