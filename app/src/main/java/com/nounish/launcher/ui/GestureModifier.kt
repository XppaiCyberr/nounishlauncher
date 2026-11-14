package com.nounish.launcher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun Modifier.launcherGestures(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onTap: () -> Unit = {},
    onPullDownDelta: ((Float) -> Unit)? = null,
    onPullDownRelease: (() -> Unit)? = null
): Modifier {
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnPullDownDelta by rememberUpdatedState(onPullDownDelta)
    val currentOnPullDownRelease by rememberUpdatedState(onPullDownRelease)

    return this
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { currentOnTap() },
                onDoubleTap = { currentOnDoubleTap() },
                onLongPress = { currentOnLongPress() }
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
                    currentOnPullDownRelease?.invoke()

                    if (absY > absX && absY > swipeThreshold) {
                        if (totalY < -swipeThreshold) {
                            currentOnSwipeUp()
                        } else if (totalY > swipeThreshold) {
                            currentOnSwipeDown()
                        }
                    } else if (absX > absY && absX > swipeThreshold) {
                        if (totalX < -swipeThreshold) {
                            currentOnSwipeLeft()
                        } else if (totalX > swipeThreshold) {
                            currentOnSwipeRight()
                        }
                    }
                },
                onDragCancel = {
                    currentOnPullDownRelease?.invoke()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    totalX += dragAmount.x
                    totalY += dragAmount.y

                    if (totalY > 0f && abs(totalY) > abs(totalX)) {
                        currentOnPullDownDelta?.invoke(totalY)
                    } else if (totalY <= 0f) {
                        currentOnPullDownDelta?.invoke(0f)
                    }
                }
            )
        }
}
