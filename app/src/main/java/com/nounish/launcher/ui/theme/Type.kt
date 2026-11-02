package com.nounish.launcher.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class LauncherTypography(
    val homeShortcut: TextStyle,
    val drawerItem: TextStyle,
    val clock: TextStyle,
    val priceLabel: TextStyle,
    val searchInput: TextStyle,
    val settingsHeading: TextStyle,
    val settingsItem: TextStyle,
    val settingsDescription: TextStyle
)

fun getLauncherTypography(fontFamily: FontFamily, fontScale: Float = 1.0f): LauncherTypography {
    val scale = fontScale.coerceIn(0.55f, 1.6f)
    return LauncherTypography(
        homeShortcut = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = (28 * scale).sp,
            lineHeight = (38 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        drawerItem = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (18 * scale).sp,
            lineHeight = (26 * scale).sp,
            letterSpacing = 0.2.sp
        ),
        clock = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (18 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        priceLabel = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * scale).sp,
            lineHeight = (18 * scale).sp,
            letterSpacing = 0.5.sp
        ),
        searchInput = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (18 * scale).sp,
            lineHeight = (24 * scale).sp
        ),
        settingsHeading = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20 * scale).sp,
            lineHeight = (26 * scale).sp
        ),
        settingsItem = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp
        ),
        settingsDescription = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13 * scale).sp,
            lineHeight = (18 * scale).sp
        )
    )
}
