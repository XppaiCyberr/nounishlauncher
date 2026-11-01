package com.nounish.launcher.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    pinnedApps: List<AppItem>,
    showClock: Boolean,
    showDate: Boolean,
    onAppClick: (AppItem) -> Unit,
    onAppInfoClick: (AppItem) -> Unit,
    onUnpinApp: (AppItem) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current

    // Live clock update every second
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    var selectedAppForDialog by remember { mutableStateOf<AppItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .launcherGestures(
                onSwipeUp = onOpenDrawer,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                onDoubleTap = onDoubleTap,
                onLongPress = onOpenSettings
            )
            .padding(horizontal = 24.dp)
    ) {
        // Clock / Date in top left
        if (showClock || showDate) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp)
                    .combinedClickable(
                        onClick = {
                            try {
                                val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(clockIntent)
                            } catch (_: Exception) {}
                        },
                        onLongClick = onOpenSettings
                    )
            ) {
                if (showClock) {
                    Text(
                        text = timeFormat.format(currentTime),
                        style = typography.clock,
                        color = colors.textSecondary
                    )
                }
                if (showDate) {
                    Text(
                        text = dateFormat.format(currentTime),
                        style = typography.clock,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Pinned shortcuts positioned in bottom-middle area for optimal one-hand reach
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (pinnedApps.isEmpty()) {
                Text(
                    text = "Swipe up for all apps\nLong-press an app to pin here",
                    style = typography.drawerItem,
                    color = colors.textSecondary
                )
            } else {
                pinnedApps.take(8).forEach { app ->
                    Text(
                        text = app.label,
                        style = typography.homeShortcut,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAppClick(app) },
                                onLongClick = { selectedAppForDialog = app }
                            )
                    )
                }
            }
        }
    }

    // App action dialog
    selectedAppForDialog?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForDialog = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = app.label,
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = app.packageName,
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUnpinApp(app)
                    selectedAppForDialog = null
                }) {
                    Text("Unpin", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onAppInfoClick(app)
                    selectedAppForDialog = null
                }) {
                    Text("App Info", color = colors.textSecondary)
                }
            }
        )
    }
}
