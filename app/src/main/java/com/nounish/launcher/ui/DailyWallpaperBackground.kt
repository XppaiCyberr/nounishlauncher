package com.nounish.launcher.ui

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun resolveWallpaperScaleType(scaleMode: String): ImageView.ScaleType = when (scaleMode) {
    "fit_center" -> ImageView.ScaleType.FIT_CENTER
    "fit_xy" -> ImageView.ScaleType.FIT_XY
    else -> ImageView.ScaleType.CENTER_CROP
}

@Composable
fun DailyWallpaperBackground(
    file: File,
    scaleMode: String = "center_crop",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var decodedDrawable by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(file.absolutePath, file.lastModified()) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) return@withContext null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(file)
                    ImageDecoder.decodeDrawable(source)
                } else {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) BitmapDrawable(context.resources, bmp) else null
                }
            } catch (_: Exception) {
                null
            }
        }
        val previous = decodedDrawable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && previous is AnimatedImageDrawable) {
            previous.stop()
        }
        decodedDrawable = loaded
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && loaded is AnimatedImageDrawable) {
            loaded.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                loaded.start()
            }
        }
    }

    // Pause animated WebP when launcher is in background / screen off to save battery
    DisposableEffect(lifecycleOwner, decodedDrawable) {
        val drawable = decodedDrawable
        val observer = LifecycleEventObserver { _, event ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable is AnimatedImageDrawable) {
                when (event) {
                    Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> {
                        if (!drawable.isRunning) drawable.start()
                    }
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                        if (drawable.isRunning) drawable.stop()
                    }
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable is AnimatedImageDrawable) {
                drawable.stop()
            }
        }
    }

    val targetScaleType = resolveWallpaperScaleType(scaleMode)

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = targetScaleType
            }
        },
        update = { imageView ->
            if (imageView.scaleType != targetScaleType) {
                imageView.scaleType = targetScaleType
            }
            if (imageView.drawable !== decodedDrawable) {
                imageView.setImageDrawable(decodedDrawable)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val d = decodedDrawable
                    if (d is AnimatedImageDrawable && !d.isRunning &&
                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    ) {
                        d.start()
                    }
                }
            }
        },
        modifier = modifier
    )
}
