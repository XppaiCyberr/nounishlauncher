package com.nounish.launcher.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.LauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsCategory(
    val title: String,
    val tabName: String,
    val description: String
) {
    GENERAL("General", "General", "Clock, date, gestures, drawer, corner text"),
    CRYPTO("Crypto", "Crypto", "Binance prices, coins, quote currency, websocket"),
    IDX("IDX Saham", "IDX", "Indonesian stock exchange, indices, watchlist"),
    NOUNS("Nouns DAO ⌐◨-◨", "Nouns", "Proposals, candidates, rotation, vote links"),
    THEME("Theme & Typography", "Theme", "AMOLED black, monospace, font scaling, icons"),
    MARKET_LAYOUT("Market Layout", "Layout", "Widget separation, carousel slideshow, currency"),
    APPS_BACKUP("Apps & Backup", "Apps", "Pinned apps, hidden apps, offline JSON backup")
}

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    allApps: List<AppItem>,
    onToggleShowClock: (Boolean) -> Unit,
    onUpdateClockTimeFormat: (String) -> Unit,
    onToggleClockSeconds: (Boolean) -> Unit,
    onToggleShowDate: (Boolean) -> Unit,
    onUpdateDateFormatPattern: (String) -> Unit,
    onToggleShowCornerText: (Boolean) -> Unit = {},
    onUpdateCornerText: (String) -> Unit = {},
    onToggleShowHomeScreenNote: (Boolean) -> Unit = {},
    onToggleAutoLaunch: (Boolean) -> Unit,
    onSelectSwipeLeftApp: (String?) -> Unit,
    onSelectSwipeRightApp: (String?) -> Unit,
    onToggleSwipeDownToRefresh: (Boolean) -> Unit = {},
    onToggleDoubleTapLock: (Boolean) -> Unit,
    onToggleCryptoEnabled: (Boolean) -> Unit,
    onToggleShowHomeCrypto: (Boolean) -> Unit,
    onToggleShowHomeIdx: (Boolean) -> Unit = {},
    onUpdateIdxDisplayMode: (String) -> Unit = {},
    onUpdateIdxSymbols: (List<String>) -> Unit = {},
    onUpdateIdxAutoRotateInterval: (Int) -> Unit = {},
    onToggleShowHomeNouns: (Boolean) -> Unit = {},
    onUpdateNounsDisplayMode: (String) -> Unit = {},
    onUpdateNounsMaxDisplayCount: (Int) -> Unit = {},
    onUpdateNounsStatusFilter: (String) -> Unit = {},
    onToggleMarketCarouselAutoRotate: (Boolean) -> Unit = {},
    onUpdateMarketCarouselInterval: (Int) -> Unit = {},
    onUpdateMarketCarouselTransition: (String) -> Unit = {},
    onToggleSeparateMarketViews: (Boolean) -> Unit = {},
    onUpdateSeparatedMarketLayout: (String) -> Unit = {},
    onUpdateMarketCurrencyFormat: (String) -> Unit = {},
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
    onToggleSearchBarAtBottom: (Boolean) -> Unit = {},
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

    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    // Nested BackHandler: if inside a subcategory, back returns to the Settings Hub
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    // Dialog state variables
    var showAppPickerForSwipe by remember { mutableStateOf<String?>(null) }
    var showEditCoinsDialog by remember { mutableStateOf(false) }
    var showEditQuoteDialog by remember { mutableStateOf(false) }
    var showIdxModeDialog by remember { mutableStateOf(false) }
    var showIdxSymbolsDialog by remember { mutableStateOf(false) }
    var showClockFormatDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showCornerTextDialog by remember { mutableStateOf(false) }
    var cornerTextInput by remember(settings.cornerText) { mutableStateOf(settings.cornerText) }
    var showCurrencyFormatDialog by remember { mutableStateOf(false) }
    var showNounsStatusFilterDialog by remember { mutableStateOf(false) }
    var showNounsDisplayModeDialog by remember { mutableStateOf(false) }
    var showNounsMaxCountDialog by remember { mutableStateOf(false) }

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
        Crossfade(
            targetState = selectedCategory,
            animationSpec = tween(durationMillis = 120),
            label = "SettingsCategoryTransition"
        ) { category ->
            if (category == null) {
                // Main Settings Hub Menu
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Default Launcher Action
                    item {
                        SettingsActionRow(
                            title = "Set as default launcher",
                            subtitle = "Set Nounish Launcher as your home app",
                            onClick = { openDefaultLauncherSettings(context) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        SettingsSectionHeader(title = "Categories")
                    }

                    // Category Cards
                    items(SettingsCategory.values()) { cat ->
                        val badge = when (cat) {
                            SettingsCategory.CRYPTO -> if (settings.cryptoPriceEnabled) "Active" else null
                            SettingsCategory.IDX -> if (settings.showHomeIdx) "Active" else null
                            SettingsCategory.NOUNS -> if (settings.showHomeNouns) "Active" else null
                            SettingsCategory.APPS_BACKUP -> "${settings.pinnedApps.size}/8 pinned"
                            else -> null
                        }
                        SettingsCategoryCard(
                            title = cat.title,
                            subtitle = cat.description,
                            badge = badge,
                            onClick = { selectedCategory = cat }
                        )
                    }

                    // About / Version Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsSectionHeader(title = "About")
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Nounish Launcher 1.0.18",
                                style = typography.settingsItem,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Minimal, text-only, AMOLED friendly, zero tracking.",
                                style = typography.settingsDescription,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // Category Detail View
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Header Bar with Back button, Category Title, and Done
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedCategory = null }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "← Back",
                                style = typography.settingsItem,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.title,
                                style = typography.settingsHeading.copy(fontSize = 20.sp),
                                color = colors.textPrimary
                            )
                        }
                        Text(
                            text = "Done",
                            style = typography.settingsItem,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .clickable(onClick = onCloseSettings)
                                .padding(6.dp)
                        )
                    }

                    // Horizontal Quick-Jump Category Switcher
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SettingsCategory.values()) { cat ->
                            val isSelected = cat == category
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(
                                        if (isSelected) colors.textPrimary.copy(alpha = 0.12f) else colors.background,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.tabName,
                                    style = typography.clock.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Detail List for the Active Category
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (category) {
                            SettingsCategory.GENERAL -> {
                                item {
                                    SettingsActionRow(
                                        title = "Set as default launcher",
                                        subtitle = "Set Nounish Launcher as your home app",
                                        onClick = { openDefaultLauncherSettings(context) }
                                    )
                                }

                                item { SettingsSectionHeader(title = "Clock & Date") }

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

                                item { SettingsSectionHeader(title = "Home Screen Details") }

                                item {
                                    SettingsSwitchRow(
                                        title = "Corner text",
                                        subtitle = if (settings.showCornerText) "Show custom text on bottom right corner" else "Hidden",
                                        checked = settings.showCornerText,
                                        onCheckedChange = onToggleShowCornerText
                                    )
                                }

                                if (settings.showCornerText) {
                                    item {
                                        SettingsActionRow(
                                            title = "Custom corner text",
                                            subtitle = if (settings.cornerText.isNotBlank()) settings.cornerText else "None (tap to set)",
                                            onClick = {
                                                cornerTextInput = settings.cornerText
                                                showCornerTextDialog = true
                                            }
                                        )
                                    }
                                }

                                item {
                                    val noteSubtitle = when {
                                        !settings.showHomeScreenNote -> "Disabled"
                                        settings.homeScreenNoteDismissed -> "Dismissed from home (tap to restore)"
                                        else -> "Showing passive learning notes on top-right"
                                    }
                                    SettingsSwitchRow(
                                        title = "Passive learning note",
                                        subtitle = noteSubtitle,
                                        checked = settings.showHomeScreenNote && !settings.homeScreenNoteDismissed,
                                        onCheckedChange = { checked ->
                                            onToggleShowHomeScreenNote(checked)
                                        }
                                    )
                                }

                                item { SettingsSectionHeader(title = "Gestures") }

                                item {
                                    SettingsSwitchRow(
                                        title = "Swipe down to refresh",
                                        subtitle = if (settings.swipeDownToRefresh) {
                                            "Manual refresh on swipe down (automatic background polling off)"
                                        } else {
                                            "Automatic background polling active (every 15s–60s)"
                                        },
                                        checked = settings.swipeDownToRefresh,
                                        onCheckedChange = onToggleSwipeDownToRefresh
                                    )
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

                                item { SettingsSectionHeader(title = "App Drawer") }

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
                                        title = "Search bar at bottom",
                                        subtitle = if (settings.searchBarAtBottom) "Search bar placed at bottom for one-handed reach" else "Search bar placed at top",
                                        checked = settings.searchBarAtBottom,
                                        onCheckedChange = onToggleSearchBarAtBottom
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
                            }

                            SettingsCategory.CRYPTO -> {
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
                                }
                            }

                            SettingsCategory.IDX -> {
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
                            }

                            SettingsCategory.NOUNS -> {
                                item {
                                    SettingsSwitchRow(
                                        title = "Show Nouns DAO feed",
                                        subtitle = if (settings.showHomeNouns) "Display live governance proposals and candidates on Home" else "Disabled",
                                        checked = settings.showHomeNouns,
                                        onCheckedChange = onToggleShowHomeNouns
                                    )
                                }

                                if (settings.showHomeNouns) {
                                    item {
                                        val modeLabel = when (settings.nounsDisplayMode) {
                                            "proposals" -> "Proposals only"
                                            "candidates" -> "Candidates only"
                                            else -> "Auto Rotate (Proposals & Candidates)"
                                        }
                                        SettingsActionRow(
                                            title = "Nouns display mode",
                                            subtitle = modeLabel,
                                            onClick = { showNounsDisplayModeDialog = true }
                                        )
                                    }

                                    item {
                                        SettingsActionRow(
                                            title = "Items to display",
                                            subtitle = "${settings.nounsMaxDisplayCount} items",
                                            onClick = { showNounsMaxCountDialog = true }
                                        )
                                    }

                                    if (settings.nounsDisplayMode != "candidates") {
                                        item {
                                            SettingsActionRow(
                                                title = "Proposal status filter",
                                                subtitle = settings.nounsStatusFilter,
                                                onClick = { showNounsStatusFilterDialog = true }
                                            )
                                        }
                                    }
                                }
                            }

                            SettingsCategory.THEME -> {
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
                            }

                            SettingsCategory.MARKET_LAYOUT -> {
                                item {
                                    SettingsSwitchRow(
                                        title = "Separate home widgets",
                                        subtitle = if (settings.separateMarketViews) "Display active widgets simultaneously (maximize screen space)" else "Rotate between widgets in a single carousel",
                                        checked = settings.separateMarketViews,
                                        onCheckedChange = onToggleSeparateMarketViews
                                    )
                                }

                                if (settings.separateMarketViews) {
                                    item {
                                        SettingsActionRow(
                                            title = "Separated widget layout",
                                            subtitle = if (settings.separatedMarketLayout == "side_by_side") "Multi-column (Side by side)" else "Stacked vertically",
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
                                            subtitle = if (settings.marketCarouselAutoRotate) "Slideshow alternates between active widgets" else "Manual tap to switch",
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

                                item {
                                    val currencyFormatLabel = when (settings.marketCurrencyFormat) {
                                        "prefix" -> "Prefix (USDT 68,450 · IDR 9,850)"
                                        "none" -> "None (Raw numbers only)"
                                        else -> "Suffix (68,450 USDT · 9,850 IDR)"
                                    }
                                    SettingsActionRow(
                                        title = "Currency format",
                                        subtitle = currencyFormatLabel,
                                        onClick = { showCurrencyFormatDialog = true }
                                    )
                                }
                            }

                            SettingsCategory.APPS_BACKUP -> {
                                item { SettingsSectionHeader(title = "Backup & Restore") }

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

                                item { SettingsSectionHeader(title = "Pinned Apps (${settings.pinnedApps.size}/8)") }

                                if (settings.pinnedApps.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No pinned apps. Long-press any app in the drawer to pin.",
                                            style = typography.settingsDescription,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                } else {
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
                                                text = "${index + 1}. ${app?.displayLabel ?: pkg}",
                                                style = typography.settingsItem,
                                                color = colors.textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
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

                                if (settings.hiddenApps.isNotEmpty()) {
                                    item { SettingsSectionHeader(title = "Hidden Apps (${settings.hiddenApps.size})") }

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
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
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
        var coinsInput by remember { mutableStateOf(settings.cryptoSymbols.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { showEditCoinsDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Edit Coins",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter Binance symbols or pairs separated by commas (e.g. BTC, ETH, SOL, PEPE, BTCUSDT):",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    BasicTextField(
                        value = coinsInput,
                        onValueChange = { coinsInput = it },
                        textStyle = typography.searchInput.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .background(colors.background, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = coinsInput.split(",")
                            .map { it.trim().uppercase() }
                            .filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) {
                            onUpdateCryptoSymbols(parsed)
                        }
                        showEditCoinsDialog = false
                    }
                ) {
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
        val popularQuotes = listOf("USDT", "USDC", "EUR", "BIDR", "TRY", "BRL", "GBP", "AUD", "BTC")
        var customQuoteInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEditQuoteDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Select Quote Currency",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Common Pairs:",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(popularQuotes) { quote ->
                            val isSelected = quote.equals(settings.cryptoQuoteCurrency, ignoreCase = true)
                            Text(
                                text = if (quote == "BIDR") "BIDR (Indonesian Rupiah)" else quote,
                                style = typography.settingsItem.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateCryptoQuote(quote)
                                        showEditQuoteDialog = false
                                    }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Custom Quote Currency:",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    BasicTextField(
                        value = customQuoteInput,
                        onValueChange = { customQuoteInput = it.uppercase() },
                        textStyle = typography.searchInput.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = customQuoteInput.trim().uppercase()
                        if (clean.isNotEmpty()) {
                            onUpdateCryptoQuote(clean)
                        }
                        showEditQuoteDialog = false
                    }
                ) {
                    Text("Apply", color = colors.textPrimary)
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
            "indices" to "Market Indices (IHSG, LQ45)",
            "gainers" to "Top Gainers",
            "losers" to "Top Losers",
            "watchlist" to "Custom Watchlist"
        )
        AlertDialog(
            onDismissRequest = { showIdxModeDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "IDX Display Mode",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(250.dp)) {
                    items(modes) { (modeKey, modeTitle) ->
                        val isSelected = modeKey == settings.idxDisplayMode
                        Text(
                            text = modeTitle,
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateIdxDisplayMode(modeKey)
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
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // IDX Symbols Dialog
    if (showIdxSymbolsDialog) {
        var idxSymbolsInput by remember { mutableStateOf(settings.idxSymbols.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { showIdxSymbolsDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "IDX Watchlist Symbols",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter Indonesian stock tickers separated by commas (e.g. IHSG, BBCA, BBRI, BMRI, TLKM, ASII, GOTO):",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    BasicTextField(
                        value = idxSymbolsInput,
                        onValueChange = { idxSymbolsInput = it },
                        textStyle = typography.searchInput.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .background(colors.background, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = idxSymbolsInput.split(",")
                            .map { it.trim().uppercase() }
                            .filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) {
                            onUpdateIdxSymbols(parsed)
                        }
                        showIdxSymbolsDialog = false
                    }
                ) {
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
        val formats = listOf(
            "system" to "System default (Android setting)",
            "24h" to "24-hour (14:30)",
            "12h" to "12-hour with AM/PM (2:30 PM)",
            "12h_no_am_pm" to "12-hour without AM/PM (2:30)"
        )
        AlertDialog(
            onDismissRequest = { showClockFormatDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Select Clock Format",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(formats) { (key, label) ->
                        val isSelected = key == settings.clockTimeFormat
                        Text(
                            text = label,
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateClockTimeFormat(key)
                                    showClockFormatDialog = false
                                }
                                .padding(vertical = 12.dp)
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
        val presets = listOf(
            "EEEE, d MMMM" to "Full (Tuesday, 10 November)",
            "EEE, d MMM" to "Short (Tue, 10 Nov)",
            "EEEE, d MMMM yyyy" to "Full with year (Tuesday, 10 November 2026)",
            "d MMMM yyyy" to "Standard (10 November 2026)",
            "yyyy-MM-dd" to "ISO 8601 (2026-11-10)",
            "MM/dd/yyyy" to "US Style (11/10/2026)",
            "dd/MM/yyyy" to "International (10/11/2026)",
            "d MMM" to "Compact (10 Nov)"
        )
        var customPatternInput by remember { mutableStateOf(settings.dateFormatPattern) }
        AlertDialog(
            onDismissRequest = { showDateFormatDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Date Format",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose a preset:",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(presets) { (pattern, label) ->
                            val isSelected = pattern == settings.dateFormatPattern
                            Text(
                                text = label,
                                style = typography.settingsItem.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateDateFormatPattern(pattern)
                                        showDateFormatDialog = false
                                    }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Or custom pattern (SimpleDateFormat):",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    BasicTextField(
                        value = customPatternInput,
                        onValueChange = { customPatternInput = it },
                        textStyle = typography.searchInput.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = customPatternInput.trim()
                        if (clean.isNotEmpty()) {
                            onUpdateDateFormatPattern(clean)
                        }
                        showDateFormatDialog = false
                    }
                ) {
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
        val fontSizes = listOf(
            "micro" to "Micro (65%)",
            "extra_small" to "Extra Small (75%)",
            "small" to "Small (85%)",
            "compact" to "Compact (92%)",
            "medium" to "Default (100%)",
            "large" to "Large (115%)",
            "extra_large" to "Extra Large (130%)"
        )
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Font Size",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(fontSizes) { (key, label) ->
                        val isSelected = key == settings.fontSize
                        Text(
                            text = label,
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateFontSize(key)
                                    showFontSizeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
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

    // Corner Text Dialog
    if (showCornerTextDialog) {
        AlertDialog(
            onDismissRequest = { showCornerTextDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Custom Corner Text",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter text to display at the bottom-right corner of the Home screen:",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    BasicTextField(
                        value = cornerTextInput,
                        onValueChange = { cornerTextInput = it },
                        textStyle = typography.searchInput.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background, RoundedCornerShape(8.dp))
                            .border(1.dp, colors.textSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateCornerText(cornerTextInput.trim())
                        showCornerTextDialog = false
                    }
                ) {
                    Text("Save", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        cornerTextInput = "xppaicyber"
                        onUpdateCornerText("xppaicyber")
                        showCornerTextDialog = false
                    }
                ) {
                    Text("Reset Default", color = colors.textSecondary)
                }
            }
        )
    }

    // Currency Format Dialog
    if (showCurrencyFormatDialog) {
        val formats = listOf(
            "suffix" to "Suffix (68,450 USDT · 9,850 IDR)",
            "prefix" to "Prefix (USDT 68,450 · IDR 9,850)",
            "none" to "None (Raw numbers only)"
        )
        AlertDialog(
            onDismissRequest = { showCurrencyFormatDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Currency Format",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(formats) { (key, label) ->
                        val isSelected = key == settings.marketCurrencyFormat
                        Text(
                            text = label,
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateMarketCurrencyFormat(key)
                                    showCurrencyFormatDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyFormatDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Nouns Display Mode Dialog
    if (showNounsDisplayModeDialog) {
        val modes = listOf(
            "auto_rotate" to "Auto Rotate (Proposals & Candidates)",
            "proposals" to "Proposals only",
            "candidates" to "Candidates only"
        )
        AlertDialog(
            onDismissRequest = { showNounsDisplayModeDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Nouns Display Mode",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(modes) { (modeKey, modeTitle) ->
                        val isSelected = modeKey == settings.nounsDisplayMode
                        Text(
                            text = modeTitle,
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateNounsDisplayMode(modeKey)
                                    showNounsDisplayModeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNounsDisplayModeDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Nouns Max Count Dialog
    if (showNounsMaxCountDialog) {
        val counts = listOf(2, 3, 4, 5, 6)
        AlertDialog(
            onDismissRequest = { showNounsMaxCountDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Items to Display",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(counts) { count ->
                        val isSelected = count == settings.nounsMaxDisplayCount
                        Text(
                            text = "$count items",
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateNounsMaxDisplayCount(count)
                                    showNounsMaxCountDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNounsMaxCountDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Nouns Status Filter Dialog
    if (showNounsStatusFilterDialog) {
        val filters = listOf("All", "Active", "Executed")
        AlertDialog(
            onDismissRequest = { showNounsStatusFilterDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Proposal Status Filter",
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(filters) { filter ->
                        val isSelected = filter == settings.nounsStatusFilter
                        Text(
                            text = filter,
                            style = typography.settingsItem.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateNounsStatusFilter(filter)
                                    showNounsStatusFilterDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNounsStatusFilterDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = typography.settingsItem.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badge,
                        style = typography.clock.copy(fontSize = 11.sp),
                        color = CryptoGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.settingsDescription,
                color = colors.textSecondary
            )
        }
        Text(
            text = "→",
            style = typography.settingsItem,
            color = colors.textSecondary.copy(alpha = 0.5f)
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
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
