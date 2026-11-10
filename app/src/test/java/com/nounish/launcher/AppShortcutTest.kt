package com.nounish.launcher

import com.nounish.launcher.model.AppItem
import com.nounish.launcher.model.AppShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutTest {

    @Test
    fun testAppShortcutCreationAndProperties() {
        val shortcut = AppShortcut(
            id = "new_tab",
            packageName = "com.android.chrome",
            label = "New Tab",
            shortLabel = "New Tab",
            longLabel = "Open a new tab",
            rank = 1,
            isEnabled = true
        )

        assertEquals("new_tab", shortcut.id)
        assertEquals("com.android.chrome", shortcut.packageName)
        assertEquals("New Tab", shortcut.label)
        assertEquals("New Tab", shortcut.shortLabel)
        assertEquals("Open a new tab", shortcut.longLabel)
        assertEquals(1, shortcut.rank)
        assertTrue(shortcut.isEnabled)
    }

    @Test
    fun testShortcutLabelFallbackLogic() {
        fun resolveLabel(id: String, shortLabel: String?, longLabel: String?): String {
            val short = shortLabel?.trim()
            val long = longLabel?.trim()
            return when {
                !short.isNullOrBlank() -> short
                !long.isNullOrBlank() -> long
                else -> id
            }
        }

        // Short label takes precedence
        assertEquals("New tab", resolveLabel("new_tab", "New tab", "Open a new tab"))

        // Blank short label falls back to long label
        assertEquals("Open a new tab", resolveLabel("new_tab", "   ", "Open a new tab"))

        // Null short label falls back to long label
        assertEquals("Open a new tab", resolveLabel("new_tab", null, "Open a new tab"))

        // Both null or blank fall back to shortcut id
        assertEquals("new_tab", resolveLabel("new_tab", null, null))
        assertEquals("new_tab", resolveLabel("new_tab", "", "   "))
    }

    @Test
    fun testShortcutFilteringAndSorting() {
        val shortcuts = listOf(
            AppShortcut(id = "s3", packageName = "com.test", label = "Three", rank = 3, isEnabled = true),
            AppShortcut(id = "s1", packageName = "com.test", label = "One", rank = 0, isEnabled = true),
            AppShortcut(id = "s_disabled", packageName = "com.test", label = "Disabled", rank = 1, isEnabled = false),
            AppShortcut(id = "s2", packageName = "com.test", label = "Two", rank = 2, isEnabled = true),
            AppShortcut(id = "s1_dup", packageName = "com.test", label = "One Dup", rank = 0, isEnabled = true)
        )

        val processed = shortcuts
            .filter { it.isEnabled }
            .sortedWith(compareBy({ it.rank }, { it.id }))
            .take(5)

        assertEquals(4, processed.size)
        assertEquals("s1", processed[0].id)
        assertEquals("s1_dup", processed[1].id)
        assertEquals("s2", processed[2].id)
        assertEquals("s3", processed[3].id)
        assertFalse(processed.any { it.id == "s_disabled" })
    }

    @Test
    fun testShortcutMaxCountEnforced() {
        val shortcuts = (1..10).map { i ->
            AppShortcut(
                id = "shortcut_$i",
                packageName = "com.test",
                label = "Shortcut $i",
                rank = i,
                isEnabled = true
            )
        }

        val limited = shortcuts
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(5)

        assertEquals(5, limited.size)
        assertEquals("shortcut_1", limited.first().id)
        assertEquals("shortcut_5", limited.last().id)
    }
}
