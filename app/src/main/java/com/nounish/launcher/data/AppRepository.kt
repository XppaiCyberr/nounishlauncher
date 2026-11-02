package com.nounish.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.widget.Toast
import com.nounish.launcher.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val launcherApps: LauncherApps? by lazy {
        try {
            context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        } catch (_: Exception) {
            null
        }
    }

    private val packageManager: PackageManager = context.packageManager
    private val cacheFile = File(context.filesDir, "apps_cache.json")

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            reloadApps()
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            reloadApps()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            reloadApps()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            reloadApps()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            reloadApps()
        }
    }

    init {
        try {
            launcherApps?.registerCallback(launcherAppsCallback)
        } catch (_: Exception) {}

        // Cold start optimization:
        // 1. Read last known installed apps from fast local disk cache (< 2 ms)
        // 2. Query LauncherApps asynchronously in the background on Dispatchers.IO
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
     * Called on package change callback or broadcast.
     */
    fun reloadApps() {
        scope.launch(Dispatchers.IO) {
            val list = queryInstalledApps()
            _apps.value = list
            saveCachedAppsToDisk(list)
        }
    }

    private fun queryInstalledApps(): List<AppItem> {
        val appsList = mutableListOf<AppItem>()
        val ownPackage = context.packageName
        val lApps = launcherApps

        if (lApps != null) {
            try {
                val myUser = Process.myUserHandle()
                val profiles = lApps.profiles

                for (profile in profiles) {
                    val isWork = (profile != myUser)
                    val activities = lApps.getActivityList(null, profile)

                    for (info in activities) {
                        val pkgName = info.applicationInfo.packageName
                        if (pkgName == ownPackage) continue

                        val label = info.label?.toString() ?: pkgName
                        val actName = info.componentName.className

                        appsList.add(
                            AppItem(
                                label = if (label.isNotBlank()) label else pkgName,
                                packageName = pkgName,
                                activityName = actName,
                                userHandle = profile,
                                isWorkProfile = isWork
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback to PackageManager if LauncherApps returned nothing or failed
        if (appsList.isEmpty()) {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == ownPackage) continue
                val label = resolveInfo.loadLabel(packageManager).toString()
                val actName = resolveInfo.activityInfo.name
                appsList.add(
                    AppItem(
                        label = if (label.isNotBlank()) label else pkgName,
                        packageName = pkgName,
                        activityName = actName
                    )
                )
            }
        }

        return appsList.sortedWith(AppItem.COMPARATOR)
    }

    private fun loadCachedAppsFromDisk(): List<AppItem> {
        return try {
            if (!cacheFile.exists()) return emptyList()
            val content = cacheFile.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            val array = JSONArray(content)
            val list = mutableListOf<AppItem>()
            val myUser = try { Process.myUserHandle() } catch (_: Exception) { null }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isWork = obj.optBoolean("isWorkProfile", false)
                list.add(
                    AppItem(
                        label = obj.getString("label"),
                        packageName = obj.getString("packageName"),
                        activityName = obj.optString("activityName").takeIf { it.isNotBlank() },
                        userHandle = if (isWork) null else myUser,
                        isWorkProfile = isWork
                    )
                )
            }
            list.sortedWith(AppItem.COMPARATOR)
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
                obj.put("isWorkProfile", app.isWorkProfile)
                array.put(obj)
            }
            cacheFile.writeText(array.toString(), Charsets.UTF_8)
        } catch (_: Exception) {}
    }

    fun launchApp(item: AppItem) {
        try {
            // Work profile or specific user profile launch via LauncherApps
            if (item.userHandle != null && item.activityName != null && launcherApps != null) {
                launcherApps?.startMainActivity(
                    ComponentName(item.packageName, item.activityName),
                    item.userHandle,
                    null,
                    null
                )
                return
            }

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

    fun launchPackage(keyOrPackage: String) {
        val app = _apps.value.find { it.key == keyOrPackage || it.packageName == keyOrPackage }
        if (app != null) {
            launchApp(app)
        } else {
            val cleanPkg = keyOrPackage.substringBefore("/").substringBefore("#")
            try {
                val intent = packageManager.getLaunchIntentForPackage(cleanPkg)?.apply {
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

    fun openAppInfo(item: AppItem) {
        try {
            if (item.userHandle != null && item.activityName != null && launcherApps != null) {
                launcherApps?.startAppDetailsActivity(
                    ComponentName(item.packageName, item.activityName),
                    item.userHandle,
                    null,
                    null
                )
                return
            }

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${item.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open App Info", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(item: AppItem) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${item.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot uninstall app", Toast.LENGTH_SHORT).show()
        }
    }
}
