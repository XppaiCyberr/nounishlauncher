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
import com.nounish.launcher.data.LilDigestEventUi
import com.nounish.launcher.ui.theme.CryptoGreen
import com.nounish.launcher.ui.theme.CryptoRed
import com.nounish.launcher.ui.theme.LauncherTheme
import com.nounish.launcher.util.LauncherHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun LilDigestScreen(
    events: List<LilDigestEventUi>,
    isRefreshing: Boolean = false,
    isLoadingOlder: Boolean = false,
    hasMore: Boolean = true,
    hapticFeedbackLevel: String = "standard",
    onRefresh: () -> Unit,
    onLoadOlder: () -> Unit = {},
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Votes", "Proposals", "Delegations"
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(listState, isLoadingOlder, hasMore) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 2 && !isLoadingOlder && hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadOlder()
        }
    }

    val refreshInfiniteTransition = rememberInfiniteTransition(label = "DigestRefreshBreathing")
    val refreshBreathAlpha by refreshInfiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "digestRefreshBreathAlpha"
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

    val filteredEvents = remember(events, selectedFilter) {
        when (selectedFilter) {
            "All" -> events
            "Votes" -> events.filter { it.kind.equals("vote", ignoreCase = true) }
            "Proposals" -> events.filter {
                it.kind.equals("proposal-created", ignoreCase = true) ||
                    it.kind.equals("voting-started", ignoreCase = true) ||
                    it.kind.equals("closing-soon", ignoreCase = true)
            }
            "Delegations" -> events.filter { it.kind.equals("delegate", ignoreCase = true) }
            else -> events
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .launcherGestures(
                onSwipeLeft = onBack,
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

            // Navigation Bar with Back Button, Title, and Refresh Action
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

                // Digest indicator dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = colors.accent,
                            shape = CircleShape
                        )
                )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "LIL NOUNS DIGEST",
                        style = typography.clock,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Live Activity Timeline · lilnouns.wtf/digest",
                        style = typography.priceLabel,
                        color = colors.textSecondary.copy(alpha = 0.65f)
                    )
                }

                Text(
                    text = "lilnouns.wtf ↗",
                    style = typography.clock,
                    color = colors.accent,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                            openDigestUrl(context, "https://lilnouns.wtf/digest")
                        }
                        .padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            val filters = listOf("All", "Votes", "Proposals", "Delegations")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { f ->
                    val isSelected = (selectedFilter == f)
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
                                selectedFilter = f
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = f,
                            style = typography.clock,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Events Timeline List
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (events.isEmpty()) "No Lil Nouns digest events loaded" else "No events match \"$selectedFilter\"",
                            style = typography.drawerItem,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Swipe down to refresh from Lil Nouns Digest",
                            style = typography.priceLabel,
                            color = colors.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEvents, key = { "digest_${it.id}" }) { event ->
                        LilDigestEventCard(
                            event = event,
                            context = context,
                            hapticFeedbackLevel = hapticFeedbackLevel
                        )
                    }

                    if (isLoadingOlder) {
                        item(key = "loading_older_digest") {
                            DigestNogglesLoadingIndicator(
                                label = "Loading older digest events",
                                accentColor = colors.accent,
                                textColor = colors.textSecondary
                            )
                        }
                    } else if (!hasMore && events.isNotEmpty()) {
                        item(key = "end_digest_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All Lil Nouns digest events loaded · ⌐◨-◨",
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

@Composable
private fun LilDigestEventCard(
    event: LilDigestEventUi,
    context: Context,
    hapticFeedbackLevel: String
) {
    val colors = LauncherTheme.colors
    val typography = LauncherTheme.typography
    val haptic = LocalHapticFeedback.current

    val (badgeText, badgeColor) = when (event.kind.lowercase(Locale.US)) {
        "vote" -> {
            val supportText = when (event.support) {
                1 -> "VOTED FOR"
                0 -> "VOTED AGAINST"
                2 -> "ABSTAINED"
                else -> "VOTE"
            }
            val color = when (event.support) {
                1 -> CryptoGreen
                0 -> CryptoRed
                else -> colors.textSecondary
            }
            supportText to color
        }
        "proposal-created" -> "NEW PROPOSAL" to CryptoGreen
        "voting-started" -> "VOTING ACTIVE" to colors.accent
        "closing-soon" -> "CLOSING SOON" to CryptoRed.copy(alpha = 0.85f)
        "delegate" -> {
            if (event.undelegated) "UNDELEGATED" to CryptoRed
            else "DELEGATION" to colors.accent
        }
        "auction-bid", "auction-created", "auction-settled" -> "AUCTION" to colors.accent
        else -> event.kind.uppercase(Locale.US) to colors.textSecondary
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
            .padding(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            // Header: Badge + Weight + Relative Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = badgeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = badgeColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = typography.clock,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    if (event.voteWeight != null && event.voteWeight > 0) {
                        Text(
                            text = "${event.voteWeight} votes",
                            style = typography.clock,
                            fontWeight = FontWeight.SemiBold,
                            color = if (event.support == 1) CryptoGreen else if (event.support == 0) CryptoRed else colors.textPrimary
                        )
                    } else if (event.nounIdsCount > 0) {
                        val nounTokenText = if (event.nounIds.isNotEmpty()) {
                            if (event.nounIds.size <= 3) {
                                "Lil #${event.nounIds.joinToString(", #")}"
                            } else {
                                "Lil #${event.nounIds.take(2).joinToString(", #")} +${event.nounIds.size - 2} more"
                            }
                        } else {
                            "${event.nounIdsCount} Lil ${if (event.nounIdsCount == 1) "Noun" else "Nouns"}"
                        }
                        Text(
                            text = nounTokenText,
                            style = typography.clock,
                            color = colors.textSecondary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (event.timeAgoFormatted.isNotEmpty()) {
                    Text(
                        text = event.timeAgoFormatted,
                        style = typography.clock,
                        color = colors.textSecondary.copy(alpha = 0.55f)
                    )
                }
            }

            // Proposal Title (if available)
            if (event.proposalTitle.isNotBlank()) {
                val fullTitle = if (event.proposalId.isNotBlank() && !event.proposalTitle.startsWith("#")) {
                    "#${event.proposalId} ${event.proposalTitle}"
                } else event.proposalTitle

                Text(
                    text = fullTitle,
                    style = typography.settingsItem,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sender / Recipient / Voter / Proposer / Delegator details
            when (event.kind.lowercase(Locale.US)) {
                "delegate" -> {
                    val fromActor = if (event.actorFormatted.isNotBlank()) event.actorFormatted
                        else if (event.actor.length >= 10) "${event.actor.take(6)}...${event.actor.takeLast(4)}"
                        else event.actor

                    val toCounterpart = if (event.counterpartFormatted.isNotBlank()) event.counterpartFormatted
                        else if (event.counterpart.length >= 10) "${event.counterpart.take(6)}...${event.counterpart.takeLast(4)}"
                        else event.counterpart

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (event.undelegated) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "From: ",
                                    style = typography.clock,
                                    color = colors.textSecondary.copy(alpha = 0.55f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = fromActor,
                                    style = typography.clock,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "-> Undelegated",
                                    style = typography.clock,
                                    color = CryptoRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (toCounterpart.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Previous delegate: ",
                                        style = typography.clock,
                                        color = colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = toCounterpart,
                                        style = typography.clock,
                                        color = colors.textSecondary.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "From: ",
                                    style = typography.clock,
                                    color = colors.textSecondary.copy(alpha = 0.55f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = fromActor,
                                    style = typography.clock,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (toCounterpart.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "To: ",
                                        style = typography.clock,
                                        color = colors.textSecondary.copy(alpha = 0.55f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = toCounterpart,
                                        style = typography.clock,
                                        color = colors.accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                "vote" -> {
                    val voterText = if (event.actorFormatted.isNotBlank()) event.actorFormatted
                        else if (event.actor.length >= 10) "${event.actor.take(6)}...${event.actor.takeLast(4)}"
                        else event.actor
                    if (voterText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Voter: ",
                                style = typography.clock,
                                color = colors.textSecondary.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = voterText,
                                style = typography.clock,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                "proposal-created" -> {
                    val proposerText = if (event.actorFormatted.isNotBlank()) event.actorFormatted
                        else if (event.actor.length >= 10) "${event.actor.take(6)}...${event.actor.takeLast(4)}"
                        else event.actor
                    if (proposerText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Proposer: ",
                                style = typography.clock,
                                color = colors.textSecondary.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = proposerText,
                                style = typography.clock,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                "transfer" -> {
                    val fromActor = if (event.actorFormatted.isNotBlank()) event.actorFormatted
                        else if (event.actor.length >= 10) "${event.actor.take(6)}...${event.actor.takeLast(4)}"
                        else event.actor
                    val toCounterpart = if (event.counterpartFormatted.isNotBlank()) event.counterpartFormatted
                        else if (event.counterpart.length >= 10) "${event.counterpart.take(6)}...${event.counterpart.takeLast(4)}"
                        else event.counterpart
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Transfer: $fromActor -> $toCounterpart",
                            style = typography.clock,
                            color = colors.textSecondary.copy(alpha = 0.75f)
                        )
                    }
                }
                "sale" -> {
                    val buyerText = if (event.actorFormatted.isNotBlank()) event.actorFormatted
                        else if (event.actor.length >= 10) "${event.actor.take(6)}...${event.actor.takeLast(4)}"
                        else event.actor
                    if (buyerText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Buyer: ",
                                style = typography.clock,
                                color = colors.textSecondary.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = buyerText,
                                style = typography.clock,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                else -> {
                    if (event.actor.isNotBlank()) {
                        val userText = if (event.actorFormatted.isNotBlank()) event.actorFormatted
                            else if (event.actor.length >= 10) "${event.actor.take(6)}...${event.actor.takeLast(4)}"
                            else event.actor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Account: ",
                                style = typography.clock,
                                color = colors.textSecondary.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = userText,
                                style = typography.clock,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action links
            val targetUrl = when {
                event.href.isNotBlank() && event.href.startsWith("http") -> event.href
                event.href.isNotBlank() && event.href.startsWith("/") -> "https://lilnouns.wtf${event.href}"
                event.proposalId.isNotBlank() -> "https://lilnouns.wtf/vote/${event.proposalId}"
                else -> "https://lilnouns.wtf/digest"
            }

            val actionLabel = when (event.kind) {
                "vote", "voting-started", "closing-soon" -> "Vote on lilnouns.wtf ->"
                "proposal-created" -> "View proposal on lilnouns.wtf ->"
                "delegate" -> "View profile on lilnouns.wtf ->"
                "sale" -> "View auction on lilnouns.wtf ->"
                else -> "lilnouns.wtf ->"
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = actionLabel,
                style = typography.clock,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                modifier = Modifier.clickable {
                    LauncherHaptic.performTick(haptic, hapticFeedbackLevel)
                    openDigestUrl(context, targetUrl)
                }
            )
        }
    }
}

@Composable
private fun DigestNogglesLoadingIndicator(
    label: String,
    accentColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "digestNogglesLoading")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "digestNogglesBreathScale"
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "digestNogglesBreathAlpha"
    )

    var typingStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            for (step in 0..3) {
                typingStep = step
                delay(300L)
            }
        }
    }

    val dots = when (typingStep) {
        1 -> "."
        2 -> ".."
        3 -> "..."
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⌐◨-◨",
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

private fun openDigestUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
