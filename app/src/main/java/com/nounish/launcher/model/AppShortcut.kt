package com.nounish.launcher.model

import android.os.UserHandle

/**
 * Represents a deep Android shortcut (dynamic, manifest, or pinned)
 * provided by an installed application via LauncherApps.
 */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val shortLabel: String? = null,
    val longLabel: String? = null,
    val rank: Int = 0,
    val isEnabled: Boolean = true,
    val disabledMessage: String? = null,
    val userHandle: UserHandle? = null
)
