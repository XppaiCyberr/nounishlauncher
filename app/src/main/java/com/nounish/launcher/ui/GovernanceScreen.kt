package com.nounish.launcher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nounish.launcher.data.NounsCandidateUi
import com.nounish.launcher.data.NounsProposalUi
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
import com.nounish.launcher.ui.theme.LauncherTheme
import com.nounish.launcher.util.LauncherHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun GovernanceScreen(
    proposals: List<NounsProposalUi>,
    candidates: List<NounsCandidateUi>,
    isRefreshing: Boolean = false,
    isLoadingOlderProposals: Boolean = false,
    isLoadingOlderCandidates: Boolean = false,
    hasMoreProposals: Boolean = true,
    hasMoreCandidates: Boolean = true,
    hapticFeedbackLevel: String = "standard",
    onRefresh: () -> Unit,
    onLoadOlderProposals: () -> Unit = {},
    onLoadOlderCandidates: () -> Unit = {},
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableStateOf("proposals") } // "proposals" or "candidates"
    var proposalFilter by remember { mutableStateOf("All") } // "All", "Active", "Succeeded", "Defeated", "Executed"

    val proposalsListState = rememberLazyListState()
    val candidatesListState = rememberLazyListState()

    val shouldLoadMoreProposals by remember(proposalsListState, isLoadingOlderProposals, hasMoreProposals) {
        derivedStateOf {
            val total = proposalsListState.layoutInfo.totalItemsCount
            val lastVisible = proposalsListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 2 && !isLoadingOlderProposals && hasMoreProposals
        }
    }

    LaunchedEffect(shouldLoadMoreProposals) {
        if (shouldLoadMoreProposals) {
            onLoadOlderProposals()
        }
    }

    val shouldLoadMoreCandidates by remember(candidatesListState, isLoadingOlderCandidates, hasMoreCandidates) {
        derivedStateOf {
            val total = candidatesListState.layoutInfo.totalItemsCount
            val lastVisible = candidatesListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 2 && !isLoadingOlderCandidates && hasMoreCandidates
        }
    }

    LaunchedEffect(shouldLoadMoreCandidates) {
        if (shouldLoadMoreCandidates) {
            onLoadOlderCandidates()
        }
    }

    val refreshInfiniteTransition = rememberInfiniteTransition(label = "GovernanceRefreshBreathing")
    val refreshBreathAlpha by refreshInfiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "refreshBreathAlpha"
    )

    var refreshTypingIndex by remember { mutableIntStateOf(3) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (isActive) {
                for (i in 0..3) {
                    refreshTypingIndex = i
                    delay(120L)
                }
                delay(300L)
            }
        } else {
            refreshTypingIndex = 3
        }
    }

    val refreshGlasses = when (refreshTypingIndex) {
        0 -> "⌐"
        1 -> "⌐◨"
        2 -> "⌐◨-"
        else -> "⌐◨-◨"
    }

    BackHandler(onBack = onBack)

    val filteredProposals = remember(proposals, proposalFilter) {
        when (proposalFilter) {
            "All" -> proposals
            "Active" -> proposals.filter { it.statusLabel.equals("Active", ignoreCase = true) }
            "Succeeded" -> proposals.filter {
                it.statusLabel.equals("Succeeded", ignoreCase = true) || it.statusLabel.equals("Queued", ignoreCase = true)
            }
            "Defeated" -> proposals.filter {
                it.statusLabel.equals("Defeated", ignoreCase = true) ||
                    it.statusLabel.equals("Canceled", ignoreCase = true) ||
                    it.statusLabel.equals("Vetoed", ignoreCase = true)
            }
            "Executed" -> proposals.filter { it.statusLabel.equals("Executed", ignoreCase = true) }
            else -> proposals
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .launcherGestures(
                onSwipeRight = onBack,
                onSwipeDown = {
                    LauncherHaptic.performStrong(haptic, hapticFeedbackLevel)
                    onRefresh()
                },
                onDoubleTap = onDoubleTap,
                onLongPress = onOpenSettings
            )
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Bar with Back Arrow, Page Dots, and Refresh Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to Home
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                            onBack()
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Text(
                        text = "<- Home",
                        style = typography.settingsDescription,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Page Indicator Dots: Page 1 (Home) & Page 2 (Governance)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Page 1 (Home) - Clickable to return
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = colors.textSecondary.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable {
                                LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                                onBack()
                            }
                    )
                    // Page 2 (Governance - Active)
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = colors.accent,
                                shape = CircleShape
                            )
                    )
                }

                // Refresh Status or Action with Noggles
                if (isRefreshing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Syncing...",
                            style = typography.priceLabel,
                            color = colors.accent,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = refreshGlasses,
                            style = typography.priceLabel,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                alpha = refreshBreathAlpha
                            }
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                                onRefresh()
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Refresh",
                            style = typography.priceLabel,
                            color = colors.textSecondary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "⌐◨-◨",
                            style = typography.priceLabel,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title and Subtitle
            Text(
                text = "NOUNS GOVERNANCE",
                style = typography.clock,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "On-chain DAO Proposals & Candidates · Ethereum Mainnet",
                style = typography.priceLabel,
                color = colors.textSecondary.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector: Proposals vs Candidates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isProposals = (selectedTab == "proposals")
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isProposals) colors.accent.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isProposals) 1.dp else 0.5.dp,
                            color = if (isProposals) colors.accent else colors.border.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                            selectedTab = "proposals"
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Proposals (${proposals.size})",
                        style = typography.priceLabel,
                        fontWeight = if (isProposals) FontWeight.Bold else FontWeight.Normal,
                        color = if (isProposals) colors.textPrimary else colors.textSecondary
                    )
                }

                val isCandidates = (selectedTab == "candidates")
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isCandidates) colors.accent.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isCandidates) 1.dp else 0.5.dp,
                            color = if (isCandidates) colors.accent else colors.border.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                            selectedTab = "candidates"
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Candidates (${candidates.size})",
                        style = typography.priceLabel,
                        fontWeight = if (isCandidates) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCandidates) colors.textPrimary else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (when on Proposals tab)
            if (selectedTab == "proposals" && proposals.isNotEmpty()) {
                val filters = listOf("All", "Active", "Succeeded", "Defeated", "Executed")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { f ->
                        val isSelected = (proposalFilter == f)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) colors.textPrimary.copy(alpha = 0.12f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = if (isSelected) colors.textPrimary.copy(alpha = 0.4f) else colors.border.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                                    proposalFilter = f
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = f,
                                style = typography.clock,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main Content Area
            if (selectedTab == "proposals") {
                if (filteredProposals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (proposals.isEmpty()) "Loading on-chain proposals..." else "No proposals match filter '$proposalFilter'",
                                style = typography.drawerItem,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Swipe down to refresh from Ethereum Mainnet",
                                style = typography.priceLabel,
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = proposalsListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProposals, key = { "prop_${it.id}" }) { prop ->
                            GovernanceProposalCard(
                                prop = prop,
                                context = context,
                                hapticFeedbackLevel = hapticFeedbackLevel
                            )
                        }

                        if (isLoadingOlderProposals) {
                            item(key = "loading_proposals_indicator") {
                                NogglesLoadingIndicator(
                                    label = "Loading older proposals",
                                    accentColor = colors.accent,
                                    textColor = colors.textSecondary
                                )
                            }
                        } else if (!hasMoreProposals && filteredProposals.isNotEmpty()) {
                            item(key = "end_proposals_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All on-chain proposals loaded · ⌐◨-◨",
                                        style = typography.priceLabel,
                                        color = colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                // Candidates Tab
                if (candidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No candidate proposals found",
                                style = typography.drawerItem,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Swipe down to refresh from Ethereum Mainnet",
                                style = typography.priceLabel,
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = candidatesListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(candidates, key = { "cand_${it.slug}" }) { cand ->
                            GovernanceCandidateCard(
                                cand = cand,
                                context = context
                            )
                        }

                        if (isLoadingOlderCandidates) {
                            item(key = "loading_candidates_indicator") {
                                NogglesLoadingIndicator(
                                    label = "Loading older candidates",
                                    accentColor = colors.accent,
                                    textColor = colors.textSecondary
                                )
                            }
                        } else if (!hasMoreCandidates && candidates.isNotEmpty()) {
                            item(key = "end_candidates_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All candidate proposals loaded · ⌐◨-◨",
                                        style = typography.priceLabel,
                                        color = colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NogglesLoadingIndicator(
    label: String,
    accentColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nogglesLoading")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nogglesBreathScale"
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nogglesBreathAlpha"
    )

    var typingIndex by remember { mutableIntStateOf(3) }
    var dotCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            for (i in 0..3) {
                typingIndex = i
                delay(110L)
            }
            for (d in 0..3) {
                dotCount = (dotCount + 1) % 4
                delay(220L)
            }
        }
    }

    val glassesText = when (typingIndex) {
        0 -> "⌐"
        1 -> "⌐◨"
        2 -> "⌐◨-"
        else -> "⌐◨-◨"
    }

    val dots = ".".repeat(dotCount)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = glassesText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.graphicsLayer {
                    scaleX = breathScale
                    scaleY = breathScale
                    alpha = breathAlpha
                }
            )
            Text(
                text = "$label$dots",
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.75f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun GovernanceProposalCard(
    prop: NounsProposalUi,
    context: Context,
    hapticFeedbackLevel: String
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val haptic = LocalHapticFeedback.current

    val statusColor = when (prop.statusLabel.lowercase(Locale.US)) {
        "active" -> CryptoGreen
        "executed" -> colors.textSecondary.copy(alpha = 0.85f)
        "defeated", "canceled", "vetoed" -> CryptoRed.copy(alpha = 0.85f)
        "succeeded", "queued" -> colors.textPrimary
        else -> colors.textSecondary.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.surface.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 0.5.dp,
                color = colors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header: ID + Status + Age
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${prop.id}",
                        style = typography.settingsItem,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = statusColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = statusColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = prop.statusLabel,
                            style = typography.clock,
                            fontWeight = if (prop.statusLabel.equals("Active", ignoreCase = true)) FontWeight.Bold else FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }

                if (prop.isStale && prop.ageMarker.isNotEmpty()) {
                    Text(
                        text = prop.ageMarker,
                        style = typography.clock,
                        color = colors.textSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            // Title
            Text(
                text = prop.title,
                style = typography.settingsItem,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Proposer (Reverse ENS)
            val shortProposer = if (prop.proposerFormatted.isNotBlank()) {
                prop.proposerFormatted
            } else if (prop.proposer.length >= 10) {
                "${prop.proposer.take(6)}...${prop.proposer.takeLast(4)}"
            } else prop.proposer

            if (shortProposer.isNotBlank()) {
                Text(
                    text = "Proposer: $shortProposer",
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.65f)
                )
            }

            // Voting Stats & Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "For: ${prop.forVotes}",
                    style = typography.clock,
                    fontWeight = FontWeight.SemiBold,
                    color = CryptoGreen
                )
                Text(
                    text = "Against: ${prop.againstVotes}",
                    style = typography.clock,
                    fontWeight = FontWeight.SemiBold,
                    color = CryptoRed
                )
                Text(
                    text = "Abstain: ${prop.abstainVotes}",
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )
                Text(
                    text = "Quorum: ${prop.quorum}",
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )
            }

            // Visual Voting Bar
            val totalVotes = (prop.forVotes + prop.againstVotes).coerceAtLeast(0)
            if (totalVotes > 0) {
                val forRatio = prop.forVotes.toFloat() / totalVotes.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CryptoRed.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(forRatio)
                            .height(3.5.dp)
                            .background(CryptoGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Direct Links to Vote on Nouns Clients
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vote on nouns.wtf ->",
                    style = typography.clock,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                    modifier = Modifier.clickable {
                        LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                        openUrl(context, "https://nouns.wtf/vote/${prop.id}")
                    }
                )

                Text(
                    text = "noun.wtf ->",
                    style = typography.clock,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.clickable {
                        LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                        openUrl(context, "https://noun.wtf/noun/${prop.id}")
                    }
                )
            }
        }
    }
}

@Composable
private fun GovernanceCandidateCard(
    cand: NounsCandidateUi,
    context: Context
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.surface.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 0.5.dp,
                color = colors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Candidate",
                    style = typography.settingsItem,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                if (cand.isStale && cand.ageMarker.isNotEmpty()) {
                    Text(
                        text = cand.ageMarker,
                        style = typography.clock,
                        color = colors.textSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = cand.title,
                style = typography.settingsItem,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            val shortProposer = if (cand.proposerFormatted.isNotBlank()) {
                cand.proposerFormatted
            } else if (cand.proposer.length >= 10) {
                "${cand.proposer.take(6)}...${cand.proposer.takeLast(4)}"
            } else cand.proposer

            if (shortProposer.isNotBlank()) {
                Text(
                    text = "Proposer: $shortProposer",
                    style = typography.clock,
                    color = colors.textSecondary.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "View Candidate on nouns.wtf ->",
                style = typography.clock,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                modifier = Modifier.clickable {
                    openUrl(context, "https://nouns.wtf/candidates/${cand.slug}")
                }
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
