package com.nounish.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nounish.launcher.data.AppRepository
import com.nounish.launcher.data.BinanceWebSocketClient
import com.nounish.launcher.data.CachedCoinPrice
import com.nounish.launcher.data.CoinTicker
import com.nounish.launcher.data.LauncherSettings
import com.nounish.launcher.data.NetworkHelper
import com.nounish.launcher.data.PriceRepository
import com.nounish.launcher.data.PriceUiState
import com.nounish.launcher.data.RateLimitException
import com.nounish.launcher.data.SettingsStore
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.service.PackageChangeReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class CoinTickerUi(
    val symbol: String,
    val price: String,
    val changeText: String,
    val changePercent: Double,
    val highPrice: String = "",
    val lowPrice: String = "",
    val isStale: Boolean = false,
    val ageMarker: String = "",
    val sparkline: String = ""
)

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    val appRepository = AppRepository(application, viewModelScope)
    private val priceRepository = PriceRepository()

    val settings: StateFlow<LauncherSettings> = settingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherSettings()
    )

    val allApps: StateFlow<List<AppItem>> = combine(appRepository.apps, settings) { apps, pref ->
        apps.map { app ->
            val custom = pref.customAppLabels[app.packageName]
            if (custom != null) app.copy(customLabel = custom) else app
        }.sortedBy { it.displayLabel.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Process-death safe search query state
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow("search_query", "")

    // Cold start optimization:
    // Emits immediately on frame 1 using DataStore pinnedAppLabels cache,
    // then seamlessly updates with full AppItem once PackageManager loads.
    val pinnedApps: StateFlow<List<AppItem>> = combine(allApps, settings) { apps, pref ->
        val appMap = apps.associateBy { it.packageName }
        pref.pinnedApps.map { pkg ->
            val fullApp = appMap[pkg]
            if (fullApp != null) {
                fullApp
            } else {
                val label = pref.pinnedAppLabels[pkg] ?: pkg
                val custom = pref.customAppLabels[pkg]
                AppItem(
                    label = label,
                    packageName = pkg,
                    customLabel = custom
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredApps: StateFlow<List<AppItem>> = combine(allApps, settings, searchQuery) { apps, pref, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            apps.filter { !pref.hiddenApps.contains(it.packageName) }
        } else {
            apps.filter { app ->
                val matches = app.displayLabel.contains(trimmed, ignoreCase = true) ||
                              app.label.contains(trimmed, ignoreCase = true) ||
                              app.packageName.contains(trimmed, ignoreCase = true)
                if (pref.hiddenApps.contains(app.packageName)) {
                    app.displayLabel.equals(trimmed, ignoreCase = true) ||
                    app.label.equals(trimmed, ignoreCase = true) ||
                    app.packageName.equals(trimmed, ignoreCase = true)
                } else {
                    matches
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _priceState = MutableStateFlow(PriceUiState())
    val priceState: StateFlow<PriceUiState> = _priceState.asStateFlow()

    val homeCryptoList: StateFlow<List<CoinTickerUi>> = settings.map { pref ->
        val now = System.currentTimeMillis()
        pref.cryptoSymbols.map { sym ->
            val cleanSym = sym.trim().uppercase(Locale.US)
            val cached = pref.cachedCryptoPrices[cleanSym]
            val timestamp = cached?.timestamp ?: 0L
            val isStale = timestamp <= 0L || (now - timestamp) > 60_000L
            val ageMarker = if (cached != null && isStale) PriceRepository.formatAgeMarker(timestamp) else ""
            CoinTickerUi(
                symbol = cleanSym,
                price = cached?.price ?: "--",
                changeText = cached?.changeText ?: "",
                changePercent = cached?.changePercent ?: 0.0,
                highPrice = cached?.highPrice ?: "",
                lowPrice = cached?.lowPrice ?: "",
                isStale = isStale,
                ageMarker = ageMarker,
                sparkline = cached?.sparkline ?: ""
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var priceJob: Job? = null
    private var webSocketClient: BinanceWebSocketClient? = null
    private var consecutiveFailures = 0
    private var lastKlineFetchTime = 0L

    init {
        // Refresh apps ONLY on package changes (installed / uninstalled / updated)
        PackageChangeReceiver.onPackageChangedListener = {
            appRepository.reloadApps()
        }

        // Cache pinned app labels to DataStore whenever apps load
        viewModelScope.launch(Dispatchers.IO) {
            allApps.collect { appsList ->
                if (appsList.isNotEmpty()) {
                    val pref = settings.value
                    val currentLabels = pref.pinnedAppLabels
                    val newLabels = currentLabels.toMutableMap()
                    var changed = false
                    appsList.forEach { app ->
                        if (pref.pinnedApps.contains(app.packageName)) {
                            if (newLabels[app.packageName] != app.label) {
                                newLabels[app.packageName] = app.label
                                changed = true
                            }
                        }
                    }
                    if (changed) {
                        settingsStore.updatePinnedAppLabels(newLabels)
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        savedStateHandle["search_query"] = newQuery
    }

    fun startPriceRefresh(isHomeActive: Boolean = false) {
        val currentSettings = settings.value
        if (!currentSettings.cryptoPriceEnabled) {
            stopAllPriceUpdates()
            _priceState.value = PriceUiState()
            return
        }

        updatePriceDisplayFromCache(currentSettings)

        if (isHomeActive && currentSettings.useWebSocketOnHome && currentSettings.showHomeCrypto) {
            stopPolling()
            startHomeWebSocket(currentSettings)
        } else {
            stopHomeWebSocket()
            startPolling()
        }
    }

    fun stopAllPriceUpdates() {
        stopPolling()
        stopHomeWebSocket()
    }

    private fun startPolling() {
        if (priceJob?.isActive == true) return

        priceJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchFreshPrices()

                val baseInterval = NetworkHelper.getBasePollIntervalMs(getApplication())
                val backoffMultiplier = (1 shl consecutiveFailures.coerceIn(0, 4))
                val delayTime = (baseInterval * backoffMultiplier).coerceIn(15_000L, 240_000L)

                delay(delayTime)
            }
        }
    }

    private fun stopPolling() {
        priceJob?.cancel()
        priceJob = null
    }

    private fun startHomeWebSocket(currentSettings: LauncherSettings) {
        if (webSocketClient != null) return

        val pairs = currentSettings.cryptoSymbols.map { sym ->
            priceRepository.resolvePair(sym, currentSettings.cryptoQuoteCurrency)
        }.distinct()

        val client = BinanceWebSocketClient(
            scope = viewModelScope,
            pairs = pairs,
            onTickerUpdate = { sym, price, changePercent, changeText, high, low ->
                handleWebSocketTicker(sym, price, changePercent, changeText, high, low)
            },
            onError = { _ ->
                stopHomeWebSocket()
                consecutiveFailures++
                startPolling()
            }
        )

        webSocketClient = client
        client.start()

        viewModelScope.launch(Dispatchers.IO) {
            fetchSparklinesOnly(currentSettings)
        }
    }

    private fun stopHomeWebSocket() {
        webSocketClient?.close()
        webSocketClient = null
    }

    private fun handleWebSocketTicker(
        tradingPair: String,
        price: String,
        changePercent: Double,
        changeText: String,
        high: String,
        low: String
    ) {
        val currentSettings = settings.value
        val now = System.currentTimeMillis()
        val updatedCache = currentSettings.cachedCryptoPrices.toMutableMap()

        currentSettings.cryptoSymbols.forEach { origSym ->
            val resolved = priceRepository.resolvePair(origSym, currentSettings.cryptoQuoteCurrency)
            val cleanOrig = origSym.trim().uppercase(Locale.US)
            if (resolved.equals(tradingPair, ignoreCase = true)) {
                val existingSpark = updatedCache[cleanOrig]?.sparkline ?: ""
                updatedCache[cleanOrig] = CachedCoinPrice(
                    price = price,
                    changeText = changeText,
                    changePercent = changePercent,
                    highPrice = high,
                    lowPrice = low,
                    timestamp = now,
                    sparkline = existingSpark
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.updateCachedPrices(updatedCache)
        }

        consecutiveFailures = 0

        val symbols = currentSettings.cryptoSymbols
        val index = currentSettings.currentCryptoIndex.coerceIn(0, (symbols.size - 1).coerceAtLeast(0))
        val currentSymbol = (symbols.getOrNull(index) ?: symbols.firstOrNull() ?: "BTC").trim().uppercase(Locale.US)
        val resolvedCurrent = priceRepository.resolvePair(currentSymbol, currentSettings.cryptoQuoteCurrency)

        if (resolvedCurrent.equals(tradingPair, ignoreCase = true)) {
            _priceState.value = PriceUiState(
                coinSymbol = currentSymbol,
                priceText = price,
                changeText = changeText,
                changePercent = changePercent,
                isStale = false,
                ageMarker = "",
                isLoading = false,
                sparkline = updatedCache[currentSymbol]?.sparkline ?: ""
            )
        }
    }

    private suspend fun fetchSparklinesOnly(currentSettings: LauncherSettings) {
        val now = System.currentTimeMillis()
        if (lastKlineFetchTime > 0L && now - lastKlineFetchTime < 300_000L) return
        lastKlineFetchTime = now

        val updatedCache = currentSettings.cachedCryptoPrices.toMutableMap()
        var hasChanges = false

        for (sym in currentSettings.cryptoSymbols) {
            val clean = sym.trim().uppercase(Locale.US)
            val pair = priceRepository.resolvePair(clean, currentSettings.cryptoQuoteCurrency)
            val klines = priceRepository.fetchKlines(pair)
            if (klines.isNotEmpty()) {
                val spark = PriceRepository.generateSparkline(klines)
                val curr = updatedCache[clean]
                if (curr != null) {
                    updatedCache[clean] = curr.copy(sparkline = spark)
                    hasChanges = true
                }
            }
        }

        if (hasChanges) {
            settingsStore.updateCachedPrices(updatedCache)
        }
    }

    private suspend fun fetchFreshPrices() {
        val currentSettings = settings.value
        if (!currentSettings.cryptoPriceEnabled || currentSettings.cryptoSymbols.isEmpty()) return

        _priceState.value = _priceState.value.copy(isLoading = true)

        val now = System.currentTimeMillis()
        val shouldFetchKlines = (lastKlineFetchTime == 0L || now - lastKlineFetchTime > 300_000L)
        if (shouldFetchKlines) {
            lastKlineFetchTime = now
        }

        val cachedSparks = currentSettings.cachedCryptoPrices.mapValues { it.value.sparkline }

        val result = priceRepository.fetchPrices(
            symbols = currentSettings.cryptoSymbols,
            quoteCurrency = currentSettings.cryptoQuoteCurrency,
            cachedSparklines = cachedSparks,
            fetchSparklines = shouldFetchKlines
        )

        result.onSuccess { newPrices ->
            consecutiveFailures = 0

            val updatedCache = currentSettings.cachedCryptoPrices.toMutableMap()
            newPrices.forEach { (sym, ticker) ->
                updatedCache[sym] = CachedCoinPrice(
                    price = ticker.price,
                    changeText = ticker.formattedChange,
                    changePercent = ticker.changePercent,
                    highPrice = ticker.highPrice,
                    lowPrice = ticker.lowPrice,
                    timestamp = ticker.timestamp,
                    sparkline = ticker.sparkline
                )
            }
            settingsStore.updateCachedPrices(updatedCache)

            val symbols = currentSettings.cryptoSymbols
            val index = currentSettings.currentCryptoIndex.coerceIn(0, (symbols.size - 1).coerceAtLeast(0))
            val currentSymbol = (symbols.getOrNull(index) ?: symbols.firstOrNull() ?: "BTC").trim().uppercase(Locale.US)
            val ticker = newPrices[currentSymbol] ?: updatedCache[currentSymbol]?.let {
                CoinTicker(it.price, it.changePercent, it.changeText, it.highPrice, it.lowPrice, it.timestamp, it.sparkline)
            }

            _priceState.value = PriceUiState(
                coinSymbol = currentSymbol,
                priceText = ticker?.price ?: "--",
                changeText = ticker?.formattedChange ?: "",
                changePercent = ticker?.changePercent ?: 0.0,
                isStale = false,
                ageMarker = "",
                isLoading = false,
                sparkline = ticker?.sparkline ?: ""
            )
        }.onFailure { err ->
            consecutiveFailures++
            if (err is RateLimitException) {
                consecutiveFailures = maxOf(consecutiveFailures, 2)
            }

            val symbols = currentSettings.cryptoSymbols
            val index = currentSettings.currentCryptoIndex.coerceIn(0, (symbols.size - 1).coerceAtLeast(0))
            val currentSymbol = (symbols.getOrNull(index) ?: symbols.firstOrNull() ?: "BTC").trim().uppercase(Locale.US)
            val cached = currentSettings.cachedCryptoPrices[currentSymbol]
            val ageMarker = PriceRepository.formatAgeMarker(cached?.timestamp ?: 0L)

            _priceState.value = PriceUiState(
                coinSymbol = currentSymbol,
                priceText = cached?.price ?: "--",
                changeText = cached?.changeText ?: "",
                changePercent = cached?.changePercent ?: 0.0,
                isStale = true,
                ageMarker = ageMarker,
                isLoading = false,
                sparkline = cached?.sparkline ?: ""
            )
        }
    }

    private fun updatePriceDisplayFromCache(currentSettings: LauncherSettings) {
        val symbols = currentSettings.cryptoSymbols
        if (symbols.isEmpty()) return

        val index = currentSettings.currentCryptoIndex.coerceIn(0, symbols.size - 1)
        val symbol = symbols[index].trim().uppercase(Locale.US)
        val cached = currentSettings.cachedCryptoPrices[symbol]

        if (cached != null) {
            val now = System.currentTimeMillis()
            val isStale = (now - cached.timestamp) > 60_000L
            val ageMarker = if (isStale) PriceRepository.formatAgeMarker(cached.timestamp) else ""
            _priceState.value = PriceUiState(
                coinSymbol = symbol,
                priceText = cached.price,
                changeText = cached.changeText,
                changePercent = cached.changePercent,
                isStale = isStale,
                ageMarker = ageMarker,
                isLoading = false,
                sparkline = cached.sparkline
            )
        }
    }

    fun cycleNextCryptoCoin() {
        val currentSettings = settings.value
        val symbols = currentSettings.cryptoSymbols
        if (symbols.isEmpty()) return

        val nextIndex = (currentSettings.currentCryptoIndex + 1) % symbols.size
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setCurrentCryptoIndex(nextIndex)
            val symbol = symbols[nextIndex].trim().uppercase(Locale.US)
            val cached = currentSettings.cachedCryptoPrices[symbol]
            val now = System.currentTimeMillis()
            val isStale = (cached == null) || (now - cached.timestamp > 60_000L)
            val ageMarker = if (cached != null && isStale) PriceRepository.formatAgeMarker(cached.timestamp) else ""
            _priceState.value = _priceState.value.copy(
                coinSymbol = symbol,
                priceText = cached?.price ?: "--",
                changeText = cached?.changeText ?: "",
                changePercent = cached?.changePercent ?: 0.0,
                isStale = isStale,
                ageMarker = ageMarker,
                sparkline = cached?.sparkline ?: ""
            )
            fetchFreshPrices()
        }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = allApps.value.find { it.packageName == packageName }
            val label = app?.label ?: packageName
            settingsStore.pinApp(packageName, label)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.unpinApp(packageName)
        }
    }

    fun reorderPinnedApps(list: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.reorderPinnedApps(list)
        }
    }

    fun toggleHideApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isHidden = settings.value.hiddenApps.contains(packageName)
            settingsStore.setAppHidden(packageName, !isHidden)
        }
    }

    fun setAppCustomLabel(packageName: String, customLabel: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setAppCustomLabel(packageName, customLabel)
        }
    }

    fun exportSettingsJson(): String {
        return settingsStore.exportSettingsJson(settings.value)
    }

    suspend fun importSettingsJson(json: String): Boolean {
        return settingsStore.importSettingsJson(json)
    }

    fun setSwipeLeft(packageName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setSwipeLeftPackage(packageName)
        }
    }

    fun setSwipeRight(packageName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setSwipeRightPackage(packageName)
        }
    }

    fun setDoubleTapLock(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setDoubleTapLock(enabled)
        }
    }

    fun setShowClock(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowClock(enabled)
        }
    }

    fun setShowDate(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowDate(enabled)
        }
    }

    fun setShowHomeCrypto(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowHomeCrypto(enabled)
        }
    }

    fun setAutoLaunchSingleMatch(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setAutoLaunchSingleMatch(enabled)
        }
    }

    fun setCryptoPriceEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setCryptoPriceEnabled(enabled)
            if (enabled) {
                startPriceRefresh(false)
            } else {
                stopAllPriceUpdates()
                _priceState.value = PriceUiState()
            }
        }
    }

    fun setCryptoSymbols(symbols: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setCryptoSymbols(symbols)
            fetchFreshPrices()
        }
    }

    fun setCryptoQuoteCurrency(quote: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setCryptoQuoteCurrency(quote)
            fetchFreshPrices()
        }
    }

    fun setUseWebSocketOnHome(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setUseWebSocketOnHome(enabled)
        }
    }

    fun setLightTheme(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setLightTheme(enabled)
        }
    }

    fun setUseMonospace(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setUseMonospace(enabled)
        }
    }

    fun setAlphabetIndexOnRight(onRight: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setAlphabetIndexOnRight(onRight)
        }
    }

    fun setIndexAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setIndexAnimationEnabled(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        PackageChangeReceiver.onPackageChangedListener = null
        stopAllPriceUpdates()
    }
}
