package com.nounish.launcher.ui

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nounish.launcher.CoinTickerUi
import com.nounish.launcher.IdxCategoryUi
import com.nounish.launcher.data.AppIcon
import com.nounish.launcher.data.IdxItemUi
import com.nounish.launcher.data.NounsAuctionUi
import com.nounish.launcher.data.NounsCandidateUi
import com.nounish.launcher.data.NounsProposalUi
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.model.AppShortcut
import com.nounish.launcher.model.DEFAULT_PASSIVE_NOTES
import com.nounish.launcher.model.PassiveNote
import com.nounish.launcher.service.NotificationHelper
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
import com.nounish.launcher.util.LauncherHaptic
import com.nounish.launcher.ui.theme.LauncherTheme
import com.nounish.launcher.ui.theme.getFontScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    showNouns: Boolean = true,
    nounsAuction: NounsAuctionUi? = null,
    nounsProposals: List<NounsProposalUi> = emptyList(),
    nounsCandidates: List<NounsCandidateUi> = emptyList(),
    nounsDisplayMode: String = "auto_rotate",
    nounsMaxDisplayCount: Int = 3,
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
    showNote: Boolean = true,
    isNoteDismissed: Boolean = false,
    noteIndex: Int = 0,
    onDismissNote: () -> Unit = {},
    onNextNote: () -> Unit = {},
    onAppClick: (AppItem) -> Unit,
    onAppInfoClick: (AppItem) -> Unit,
    onUnpinApp: (AppItem) -> Unit,
    onRenameApp: (AppItem, String?) -> Unit,
    onLoadShortcuts: suspend (AppItem) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit,
    isRefreshing: Boolean = false,
    swipeDownAction: String = "notifications",
    hapticFeedbackLevel: String = "standard",
    onManualRefresh: () -> Unit = {}
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
    var homeShortcuts by remember(selectedAppForDialog) { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var appToRename by remember { mutableStateOf<AppItem?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    LaunchedEffect(selectedAppForDialog) {
        val app = selectedAppForDialog
        homeShortcuts = if (app != null) {
            try {
                onLoadShortcuts(app)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    var pullDistancePx by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .launcherGestures(
                onSwipeUp = onOpenDrawer,
                onSwipeDown = {
                    when (swipeDownAction) {
                        "notifications" -> {
                            NotificationHelper.expandNotificationShade(context)
                        }
                        "refresh" -> {
                            LauncherHaptic.performStrong(haptic, hapticFeedbackLevel)
                            onManualRefresh()
                        }
                        "drawer" -> {
                            onOpenDrawer()
                        }
                        "none" -> {
                            // Disabled
                        }
                    }
                },
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                onDoubleTap = onDoubleTap,
                onLongPress = onOpenSettings,
                onPullDownDelta = { delta ->
                    if (swipeDownAction == "refresh") {
                        pullDistancePx = delta
                    }
                },
                onPullDownRelease = {
                    pullDistancePx = 0f
                }
            )
            .padding(horizontal = 24.dp)
    ) {
        // Nounish Manual Refresh Indicator (breathing / animated typing ⌐◨-◨)
        if (swipeDownAction == "refresh") {
            NounishRefreshIndicator(
                isRefreshing = isRefreshing,
                pullDistancePx = pullDistancePx
            )
        }

        val currentNote = remember(noteIndex) {
            DEFAULT_PASSIVE_NOTES.getOrNull(noteIndex.coerceIn(0, DEFAULT_PASSIVE_NOTES.size - 1))
                ?: DEFAULT_PASSIVE_NOTES.first()
        }

        // Top section: Clock & Date on left, Closeable Passive Learning Note on right
        val isNoteVisible = showNote && !isNoteDismissed
        if (showClock || showDate || isNoteVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (showClock || showDate) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 12.dp)
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
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (isNoteVisible) {
                    PassiveNoteCard(
                        note = currentNote,
                        onDismiss = onDismissNote,
                        onClick = onNextNote
                    )
                }
            }
        }

        // Market Section (middle area): Takes all available vertical space between Clock and Pinned Apps
        val hasCrypto = showCrypto && cryptoCoins.isNotEmpty()
        val hasIdx = showIdx && (idxCategories.isNotEmpty() || idxStocks.isNotEmpty())
        val hasNouns = showNouns && (nounsAuction != null || nounsProposals.isNotEmpty() || nounsCandidates.isNotEmpty())

        val activeViews = remember(hasCrypto, hasIdx, hasNouns) {
            buildList {
                if (hasCrypto) add("crypto")
                if (hasIdx) add("idx")
                if (hasNouns) add("nouns")
            }
        }

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

        // Available Nouns sub-tabs and auto-rotation (Auction, Proposals, Candidates)
        val isNounsAutoRotate = nounsDisplayMode == "auto_rotate"
        val availableNounsSubs = remember(nounsAuction, nounsProposals, nounsCandidates, nounsDisplayMode) {
            when (nounsDisplayMode) {
                "auction" -> if (nounsAuction != null) listOf("auction") else emptyList()
                "proposals" -> if (nounsProposals.isNotEmpty()) listOf("proposals") else emptyList()
                "candidates" -> if (nounsCandidates.isNotEmpty()) listOf("candidates") else emptyList()
                else -> buildList {
                    if (nounsAuction != null) add("auction")
                    if (nounsProposals.isNotEmpty()) add("proposals")
                    if (nounsCandidates.isNotEmpty()) add("candidates")
                }
            }
        }
        var currentNounsSubIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(isNounsAutoRotate, availableNounsSubs.size) {
            if (isNounsAutoRotate && availableNounsSubs.size > 1) {
                while (true) {
                    delay(7000L)
                    currentNounsSubIndex = (currentNounsSubIndex + 1) % availableNounsSubs.size
                }
            }
        }

        val activeNounsSub = if (availableNounsSubs.isNotEmpty()) {
            availableNounsSubs[currentNounsSubIndex.coerceIn(0, availableNounsSubs.size - 1)]
        } else {
            when (nounsDisplayMode) {
                "auction" -> "auction"
                "candidates" -> "candidates"
                else -> "proposals"
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

        val nounsHeaderLabel = remember(activeNounsSub, isNounsAutoRotate, availableNounsSubs.size) {
            val subLabel = when (activeNounsSub) {
                "auction" -> "Auction"
                "candidates" -> "Candidates"
                "proposals" -> "Proposals"
                else -> ""
            }
            if (subLabel.isNotEmpty() && (isNounsAutoRotate || availableNounsSubs.size > 1)) {
                "NOUNS DAO ⌐◨-◨ | $subLabel"
            } else {
                "NOUNS DAO ⌐◨-◨"
            }
        }

        // Auto-advance slideshow timer (for carousel mode across all active views)
        LaunchedEffect(carouselAutoRotate, carouselIntervalSeconds, activeViews.size, separateMarketViews) {
            if (!separateMarketViews && carouselAutoRotate && activeViews.size > 1) {
                val intervalMs = (carouselIntervalSeconds.coerceIn(3, 30) * 1000L)
                while (true) {
                    delay(intervalMs)
                    currentViewIndex = (currentViewIndex + 1) % activeViews.size
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (activeViews.isNotEmpty()) {
                if (activeViews.size > 1 && separateMarketViews) {
                    // SEPARATED MODE: Display active views simultaneously
                    if (separatedMarketLayout == "side_by_side") {
                        // Multi-column side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (hasCrypto) {
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
                            }

                            if (hasIdx) {
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

                            if (hasNouns) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = nounsHeaderLabel,
                                        transitionSpec = {
                                            (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(200)))
                                                .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(150)))
                                        },
                                        label = "nounsSideHeaderLabel"
                                    ) { labelText ->
                                        Text(
                                            text = labelText,
                                            style = typography.clock,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textSecondary.copy(alpha = 0.45f),
                                            letterSpacing = 1.sp,
                                            modifier = Modifier
                                                .clickable(enabled = isNounsAutoRotate && availableNounsSubs.size > 1) {
                                                    currentNounsSubIndex = (currentNounsSubIndex + 1) % availableNounsSubs.size
                                                }
                                                .padding(bottom = 2.dp)
                                        )
                                    }
                                    NounsSectionContent(
                                        activeNounsSub = activeNounsSub,
                                        auction = nounsAuction,
                                        proposals = nounsProposals,
                                        candidates = nounsCandidates,
                                        maxCount = nounsMaxDisplayCount,
                                        compact = true
                                    )
                                }
                            }
                        }
                    } else {
                        // Stacked vertically (Default separated mode)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (hasCrypto) {
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
                            }

                            if (hasIdx) {
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

                            if (hasNouns) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AnimatedContent(
                                        targetState = nounsHeaderLabel,
                                        transitionSpec = {
                                            (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(200)))
                                                .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(150)))
                                        },
                                        label = "nounsStackedHeaderLabel"
                                    ) { labelText ->
                                        Text(
                                            text = labelText,
                                            style = typography.clock,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textSecondary.copy(alpha = 0.45f),
                                            letterSpacing = 1.sp,
                                            modifier = Modifier
                                                .clickable(enabled = isNounsAutoRotate && availableNounsSubs.size > 1) {
                                                    currentNounsSubIndex = (currentNounsSubIndex + 1) % availableNounsSubs.size
                                                }
                                                .padding(bottom = 2.dp)
                                        )
                                    }
                                    NounsSectionContent(
                                        activeNounsSub = activeNounsSub,
                                        auction = nounsAuction,
                                        proposals = nounsProposals,
                                        candidates = nounsCandidates,
                                        maxCount = nounsMaxDisplayCount,
                                        compact = false
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // CAROUSEL MODE (or single view active)
                    Column {
                        // If multiple views are active, show subtle tab selectors
                        if (activeViews.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                activeViews.forEachIndexed { i, viewKey ->
                                    if (i > 0) {
                                        Text(
                                            text = "·",
                                            style = typography.clock,
                                            color = colors.textSecondary.copy(alpha = 0.35f)
                                        )
                                    }
                                    val label = when (viewKey) {
                                        "crypto" -> cryptoHeaderLabel
                                        "idx" -> idxHeaderLabel
                                        "nouns" -> nounsHeaderLabel
                                        else -> ""
                                    }
                                    val isSelected = (currentViewIndex % activeViews.size) == i
                                    Text(
                                        text = label,
                                        style = typography.clock,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .clickable {
                                                if (isSelected && viewKey == "nouns" && isNounsAutoRotate && availableNounsSubs.size > 1) {
                                                    currentNounsSubIndex = (currentNounsSubIndex + 1) % availableNounsSubs.size
                                                } else {
                                                    currentViewIndex = i
                                                }
                                            }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val activeViewKey = activeViews.getOrNull(currentViewIndex % activeViews.size) ?: activeViews.first()

                        AnimatedContent(
                            targetState = activeViewKey,
                            transitionSpec = {
                                if (carouselTransition == "crossfade") {
                                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                                } else {
                                    (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut(tween(200)))
                                }
                            },
                            label = "marketCarouselTransition"
                        ) { targetView ->
                            when (targetView) {
                                "crypto" -> {
                                    CryptoMarketList(
                                        cryptoCoins = cryptoCoins,
                                        compact = false,
                                        currency = cryptoCurrency,
                                        currencyFormat = marketCurrencyFormat
                                    )
                                }
                                "idx" -> {
                                    IdxWaveMarketList(
                                        category = activeIdxCategory,
                                        categoryIndex = currentIdxCategoryIndex,
                                        compact = false,
                                        currency = "IDR",
                                        currencyFormat = marketCurrencyFormat
                                    )
                                }
                                "nouns" -> {
                                    NounsSectionContent(
                                        activeNounsSub = activeNounsSub,
                                        auction = nounsAuction,
                                        proposals = nounsProposals,
                                        candidates = nounsCandidates,
                                        maxCount = nounsMaxDisplayCount,
                                        compact = false
                                    )
                                }
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Deep app shortcuts (e.g. Chrome's "New Tab", WhatsApp's recent chats)
                    if (homeShortcuts.isNotEmpty()) {
                        homeShortcuts.forEach { shortcut ->
                            Text(
                                text = shortcut.label,
                                style = typography.settingsItem,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onShortcutClick(shortcut)
                                        selectedAppForDialog = null
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(0.5.dp)
                                .background(colors.border)
                        )
                    }

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

@Composable
private fun PassiveNoteCard(
    note: PassiveNote,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Column(
        modifier = Modifier
            .widthIn(max = 205.dp)
            .background(
                color = colors.surface.copy(alpha = 0.55f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 0.5.dp,
                color = colors.textSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.title,
                style = typography.clock.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 11.sp,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )
            }
        }

        Text(
            text = note.body,
            style = typography.clock.copy(fontSize = 10.sp, lineHeight = 14.sp),
            color = colors.textSecondary.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 3.dp)
        )

        if (note.attribution.isNotEmpty()) {
            Text(
                text = note.attribution,
                style = typography.clock.copy(fontSize = 9.sp),
                color = colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun NounsSectionContent(
    activeNounsSub: String,
    auction: NounsAuctionUi?,
    proposals: List<NounsProposalUi>,
    candidates: List<NounsCandidateUi>,
    maxCount: Int,
    compact: Boolean
) {
    AnimatedContent(
        targetState = activeNounsSub,
        transitionSpec = {
            (slideInHorizontally(tween(250)) { it / 3 } + fadeIn(tween(200)))
                .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(150)))
        },
        label = "nounsSubContentTransition"
    ) { sub ->
        when (sub) {
            "auction" -> {
                if (auction != null) {
                    NounsAuctionCard(
                        auction = auction,
                        compact = compact
                    )
                } else {
                    NounsProposalList(
                        proposals = proposals,
                        maxCount = maxCount,
                        compact = compact
                    )
                }
            }
            "candidates" -> {
                NounsCandidateList(
                    candidates = candidates,
                    maxCount = maxCount,
                    compact = compact
                )
            }
            else -> {
                NounsProposalList(
                    proposals = proposals,
                    maxCount = maxCount,
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun NounsAuctionCard(
    auction: NounsAuctionUi,
    compact: Boolean = false
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clickable { showDetails = !showDetails }
            .graphicsLayer {
                alpha = if (auction.isStale) 0.55f else 1.0f
            }
            .padding(vertical = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            Text(
                text = "Noun #${auction.nounId}",
                style = if (compact) typography.priceLabel else typography.drawerItem,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Text(
                text = auction.amountFormatted,
                style = if (compact) typography.priceLabel else typography.drawerItem,
                color = if (auction.amountEth > 0.0) CryptoGreen else colors.textSecondary
            )

            val timeColor = if (auction.settled || auction.timeLeftFormatted == "Auction ended") {
                CryptoRed.copy(alpha = 0.8f)
            } else {
                colors.textSecondary.copy(alpha = 0.7f)
            }

            Text(
                text = auction.timeLeftFormatted,
                style = typography.priceLabel,
                fontWeight = if (!auction.settled && auction.timeLeftFormatted != "Auction ended") FontWeight.Medium else FontWeight.Normal,
                color = timeColor
            )

            if (auction.isStale && auction.ageMarker.isNotEmpty()) {
                Text(
                    text = auction.ageMarker,
                    style = typography.priceLabel,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )
            }
        }

        if (showDetails) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "High Bidder: ${auction.bidderFormatted}",
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )

                Text(
                    text = "Bid on nouns.wtf ↗",
                    style = typography.clock,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.clickable {
                        try {
                            val uri = Uri.parse("https://nouns.wtf")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
        }
    }
}

@Composable
private fun NounsProposalList(
    proposals: List<NounsProposalUi>,
    maxCount: Int = 3,
    compact: Boolean = false
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current

    val limit = if (compact) (maxCount - 1).coerceAtLeast(2) else maxCount

    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        proposals.take(limit).forEach { prop ->
            var showDetails by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .clickable { showDetails = !showDetails }
                    .graphicsLayer {
                        alpha = if (prop.isStale) 0.55f else 1.0f
                    }
                    .padding(vertical = 1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
                ) {
                    Text(
                        text = "#${prop.id}",
                        style = if (compact) typography.priceLabel else typography.drawerItem,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )

                    Text(
                        text = prop.title,
                        style = if (compact) typography.priceLabel else typography.drawerItem,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val statusColor = when (prop.statusLabel.lowercase(Locale.US)) {
                        "active" -> CryptoGreen
                        "executed" -> colors.textSecondary.copy(alpha = 0.8f)
                        "defeated", "canceled", "vetoed" -> CryptoRed.copy(alpha = 0.7f)
                        "succeeded", "queued" -> colors.textPrimary
                        else -> colors.textSecondary.copy(alpha = 0.6f)
                    }

                    Text(
                        text = prop.statusLabel,
                        style = typography.priceLabel,
                        fontWeight = if (prop.statusLabel.equals("Active", ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                        color = statusColor
                    )

                    if (prop.isStale && prop.ageMarker.isNotEmpty()) {
                        Text(
                            text = prop.ageMarker,
                            style = typography.priceLabel,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }

                if (showDetails) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "For: ${prop.forVotes}  ·  Against: ${prop.againstVotes}  ·  Quorum: ${prop.quorum}",
                            style = typography.clock,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "Vote ↗",
                            style = typography.clock,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.clickable {
                                try {
                                    val uri = Uri.parse("https://nouns.wtf/vote/${prop.id}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NounsCandidateList(
    candidates: List<NounsCandidateUi>,
    maxCount: Int = 3,
    compact: Boolean = false
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current

    val limit = if (compact) (maxCount - 1).coerceAtLeast(2) else maxCount

    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
    ) {
        candidates.take(limit).forEach { cand ->
            var showDetails by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .clickable { showDetails = !showDetails }
                    .graphicsLayer {
                        alpha = if (cand.isStale) 0.55f else 1.0f
                    }
                    .padding(vertical = 1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
                ) {
                    Text(
                        text = "Draft",
                        style = if (compact) typography.priceLabel else typography.drawerItem,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )

                    Text(
                        text = cand.title,
                        style = if (compact) typography.priceLabel else typography.drawerItem,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = "Candidate",
                        style = typography.priceLabel,
                        color = colors.textSecondary.copy(alpha = 0.6f)
                    )

                    if (cand.isStale && cand.ageMarker.isNotEmpty()) {
                        Text(
                            text = cand.ageMarker,
                            style = typography.priceLabel,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }

                if (showDetails) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val shortProposer = if (cand.proposer.length >= 10) {
                            "${cand.proposer.take(6)}...${cand.proposer.takeLast(4)}"
                        } else cand.proposer

                        Text(
                            text = "By: $shortProposer",
                            style = typography.clock,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "Candidate ↗",
                            style = typography.clock,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.clickable {
                                try {
                                    val url = "https://nouns.wtf/candidates/${cand.proposer.lowercase(Locale.US)}-${cand.slug}"
                                    val uri = Uri.parse(url)
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NounishRefreshIndicator(
    isRefreshing: Boolean,
    pullDistancePx: Float,
    pullThresholdPx: Float = 160f,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    // Track when refresh just completed to show "updated" briefly before collapsing
    var showUpdatedState by remember { mutableStateOf(false) }
    var prevRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && prevRefreshing) {
            showUpdatedState = true
            delay(400L)
            showUpdatedState = false
        }
        prevRefreshing = isRefreshing
    }

    val pullProgress = (pullDistancePx / pullThresholdPx).coerceIn(0f, 1f)

    val targetHeight = when {
        isRefreshing || showUpdatedState -> 56.dp
        pullDistancePx > 0f -> (pullProgress * 56f).dp
        else -> 0.dp
    }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "refreshHeight"
    )

    if (animatedHeight > 2.dp) {
        // Breathing pulse animation for ⌐◨-◨
        val infiniteTransition = rememberInfiniteTransition(label = "RefreshBreathing")
        val breathScale by infiniteTransition.animateFloat(
            initialValue = 0.93f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathScale"
        )
        val breathAlpha by infiniteTransition.animateFloat(
            initialValue = 0.60f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathAlpha"
        )

        // Animated typing of ⌐◨-◨ while refreshing
        var typingIndex by remember { mutableIntStateOf(3) }
        var dotCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                while (isActive) {
                    // Type glasses: ⌐ -> ⌐◨ -> ⌐◨- -> ⌐◨-◨
                    for (i in 0..3) {
                        typingIndex = i
                        delay(100L)
                    }
                    // Hold full glasses and cycle typing dots while breathing
                    for (d in 0..2) {
                        dotCount = (dotCount + 1) % 4
                        delay(220L)
                    }
                }
            } else {
                typingIndex = 3
                dotCount = 0
            }
        }

        val glassesText = when {
            showUpdatedState -> "⌐◨-◨"
            isRefreshing -> when (typingIndex) {
                0 -> "⌐"
                1 -> "⌐◨"
                2 -> "⌐◨-"
                else -> "⌐◨-◨"
            }
            pullProgress < 0.25f -> "⌐"
            pullProgress < 0.50f -> "⌐◨"
            pullProgress < 0.75f -> "⌐◨-"
            else -> "⌐◨-◨"
        }

        val subText = when {
            showUpdatedState -> "updated"
            isRefreshing -> {
                val dots = ".".repeat(dotCount)
                "refreshing$dots"
            }
            pullProgress >= 1.0f -> "release to refresh"
            else -> "pull to refresh"
        }

        val currentScale = if (isRefreshing) breathScale else (0.75f + pullProgress * 0.25f)
        val currentAlpha = if (isRefreshing) breathAlpha else (0.3f + pullProgress * 0.7f)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .padding(top = 6.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = glassesText,
                    style = typography.drawerItem.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = colors.accent,
                    modifier = Modifier.graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                        alpha = currentAlpha
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    style = typography.clock.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = if (showUpdatedState) CryptoGreen else colors.textSecondary.copy(alpha = 0.75f)
                )
            }
        }
    }
}
