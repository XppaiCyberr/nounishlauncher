package com.nounish.launcher.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager

object NetworkHelper {

    fun isMeteredOrMobile(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } catch (_: Exception) {
            false
        }
    }

    fun isPowerSaveMode(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
            pm.isPowerSaveMode
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Determines poll interval based on network condition and battery saver:
     * - Unmetered Wi-Fi: 15 seconds
     * - Mobile data or Battery saver: 60 seconds (4x longer)
     * - Both Mobile data AND Battery saver: 90 seconds (6x longer)
     */
    fun getBasePollIntervalMs(context: Context): Long {
        val isMobile = isMeteredOrMobile(context)
        val isBatterySaver = isPowerSaveMode(context)
        return when {
            isMobile && isBatterySaver -> 90_000L
            isMobile || isBatterySaver -> 60_000L
            else -> 15_000L
        }
    }
}
