package com.nounish.launcher.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast

object NotificationHelper {

    /**
     * Expands the system notification shade.
     * Uses reflection on StatusBarManager.expandNotificationsPanel() with EXPAND_STATUS_BAR permission,
     * with graceful fallback to StatusBarManager.expand() and LockScreenService accessibility action.
     */
    @SuppressLint("WrongConstant")
    fun expandNotificationShade(context: Context): Boolean {
        // 1. Preferred modern reflection on StatusBarManager.expandNotificationsPanel()
        try {
            val statusBarService = context.getSystemService("statusbar")
            if (statusBarService != null) {
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                val method = statusBarManager.getMethod("expandNotificationsPanel")
                method.isAccessible = true
                method.invoke(statusBarService)
                return true
            }
        } catch (_: Exception) {}

        // 2. Fallback to StatusBarManager.expand()
        try {
            val statusBarService = context.getSystemService("statusbar")
            if (statusBarService != null) {
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                val method = statusBarManager.getMethod("expand")
                method.isAccessible = true
                method.invoke(statusBarService)
                return true
            }
        } catch (_: Exception) {}

        // 3. Fallback to AccessibilityService if active
        if (LockScreenService.isServiceRunning()) {
            val service = LockScreenService.getService()
            if (service != null && service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) {
                return true
            }
        }

        Toast.makeText(context, "Unable to open notifications panel", Toast.LENGTH_SHORT).show()
        return false
    }
}
