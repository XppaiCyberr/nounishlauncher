package com.nounish.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class DailyWallpaperEntry(
    val id: String,
    val url: String,
    val animated: Boolean = false
)

data class DailyWallpaperState(
    val file: File? = null,
    val activeEntry: DailyWallpaperEntry? = null,
    val activeIndex: Int = 0,
    val totalCount: Int = 0
)

class DailyWallpaperRepository(private val context: Context) {

    companion object {
        const val MANIFEST_URL = "https://pip.xpp.wtf/xpp.json"
        private const val CACHE_DIR_NAME = "daily_wallpapers"
        private const val MANIFEST_FILE_NAME = "manifest.json"
    }

    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    private val manifestFile: File
        get() = File(cacheDir, MANIFEST_FILE_NAME)

    /**
     * Loads the cached wallpaper immediately if available on disk for today's index,
     * with zero network delay.
     */
    fun getCachedWallpaperSync(manualOffset: Int): DailyWallpaperState {
        val entries = readCachedManifest()
        if (entries.isEmpty()) return DailyWallpaperState()
        val index = computeDailyIndex(entries.size, manualOffset)
        val entry = entries[index]
        val file = cacheFileFor(entry)
        return DailyWallpaperState(
            file = if (file.exists() && file.length() > 0L) file else null,
            activeEntry = entry,
            activeIndex = index,
            totalCount = entries.size
        )
    }

    /**
     * Refreshes the manifest from [MANIFEST_URL] (falling back to cached manifest if offline),
     * computes today's wallpaper index, downloads the .webp to local cache if not already cached,
     * and returns the ready [DailyWallpaperState].
     */
    suspend fun syncTodayWallpaper(
        manualOffset: Int,
        forceManifestRefresh: Boolean = false
    ): DailyWallpaperState = withContext(Dispatchers.IO) {
        var entries = if (!forceManifestRefresh && isManifestFreshToday()) {
            readCachedManifest()
        } else {
            emptyList()
        }

        if (entries.isEmpty()) {
            val fetchedJson = fetchString(MANIFEST_URL)
            if (!fetchedJson.isNullOrBlank()) {
                val parsed = parseManifest(fetchedJson)
                if (parsed.isNotEmpty()) {
                    entries = parsed
                    try {
                        manifestFile.writeText(fetchedJson)
                    } catch (_: Exception) {
                    }
                    cleanupStaleFiles(entries)
                }
            }
        }

        if (entries.isEmpty()) {
            entries = readCachedManifest()
        }

        if (entries.isEmpty()) {
            return@withContext DailyWallpaperState()
        }

        val index = computeDailyIndex(entries.size, manualOffset)
        val entry = entries[index]
        val targetFile = cacheFileFor(entry)

        if (!targetFile.exists() || targetFile.length() == 0L) {
            downloadFile(entry.url, targetFile)
        }

        val readyFile = if (targetFile.exists() && targetFile.length() > 0L) {
            targetFile
        } else {
            // Fallback to any existing cached wallpaper if offline and today's isn't downloaded yet
            entries.firstNotNullOfOrNull { candidate ->
                val f = cacheFileFor(candidate)
                if (f.exists() && f.length() > 0L) f else null
            }
        }

        DailyWallpaperState(
            file = readyFile,
            activeEntry = entry,
            activeIndex = index,
            totalCount = entries.size
        )
    }

    fun computeRandomOffset(size: Int, currentOffset: Int): Int {
        if (size <= 1) return currentOffset
        val currentIdx = computeDailyIndex(size, currentOffset)
        val candidates = (0 until size).filter { it != currentIdx }
        val targetIdx = candidates.random()
        val delta = ((targetIdx - currentIdx) % size + size) % size
        return currentOffset + delta
    }

    suspend fun getManifestCount(forceManifestRefresh: Boolean = false): Int = withContext(Dispatchers.IO) {
        if (!forceManifestRefresh && isManifestFreshToday()) {
            val cached = readCachedManifest()
            if (cached.isNotEmpty()) return@withContext cached.size
        }
        val fetchedJson = fetchString(MANIFEST_URL)
        if (!fetchedJson.isNullOrBlank()) {
            val parsed = parseManifest(fetchedJson)
            if (parsed.isNotEmpty()) {
                try {
                    manifestFile.writeText(fetchedJson)
                } catch (_: Exception) {
                }
                cleanupStaleFiles(parsed)
                return@withContext parsed.size
            }
        }
        readCachedManifest().size
    }

    private fun computeDailyIndex(size: Int, manualOffset: Int): Int {
        if (size <= 0) return 0
        val epochDay = try {
            LocalDate.now().toEpochDay()
        } catch (_: Exception) {
            System.currentTimeMillis() / 86_400_000L
        }
        val raw = (epochDay + manualOffset) % size.toLong()
        return ((raw + size) % size).toInt()
    }

    private fun isManifestFreshToday(): Boolean {
        val f = manifestFile
        if (!f.exists() || f.length() == 0L) return false
        val ageMs = System.currentTimeMillis() - f.lastModified()
        return ageMs in 0L..6 * 60 * 60 * 1000L // 6 hours
    }

    private fun readCachedManifest(): List<DailyWallpaperEntry> {
        return try {
            val f = manifestFile
            if (!f.exists()) return emptyList()
            parseManifest(f.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseManifest(raw: String): List<DailyWallpaperEntry> {
        return try {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("wallpapers") ?: return emptyList()
            val result = ArrayList<DailyWallpaperEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", i.toString()).trim()
                val url = obj.optString("url", "").trim()
                val animated = obj.optBoolean("animated", false)
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    result.add(DailyWallpaperEntry(id = id, url = url, animated = animated))
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun cacheFileFor(entry: DailyWallpaperEntry): File {
        val safeId = entry.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val urlHash = Integer.toHexString(entry.url.hashCode())
        return File(cacheDir, "wp_${safeId}_${urlHash}.webp")
    }

    private fun cleanupStaleFiles(validEntries: List<DailyWallpaperEntry>) {
        try {
            val validNames = validEntries.map { cacheFileFor(it).name }.toSet() + MANIFEST_FILE_NAME
            cacheDir.listFiles()?.forEach { file ->
                if (file.name !in validNames) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun fetchString(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun downloadFile(urlStr: String, targetFile: File): Boolean {
        var conn: HttpURLConnection? = null
        val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                if (tmpFile.exists() && tmpFile.length() > 0L) {
                    if (targetFile.exists()) targetFile.delete()
                    tmpFile.renameTo(targetFile)
                } else {
                    tmpFile.delete()
                    false
                }
            } else {
                false
            }
        } catch (_: Exception) {
            tmpFile.delete()
            false
        } finally {
            conn?.disconnect()
        }
    }
}
