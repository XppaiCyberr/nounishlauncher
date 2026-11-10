package com.nounish.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.lang.ref.WeakReference

class LockScreenService : AccessibilityService() {

    companion object {
        private var serviceRef: WeakReference<LockScreenService>? = null

        fun isServiceRunning(): Boolean = serviceRef?.get() != null
        fun getService(): LockScreenService? = serviceRef?.get()

        fun lockScreen(context: Context): Boolean {
            val service = serviceRef?.get()
            return if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                true
            } else {
                Toast.makeText(
                    context,
                    "Enable Nounish Lock in Accessibility settings to double-tap lock",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event handling needed; strictly for lock screen global action
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (serviceRef?.get() == this) {
            serviceRef = null
        }
    }
}
