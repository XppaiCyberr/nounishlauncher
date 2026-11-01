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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nounish.launcher.data.LauncherSettings
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.service.LockScreenService
import com.nounish.launcher.ui.theme.LauncherTheme

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    allApps: List<AppItem>,
    onToggleShowClock: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onToggleAutoLaunch: (Boolean) -> Unit,
    onSelectSwipeLeftApp: (String?) -> Unit,
    onSelectSwipeRightApp: (String?) -> Unit,
    onToggleDoubleTapLock: (Boolean) -> Unit,
    onToggleCryptoEnabled: (Boolean) -> Unit,
    onUpdateCryptoSymbols: (List<String>) -> Unit,
    onUpdateCryptoQuote: (String) -> Unit,
    onToggleLightTheme: (Boolean) -> Unit,
    onToggleMonospace: (Boolean) -> Unit,
    onToggleIndexOnRight: (Boolean) -> Unit,
    onToggleIndexAnimation: (Boolean) -> Unit,
    onUnpinApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onCloseSettings: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current

    var showAppPickerForSwipe by remember { mutableStateOf<String?>(null) } // "left" or "right"
    var showEditCoinsDialog by remember { mutableStateOf(false) }
    var showEditQuoteDialog by remember { mutableStateOf(false) }

    val swipeLeftAppName = remember(settings.swipeLeftPackage, allApps) {
        allApps.find { it.packageName == settings.swipeLeftPackage }?.label ?: "None"
    }
    val swipeRightAppName = remember(settings.swipeRightPackage, allApps) {
        allApps.find { it.packageName == settings.swipeRightPackage }?.label ?: "None"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Default Launcher button
            item {
                SettingsActionRow(
                    title = "Set as default launcher",
                    subtitle = "Set Nounish Launcher as your home app",
                    onClick = { openDefaultLauncherSettings(context) }
                )
            }

            // General Section
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

            item {
                SettingsSwitchRow(
                    title = "Show date",
                    checked = settings.showDate,
                    onCheckedChange = onToggleShowDate
                )
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

            // Crypto Price Section
            item {
                SettingsSectionHeader(title = "Crypto Price (Optional)")
            }

            item {
                SettingsSwitchRow(
                    title = "Enable crypto price label",
                    subtitle = if (settings.cryptoPriceEnabled) "Fetches from Binance while drawer is open" else "Disabled (Zero network usage)",
                    checked = settings.cryptoPriceEnabled,
                    onCheckedChange = onToggleCryptoEnabled
                )
            }

            if (settings.cryptoPriceEnabled) {
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
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app?.label ?: pkg,
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
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Pinned Apps Section
            if (settings.pinnedApps.isNotEmpty()) {
                item {
                    SettingsSectionHeader(title = "Pinned Apps (${settings.pinnedApps.size})")
                }

                items(settings.pinnedApps) { pkg ->
                    val app = allApps.find { it.packageName == pkg }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app?.label ?: pkg,
                            style = typography.settingsItem,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Remove",
                            style = typography.settingsDescription,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .clickable { onUnpinApp(pkg) }
                                .padding(8.dp)
                        )
                    }
                }
            }

            // About section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Nounish Launcher 1.0.0",
                        style = typography.settingsItem,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Minimal, text-only, AMOLED friendly, zero tracking.",
                        style = typography.settingsDescription,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
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
                            text = app.label,
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
                        "Enter coin symbols separated by comma (e.g. BTC, ETH, SOL):",
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
        AlertDialog(
            onDismissRequest = { showEditQuoteDialog = false },
            containerColor = colors.surface,
            title = {
                Text("Quote Currency", style = typography.settingsHeading, color = colors.textPrimary)
            },
            text = {
                Column {
                    Text(
                        "Enter quote currency symbol (e.g. USDT, BUSD, EUR):",
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
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    Text(
        text = title.uppercase(),
        style = typography.settingsDescription,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
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
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
            .padding(vertical = 10.dp)
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
