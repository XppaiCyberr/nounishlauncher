package com.nounish.launcher.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object LauncherHaptic {

    /**
     * Performs a light/tick haptic feedback based on user-configured level.
     * Levels: "subtle", "standard", "firm", "off"
     */
    fun performTick(haptic: HapticFeedback?, level: String = "standard") {
        if (haptic == null || level == "off") return
        try {
            when (level) {
                "subtle" -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                "standard" -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                "firm" -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                else -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {}
    }

    /**
     * Performs a standard/medium haptic feedback.
     */
    fun performMedium(haptic: HapticFeedback?, level: String = "standard") {
        if (haptic == null || level == "off") return
        try {
            when (level) {
                "subtle" -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                "standard" -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                "firm" -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                else -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {}
    }

    /**
     * Performs a strong / long-press haptic feedback.
     */
    fun performLongPress(haptic: HapticFeedback?, level: String = "standard") {
        if (haptic == null || level == "off") return
        try {
            when (level) {
                "subtle" -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                "standard" -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                "firm" -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                else -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {}
    }

    fun performStrong(haptic: HapticFeedback?, level: String = "standard") {
        performLongPress(haptic, level)
    }
}
