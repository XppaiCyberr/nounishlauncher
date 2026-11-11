package com.nounish.launcher

import com.nounish.launcher.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsAndWallpaperTest {

    @Test
    fun testDefaultSettings() {
        val settings = LauncherSettings()
        assertTrue(settings.showRecentApps)
        assertTrue(settings.recentApps.isEmpty())
        assertFalse(settings.showWallpaper)
        assertEquals(0.80f, settings.wallpaperDimLevel, 0.001f)
    }

    @Test
    fun testRecentAppsDeduplicationAndCapacity() {
        val history = mutableListOf<String>()

        fun record(pkg: String) {
            val updated = (listOf(pkg) + history.filter { it != pkg }).take(5)
            history.clear()
            history.addAll(updated)
        }

        record("app.a")
        record("app.b")
        record("app.c")
        assertEquals(listOf("app.c", "app.b", "app.a"), history)

        // Launch app.b again -> moves to front
        record("app.b")
        assertEquals(listOf("app.b", "app.c", "app.a"), history)

        record("app.d")
        record("app.e")
        record("app.f")
        // Max capacity 5 enforced
        assertEquals(listOf("app.f", "app.e", "app.d", "app.b", "app.c"), history)
        assertEquals(5, history.size)
    }

    @Test
    fun testWallpaperDimLevelCoercion() {
        fun coerceDim(level: Float): Float = level.coerceIn(0.2f, 0.98f)

        assertEquals(0.80f, coerceDim(0.80f), 0.001f)
        assertEquals(0.20f, coerceDim(0.05f), 0.001f)
        assertEquals(0.98f, coerceDim(1.50f), 0.001f)
    }
}
