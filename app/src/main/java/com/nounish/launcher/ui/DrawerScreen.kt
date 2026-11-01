package com.nounish.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nounish.launcher.data.PriceUiState
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
import com.nounish.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DrawerScreen(
    apps: List<AppItem>,
    searchQuery: String,
    pinnedPackages: List<String>,
    hiddenPackages: Set<String>,
    priceState: PriceUiState,
    cryptoPriceEnabled: Boolean,
    autoLaunchSingleMatch: Boolean,
    alphabetIndexOnRight: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onTogglePinApp: (AppItem) -> Unit,
    onToggleHideApp: (AppItem) -> Unit,
    onAppInfoClick: (AppItem) -> Unit,
    onUninstallClick: (AppItem) -> Unit,
    onCycleCrypto: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }

    val alphabet = remember { ('A'..'Z').toList() + listOf('#') }
    val activeLetters = remember(apps) {
        apps.mapNotNull {
            val first = it.label.firstOrNull()?.uppercaseChar()
            when {
                first == null -> null
                first in 'A'..'Z' -> first
                else -> '#'
            }
        }.toSet()
    }

    // Niagara-style wave animation state
    var isInteractingWithIndex by remember { mutableStateOf(false) }
    var selectedTargetIndex by remember { mutableIntStateOf(-1) }

    val interactionProgress by animateFloatAsState(
        targetValue = if (isInteractingWithIndex) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 320f
        ),
        label = "niagaraWaveProgress"
    )

    // Auto-launch if exactly one match remains (optional setting)
    LaunchedEffect(searchQuery, apps, autoLaunchSingleMatch) {
        if (autoLaunchSingleMatch && searchQuery.trim().length >= 2 && apps.size == 1) {
            onAppClick(apps.first())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .launcherGestures(
                onSwipeDown = onCloseDrawer
            )
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar: Search field on the left, Crypto price on the right
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
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                if (apps.isNotEmpty()) {
                                    onAppClick(apps.first())
                                }
                            }
                        )
                    )
                }

                // Crypto Price Label with 24h percentage change & red/green color
                if (cryptoPriceEnabled && priceState.isNotEmpty) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onCycleCrypto)
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "${priceState.coinSymbol} ${priceState.priceText}",
                            style = typography.priceLabel,
                            color = colors.textSecondary.copy(alpha = if (priceState.isStale) 0.45f else 0.9f)
                        )
                        if (priceState.changeText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val changeColor = if (priceState.changePercent >= 0.0) CryptoGreen else CryptoRed
                            Text(
                                text = priceState.changeText,
                                style = typography.priceLabel,
                                color = changeColor.copy(alpha = if (priceState.isStale) 0.45f else 1.0f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content: Alphabet Index + Niagara-style Animated App List
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No apps found" else "Loading...",
                        style = typography.drawerItem,
                        color = colors.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT Alphabet Index (when placed on left and not searching)
                    if (!alphabetIndexOnRight && searchQuery.isEmpty()) {
                        AlphabetIndex(
                            letters = alphabet,
                            activeLetters = activeLetters,
                            onInteractionStart = {
                                isInteractingWithIndex = true
                            },
                            onInteractionEnd = {
                                isInteractingWithIndex = false
                            },
                            onLetterSelected = { char, fraction ->
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
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // App list with Niagara wave arc animation
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(apps, key = { _, item -> item.packageName + (item.activityName ?: "") }) { index, app ->
                            val isHidden = hiddenPackages.contains(app.packageName)
                            val isTarget = selectedTargetIndex == index

                            // Calculate wave curve factor based on distance from selectedTargetIndex
                            val diff = if (selectedTargetIndex >= 0) abs(index - selectedTargetIndex) else 99
                            val waveFactor = when (diff) {
                                0 -> 1.0f
                                1 -> 0.72f
                                2 -> 0.44f
                                3 -> 0.20f
                                4 -> 0.06f
                                else -> 0.0f
                            }

                            val maxShiftPx = with(density) {
                                if (alphabetIndexOnRight) 90.dp.toPx() else 45.dp.toPx()
                            }
                            val baseShiftPx = with(density) {
                                if (alphabetIndexOnRight) 16.dp.toPx() else 0.dp.toPx()
                            }

                            val currentShift = (baseShiftPx + maxShiftPx * waveFactor) * interactionProgress
                            val currentScale = 1f + (0.16f * waveFactor * interactionProgress)

                            Text(
                                text = if (isHidden) "${app.label} (hidden)" else app.label,
                                style = typography.drawerItem,
                                fontWeight = if (isTarget && interactionProgress > 0.4f) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isTarget && interactionProgress > 0.2f -> colors.textPrimary
                                    interactionProgress > 0.2f && waveFactor > 0.1f -> colors.textPrimary.copy(alpha = 0.85f)
                                    isHidden -> colors.textSecondary.copy(alpha = 0.6f)
                                    interactionProgress > 0.2f -> colors.textSecondary.copy(alpha = 0.35f)
                                    else -> colors.textPrimary
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationX = currentShift
                                        scaleX = currentScale
                                        scaleY = currentScale
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
                            )
                        }
                    }

                    // RIGHT Alphabet Index (when placed on right and not searching)
                    if (alphabetIndexOnRight && searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.width(16.dp))
                        AlphabetIndex(
                            letters = alphabet,
                            activeLetters = activeLetters,
                            onInteractionStart = {
                                isInteractingWithIndex = true
                            },
                            onInteractionEnd = {
                                isInteractingWithIndex = false
                            },
                            onLetterSelected = { char, fraction ->
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
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Long press action dialog for Drawer app item
    selectedAppForMenu?.let { app ->
        val isPinned = pinnedPackages.contains(app.packageName)
        val isHidden = hiddenPackages.contains(app.packageName)

        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            containerColor = colors.surface,
            title = {
                Text(
                    text = app.label,
                    style = typography.settingsHeading,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}

@Composable
private fun AlphabetIndex(
    letters: List<Char>,
    activeLetters: Set<Char>,
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
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        isHovered -> colors.textPrimary
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
            val first = it.label.firstOrNull()?.uppercaseChar() ?: ' '
            first !in 'A'..'Z'
        }
        return if (idx >= 0) idx else 0
    }

    // Exact match first
    val exactIdx = apps.indexOfFirst {
        it.label.firstOrNull()?.uppercaseChar() == target
    }
    if (exactIdx >= 0) return exactIdx

    // Find first app starting with a letter after target
    val nextIdx = apps.indexOfFirst {
        val first = it.label.firstOrNull()?.uppercaseChar() ?: ' '
        first in 'A'..'Z' && first > target
    }
    if (nextIdx >= 0) return nextIdx

    return apps.lastIndex
}
