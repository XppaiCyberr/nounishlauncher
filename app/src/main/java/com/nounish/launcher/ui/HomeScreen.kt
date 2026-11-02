package com.nounish.launcher.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nounish.launcher.CoinTickerUi
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
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
    clockTimeFormat: String = "system",
    showClockSeconds: Boolean = false,
    dateFormatPattern: String = "EEEE, d MMMM",
    showCrypto: Boolean,
    cryptoCoins: List<CoinTickerUi>,
    onAppClick: (AppItem) -> Unit,
    onAppInfoClick: (AppItem) -> Unit,
    onUnpinApp: (AppItem) -> Unit,
    onRenameApp: (AppItem, String?) -> Unit,
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

    val formattedTime = remember(currentTime, clockTimeFormat, showClockSeconds) {
        val is24 = when (clockTimeFormat) {
            "24h" -> true
            "12h", "12h_no_am_pm" -> false
            else -> android.text.format.DateFormat.is24HourFormat(context)
        }
        val pattern = when {
            is24 && showClockSeconds -> "HH:mm:ss"
            is24 -> "HH:mm"
            clockTimeFormat == "12h_no_am_pm" && showClockSeconds -> "h:mm:ss"
            clockTimeFormat == "12h_no_am_pm" -> "h:mm"
            showClockSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
        SimpleDateFormat(pattern, Locale.getDefault()).format(currentTime)
    }

    val formattedDate = remember(currentTime, dateFormatPattern) {
        try {
            SimpleDateFormat(dateFormatPattern, Locale.getDefault()).format(currentTime)
        } catch (_: Exception) {
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(currentTime)
        }
    }

    var selectedAppForDialog by remember { mutableStateOf<AppItem?>(null) }
    var appToRename by remember { mutableStateOf<AppItem?>(null) }
    var renameInputText by remember { mutableStateOf("") }

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
                        text = formattedTime,
                        style = typography.clock,
                        color = colors.textSecondary
                    )
                }
                if (showDate) {
                    Text(
                        text = formattedDate,
                        style = typography.clock,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Binance Crypto Market Section (middle area)
        if (showCrypto && cryptoCoins.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                cryptoCoins.forEach { coin ->
                    var showDetails by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .clickable { showDetails = !showDetails }
                            .graphicsLayer {
                                alpha = if (coin.isStale) 0.55f else 1.0f
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = coin.symbol,
                                style = typography.drawerItem,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )

                            Text(
                                text = coin.price,
                                style = typography.drawerItem,
                                color = colors.textSecondary
                            )

                            if (coin.changeText.isNotEmpty()) {
                                val changeColor = if (coin.changePercent >= 0.0) CryptoGreen else CryptoRed
                                Text(
                                    text = coin.changeText,
                                    style = typography.priceLabel,
                                    color = changeColor
                                )
                            }

                            // 24h Text sparkline trend
                            if (coin.sparkline.isNotEmpty()) {
                                val sparkColor = if (coin.changePercent >= 0.0) CryptoGreen.copy(alpha = 0.85f) else CryptoRed.copy(alpha = 0.85f)
                                Text(
                                    text = coin.sparkline,
                                    style = typography.priceLabel,
                                    color = sparkColor
                                )
                            }

                            // Stale-data indicator
                            if (coin.isStale && coin.ageMarker.isNotEmpty()) {
                                Text(
                                    text = coin.ageMarker,
                                    style = typography.priceLabel,
                                    color = colors.textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Expandable 24h High/Low details on tap
                        if (showDetails && coin.highPrice.isNotEmpty() && coin.lowPrice.isNotEmpty()) {
                            Text(
                                text = "24h H: ${coin.highPrice}  ·  L: ${coin.lowPrice}",
                                style = typography.clock,
                                color = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
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
                        text = app.displayLabel,
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
                    text = app.displayLabel,
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Rename",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                renameInputText = app.customLabel ?: app.label
                                appToRename = app
                                selectedAppForDialog = null
                            }
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = "Unpin from home",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUnpinApp(app)
                                selectedAppForDialog = null
                            }
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = "App info",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAppInfoClick(app)
                                selectedAppForDialog = null
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedAppForDialog = null }) {
                    Text("Close", color = colors.textSecondary)
                }
            }
        )
    }

    // Rename dialog
    appToRename?.let { app ->
        AlertDialog(
            onDismissRequest = { appToRename = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Rename app",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Original name: ${app.label}",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        textStyle = typography.settingsItem.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    if (app.customLabel != null) {
                        TextButton(onClick = {
                            onRenameApp(app, null)
                            appToRename = null
                        }) {
                            Text("Reset", color = colors.textSecondary)
                        }
                    }
                    TextButton(onClick = {
                        val trimmed = renameInputText.trim()
                        onRenameApp(app, if (trimmed == app.label) null else trimmed)
                        appToRename = null
                    }) {
                        Text("Save", color = colors.textPrimary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { appToRename = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
