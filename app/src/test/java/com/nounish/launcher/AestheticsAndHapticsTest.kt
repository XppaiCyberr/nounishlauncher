package com.nounish.launcher

import androidx.compose.ui.graphics.Color
import com.nounish.launcher.data.LauncherSettings
import com.nounish.launcher.ui.theme.MintGreen
import com.nounish.launcher.ui.theme.NounishRed
import com.nounish.launcher.ui.theme.WarmAmber
import com.nounish.launcher.ui.theme.getAccentColor
import com.nounish.launcher.util.LauncherHaptic
import org.junit.Assert.assertEquals
import org.junit.Test

class AestheticsAndHapticsTest {

    @Test
    fun testDefaultAestheticsSettings() {
        val settings = LauncherSettings()
        assertEquals("monochrome", settings.accentColor)
        assertEquals("standard", settings.hapticFeedbackLevel)
    }

    @Test
    fun testAccentColorMapping() {
        assertEquals(NounishRed, getAccentColor("nounish_red", isLightTheme = false))
        assertEquals(WarmAmber, getAccentColor("warm_amber", isLightTheme = false))
        assertEquals(MintGreen, getAccentColor("mint_green", isLightTheme = false))
        assertEquals(Color(0xFFFFFFFF), getAccentColor("monochrome", isLightTheme = false))
        assertEquals(Color(0xFF121212), getAccentColor("monochrome", isLightTheme = true))
    }

    @Test
    fun testLauncherHapticNullSafety() {
        // Null haptic feedback should gracefully do nothing without exception
        LauncherHaptic.performTick(null, "subtle")
        LauncherHaptic.performTick(null, "standard")
        LauncherHaptic.performTick(null, "firm")
        LauncherHaptic.performTick(null, "off")

        LauncherHaptic.performMedium(null, "standard")
        LauncherHaptic.performLongPress(null, "standard")
    }
}
