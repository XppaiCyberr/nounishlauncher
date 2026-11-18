package com.nounish.launcher

import com.nounish.launcher.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemNavigationTest {

    @Test
    fun testDefaultFullScreenGestureModeIsDisabled() {
        val settings = LauncherSettings()
        assertFalse(settings.fullScreenGestureMode)
    }

    @Test
    fun testCustomFullScreenGestureModeEnabled() {
        val settings = LauncherSettings(fullScreenGestureMode = true)
        assertTrue(settings.fullScreenGestureMode)
    }

    @Test
    fun testFullScreenGestureModeToggle() {
        val settings = LauncherSettings()
        assertFalse(settings.fullScreenGestureMode)

        val updated = settings.copy(fullScreenGestureMode = true)
        assertTrue(updated.fullScreenGestureMode)

        val reverted = updated.copy(fullScreenGestureMode = false)
        assertFalse(reverted.fullScreenGestureMode)
    }

    @Test
    fun testResolutionLogic() {
        fun resolveGestureMode(savedMode: Boolean?): Boolean {
            return savedMode ?: false
        }

        assertFalse(resolveGestureMode(null))
        assertTrue(resolveGestureMode(true))
        assertFalse(resolveGestureMode(false))
    }
}
