package com.nounish.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.nounish.launcher.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val packageManager: PackageManager = context.packageManager
    private val cacheFile = File(context.filesDir, "apps_cache.json")

    init {
        // Cold start optimization:
        // 1. Read last known installed apps from fast local disk cache (< 2 ms)
        // 2. Query PackageManager asynchronously in the background on Dispatchers.IO
        scope.launch(Dispatchers.IO) {
            val cached = loadCachedAppsFromDisk()
            if (cached.isNotEmpty()) {
                _apps.value = cached
            }
            val fresh = queryInstalledApps()
            _apps.value = fresh
            saveCachedAppsToDisk(fresh)
        }
    }

    /**
     * Refreshes installed applications.
     * Should ONLY be called on package change broadcast (install / uninstall / update).
     */
    fun reloadApps() {
        scope.launch(Dispatchers.IO) {
            val list = queryInstalledApps()
            _apps.value = list
            saveCachedAppsToDisk(list)
        }
    }

    private fun queryInstalledApps(): List<AppItem> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val ownPackage = context.packageName

        return resolveInfos
            .filter { it.activityInfo.packageName != ownPackage }
            .map { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager).toString()
                val pkgName = resolveInfo.activityInfo.packageName
                val actName = resolveInfo.activityInfo.name
                AppItem(
                    label = if (label.isNotBlank()) label else pkgName,
                    packageName = pkgName,
                    activityName = actName
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun loadCachedAppsFromDisk(): List<AppItem> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            val content = cacheFile.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            val array = JSONArray(content)
            val list = mutableListOf<AppItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AppItem(
                        label = obj.getString("label"),
                        packageName = obj.getString("packageName"),
                        activityName = obj.optString("activityName").takeIf { it.isNotBlank() }
                    )
                )
            }
            list.sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCachedAppsToDisk(list: List<AppItem>) {
        try {
            val array = JSONArray()
            for (app in list) {
                val obj = JSONObject()
                obj.put("label", app.label)
                obj.put("packageName", app.packageName)
                obj.put("activityName", app.activityName ?: "")
                array.put(obj)
            }
            cacheFile.writeText(array.toString(), Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    fun launchApp(item: AppItem) {
        try {
            val intent = if (item.activityName != null) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(item.packageName, item.activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            } else {
                packageManager.getLaunchIntentForPackage(item.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            }

            if (intent != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPackage(packageName: String) {
        val app = _apps.value.find { it.packageName == packageName }
        if (app != null) {
            launchApp(app)
        } else {
            try {
                val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (intent != null) {
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open App Info", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot uninstall app", Toast.LENGTH_SHORT).show()
        }
    }
}
