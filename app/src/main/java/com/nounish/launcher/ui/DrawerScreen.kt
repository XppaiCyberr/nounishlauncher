package com.nounish.launcher.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nounish.launcher.data.PriceUiState
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.ui.theme.LauncherTheme

@Composable
fun DrawerScreen(
    apps: List<AppItem>,
    searchQuery: String,
    pinnedPackages: List<String>,
    hiddenPackages: Set<String>,
    priceState: PriceUiState,
    cryptoPriceEnabled: Boolean,
    autoLaunchSingleMatch: Boolean,
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

    var selectedAppForMenu by remember { mutableStateOf<AppItem?>(null) }

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
            .padding(horizontal = 24.dp)
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

                // Crypto Price Label (top right corner, section 7)
                if (cryptoPriceEnabled && priceState.displayText.isNotEmpty()) {
                    Text(
                        text = priceState.displayText,
                        style = typography.priceLabel,
                        color = colors.textSecondary.copy(alpha = if (priceState.isStale) 0.45f else 0.9f),
                        modifier = Modifier
                            .clickable(onClick = onCycleCrypto)
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App list (LazyColumn, plain Text items, no icons for speed and battery)
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps, key = { it.packageName + (it.activityName ?: "") }) { app ->
                        val isHidden = hiddenPackages.contains(app.packageName)

                        Text(
                            text = if (isHidden) "${app.label} (hidden)" else app.label,
                            style = typography.drawerItem,
                            color = if (isHidden) colors.textSecondary.copy(alpha = 0.6f) else colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onAppClick(app) },
                                    onLongClick = { selectedAppForMenu = app }
                                )
                                .padding(vertical = 4.dp)
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
