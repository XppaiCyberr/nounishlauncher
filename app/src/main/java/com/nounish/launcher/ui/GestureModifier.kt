package com.nounish.launcher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.launcherGestures(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onTap: () -> Unit = {}
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { onDoubleTap() },
            onLongPress = { onLongPress() }
        )
    }
    .pointerInput(Unit) {
        var totalX = 0f
        var totalY = 0f
        val swipeThreshold = 80f

        detectDragGestures(
            onDragStart = {
                totalX = 0f
                totalY = 0f
            },
            onDragEnd = {
                val absX = abs(totalX)
                val absY = abs(totalY)

                if (absY > absX && absY > swipeThreshold) {
                    if (totalY < -swipeThreshold) {
                        onSwipeUp()
                    } else if (totalY > swipeThreshold) {
                        onSwipeDown()
                    }
                } else if (absX > absY && absX > swipeThreshold) {
                    if (totalX < -swipeThreshold) {
                        onSwipeLeft()
                    } else if (totalX > swipeThreshold) {
                        onSwipeRight()
                    }
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                totalX += dragAmount.x
                totalY += dragAmount.y
            }
        )
    }
