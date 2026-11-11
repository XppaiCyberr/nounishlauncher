package com.nounish.launcher.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nounish.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object NounsImageCache {
    private val memoryCache = LruCache<Int, ImageBitmap>(30)

    fun getNounPicUrl(nounId: Int): String {
        return "https://noun.pics/$nounId.png"
    }

    fun get(nounId: Int): ImageBitmap? {
        return memoryCache.get(nounId)
    }

    fun put(nounId: Int, bitmap: ImageBitmap) {
        memoryCache.put(nounId, bitmap)
    }

    suspend fun loadNounImage(context: Context, nounId: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(nounId)?.let { return@withContext it }

        val cacheDir = File(context.cacheDir, "noun_pics")
        val cacheFile = File(cacheDir, "noun_$nounId.png")

        // 1. Try disk cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bmp != null) {
                    val imageBitmap = bmp.asImageBitmap()
                    memoryCache.put(nounId, imageBitmap)
                    return@withContext imageBitmap
                }
            } catch (_: Exception) {}
        }

        // 2. Fetch from endpoint https://noun.pics/{nounId}.png
        var conn: HttpURLConnection? = null
        try {
            val url = URL(getNounPicUrl(nounId))
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    // Persist to disk cache
                    try {
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        cacheFile.writeBytes(bytes)
                    } catch (_: Exception) {}

                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val imageBitmap = bmp.asImageBitmap()
                        memoryCache.put(nounId, imageBitmap)
                        return@withContext imageBitmap
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback when offline or timeout
        } finally {
            conn?.disconnect()
        }

        null
    }

    fun clear() {
        memoryCache.evictAll()
    }
}

@Composable
fun NounsAvatar(
    nounId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showBorder: Boolean = true
) {
    val context = LocalContext.current
    val colors = LauncherTheme.colors
    var bitmap by remember(nounId) { mutableStateOf(NounsImageCache.get(nounId)) }

    LaunchedEffect(nounId) {
        if (bitmap == null) {
            val loaded = NounsImageCache.loadNounImage(context, nounId)
            bitmap = loaded
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface.copy(alpha = 0.5f))
            .then(
                if (showBorder) {
                    Modifier.border(0.5.dp, colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = "Noun #$nounId",
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "⌐◨-◨",
                fontSize = (size.value * 0.28f).coerceAtLeast(8f).sp,
                color = colors.textSecondary.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
