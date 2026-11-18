package com.nounish.launcher.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object SystemNavigationHelper {

    fun openSystemNavigationSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        // 1. Android 10+ system gesture navigation settings
        intents.add(Intent("android.settings.SYSTEM_GESTURE_SETTINGS"))

        // 2. AOSP / Pixel gesture navigation settings
        intents.add(Intent("com.android.settings.GESTURE_NAVIGATION_SETTINGS"))

        // 3. Xiaomi / HyperOS / MIUI Full screen display settings
        intents.add(Intent("com.android.settings.action.FULL_SCREEN_DISPLAY"))
        intents.add(
            Intent(Intent.ACTION_MAIN).setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$FullScreenDisplaySettingsActivity"
            )
        )

        // 4. Samsung One UI Navigation Bar settings
        intents.add(Intent("com.samsung.settings.NAVIGATION_BAR_SETTINGS"))
        intents.add(Intent("com.android.settings.NAVIGATION_BAR_SETTINGS"))

        // 5. Generic display settings where navigation bar is often found
        intents.add(Intent(Settings.ACTION_DISPLAY_SETTINGS))

        // 6. Main device settings fallback
        intents.add(Intent(Settings.ACTION_SETTINGS))

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        return try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open system settings", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
