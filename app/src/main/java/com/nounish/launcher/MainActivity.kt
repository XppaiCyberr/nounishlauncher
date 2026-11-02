package com.nounish.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.nounish.launcher.service.LockScreenService
import com.nounish.launcher.ui.DrawerScreen
import com.nounish.launcher.ui.HomeScreen
import com.nounish.launcher.ui.SettingsScreen
import com.nounish.launcher.ui.theme.NounishLauncherTheme
import kotlinx.coroutines.launch

enum class Screen {
    Home,
    Drawer,
    Settings
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Track active screen (restored on process recreation)
    private val currentScreenState = mutableStateOf(Screen.Home)

    private val exportSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { stream ->
                    val json = viewModel.exportSettingsJson()
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "Settings exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to export settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    val json = contentResolver.openInputStream(it)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (json != null && viewModel.importSettingsJson(json)) {
                        Toast.makeText(this@MainActivity, "Settings restored successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to parse settings backup file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error importing settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Process-death recovery
        if (savedInstanceState != null) {
            val screenName = savedInstanceState.getString("KEY_CURRENT_SCREEN")
            if (screenName != null) {
                try {
                    currentScreenState.value = Screen.valueOf(screenName)
                } catch (_: Exception) {}
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            val allApps by viewModel.allApps.collectAsState()
            val pinnedApps by viewModel.pinnedApps.collectAsState()
            val filteredApps by viewModel.filteredApps.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val priceState by viewModel.priceState.collectAsState()
            val homeCryptoList by viewModel.homeCryptoList.collectAsState()

            var currentScreen by remember { currentScreenState }

            // Manage price refresh lifecycle: runs while drawer or home crypto is active
            LaunchedEffect(currentScreen, settings.cryptoPriceEnabled, settings.showHomeCrypto, settings.useWebSocketOnHome) {
                when {
                    !settings.cryptoPriceEnabled -> {
                        viewModel.stopAllPriceUpdates()
                    }
                    currentScreen == Screen.Home && settings.showHomeCrypto -> {
                        viewModel.startPriceRefresh(isHomeActive = true)
                    }
                    currentScreen == Screen.Drawer -> {
                        viewModel.startPriceRefresh(isHomeActive = false)
                    }
                    else -> {
                        viewModel.stopAllPriceUpdates()
                    }
                }
            }

            NounishLauncherTheme(
                isLightTheme = settings.isLightTheme,
                useMonospace = settings.useMonospace,
                fontSize = settings.fontSize
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
                                    clockTimeFormat = settings.clockTimeFormat,
                                    showClockSeconds = settings.showClockSeconds,
                                    dateFormatPattern = settings.dateFormatPattern,
                                    showCrypto = settings.cryptoPriceEnabled && settings.showHomeCrypto,
                                    cryptoCoins = homeCryptoList,
                                    showAppIcons = settings.showAppIcons,
                                    monochromeIcons = settings.monochromeIcons,
                                    fontSize = settings.fontSize,
                                    onAppClick = { app ->
                                        viewModel.appRepository.launchApp(app)
                                    },
                                    onAppInfoClick = { app ->
                                        viewModel.appRepository.openAppInfo(app)
                                    },
                                    onUnpinApp = { app ->
                                        viewModel.unpinApp(app)
                                    },
                                    onRenameApp = { app, newLabel ->
                                        viewModel.setAppCustomLabel(app, newLabel)
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
                                    alphabetIndexOnRight = settings.alphabetIndexOnRight,
                                    enableIndexAnimation = settings.enableIndexAnimation,
                                    showAppIcons = settings.showAppIcons,
                                    monochromeIcons = settings.monochromeIcons,
                                    fontSize = settings.fontSize,
                                    onSearchQueryChange = { query ->
                                        viewModel.onSearchQueryChanged(query)
                                    },
                                    onAppClick = { app ->
                                        viewModel.appRepository.launchApp(app)
                                        viewModel.onSearchQueryChanged("")
                                        currentScreen = Screen.Home
                                    },
                                    onTogglePinApp = { app ->
                                        val isPinned = settings.pinnedApps.any { it == app.key || it == app.packageName }
                                        if (isPinned) {
                                            viewModel.unpinApp(app)
                                        } else {
                                            if (!viewModel.pinApp(app)) {
                                                Toast.makeText(this@MainActivity, "Maximum 8 pinned apps reached. Unpin an app first.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onToggleHideApp = { app ->
                                        viewModel.toggleHideApp(app)
                                    },
                                    onRenameApp = { app, newLabel ->
                                        viewModel.setAppCustomLabel(app, newLabel)
                                    },
                                    onAppInfoClick = { app ->
                                        viewModel.appRepository.openAppInfo(app)
                                    },
                                    onUninstallClick = { app ->
                                        viewModel.appRepository.uninstallApp(app)
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
                                    onUpdateClockTimeFormat = { viewModel.setClockTimeFormat(it) },
                                    onToggleClockSeconds = { viewModel.setShowClockSeconds(it) },
                                    onToggleShowDate = { viewModel.setShowDate(it) },
                                    onUpdateDateFormatPattern = { viewModel.setDateFormatPattern(it) },
                                    onToggleAutoLaunch = { viewModel.setAutoLaunchSingleMatch(it) },
                                    onSelectSwipeLeftApp = { viewModel.setSwipeLeft(it) },
                                    onSelectSwipeRightApp = { viewModel.setSwipeRight(it) },
                                    onToggleDoubleTapLock = { viewModel.setDoubleTapLock(it) },
                                    onToggleCryptoEnabled = { viewModel.setCryptoPriceEnabled(it) },
                                    onToggleShowHomeCrypto = { viewModel.setShowHomeCrypto(it) },
                                    onToggleWebSocketOnHome = { viewModel.setUseWebSocketOnHome(it) },
                                    onUpdateCryptoSymbols = { viewModel.setCryptoSymbols(it) },
                                    onUpdateCryptoQuote = { viewModel.setCryptoQuoteCurrency(it) },
                                    onToggleLightTheme = { viewModel.setLightTheme(it) },
                                    onToggleMonospace = { viewModel.setUseMonospace(it) },
                                    onUpdateFontSize = { viewModel.setFontSize(it) },
                                    onToggleShowAppIcons = { viewModel.setShowAppIcons(it) },
                                    onToggleMonochromeIcons = { viewModel.setMonochromeIcons(it) },
                                    onToggleIndexOnRight = { viewModel.setAlphabetIndexOnRight(it) },
                                    onToggleIndexAnimation = { viewModel.setIndexAnimationEnabled(it) },
                                    onReorderPinnedApps = { viewModel.reorderPinnedApps(it) },
                                    onUnpinApp = { id ->
                                        viewModel.unpinAppById(id)
                                    },
                                    onUnhideApp = { viewModel.unhideAppById(it) },
                                    onExportSettings = {
                                        exportSettingsLauncher.launch("nounish_launcher_backup.json")
                                    },
                                    onImportSettings = {
                                        importSettingsLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    },
                                    onCloseSettings = { currentScreen = Screen.Home }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("KEY_CURRENT_SCREEN", currentScreenState.value.name)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed while launcher is running: return home
        currentScreenState.value = Screen.Home
        viewModel.onSearchQueryChanged("")
    }

    override fun onResume() {
        super.onResume()
        // App list is kept in memory and refreshed strictly via PackageChangeReceiver / LauncherApps.Callback.
        val settings = viewModel.settings.value
        if (settings.cryptoPriceEnabled) {
            val isHome = currentScreenState.value == Screen.Home
            viewModel.startPriceRefresh(isHomeActive = isHome)
        }
    }

    override fun onPause() {
        super.onPause()
        // Immediately terminate WebSocket and polling when launcher is paused or asleep
        viewModel.stopAllPriceUpdates()
    }
}
