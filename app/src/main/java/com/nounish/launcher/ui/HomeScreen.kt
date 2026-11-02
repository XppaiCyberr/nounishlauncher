package com.nounish.launcher.ui

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.nounish.launcher.CoinTickerUi
import com.nounish.launcher.IdxCategoryUi
import com.nounish.launcher.data.AppIcon
import com.nounish.launcher.data.IdxItemUi
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
import com.nounish.launcher.ui.theme.LauncherTheme
import com.nounish.launcher.ui.theme.getFontScale
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
    showIdx: Boolean = true,
    idxStocks: List<IdxItemUi> = emptyList(),
    idxCategories: List<IdxCategoryUi> = emptyList(),
    idxDisplayMode: String = "auto_rotate",
    idxAutoRotateIntervalSeconds: Int = 6,
    carouselAutoRotate: Boolean = true,
    carouselIntervalSeconds: Int = 7,
    carouselTransition: String = "slide",
    separateMarketViews: Boolean = true,
    separatedMarketLayout: String = "stacked",
    showAppIcons: Boolean = true,
    monochromeIcons: Boolean = true,
    fontSize: String = "medium",
    showCornerText: Boolean = true,
    cornerText: String = "xppaicyber",
    marketCurrencyFormat: String = "suffix",
    cryptoCurrency: String = "USDT",
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

    Column(
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

        // Market Section (middle area): Takes all available vertical space between Clock and Pinned Apps
        val hasCrypto = showCrypto && cryptoCoins.isNotEmpty()
        val hasIdx = showIdx && (idxCategories.isNotEmpty() || idxStocks.isNotEmpty())

        var currentViewIndex by remember { mutableIntStateOf(0) }
        val isIdxAutoRotate = idxDisplayMode == "auto_rotate"
        var currentIdxCategoryIndex by remember { mutableIntStateOf(0) }

        // Category auto-rotation timer (wave rotation for IDX categories)
        LaunchedEffect(isIdxAutoRotate, idxCategories.size, idxAutoRotateIntervalSeconds, hasIdx) {
            if (isIdxAutoRotate && hasIdx && idxCategories.size > 1) {
                val intervalMs = (idxAutoRotateIntervalSeconds.coerceIn(3, 20) * 1000L)
                while (true) {
                    delay(intervalMs)
                    currentIdxCategoryIndex = (currentIdxCategoryIndex + 1) % idxCategories.size
                }
            }
        }

        val activeIdxCategory = remember(idxCategories, currentIdxCategoryIndex, idxDisplayMode, idxStocks) {
            when {
                idxCategories.isEmpty() -> {
                    if (idxStocks.isNotEmpty()) IdxCategoryUi("watchlist", "Watchlist", idxStocks) else null
                }
                idxDisplayMode == "auto_rotate" -> {
                    idxCategories.getOrNull(currentIdxCategoryIndex.coerceIn(0, idxCategories.size - 1)) ?: idxCategories.firstOrNull()
                }
                idxDisplayMode == "indices" -> {
                    idxCategories.find { it.categoryId == "indices" } ?: idxCategories.firstOrNull()
                }
                idxDisplayMode == "gainers" -> {
                    idxCategories.find { it.categoryId == "gainers" } ?: idxCategories.firstOrNull()
                }
                idxDisplayMode == "losers" -> {
                    idxCategories.find { it.categoryId == "losers" } ?: idxCategories.firstOrNull()
                }
                else -> {
                    idxCategories.find { it.categoryId == "watchlist" } ?: idxCategories.firstOrNull()
                }
            }
        }

        val cryptoHeaderLabel = remember(cryptoCurrency, marketCurrencyFormat) {
            if (marketCurrencyFormat != "none") "CRYPTO ($cryptoCurrency)" else "CRYPTO"
        }

        val idxHeaderLabel = remember(activeIdxCategory, isIdxAutoRotate, marketCurrencyFormat) {
            val base = if (marketCurrencyFormat != "none") "IDX SAHAM (IDR)" else "IDX SAHAM"
            if (activeIdxCategory != null) {
                "$base | ${activeIdxCategory.label}"
            } else {
                base
            }
        }

        // Auto-advance slideshow timer (for carousel mode between Crypto and IDX)
        LaunchedEffect(carouselAutoRotate, carouselIntervalSeconds, hasCrypto, hasIdx, separateMarketViews) {
            if (!separateMarketViews && carouselAutoRotate && hasCrypto && hasIdx) {
                val intervalMs = (carouselIntervalSeconds.coerceIn(3, 30) * 1000L)
                while (true) {
                    delay(intervalMs)
                    currentViewIndex = (currentViewIndex + 1) % 2
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (hasCrypto || hasIdx) {
                if (hasCrypto && hasIdx && separateMarketViews) {
                    // SEPARATED MODE: Display both simultaneously to maximize screen space
                    if (separatedMarketLayout == "side_by_side") {
                        // Two columns (Side by side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = cryptoHeaderLabel,
                                    style = typography.clock,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary.copy(alpha = 0.45f),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                CryptoMarketList(
                                    cryptoCoins = cryptoCoins,
                                    compact = true,
                                    currency = cryptoCurrency,
                                    currencyFormat = marketCurrencyFormat
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedContent(
                                    targetState = idxHeaderLabel,
                                    transitionSpec = {
                                        (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(200)))
                                            .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(150)))
                                    },
                                    label = "idxSideHeaderLabel"
                                ) { labelText ->
                                    Text(
                                        text = labelText,
                                        style = typography.clock,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary.copy(alpha = 0.45f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier
                                            .clickable(enabled = isIdxAutoRotate && idxCategories.size > 1) {
                                                currentIdxCategoryIndex = (currentIdxCategoryIndex + 1) % idxCategories.size
                                            }
                                            .padding(bottom = 2.dp)
                                    )
                                }
                                IdxWaveMarketList(
                                    category = activeIdxCategory,
                                    categoryIndex = currentIdxCategoryIndex,
                                    compact = true,
                                    currency = "IDR",
                                    currencyFormat = marketCurrencyFormat
                                )
                            }
                        }
                    } else {
                        // Stacked vertically (Default separated mode)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = cryptoHeaderLabel,
                                    style = typography.clock,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary.copy(alpha = 0.45f),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                CryptoMarketList(
                                    cryptoCoins = cryptoCoins,
                                    compact = false,
                                    currency = cryptoCurrency,
                                    currencyFormat = marketCurrencyFormat
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AnimatedContent(
                                    targetState = idxHeaderLabel,
                                    transitionSpec = {
                                        (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(200)))
                                            .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(150)))
                                    },
                                    label = "idxStackedHeaderLabel"
                                ) { labelText ->
                                    Text(
                                        text = labelText,
                                        style = typography.clock,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary.copy(alpha = 0.45f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier
                                            .clickable(enabled = isIdxAutoRotate && idxCategories.size > 1) {
                                                currentIdxCategoryIndex = (currentIdxCategoryIndex + 1) % idxCategories.size
                                            }
                                            .padding(bottom = 2.dp)
                                    )
                                }
                                IdxWaveMarketList(
                                    category = activeIdxCategory,
                                    categoryIndex = currentIdxCategoryIndex,
                                    compact = false,
                                    currency = "IDR",
                                    currencyFormat = marketCurrencyFormat
                                )
                            }
                        }
                    }
                } else {
                    // CAROUSEL MODE (or single market active)
                    Column {
                        // If both views are active, show a subtle selector tab
                        if (hasCrypto && hasIdx) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = cryptoHeaderLabel,
                                    style = typography.clock,
                                    fontWeight = if (currentViewIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentViewIndex == 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .clickable { currentViewIndex = 0 }
                                        .padding(vertical = 2.dp)
                                )
                                Text(
                                    text = "·",
                                    style = typography.clock,
                                    color = colors.textSecondary.copy(alpha = 0.35f)
                                )
                                Text(
                                    text = idxHeaderLabel,
                                    style = typography.clock,
                                    fontWeight = if (currentViewIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentViewIndex == 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .clickable { currentViewIndex = 1 }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }

                        val activeIndex = when {
                            !hasCrypto -> 1
                            !hasIdx -> 0
                            else -> currentViewIndex
                        }

                        AnimatedContent(
                            targetState = activeIndex,
                            transitionSpec = {
                                if (carouselTransition == "crossfade") {
                                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                                } else {
                                    if (targetState > initialState) {
                                        (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(250)))
                                            .togetherWith(slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut(tween(200)))
                                    } else {
                                        (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(tween(250)))
                                            .togetherWith(slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut(tween(200)))
                                    }
                                }
                            },
                            label = "marketCarouselTransition"
                        ) { targetView ->
                            if (targetView == 0) {
                                CryptoMarketList(
                                    cryptoCoins = cryptoCoins,
                                    compact = false,
                                    currency = cryptoCurrency,
                                    currencyFormat = marketCurrencyFormat
                                )
                            } else {
                                IdxWaveMarketList(
                                    category = activeIdxCategory,
                                    categoryIndex = currentIdxCategoryIndex,
                                    compact = false,
                                    currency = "IDR",
                                    currencyFormat = marketCurrencyFormat
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned shortcuts positioned in bottom area for optimal one-hand reach
        val fontScale = getFontScale(fontSize)
        val iconSize = (24 * fontScale).coerceIn(16f, 32f).dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (showCornerText && cornerText.isNotBlank()) 12.dp else 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (pinnedApps.isEmpty()) {
                Text(
                    text = "Swipe up for all apps\nLong-press an app to pin here",
                    style = typography.drawerItem,
                    color = colors.textSecondary
                )
            } else {
                pinnedApps.take(8).forEach { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAppClick(app) },
                                onLongClick = { selectedAppForDialog = app }
                            )
                    ) {
                        if (showAppIcons) {
                            AppIcon(
                                item = app,
                                modifier = Modifier.size(iconSize),
                                isMonochrome = monochromeIcons
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        }
                        Text(
                            text = app.displayLabel,
                            style = typography.homeShortcut,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        // Custom corner text (bottom right)
        if (showCornerText && cornerText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = cornerText,
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clickable { onOpenSettings() }
                )
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

@Composable
private fun CryptoMarketList(
    cryptoCoins: List<CoinTickerUi>,
    compact: Boolean = false,
    currency: String = "USDT",
    currencyFormat: String = "suffix"
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        cryptoCoins.forEach { coin ->
            var showDetails by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .clickable { showDetails = !showDetails }
                    .graphicsLayer {
                        alpha = if (coin.isStale) 0.55f else 1.0f
                    }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
                ) {
                    Text(
                        text = coin.symbol,
                        style = if (compact) typography.priceLabel else typography.drawerItem,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )

                    Text(
                        text = formatPriceWithCurrency(coin.price, currency, currencyFormat),
                        style = if (compact) typography.priceLabel else typography.drawerItem,
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

                    if (!compact && coin.sparkline.isNotEmpty()) {
                        val sparkColor = if (coin.changePercent >= 0.0) CryptoGreen.copy(alpha = 0.85f) else CryptoRed.copy(alpha = 0.85f)
                        Text(
                            text = coin.sparkline,
                            style = typography.priceLabel,
                            color = sparkColor
                        )
                    }

                    if (coin.isStale && coin.ageMarker.isNotEmpty()) {
                        Text(
                            text = coin.ageMarker,
                            style = typography.priceLabel,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }

                if (showDetails && coin.highPrice.isNotEmpty() && coin.lowPrice.isNotEmpty()) {
                    Text(
                        text = formatRangeWithCurrency(coin.highPrice, coin.lowPrice, currency, currencyFormat),
                        style = typography.clock,
                        color = colors.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun IdxWaveMarketList(
    category: IdxCategoryUi?,
    categoryIndex: Int,
    compact: Boolean = false,
    currency: String = "IDR",
    currencyFormat: String = "suffix"
) {
    if (category == null || category.items.isEmpty()) return

    AnimatedContent(
        targetState = categoryIndex,
        transitionSpec = {
            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
        },
        label = "idxWaveAnimatedContent"
    ) { _ ->
        Column(
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            category.items.forEachIndexed { index, stock ->
                val slideDelay = index * 50 // Staggered delay for wave effect: 0ms, 50ms, 100ms, 150ms...

                Box(
                    modifier = Modifier.animateEnterExit(
                        enter = slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 320,
                                delayMillis = slideDelay,
                                easing = FastOutSlowInEasing
                            )
                        ) { fullWidth -> fullWidth } + fadeIn(
                            animationSpec = tween(
                                durationMillis = 260,
                                delayMillis = slideDelay
                            )
                        ),
                        exit = slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 280,
                                delayMillis = slideDelay,
                                easing = FastOutLinearInEasing
                            )
                        ) { fullWidth -> -fullWidth } + fadeOut(
                            animationSpec = tween(
                                durationMillis = 220,
                                delayMillis = slideDelay
                            )
                        )
                    )
                ) {
                    IdxStockItemRow(
                        stock = stock,
                        compact = compact,
                        currency = currency,
                        currencyFormat = currencyFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun IdxStockItemRow(
    stock: IdxItemUi,
    compact: Boolean = false,
    currency: String = "IDR",
    currencyFormat: String = "suffix"
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    var showDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clickable { showDetails = !showDetails }
            .graphicsLayer {
                alpha = if (stock.isStale) 0.55f else 1.0f
            }
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Text(
                text = stock.symbol,
                style = if (compact) typography.priceLabel else typography.drawerItem,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Text(
                text = formatPriceWithCurrency(stock.priceText, currency, currencyFormat),
                style = if (compact) typography.priceLabel else typography.drawerItem,
                color = colors.textSecondary
            )

            if (stock.changeText.isNotEmpty()) {
                val changeColor = if (stock.changePercent >= 0.0) CryptoGreen else CryptoRed
                Text(
                    text = stock.changeText,
                    style = typography.priceLabel,
                    color = changeColor
                )
            }

            if (stock.isStale && stock.ageMarker.isNotEmpty()) {
                Text(
                    text = stock.ageMarker,
                    style = typography.priceLabel,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )
            }
        }

        if (showDetails) {
            val details = mutableListOf<String>()
            if (stock.highPrice.isNotEmpty() && stock.lowPrice.isNotEmpty()) {
                details.add(formatRangeWithCurrency(stock.highPrice, stock.lowPrice, currency, currencyFormat))
            }
            if (stock.volumeText.isNotEmpty()) {
                details.add("Vol: ${stock.volumeText}")
            }
            if (stock.name.isNotEmpty() && !stock.name.equals(stock.symbol, ignoreCase = true)) {
                details.add(stock.name)
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString("  ·  "),
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatPriceWithCurrency(price: String, currency: String, format: String): String {
    if (price.isBlank() || price == "--" || format == "none" || currency.isBlank()) {
        return price
    }
    return when (format) {
        "prefix" -> "$currency $price"
        else -> "$price $currency"
    }
}

private fun formatRangeWithCurrency(high: String, low: String, currency: String, format: String): String {
    val h = formatPriceWithCurrency(high, currency, format)
    val l = formatPriceWithCurrency(low, currency, format)
    return "24h H: $h  ·  L: $l"
}
