package com.nounish.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.nounish.launcher.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IconCache {
    private val memoryCache = LruCache<String, ImageBitmap>(200)

    fun get(key: String): ImageBitmap? {
        return memoryCache.get(key)
    }

    fun loadIcon(context: Context, item: AppItem, targetSize: Int = 96): ImageBitmap? {
        memoryCache.get(item.key)?.let { return it }

        val drawable: Drawable? = try {
            val lApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val user = item.userHandle ?: Process.myUserHandle()
            val actList = lApps?.getActivityList(item.packageName, user)
            val match = actList?.find {
                item.activityName == null || it.componentName.className == item.activityName
            }
            match?.getIcon(0) ?: if (item.activityName != null) {
                try {
                    context.packageManager.getActivityIcon(ComponentName(item.packageName, item.activityName))
                } catch (_: Exception) {
                    context.packageManager.getApplicationIcon(item.packageName)
                }
            } else {
                context.packageManager.getApplicationIcon(item.packageName)
            }
        } catch (_: Exception) {
            try {
                context.packageManager.getApplicationIcon(item.packageName)
            } catch (_: Exception) {
                null
            }
        }

        if (drawable == null) return null

        val imageBitmap = try {
            val bmp = drawableToBitmap(drawable, targetSize, targetSize)
            bmp.asImageBitmap()
        } catch (_: Exception) {
            null
        }

        if (imageBitmap != null) {
            memoryCache.put(item.key, imageBitmap)
        }

        return imageBitmap
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return if (drawable.bitmap.width == width && drawable.bitmap.height == height) {
                drawable.bitmap
            } else {
                Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
            }
        }
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return if (w != width || h != height) {
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
            if (scaled != bitmap) {
                bitmap.recycle()
            }
            scaled
        } else {
            bitmap
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }
}

@Composable
fun AppIcon(
    item: AppItem,
    modifier: Modifier = Modifier,
    isMonochrome: Boolean = true
) {
    val context = LocalContext.current
    var bitmap by remember(item.key) { mutableStateOf(IconCache.get(item.key)) }

    LaunchedEffect(item.key) {
        if (bitmap == null) {
            withContext(Dispatchers.IO) {
                val loaded = IconCache.loadIcon(context, item)
                withContext(Dispatchers.Main) {
                    bitmap = loaded
                }
            }
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        val colorFilter = if (isMonochrome) {
            remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
        } else {
            null
        }

        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            modifier = modifier,
            colorFilter = colorFilter,
            alpha = if (isMonochrome) 0.85f else 1.0f
        )
    } else {
        Spacer(modifier = modifier)
    }
}
