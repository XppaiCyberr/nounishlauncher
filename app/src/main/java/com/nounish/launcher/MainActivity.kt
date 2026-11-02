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
import androidx.compose.runtime.DisposableEffect
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

    // Track active screen
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
            LaunchedEffect(currentScreen, settings.cryptoPriceEnabled, settings.showHomeCrypto) {
                val shouldPoll = settings.cryptoPriceEnabled &&
                    (currentScreen == Screen.Drawer || (currentScreen == Screen.Home && settings.showHomeCrypto))
                if (shouldPoll) {
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
                                    showCrypto = settings.cryptoPriceEnabled && settings.showHomeCrypto,
                                    cryptoCoins = homeCryptoList,
                                    onAppClick = { app ->
                                        viewModel.appRepository.launchApp(app)
                                    },
                                    onAppInfoClick = { app ->
                                        viewModel.appRepository.openAppInfo(app.packageName)
                                    },
                                    onUnpinApp = { app ->
                                        viewModel.unpinApp(app.packageName)
                                    },
                                    onRenameApp = { app, newLabel ->
                                        viewModel.setAppCustomLabel(app.packageName, newLabel)
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
                                    onRenameApp = { app, newLabel ->
                                        viewModel.setAppCustomLabel(app.packageName, newLabel)
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
                                    onToggleShowHomeCrypto = { viewModel.setShowHomeCrypto(it) },
                                    onUpdateCryptoSymbols = { viewModel.setCryptoSymbols(it) },
                                    onUpdateCryptoQuote = { viewModel.setCryptoQuoteCurrency(it) },
                                    onToggleLightTheme = { viewModel.setLightTheme(it) },
                                    onToggleMonospace = { viewModel.setUseMonospace(it) },
                                    onToggleIndexOnRight = { viewModel.setAlphabetIndexOnRight(it) },
                                    onToggleIndexAnimation = { viewModel.setIndexAnimationEnabled(it) },
                                    onUnpinApp = { viewModel.unpinApp(it) },
                                    onUnhideApp = { viewModel.toggleHideApp(it) },
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
