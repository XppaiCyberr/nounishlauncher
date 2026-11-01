package com.nounish.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nounish.launcher.service.LockScreenService
import com.nounish.launcher.ui.DrawerScreen
import com.nounish.launcher.ui.HomeScreen
import com.nounish.launcher.ui.SettingsScreen
import com.nounish.launcher.ui.theme.NounishLauncherTheme

enum class Screen {
    Home,
    Drawer,
    Settings
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Track active screen
    private val currentScreenState = mutableStateOf(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val allApps by viewModel.allApps.collectAsState()
            val pinnedApps by viewModel.pinnedApps.collectAsState()
            val filteredApps by viewModel.filteredApps.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val priceState by viewModel.priceState.collectAsState()

            var currentScreen by remember { currentScreenState }

            // Manage price refresh lifecycle: only runs while drawer is active
            LaunchedEffect(currentScreen, settings.cryptoPriceEnabled) {
                if (currentScreen == Screen.Drawer && settings.cryptoPriceEnabled) {
                    viewModel.startPriceRefresh()
                } else {
                    viewModel.stopPriceRefresh()
                }
            }

            NounishLauncherTheme(
                isLightTheme = settings.isLightTheme,
                useMonospace = settings.useMonospace
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Back button handling
                    BackHandler(enabled = currentScreen != Screen.Home) {
                        if (currentScreen == Screen.Drawer && searchQuery.isNotEmpty()) {
                            viewModel.onSearchQueryChanged("")
                        } else {
                            currentScreen = Screen.Home
                        }
                    }

                    // Fast fade animation under 150 ms
                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 120),
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> {
                                HomeScreen(
                                    pinnedApps = pinnedApps,
                                    showClock = settings.showClock,
                                    showDate = settings.showDate,
                                    onAppClick = { app ->
                                        viewModel.appRepository.launchApp(app)
                                    },
                                    onAppInfoClick = { app ->
                                        viewModel.appRepository.openAppInfo(app.packageName)
                                    },
                                    onUnpinApp = { app ->
                                        viewModel.unpinApp(app.packageName)
                                    },
                                    onOpenDrawer = {
                                        viewModel.onSearchQueryChanged("")
                                        currentScreen = Screen.Drawer
                                    },
                                    onOpenSettings = {
                                        currentScreen = Screen.Settings
                                    },
                                    onSwipeLeft = {
                                        settings.swipeLeftPackage?.let { pkg ->
                                            viewModel.appRepository.launchPackage(pkg)
                                        }
                                    },
                                    onSwipeRight = {
                                        settings.swipeRightPackage?.let { pkg ->
                                            viewModel.appRepository.launchPackage(pkg)
                                        }
                                    },
                                    onDoubleTap = {
                                        if (settings.doubleTapLock) {
                                            LockScreenService.lockScreen(this@MainActivity)
                                        }
                                    }
                                )
                            }

                            Screen.Drawer -> {
                                DrawerScreen(
                                    apps = filteredApps,
                                    searchQuery = searchQuery,
                                    pinnedPackages = settings.pinnedApps,
                                    hiddenPackages = settings.hiddenApps,
                                    priceState = priceState,
                                    cryptoPriceEnabled = settings.cryptoPriceEnabled,
                                    autoLaunchSingleMatch = settings.autoLaunchSingleMatch,
                                    onSearchQueryChange = { query ->
                                        viewModel.onSearchQueryChanged(query)
                                    },
                                    onAppClick = { app ->
                                        viewModel.appRepository.launchApp(app)
                                        viewModel.onSearchQueryChanged("")
                                        currentScreen = Screen.Home
                                    },
                                    onTogglePinApp = { app ->
                                        if (settings.pinnedApps.contains(app.packageName)) {
                                            viewModel.unpinApp(app.packageName)
                                        } else {
                                            viewModel.pinApp(app.packageName)
                                        }
                                    },
                                    onToggleHideApp = { app ->
                                        viewModel.toggleHideApp(app.packageName)
                                    },
                                    onAppInfoClick = { app ->
                                        viewModel.appRepository.openAppInfo(app.packageName)
                                    },
                                    onUninstallClick = { app ->
                                        viewModel.appRepository.uninstallApp(app.packageName)
                                    },
                                    onCycleCrypto = {
                                        viewModel.cycleNextCryptoCoin()
                                    },
                                    onCloseDrawer = {
                                        viewModel.onSearchQueryChanged("")
                                        currentScreen = Screen.Home
                                    }
                                )
                            }

                            Screen.Settings -> {
                                SettingsScreen(
                                    settings = settings,
                                    allApps = allApps,
                                    onToggleShowClock = { viewModel.setShowClock(it) },
                                    onToggleShowDate = { viewModel.setShowDate(it) },
                                    onToggleAutoLaunch = { viewModel.setAutoLaunchSingleMatch(it) },
                                    onSelectSwipeLeftApp = { viewModel.setSwipeLeft(it) },
                                    onSelectSwipeRightApp = { viewModel.setSwipeRight(it) },
                                    onToggleDoubleTapLock = { viewModel.setDoubleTapLock(it) },
                                    onToggleCryptoEnabled = { viewModel.setCryptoPriceEnabled(it) },
                                    onUpdateCryptoSymbols = { viewModel.setCryptoSymbols(it) },
                                    onUpdateCryptoQuote = { viewModel.setCryptoQuoteCurrency(it) },
                                    onToggleLightTheme = { viewModel.setLightTheme(it) },
                                    onToggleMonospace = { viewModel.setUseMonospace(it) },
                                    onUnpinApp = { viewModel.unpinApp(it) },
                                    onUnhideApp = { viewModel.toggleHideApp(it) },
                                    onCloseSettings = { currentScreen = Screen.Home }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed while launcher is running: return home
        currentScreenState.value = Screen.Home
        viewModel.onSearchQueryChanged("")
    }

    override fun onResume() {
        super.onResume()
        // Refresh installed apps whenever launcher returns to foreground
        viewModel.appRepository.reloadApps()
    }
}
