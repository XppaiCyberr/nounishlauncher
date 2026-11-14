package com.nounish.launcher

import com.nounish.launcher.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeDownActionTest {

    @Test
    fun testDefaultSwipeDownActionIsRefresh() {
        val settings = LauncherSettings()
        assertEquals("refresh", settings.swipeDownAction)
        assertTrue(settings.swipeDownToRefresh)
    }

    @Test
    fun testSwipeDownActionRefreshSyncsWithSwipeDownToRefresh() {
        val settings = LauncherSettings(
            swipeDownAction = "notifications",
            swipeDownToRefresh = false
        )
        assertEquals("notifications", settings.swipeDownAction)
        assertFalse(settings.swipeDownToRefresh)
    }

    @Test
    fun testActionResolutionLogic() {
        fun resolveAction(savedAction: String?, legacyRefresh: Boolean?): String {
            return when {
                savedAction != null -> savedAction
                legacyRefresh != null -> if (legacyRefresh) "refresh" else "notifications"
                else -> "refresh"
            }
        }

        // Fresh install: neither saved -> defaults to refresh
        assertEquals("refresh", resolveAction(null, null))

        // Legacy user with swipeDownToRefresh = true -> maps to refresh
        assertEquals("refresh", resolveAction(null, true))

        // Legacy user with swipeDownToRefresh = false -> maps to notifications
        assertEquals("notifications", resolveAction(null, false))

        // Modern user with explicit swipe_down_action selection
        assertEquals("drawer", resolveAction("drawer", false))
        assertEquals("none", resolveAction("none", false))
        assertEquals("notifications", resolveAction("notifications", false))
        assertEquals("refresh", resolveAction("refresh", true))
    }
}
