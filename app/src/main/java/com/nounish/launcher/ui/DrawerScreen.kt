package com.nounish.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nounish.launcher.data.AppIcon
import com.nounish.launcher.data.PriceUiState
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.model.AppShortcut
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
import com.nounish.launcher.ui.theme.LauncherTheme
import com.nounish.launcher.ui.theme.getFontScale
import com.nounish.launcher.util.LauncherHaptic
import com.nounish.launcher.util.MathEvaluator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DrawerScreen(
    apps: List<AppItem>,
    searchQuery: String,
    pinnedPackages: List<String>,
    hiddenPackages: Set<String>,
    usdRateState: PriceUiState,
    cryptoPriceEnabled: Boolean,
    autoLaunchSingleMatch: Boolean,
    alphabetIndexOnRight: Boolean,
    enableIndexAnimation: Boolean,
    showAppIcons: Boolean = true,
    monochromeIcons: Boolean = true,
    fontSize: String = "medium",
    searchBarAtBottom: Boolean = false,
    recentApps: List<AppItem> = emptyList(),
    hapticFeedbackLevel: String = "standard",
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onTogglePinApp: (AppItem) -> Unit,
    onToggleHideApp: (AppItem) -> Unit,
    onRenameApp: (AppItem, String?) -> Unit,
    onAppInfoClick: (AppItem) -> Unit,
    onUninstallClick: (AppItem) -> Unit,
    onRefreshUsdRate: () -> Unit,
    onCloseDrawer: () -> Unit,
    onLoadShortcuts: suspend (AppItem) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {}
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val calculationResult = remember(searchQuery) {
        MathEvaluator.evaluate(searchQuery)?.let { MathEvaluator.formatResult(it) }
    }

    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }
    var menuShortcuts by remember(selectedAppForMenu) { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var appToRename by remember { mutableStateOf<AppItem?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    LaunchedEffect(selectedAppForMenu) {
        val app = selectedAppForMenu
        menuShortcuts = if (app != null) {
            try {
                onLoadShortcuts(app)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val alphabet = remember { ('A'..'Z').toList() + listOf('#') }
    val activeLetters = remember(apps) {
        apps.map { AppItem.getAlphabetBucket(it.displayLabel) }.toSet()
    }

    // Niagara-style wave animation state
    var isInteractingWithIndex by remember { mutableStateOf(false) }
    var selectedTargetIndex by remember { mutableIntStateOf(-1) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    val interactionProgress by animateFloatAsState(
        targetValue = if (isInteractingWithIndex) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 320f
        ),
        label = "niagaraWaveProgress"
    )

    LaunchedEffect(isInteractingWithIndex, interactionProgress) {
        if (!isInteractingWithIndex && interactionProgress == 0f) {
            selectedLetter = null
            selectedTargetIndex = -1
        }
    }

    // Auto-launch if exactly one match remains (optional setting)
    LaunchedEffect(searchQuery, apps, autoLaunchSingleMatch, calculationResult) {
        if (autoLaunchSingleMatch && calculationResult == null && searchQuery.trim().length >= 2 && apps.size == 1) {
            onAppClick(apps.first())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .launcherGestures(
                onSwipeDown = onCloseDrawer
            )
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val searchBarContent: @Composable () -> Unit = {
                // Search field on the left, Crypto price on the right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Search Input Field
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search apps...",
                                style = typography.searchInput,
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = typography.searchInput.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    if (calculationResult != null) {
                                        LauncherHaptic.performMedium(haptic, hapticFeedbackLevel)
                                        clipboardManager.setText(AnnotatedString(calculationResult))
                                    } else if (apps.isNotEmpty()) {
                                        onAppClick(apps.first())
                                    }
                                }
                            )
                        )
                    }

                    // USD Rate in IDR with 24h percentage change & stale-data indicator
                    if (cryptoPriceEnabled && usdRateState.isNotEmpty) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = {
                                    LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                                    onRefreshUsdRate()
                                })
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                .graphicsLayer {
                                    alpha = if (usdRateState.isStale) 0.55f else 1.0f
                                }
                        ) {
                            Text(
                                text = "${usdRateState.coinSymbol} ${usdRateState.priceText}",
                                style = typography.priceLabel,
                                color = colors.textSecondary
                            )
                            if (usdRateState.changeText.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                val changeColor = if (usdRateState.changePercent >= 0.0) CryptoGreen else CryptoRed
                                Text(
                                    text = usdRateState.changeText,
                                    style = typography.priceLabel,
                                    color = changeColor
                                )
                            }
                            if (usdRateState.isStale && usdRateState.ageMarker.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = usdRateState.ageMarker,
                                    style = typography.priceLabel,
                                    color = colors.textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            val recentAppsContent: @Composable () -> Unit = {
                if (searchQuery.isEmpty() && recentApps.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "RECENT",
                            style = typography.settingsDescription.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentApps, key = { "recent_" + it.key }) { app ->
                                val fontScale = getFontScale(fontSize)
                                val iconSize = (16 * fontScale).coerceIn(12f, 22f).dp
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            color = colors.surface.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = colors.border.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .combinedClickable(
                                            onClick = { onAppClick(app) },
                                            onLongClick = { selectedAppForMenu = app }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (showAppIcons) {
                                        AppIcon(
                                            item = app,
                                            modifier = Modifier.size(iconSize),
                                            isMonochrome = monochromeIcons
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = app.displayLabel,
                                        style = typography.drawerItem.copy(fontSize = (13 * fontScale).sp),
                                        color = colors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!searchBarAtBottom) {
                Spacer(modifier = Modifier.height(16.dp))
                searchBarContent()
                Spacer(modifier = Modifier.height(12.dp))
                recentAppsContent()
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Minimalist Math Calculation Result Row
            if (calculationResult != null) {
                CalculationResultRow(
                    result = calculationResult,
                    fontSize = fontSize,
                    hapticFeedbackLevel = hapticFeedbackLevel
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Content: Alphabet Index + Niagara-style Animated App List
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (calculationResult == null) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No apps found" else "Loading...",
                            style = typography.drawerItem,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val onLetterScrub: (Char, Float) -> Unit = { char, fraction ->
                        selectedLetter = char
                        val targetIndex = findTargetIndex(apps, char)
                        if (targetIndex != null) {
                            selectedTargetIndex = targetIndex
                            val itemsAbove = (fraction * 10).toInt().coerceIn(0, 8)
                            val scrollIndex = (targetIndex - itemsAbove).coerceAtLeast(0)
                            coroutineScope.launch {
                                listState.scrollToItem(scrollIndex)
                            }
                        }
                    }

                    val matchingIndices = remember(apps, selectedLetter) {
                        if (selectedLetter == null) {
                            emptyList()
                        } else {
                            apps.mapIndexedNotNull { idx, app ->
                                if (AppItem.getAlphabetBucket(app.displayLabel) == selectedLetter) idx else null
                            }
                        }
                    }
                    val hasDirectMatches = matchingIndices.isNotEmpty()
                    val firstMatchIndex = if (hasDirectMatches) matchingIndices.first() else selectedTargetIndex
                    val lastMatchIndex = if (hasDirectMatches) matchingIndices.last() else selectedTargetIndex

                    // LEFT Alphabet Index (when placed on left and not searching)
                    if (!alphabetIndexOnRight && searchQuery.isEmpty()) {
                        AlphabetIndex(
                            letters = alphabet,
                            activeLetters = activeLetters,
                            hapticFeedbackLevel = hapticFeedbackLevel,
                            onInteractionStart = {
                                isInteractingWithIndex = true
                            },
                            onInteractionEnd = {
                                isInteractingWithIndex = false
                            },
                            onLetterSelected = onLetterScrub
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // App list with Niagara wave arc animation
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(apps, key = { _, item -> item.key }) { index, app ->
                            val isHidden = hiddenPackages.any { it == app.key || it == app.packageName }

                            val isMatching = if (selectedLetter != null) {
                                if (hasDirectMatches) {
                                    AppItem.getAlphabetBucket(app.displayLabel) == selectedLetter
                                } else {
                                    index == selectedTargetIndex
                                }
                            } else false

                            // Calculate wave curve factor based on distance from matching group or target
                            val diff = when {
                                firstMatchIndex < 0 -> 99
                                isMatching -> 0
                                index < firstMatchIndex -> firstMatchIndex - index
                                index > lastMatchIndex -> index - lastMatchIndex
                                else -> 0
                            }
                            val waveFactor = when (diff) {
                                0 -> 1.0f
                                1 -> 0.65f
                                2 -> 0.35f
                                3 -> 0.12f
                                else -> 0.0f
                            }

                            val maxShiftPx = with(density) {
                                if (alphabetIndexOnRight) 90.dp.toPx() else 45.dp.toPx()
                            }
                            val baseShiftPx = with(density) {
                                if (alphabetIndexOnRight) 16.dp.toPx() else 0.dp.toPx()
                            }

                            val currentShift = if (enableIndexAnimation) (baseShiftPx + maxShiftPx * waveFactor) * interactionProgress else 0f
                            val currentScale = if (enableIndexAnimation) 1f + (0.16f * waveFactor * interactionProgress) else 1f

                            val fontScale = getFontScale(fontSize)
                            val iconSize = (20 * fontScale).coerceIn(14f, 26f).dp

                            val isInteracting = interactionProgress > 0.02f

                            val itemTextColor = when {
                                isHidden -> colors.textSecondary.copy(alpha = if (isMatching) 0.85f else 0.5f)
                                isInteracting -> {
                                    if (isMatching) {
                                        colors.accent
                                    } else {
                                        colors.textSecondary
                                    }
                                }
                                else -> colors.textPrimary
                            }

                            val itemFontWeight = if (isInteracting && isMatching) FontWeight.Bold else FontWeight.Normal

                            val itemAlpha = when {
                                isHidden -> if (isInteracting) {
                                    if (isMatching) 0.85f else 0.20f
                                } else 0.6f
                                isInteracting -> {
                                    if (isMatching) {
                                        1.0f
                                    } else {
                                        (1f - 0.72f * interactionProgress).coerceIn(0.28f, 1f)
                                    }
                                }
                                else -> 1.0f
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationX = currentShift
                                        scaleX = currentScale
                                        scaleY = currentScale
                                        alpha = itemAlpha
                                        transformOrigin = TransformOrigin(
                                            pivotFractionX = if (alphabetIndexOnRight) 1f else 0f,
                                            pivotFractionY = 0.5f
                                        )
                                    }
                                    .combinedClickable(
                                        onClick = { onAppClick(app) },
                                        onLongClick = { selectedAppForMenu = app }
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                if (showAppIcons) {
                                    AppIcon(
                                        item = app,
                                        modifier = Modifier.size(iconSize),
                                        isMonochrome = monochromeIcons
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = if (isHidden) "${app.displayLabel} (hidden)" else app.displayLabel,
                                    style = typography.drawerItem,
                                    fontWeight = itemFontWeight,
                                    color = itemTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // RIGHT Alphabet Index (when placed on right and not searching)
                    if (alphabetIndexOnRight && searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.width(16.dp))
                        AlphabetIndex(
                            letters = alphabet,
                            activeLetters = activeLetters,
                            hapticFeedbackLevel = hapticFeedbackLevel,
                            onInteractionStart = {
                                isInteractingWithIndex = true
                            },
                            onInteractionEnd = {
                                isInteractingWithIndex = false
                            },
                            onLetterSelected = onLetterScrub
                        )
                    }
                }
            }

            if (searchBarAtBottom) {
                recentAppsContent()
                Spacer(modifier = Modifier.height(8.dp))
                searchBarContent()
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Long press action dialog for Drawer app item
    selectedAppForMenu?.let { app ->
        val isPinned = pinnedPackages.any { it == app.key || it == app.packageName }
        val isHidden = hiddenPackages.any { it == app.key || it == app.packageName }

        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
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
                    if (menuShortcuts.isNotEmpty()) {
                        menuShortcuts.forEach { shortcut ->
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
                                        selectedAppForMenu = null
                                        onCloseDrawer()
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
                        text = "Rename app",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                renameInputText = app.customLabel ?: app.label
                                appToRename = app
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 12.dp)
                    )

                    Text(
                        text = if (isPinned) "Unpin from home" else "Pin to home",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTogglePinApp(app)
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 12.dp)
                    )

                    Text(
                        text = if (isHidden) "Unhide app" else "Hide app",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleHideApp(app)
                                selectedAppForMenu = null
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
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 12.dp)
                    )

                    Text(
                        text = "Uninstall",
                        style = typography.settingsItem,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUninstallClick(app)
                                selectedAppForMenu = null
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
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
private fun AlphabetIndex(
    letters: List<Char>,
    activeLetters: Set<Char>,
    hapticFeedbackLevel: String = "standard",
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    onLetterSelected: (Char, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val haptic = LocalHapticFeedback.current
    var currentHoveredLetter by remember { mutableStateOf<Char?>(null) }
    var heightPx by remember { mutableFloatStateOf(1f) }

    fun selectLetterAt(y: Float) {
        if (letters.isEmpty() || heightPx <= 0f) return
        val clampedY = y.coerceIn(0f, heightPx - 1f)
        val fraction = clampedY / heightPx
        val index = (fraction * letters.size).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[index]
        if (letter != currentHoveredLetter) {
            currentHoveredLetter = letter
            LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
            onLetterSelected(letter, fraction)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
            .onGloballyPositioned { layoutCoordinates ->
                heightPx = layoutCoordinates.size.height.toFloat()
            }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onInteractionStart()
                    selectLetterAt(down.position.y)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            break
                        }
                        change.consume()
                        selectLetterAt(change.position.y)
                    }
                    currentHoveredLetter = null
                    onInteractionEnd()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { char ->
                val isActive = activeLetters.contains(char)
                val isHovered = currentHoveredLetter == char

                Text(
                    text = char.toString(),
                    fontSize = if (isHovered) 14.sp else 10.5.sp,
                    fontWeight = if (isHovered) FontWeight.Bold else if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = when {
                        isHovered -> colors.accent
                        isActive -> colors.textSecondary
                        else -> colors.textSecondary.copy(alpha = 0.25f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(22.dp)
                )
            }
        }
    }
}

private fun findTargetIndex(apps: List<AppItem>, target: Char): Int? {
    if (apps.isEmpty()) return null
    if (target == '#') {
        val idx = apps.indexOfFirst {
            AppItem.getAlphabetBucket(it.displayLabel) == '#'
        }
        return if (idx >= 0) idx else apps.lastIndex
    }

    // Exact match for target letter bucket
    val exactIdx = apps.indexOfFirst {
        AppItem.getAlphabetBucket(it.displayLabel) == target
    }
    if (exactIdx >= 0) return exactIdx

    // Find first app starting with a letter bucket after target
    val nextIdx = apps.indexOfFirst {
        val b = AppItem.getAlphabetBucket(it.displayLabel)
        b in 'A'..'Z' && b > target
    }
    if (nextIdx >= 0) return nextIdx

    // If no letter after target, jump to '#' bucket or last app
    val hashIdx = apps.indexOfFirst { AppItem.getAlphabetBucket(it.displayLabel) == '#' }
    if (hashIdx >= 0) return hashIdx

    return apps.lastIndex
}

@Composable
private fun CalculationResultRow(
    result: String,
    fontSize: String = "medium",
    hapticFeedbackLevel: String = "standard",
    modifier: Modifier = Modifier
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    val fontScale = getFontScale(fontSize)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                LauncherHaptic.performMedium(haptic, hapticFeedbackLevel)
                clipboardManager.setText(AnnotatedString(result))
                isCopied = true
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "= $result",
            style = typography.drawerItem.copy(
                fontSize = (20 * fontScale).sp,
                lineHeight = (28 * fontScale).sp,
                fontWeight = FontWeight.Medium
            ),
            color = colors.accent,
            modifier = Modifier.weight(1f)
        )
        if (isCopied) {
            Text(
                text = "Copied",
                style = typography.settingsDescription,
                color = colors.textSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

