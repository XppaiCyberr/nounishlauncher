package com.nounish.launcher.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nounish.launcher.data.LauncherSettings
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.service.LockScreenService
import com.nounish.launcher.ui.theme.LauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    allApps: List<AppItem>,
    onToggleShowClock: (Boolean) -> Unit,
    onUpdateClockTimeFormat: (String) -> Unit,
    onToggleClockSeconds: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onUpdateDateFormatPattern: (String) -> Unit,
    onToggleAutoLaunch: (Boolean) -> Unit,
    onSelectSwipeLeftApp: (String?) -> Unit,
    onSelectSwipeRightApp: (String?) -> Unit,
    onToggleDoubleTapLock: (Boolean) -> Unit,
    onToggleCryptoEnabled: (Boolean) -> Unit,
    onToggleShowHomeCrypto: (Boolean) -> Unit,
    onToggleShowHomeIdx: (Boolean) -> Unit = {},
    onUpdateIdxDisplayMode: (String) -> Unit = {},
    onUpdateIdxSymbols: (List<String>) -> Unit = {},
    onUpdateIdxAutoRotateInterval: (Int) -> Unit = {},
    onToggleMarketCarouselAutoRotate: (Boolean) -> Unit = {},
    onUpdateMarketCarouselInterval: (Int) -> Unit = {},
    onUpdateMarketCarouselTransition: (String) -> Unit = {},
    onToggleSeparateMarketViews: (Boolean) -> Unit = {},
    onUpdateSeparatedMarketLayout: (String) -> Unit = {},
    onToggleShowDrawerUsdRate: (Boolean) -> Unit = {},
    onToggleWebSocketOnHome: (Boolean) -> Unit,
    onUpdateCryptoSymbols: (List<String>) -> Unit,
    onUpdateCryptoQuote: (String) -> Unit,
    onToggleLightTheme: (Boolean) -> Unit,
    onToggleMonospace: (Boolean) -> Unit,
    onUpdateFontSize: (String) -> Unit,
    onToggleShowAppIcons: (Boolean) -> Unit,
    onToggleMonochromeIcons: (Boolean) -> Unit,
    onToggleIndexOnRight: (Boolean) -> Unit,
    onToggleIndexAnimation: (Boolean) -> Unit,
    onReorderPinnedApps: (List<String>) -> Unit,
    onUnpinApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
    onCloseSettings: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current

    var showAppPickerForSwipe by remember { mutableStateOf<String?>(null) } // "left" or "right"
    var showEditCoinsDialog by remember { mutableStateOf(false) }
    var showEditQuoteDialog by remember { mutableStateOf(false) }
    var showIdxModeDialog by remember { mutableStateOf(false) }
    var showIdxSymbolsDialog by remember { mutableStateOf(false) }
    var showClockFormatDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    val swipeLeftAppName = remember(settings.swipeLeftPackage, allApps) {
        allApps.find { it.packageName == settings.swipeLeftPackage }?.displayLabel ?: "None"
    }
    val swipeRightAppName = remember(settings.swipeRightPackage, allApps) {
        allApps.find { it.packageName == settings.swipeRightPackage }?.displayLabel ?: "None"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = typography.settingsHeading,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Done",
                        style = typography.settingsItem,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clickable(onClick = onCloseSettings)
                            .padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Default Launcher button
            item {
                SettingsActionRow(
                    title = "Set as default launcher",
                    subtitle = "Set Nounish Launcher as your home app",
                    onClick = { openDefaultLauncherSettings(context) }
                )
            }

            // Home Screen Section
            item {
                SettingsSectionHeader(title = "Home Screen")
            }

            item {
                SettingsSwitchRow(
                    title = "Show clock",
                    checked = settings.showClock,
                    onCheckedChange = onToggleShowClock
                )
            }

            if (settings.showClock) {
                val clockFormatLabel = when (settings.clockTimeFormat) {
                    "24h" -> "24-hour (14:30)"
                    "12h" -> "12-hour with AM/PM (2:30 PM)"
                    "12h_no_am_pm" -> "12-hour (2:30)"
                    else -> "System default"
                }

                item {
                    SettingsActionRow(
                        title = "Clock format",
                        subtitle = clockFormatLabel,
                        onClick = { showClockFormatDialog = true }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Show seconds",
                        subtitle = if (settings.showClockSeconds) "Seconds displayed (:ss)" else "Seconds hidden",
                        checked = settings.showClockSeconds,
                        onCheckedChange = onToggleClockSeconds
                    )
                }
            }

            item {
                SettingsSwitchRow(
                    title = "Show date",
                    checked = settings.showDate,
                    onCheckedChange = onToggleShowDate
                )
            }

            if (settings.showDate) {
                val datePreview = try {
                    SimpleDateFormat(settings.dateFormatPattern, Locale.getDefault()).format(Date())
                } catch (_: Exception) {
                    settings.dateFormatPattern
                }

                item {
                    SettingsActionRow(
                        title = "Date format",
                        subtitle = datePreview,
                        onClick = { showDateFormatDialog = true }
                    )
                }
            }

            // App Drawer Section
            item {
                SettingsSectionHeader(title = "App Drawer")
            }

            item {
                SettingsSwitchRow(
                    title = "Auto-launch on single match",
                    subtitle = "Instantly open app when search yields 1 result",
                    checked = settings.autoLaunchSingleMatch,
                    onCheckedChange = onToggleAutoLaunch
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Alphabet index on right",
                    subtitle = if (settings.alphabetIndexOnRight) "Index on the right edge" else "Index on the left edge",
                    checked = settings.alphabetIndexOnRight,
                    onCheckedChange = onToggleIndexOnRight
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Alphabet index animation",
                    subtitle = if (settings.enableIndexAnimation) "Fluid wave animation enabled" else "Static list (no wave animation)",
                    checked = settings.enableIndexAnimation,
                    onCheckedChange = onToggleIndexAnimation
                )
            }

            // Gestures Section
            item {
                SettingsSectionHeader(title = "Gestures")
            }

            item {
                SettingsActionRow(
                    title = "Swipe left app",
                    subtitle = swipeLeftAppName,
                    onClick = { showAppPickerForSwipe = "left" }
                )
            }

            item {
                SettingsActionRow(
                    title = "Swipe right app",
                    subtitle = swipeRightAppName,
                    onClick = { showAppPickerForSwipe = "right" }
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Double tap to lock screen",
                    subtitle = "Requires accessibility permission",
                    checked = settings.doubleTapLock,
                    onCheckedChange = { enabled ->
                        if (enabled && !LockScreenService.isServiceRunning()) {
                            LockScreenService.lockScreen(context)
                        }
                        onToggleDoubleTapLock(enabled)
                    }
                )
            }

            // Appearance Section
            item {
                SettingsSectionHeader(title = "Appearance")
            }

            item {
                SettingsSwitchRow(
                    title = "Light theme",
                    subtitle = if (settings.isLightTheme) "Light theme" else "AMOLED Black (Pure #000000)",
                    checked = settings.isLightTheme,
                    onCheckedChange = onToggleLightTheme
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Monospace font",
                    subtitle = if (settings.useMonospace) "Monospace" else "System default sans",
                    checked = settings.useMonospace,
                    onCheckedChange = onToggleMonospace
                )
            }

            val fontSizeLabel = when (settings.fontSize) {
                "micro", "tiny" -> "Micro (65%)"
                "extra_small" -> "Extra Small (75%)"
                "small" -> "Small (85%)"
                "compact" -> "Compact (92%)"
                "large" -> "Large (115%)"
                "extra_large" -> "Extra Large (130%)"
                else -> "Default (100%)"
            }

            item {
                SettingsActionRow(
                    title = "Font size",
                    subtitle = fontSizeLabel,
                    onClick = { showFontSizeDialog = true }
                )
            }

            item {
                SettingsSwitchRow(
                    title = "Show app icons",
                    subtitle = if (settings.showAppIcons) "App icons displayed beside names" else "Text-only (pure minimalist)",
                    checked = settings.showAppIcons,
                    onCheckedChange = onToggleShowAppIcons
                )
            }

            if (settings.showAppIcons) {
                item {
                    SettingsSwitchRow(
                        title = "Monochrome icons",
                        subtitle = if (settings.monochromeIcons) "Minimalist grayscale icons" else "Original app colors",
                        checked = settings.monochromeIcons,
                        onCheckedChange = onToggleMonochromeIcons
                    )
                }
            }

            // Crypto Price Section
            item {
                SettingsSectionHeader(title = "Crypto Price (Optional)")
            }

            item {
                SettingsSwitchRow(
                    title = "Enable crypto price label",
                    subtitle = if (settings.cryptoPriceEnabled) "Fetches from Binance while active" else "Disabled (Zero network usage)",
                    checked = settings.cryptoPriceEnabled,
                    onCheckedChange = onToggleCryptoEnabled
                )
            }

            if (settings.cryptoPriceEnabled) {
                item {
                    SettingsSwitchRow(
                        title = "Show crypto on home screen",
                        subtitle = "Display live market prices in the home screen space",
                        checked = settings.showHomeCrypto,
                        onCheckedChange = onToggleShowHomeCrypto
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Show USD/IDR rate in app drawer",
                        subtitle = "Display live USD to IDR rate at the top right of the drawer",
                        checked = settings.showDrawerUsdRate,
                        onCheckedChange = onToggleShowDrawerUsdRate
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Live WebSocket for Home screen",
                        subtitle = if (settings.useWebSocketOnHome) "Stream live tick-by-tick prices on Home (closed on pause)" else "Disabled (Standard polling with backoff)",
                        checked = settings.useWebSocketOnHome,
                        onCheckedChange = onToggleWebSocketOnHome
                    )
                }

                item {
                    SettingsActionRow(
                        title = "Coins",
                        subtitle = settings.cryptoSymbols.joinToString(", "),
                        onClick = { showEditCoinsDialog = true }
                    )
                }

                item {
                    SettingsActionRow(
                        title = "Quote currency",
                        subtitle = settings.cryptoQuoteCurrency,
                        onClick = { showEditQuoteDialog = true }
                    )
                }

                item {
                    SettingsSwitchRow(
                        title = "Show IDX stocks on home screen",
                        subtitle = "Display live Indonesian stock exchange quotes on Home",
                        checked = settings.showHomeIdx,
                        onCheckedChange = onToggleShowHomeIdx
                    )
                }

                if (settings.showHomeIdx) {
                    item {
                        val modeLabel = when (settings.idxDisplayMode) {
                            "auto_rotate" -> "Auto Rotate (Indices, Gainers, Losers, Watchlist)"
                            "indices" -> "Market Indices (IHSG, LQ45)"
                            "gainers" -> "Top Gainers"
                            "losers" -> "Top Losers"
                            else -> "Watchlist (${settings.idxSymbols.joinToString(", ")})"
                        }
                        SettingsActionRow(
                            title = "IDX display mode",
                            subtitle = modeLabel,
                            onClick = { showIdxModeDialog = true }
                        )
                    }

                    if (settings.idxDisplayMode == "auto_rotate") {
                        item {
                            SettingsActionRow(
                                title = "IDX rotation interval",
                                subtitle = "${settings.idxAutoRotateIntervalSeconds} seconds",
                                onClick = {
                                    val next = when (settings.idxAutoRotateIntervalSeconds) {
                                        5 -> 6
                                        6 -> 8
                                        8 -> 10
                                        10 -> 15
                                        else -> 5
                                    }
                                    onUpdateIdxAutoRotateInterval(next)
                                }
                            )
                        }
                    }

                    if (settings.idxDisplayMode == "watchlist" || settings.idxDisplayMode == "auto_rotate") {
                        item {
                            SettingsActionRow(
                                title = "IDX Watchlist",
                                subtitle = settings.idxSymbols.joinToString(", "),
                                onClick = { showIdxSymbolsDialog = true }
                            )
                        }
                    }
                }

                if (settings.showHomeCrypto && settings.showHomeIdx) {
                    item {
                        SettingsSwitchRow(
                            title = "Separate Crypto & Saham",
                            subtitle = if (settings.separateMarketViews) "Display both markets simultaneously (maximize screen space)" else "Rotate between both in a single carousel",
                            checked = settings.separateMarketViews,
                            onCheckedChange = onToggleSeparateMarketViews
                        )
                    }

                    if (settings.separateMarketViews) {
                        item {
                            SettingsActionRow(
                                title = "Separated market layout",
                                subtitle = if (settings.separatedMarketLayout == "side_by_side") "Two columns (Side by side)" else "Stacked vertically",
                                onClick = {
                                    val next = if (settings.separatedMarketLayout == "side_by_side") "stacked" else "side_by_side"
                                    onUpdateSeparatedMarketLayout(next)
                                }
                            )
                        }
                    } else {
                        item {
                            SettingsSwitchRow(
                                title = "Auto-rotate market carousel",
                                subtitle = if (settings.marketCarouselAutoRotate) "Slideshow alternates between Crypto and IDX" else "Manual tap to switch",
                                checked = settings.marketCarouselAutoRotate,
                                onCheckedChange = onToggleMarketCarouselAutoRotate
                            )
                        }

                        item {
                            SettingsActionRow(
                                title = "Carousel transition",
                                subtitle = if (settings.marketCarouselTransition == "slide") "Slide transition" else "Crossfade transition",
                                onClick = {
                                    val next = if (settings.marketCarouselTransition == "slide") "crossfade" else "slide"
                                    onUpdateMarketCarouselTransition(next)
                                }
                            )
                        }

                        item {
                            SettingsActionRow(
                                title = "Carousel slide interval",
                                subtitle = "${settings.marketCarouselIntervalSeconds} seconds",
                                onClick = {
                                    val next = when (settings.marketCarouselIntervalSeconds) {
                                        5 -> 7
                                        7 -> 10
                                        10 -> 15
                                        else -> 5
                                    }
                                    onUpdateMarketCarouselInterval(next)
                                }
                            )
                        }
                    }
                }
            }

            // Backup & Restore Section
            item {
                SettingsSectionHeader(title = "Backup & Restore")
            }

            item {
                SettingsActionRow(
                    title = "Export settings (JSON)",
                    subtitle = "Export pinned apps, hidden apps, custom labels, and settings to a JSON file",
                    onClick = onExportSettings
                )
            }

            item {
                SettingsActionRow(
                    title = "Import settings (JSON)",
                    subtitle = "Restore launcher setup from a JSON backup file",
                    onClick = onImportSettings
                )
            }

            // Hidden Apps Section
            if (settings.hiddenApps.isNotEmpty()) {
                item {
                    SettingsSectionHeader(title = "Hidden Apps (${settings.hiddenApps.size})")
                }

                items(settings.hiddenApps.toList()) { pkg ->
                    val app = allApps.find { it.packageName == pkg }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app?.displayLabel ?: pkg,
                            style = typography.settingsItem,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Unhide",
                            style = typography.settingsDescription,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .clickable { onUnhideApp(pkg) }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            // Pinned Apps Section
            if (settings.pinnedApps.isNotEmpty()) {
                item {
                    SettingsSectionHeader(title = "Pinned Apps (${settings.pinnedApps.size}/8)")
                }

                itemsIndexed(settings.pinnedApps) { index, pkg ->
                    val app = allApps.find { it.key == pkg || it.packageName == pkg }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app?.displayLabel ?: pkg,
                            style = typography.settingsItem,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (index > 0) {
                                Text(
                                    text = "↑",
                                    style = typography.settingsItem,
                                    color = colors.textPrimary,
                                    modifier = Modifier
                                        .clickable {
                                            val mutable = settings.pinnedApps.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index - 1, item)
                                            onReorderPinnedApps(mutable)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            if (index < settings.pinnedApps.size - 1) {
                                Text(
                                    text = "↓",
                                    style = typography.settingsItem,
                                    color = colors.textPrimary,
                                    modifier = Modifier
                                        .clickable {
                                            val mutable = settings.pinnedApps.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index + 1, item)
                                            onReorderPinnedApps(mutable)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "Remove",
                                style = typography.settingsDescription,
                                color = colors.textSecondary,
                                modifier = Modifier
                                    .clickable { onUnpinApp(pkg) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // About section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Nounish Launcher 1.0.11",
                        style = typography.settingsItem,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Minimal, text-only, AMOLED friendly, zero tracking.",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // App Picker Dialog for Swipe Gestures
    showAppPickerForSwipe?.let { direction ->
        AlertDialog(
            onDismissRequest = { showAppPickerForSwipe = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Select app for swipe $direction",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    item {
                        Text(
                            text = "None (Disable)",
                            style = typography.settingsItem,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (direction == "left") onSelectSwipeLeftApp(null)
                                    else onSelectSwipeRightApp(null)
                                    showAppPickerForSwipe = null
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                    items(allApps) { app ->
                        Text(
                            text = app.displayLabel,
                            style = typography.settingsItem,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (direction == "left") onSelectSwipeLeftApp(app.packageName)
                                    else onSelectSwipeRightApp(app.packageName)
                                    showAppPickerForSwipe = null
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppPickerForSwipe = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Edit Coins Dialog
    if (showEditCoinsDialog) {
        var textValue by remember { mutableStateOf(settings.cryptoSymbols.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { showEditCoinsDialog = false },
            containerColor = colors.surface,
            title = {
                Text("Crypto Coins", style = typography.settingsHeading, color = colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        "Enter Binance coin symbols or pairs separated by commas (e.g. BTC, ETH, SOL, PEPE, BTCUSDT, ETHBIDR):",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        textStyle = typography.settingsItem.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val list = textValue.split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }
                    if (list.isNotEmpty()) {
                        onUpdateCryptoSymbols(list)
                    }
                    showEditCoinsDialog = false
                }) {
                    Text("Save", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCoinsDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Edit Quote Currency Dialog
    if (showEditQuoteDialog) {
        var textValue by remember { mutableStateOf(settings.cryptoQuoteCurrency) }
        val commonQuotes = listOf("USDT", "USDC", "EUR", "BIDR", "TRY", "BRL", "GBP", "AUD", "BTC")

        AlertDialog(
            onDismissRequest = { showEditQuoteDialog = false },
            containerColor = colors.surface,
            title = {
                Text("Quote Currency", style = typography.settingsHeading, color = colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        "Choose a preset or type a custom quote currency (e.g. USDT, BIDR for IDR, EUR):",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commonQuotes.take(4).forEach { quote ->
                            Text(
                                text = quote,
                                fontSize = 12.sp,
                                color = if (textValue.equals(quote, ignoreCase = true)) colors.background else colors.textPrimary,
                                modifier = Modifier
                                    .background(if (textValue.equals(quote, ignoreCase = true)) colors.textPrimary else colors.surface)
                                    .clickable { textValue = quote }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commonQuotes.drop(4).forEach { quote ->
                            Text(
                                text = quote,
                                fontSize = 12.sp,
                                color = if (textValue.equals(quote, ignoreCase = true)) colors.background else colors.textPrimary,
                                modifier = Modifier
                                    .background(if (textValue.equals(quote, ignoreCase = true)) colors.textPrimary else colors.surface)
                                    .clickable { textValue = quote }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Custom quote symbol:",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
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
                TextButton(onClick = {
                    val quote = textValue.trim().uppercase()
                    if (quote.isNotEmpty()) {
                        onUpdateCryptoQuote(quote)
                    }
                    showEditQuoteDialog = false
                }) {
                    Text("Save", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditQuoteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // IDX Display Mode Dialog
    if (showIdxModeDialog) {
        val modes = listOf(
            "auto_rotate" to "Auto Rotate (Indices, Gainers, Losers, Watchlist)",
            "watchlist" to "Custom Watchlist (Selected stocks & indices)",
            "indices" to "Market Indices (IHSG, LQ45)",
            "gainers" to "Top Gainers (Largest % increase)",
            "losers" to "Top Losers (Largest % decline)"
        )

        AlertDialog(
            onDismissRequest = { showIdxModeDialog = false },
            containerColor = colors.surface,
            title = {
                Text("IDX Display Mode", style = typography.settingsHeading, color = colors.textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    modes.forEach { (mode, desc) ->
                        val isSelected = settings.idxDisplayMode == mode
                        Text(
                            text = desc,
                            style = typography.settingsItem,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateIdxDisplayMode(mode)
                                    showIdxModeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showIdxModeDialog = false }) {
                    Text("Close", color = colors.textSecondary)
                }
            }
        )
    }

    // IDX Watchlist Symbols Dialog
    if (showIdxSymbolsDialog) {
        var textValue by remember { mutableStateOf(settings.idxSymbols.joinToString(", ")) }
        val suggestedPresets = listOf("IHSG", "BBCA", "BBRI", "BMRI", "TLKM", "ASII", "GOTO", "LQ45")

        AlertDialog(
            onDismissRequest = { showIdxSymbolsDialog = false },
            containerColor = colors.surface,
            title = {
                Text("IDX Watchlist", style = typography.settingsHeading, color = colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        "Enter Indonesian stock tickers or indices (comma separated):",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedPresets.take(4).forEach { sym ->
                            Text(
                                text = sym,
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .background(colors.surface)
                                    .clickable {
                                        val currentList = textValue.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableList()
                                        if (!currentList.contains(sym)) {
                                            currentList.add(sym)
                                            textValue = currentList.joinToString(", ")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedPresets.drop(4).forEach { sym ->
                            Text(
                                text = sym,
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .background(colors.surface)
                                    .clickable {
                                        val currentList = textValue.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableList()
                                        if (!currentList.contains(sym)) {
                                            currentList.add(sym)
                                            textValue = currentList.joinToString(", ")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
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
                TextButton(onClick = {
                    val list = textValue.split(",")
                        .map { it.trim().uppercase().removeSuffix(".JK") }
                        .filter { it.isNotEmpty() }
                    if (list.isNotEmpty()) {
                        onUpdateIdxSymbols(list)
                    }
                    showIdxSymbolsDialog = false
                }) {
                    Text("Save", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIdxSymbolsDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Clock Format Dialog
    if (showClockFormatDialog) {
        val clockOptions = listOf(
            "system" to "System default",
            "24h" to "24-hour (14:30)",
            "12h" to "12-hour with AM/PM (2:30 PM)",
            "12h_no_am_pm" to "12-hour without AM/PM (2:30)"
        )

        AlertDialog(
            onDismissRequest = { showClockFormatDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Clock format",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    clockOptions.forEach { (key, label) ->
                        val isSelected = settings.clockTimeFormat == key
                        Text(
                            text = label,
                            style = typography.settingsItem,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateClockTimeFormat(key)
                                    showClockFormatDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClockFormatDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Date Format Dialog
    if (showDateFormatDialog) {
        val now = Date()
        val datePresets = listOf(
            "EEEE, d MMMM" to "Full",
            "EEE, d MMM" to "Short",
            "d MMMM yyyy" to "Standard with year",
            "yyyy-MM-dd" to "ISO 8601",
            "EEE, MMM d" to "US style",
            "d MMM" to "Compact"
        )
        var customPatternText by remember { mutableStateOf(settings.dateFormatPattern) }

        AlertDialog(
            onDismissRequest = { showDateFormatDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Date format",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    datePresets.forEach { (pattern, label) ->
                        val sample = try {
                            SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                        } catch (_: Exception) {
                            pattern
                        }
                        val isSelected = settings.dateFormatPattern == pattern

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateDateFormatPattern(pattern)
                                    showDateFormatDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$label ($sample)",
                                style = typography.settingsItem,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Or custom pattern:",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                    BasicTextField(
                        value = customPatternText,
                        onValueChange = { customPatternText = it },
                        textStyle = typography.settingsItem.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = customPatternText.trim()
                    if (trimmed.isNotEmpty()) {
                        onUpdateDateFormatPattern(trimmed)
                    }
                    showDateFormatDialog = false
                }) {
                    Text("Apply", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateFormatDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Font Size Dialog
    if (showFontSizeDialog) {
        val fontSizeOptions = listOf(
            "micro" to Pair("Micro (65%)", 0.65f),
            "extra_small" to Pair("Extra Small (75%)", 0.75f),
            "small" to Pair("Small (85%)", 0.85f),
            "compact" to Pair("Compact (92%)", 0.92f),
            "medium" to Pair("Default (100%)", 1.0f),
            "large" to Pair("Large (115%)", 1.15f),
            "extra_large" to Pair("Extra Large (130%)", 1.30f)
        )

        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Font size",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(fontSizeOptions) { (key, pair) ->
                        val (label, scale) = pair
                        val isSelected = settings.fontSize == key
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateFontSize(key)
                                    showFontSizeDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = typography.settingsItem,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Sample: Phone, Chrome, Settings",
                                fontSize = (14 * scale).sp,
                                color = colors.textSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFontSizeDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    Text(
        text = title.uppercase(),
        style = typography.settingsDescription,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = typography.settingsItem,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typography.settingsDescription,
                    color = colors.textSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.textPrimary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surface
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = title,
            style = typography.settingsItem,
            color = colors.textPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = typography.settingsDescription,
                color = colors.textSecondary
            )
        }
    }
}

private fun openDefaultLauncherSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
