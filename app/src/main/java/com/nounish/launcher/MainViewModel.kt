package com.nounish.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nounish.launcher.data.AppRepository
import com.nounish.launcher.data.BinanceWebSocketClient
import com.nounish.launcher.data.CachedCoinPrice
import com.nounish.launcher.data.CachedIdxQuote
import com.nounish.launcher.data.CoinTicker
import com.nounish.launcher.data.IdxItemUi
import com.nounish.launcher.data.IdxRepository
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

    // Sorted case- and accent-insensitively with '#' at the end
    val allApps: StateFlow<List<AppItem>> = combine(appRepository.apps, settings) { apps, pref ->
        apps.map { app ->
            val custom = pref.customAppLabels[app.key] ?: pref.customAppLabels[app.packageName]
            if (custom != null) app.copy(customLabel = custom) else app
        }.sortedWith(AppItem.COMPARATOR)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Process-death safe search query state
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow("search_query", "")

    // Cold start optimization + Clean removal of uninstalled apps
    val pinnedApps: StateFlow<List<AppItem>> = combine(allApps, settings) { apps, pref ->
        pref.pinnedApps.mapNotNull { pinnedId ->
            val fullApp = findMatchingApp(apps, pinnedId)
            if (fullApp != null) {
                fullApp
            } else if (apps.isEmpty()) {
                // Immediate frame 1 fallback from DataStore cache
                val label = pref.pinnedAppLabels[pinnedId]
                    ?: pref.pinnedAppLabels[pinnedId.substringBefore("/").substringBefore("#")]
                    ?: pinnedId
                val custom = pref.customAppLabels[pinnedId]
                    ?: pref.customAppLabels[pinnedId.substringBefore("/").substringBefore("#")]
                val isWork = pinnedId.contains("#work")
                AppItem(
                    label = label,
                    packageName = pinnedId.substringBefore("/").substringBefore("#"),
                    activityName = pinnedId.substringAfter("/").substringBefore("#").takeIf { it.isNotBlank() },
                    customLabel = custom,
                    isWorkProfile = isWork
                )
            } else {
                // App was uninstalled: drop dead entry cleanly!
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredApps: StateFlow<List<AppItem>> = combine(allApps, settings, searchQuery) { apps, pref, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            apps.filter { !isAppHidden(it, pref.hiddenApps) }
        } else {
            apps.filter { app ->
                val matches = app.displayLabel.contains(trimmed, ignoreCase = true) ||
                              app.label.contains(trimmed, ignoreCase = true) ||
                              app.packageName.contains(trimmed, ignoreCase = true)
                if (isAppHidden(app, pref.hiddenApps)) {
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

    private val _usdRateState = MutableStateFlow(PriceUiState())
    val usdRateState: StateFlow<PriceUiState> = _usdRateState.asStateFlow()

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

    val idxRepository = IdxRepository()

    val homeIdxList: StateFlow<List<IdxItemUi>> = settings.map { pref ->
        val now = System.currentTimeMillis()
        val symbolsToDisplay = when (pref.idxDisplayMode.lowercase(Locale.US)) {
            "indices" -> listOf("IHSG", "LQ45")
            else -> pref.idxSymbols
        }
        val cachedMap = pref.cachedIdxQuotes
        val timestamp = pref.cachedIdxTimestamp
        val isStale = timestamp <= 0L || (now - timestamp) > 300_000L
        val ageMarker = if (isStale && timestamp > 0L) PriceRepository.formatAgeMarker(timestamp) else ""

        if (pref.idxDisplayMode.lowercase(Locale.US) in listOf("gainers", "losers") && cachedMap.isNotEmpty()) {
            cachedMap.values.take(5).map { quote ->
                IdxItemUi(
                    symbol = quote.symbol,
                    name = quote.name,
                    priceText = IdxRepository.formatPrice(quote.symbol, quote.price),
                    changeText = IdxRepository.formatPercent(quote.changePercent),
                    changePercent = quote.changePercent,
                    highPrice = if (quote.high > 0) IdxRepository.formatPrice(quote.symbol, quote.high) else "",
                    lowPrice = if (quote.low > 0) IdxRepository.formatPrice(quote.symbol, quote.low) else "",
                    volumeText = IdxRepository.formatVolume(quote.volume),
                    isStale = isStale,
                    ageMarker = ageMarker
                )
            }
        } else {
            symbolsToDisplay.map { sym ->
                val clean = sym.trim().uppercase(Locale.US).removeSuffix(".JK")
                val quote = cachedMap[clean]
                IdxItemUi(
                    symbol = clean,
                    name = quote?.name ?: clean,
                    priceText = if (quote != null) IdxRepository.formatPrice(clean, quote.price) else "--",
                    changeText = if (quote != null) IdxRepository.formatPercent(quote.changePercent) else "",
                    changePercent = quote?.changePercent ?: 0.0,
                    highPrice = if (quote != null && quote.high > 0) IdxRepository.formatPrice(clean, quote.high) else "",
                    lowPrice = if (quote != null && quote.low > 0) IdxRepository.formatPrice(clean, quote.low) else "",
                    volumeText = if (quote != null) IdxRepository.formatVolume(quote.volume) else "",
                    isStale = isStale,
                    ageMarker = ageMarker
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var priceJob: Job? = null
    private var wsSecondaryPollJob: Job? = null
    private var webSocketClient: BinanceWebSocketClient? = null
    private var consecutiveFailures = 0
    private var lastKlineFetchTime = 0L

    init {
        // Refresh apps on package change broadcasts
        PackageChangeReceiver.onPackageChangedListener = {
            com.nounish.launcher.data.IconCache.clear()
            appRepository.reloadApps()
        }

        // Cache pinned app labels and prune dead entries when apps load
        viewModelScope.launch(Dispatchers.IO) {
            allApps.collect { appsList ->
                if (appsList.isNotEmpty()) {
                    val pref = settings.value

                    // If brand new install with 0 pinned apps, auto-seed default 4 essential apps once
                    if (!pref.hasInitializedPinned && pref.pinnedApps.isEmpty()) {
                        val defaults = findDefaultPinnedApps(appsList)
                        if (defaults.isNotEmpty()) {
                            settingsStore.reorderPinnedApps(defaults.map { it.key })
                            val labelsMap = defaults.associate { it.key to it.label }
                            settingsStore.updatePinnedAppLabels(labelsMap)
                        }
                        settingsStore.setHasInitializedPinned(true)
                    }

                    // Prune uninstalled apps from pinned list
                    val currentPinned = pref.pinnedApps
                    val validPinned = currentPinned.filter { pinnedId ->
                        findMatchingApp(appsList, pinnedId) != null
                    }
                    if (validPinned.size != currentPinned.size) {
                        settingsStore.reorderPinnedApps(validPinned)
                    }

                    // Cache labels for pinned apps
                    val currentLabels = pref.pinnedAppLabels
                    val newLabels = currentLabels.toMutableMap()
                    var changed = false
                    appsList.forEach { app ->
                        if (validPinned.contains(app.key) || validPinned.contains(app.packageName)) {
                            if (newLabels[app.key] != app.label) {
                                newLabels[app.key] = app.label
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

    private fun findDefaultPinnedApps(apps: List<AppItem>): List<AppItem> {
        if (apps.size <= 4) return apps
        val defaults = mutableListOf<AppItem>()
        val dialer = apps.find { it.packageName.contains("dialer") || it.packageName.contains("phone") }
        if (dialer != null) defaults.add(dialer)
        val sms = apps.find { it.packageName.contains("messaging") || it.packageName.contains("mms") || it.packageName.contains("sms") }
        if (sms != null && !defaults.contains(sms)) defaults.add(sms)
        val browser = apps.find { it.packageName.contains("chrome") || it.packageName.contains("browser") }
        if (browser != null && !defaults.contains(browser)) defaults.add(browser)
        val camera = apps.find { it.packageName.contains("camera") }
        if (camera != null && !defaults.contains(camera)) defaults.add(camera)

        for (app in apps) {
            if (defaults.size >= 4) break
            if (!defaults.contains(app)) {
                defaults.add(app)
            }
        }
        return defaults.take(4)
    }

    private fun findMatchingApp(apps: List<AppItem>, pinnedId: String): AppItem? {
        val exact = apps.find { it.key == pinnedId }
        if (exact != null) return exact

        val byPkg = apps.find { it.packageName == pinnedId }
        if (byPkg != null) return byPkg

        val cleanPkg = pinnedId.substringBefore("/").substringBefore("#")
        return apps.find { it.packageName == cleanPkg }
    }

    private fun isAppHidden(app: AppItem, hiddenSet: Set<String>): Boolean {
        return hiddenSet.contains(app.key) || hiddenSet.contains(app.packageName)
    }

    fun onSearchQueryChanged(newQuery: String) {
        savedStateHandle["search_query"] = newQuery
    }

    fun startPriceRefresh(isHomeActive: Boolean = false) {
        val currentSettings = settings.value
        if (!currentSettings.cryptoPriceEnabled) {
            stopAllPriceUpdates()
            _priceState.value = PriceUiState()
            _usdRateState.value = PriceUiState()
            return
        }

        updatePriceDisplayFromCache(currentSettings)
        updateUsdRateDisplayFromCache(currentSettings)

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

        // Also poll USD rate and IDX prices periodically while WebSocket handles real-time crypto
        wsSecondaryPollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                fetchUsdRate()
                fetchIdxPrices()
                delay(60_000L)
            }
        }
    }

    private fun stopHomeWebSocket() {
        webSocketClient?.close()
        webSocketClient = null
        wsSecondaryPollJob?.cancel()
        wsSecondaryPollJob = null
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
        if (!currentSettings.cryptoPriceEnabled) return

        fetchUsdRate()
        fetchIdxPrices()

        if (currentSettings.cryptoSymbols.isEmpty()) return

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

    private fun updateUsdRateDisplayFromCache(currentSettings: LauncherSettings) {
        val cachedRate = currentSettings.cachedUsdRate
        if (cachedRate.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val isStale = (now - currentSettings.cachedUsdTimestamp) > 60_000L
            val ageMarker = if (isStale) PriceRepository.formatAgeMarker(currentSettings.cachedUsdTimestamp) else ""
            _usdRateState.value = PriceUiState(
                coinSymbol = "USD",
                priceText = cachedRate,
                changeText = currentSettings.cachedUsdChange,
                changePercent = currentSettings.cachedUsdChangePercent,
                isStale = isStale,
                ageMarker = ageMarker,
                isLoading = false
            )
        }
    }

    private suspend fun fetchUsdRate() {
        val currentSettings = settings.value
        if (!currentSettings.cryptoPriceEnabled) return

        val result = priceRepository.fetchUsdIdrRate()
        result.onSuccess { usdRate ->
            _usdRateState.value = PriceUiState(
                coinSymbol = "USD",
                priceText = usdRate.formattedRate,
                changeText = usdRate.formattedChange,
                changePercent = usdRate.changePercent,
                isStale = false,
                ageMarker = "",
                isLoading = false
            )
            settingsStore.updateCachedUsdRate(
                rate = usdRate.formattedRate,
                change = usdRate.formattedChange,
                changePercent = usdRate.changePercent,
                timestamp = usdRate.timestamp
            )
        }.onFailure {
            val cachedRate = currentSettings.cachedUsdRate
            if (cachedRate.isNotEmpty()) {
                val ageMarker = PriceRepository.formatAgeMarker(currentSettings.cachedUsdTimestamp)
                _usdRateState.value = PriceUiState(
                    coinSymbol = "USD",
                    priceText = cachedRate,
                    changeText = currentSettings.cachedUsdChange,
                    changePercent = currentSettings.cachedUsdChangePercent,
                    isStale = true,
                    ageMarker = ageMarker,
                    isLoading = false
                )
            }
        }
    }

    fun refreshUsdRate() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchUsdRate()
        }
    }

    private suspend fun fetchIdxPrices() {
        val currentSettings = settings.value
        if (!currentSettings.showHomeIdx) return

        val result = idxRepository.fetchIdxData(
            displayMode = currentSettings.idxDisplayMode,
            symbols = currentSettings.idxSymbols
        )

        val quotes = result.getOrNull()
        if (!quotes.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val updated = currentSettings.cachedIdxQuotes.toMutableMap()
            quotes.forEach { q ->
                updated[q.symbol.uppercase(Locale.US)] = CachedIdxQuote(
                    symbol = q.symbol,
                    name = q.name,
                    price = q.price,
                    change = q.change,
                    changePercent = q.changePercent,
                    volume = q.volume,
                    high = q.high,
                    low = q.low,
                    timestamp = now
                )
            }
            settingsStore.updateCachedIdxQuotes(updated, now)
        }
    }

    fun refreshIdxPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchIdxPrices()
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

    fun pinApp(item: AppItem): Boolean {
        val alreadyPinned = settings.value.pinnedApps.contains(item.key) ||
                            settings.value.pinnedApps.contains(item.packageName)
        if (!alreadyPinned && settings.value.pinnedApps.size >= 8) {
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.pinApp(item.key, item.label)
        }
        return true
    }

    fun unpinApp(item: AppItem) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.unpinApp(item.key)
            settingsStore.unpinApp(item.packageName)
        }
    }

    fun unpinAppById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.unpinApp(id)
            val cleanPkg = id.substringBefore("/").substringBefore("#")
            settingsStore.unpinApp(cleanPkg)
        }
    }

    fun reorderPinnedApps(list: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.reorderPinnedApps(list)
        }
    }

    fun toggleHideApp(item: AppItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val isHidden = isAppHidden(item, settings.value.hiddenApps)
            settingsStore.setAppHidden(item.key, !isHidden)
            if (item.activityName == null || !item.isWorkProfile) {
                settingsStore.setAppHidden(item.packageName, !isHidden)
            }
        }
    }

    fun unhideAppById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setAppHidden(id, false)
            val cleanPkg = id.substringBefore("/").substringBefore("#")
            settingsStore.setAppHidden(cleanPkg, false)
        }
    }

    fun setAppCustomLabel(item: AppItem, customLabel: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setAppCustomLabel(item.key, customLabel)
            if (item.activityName == null || !item.isWorkProfile) {
                settingsStore.setAppCustomLabel(item.packageName, customLabel)
            }
        }
    }

    fun exportSettingsJson(): String {
        return settingsStore.exportSettingsJson(settings.value)
    }

    suspend fun importSettingsJson(json: String): Boolean {
        return settingsStore.importSettingsJson(json)
    }

    fun setSwipeLeft(keyOrPackage: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setSwipeLeftPackage(keyOrPackage)
        }
    }

    fun setSwipeRight(keyOrPackage: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setSwipeRightPackage(keyOrPackage)
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

    fun setClockTimeFormat(format: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setClockTimeFormat(format)
        }
    }

    fun setShowClockSeconds(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowClockSeconds(show)
        }
    }

    fun setShowDate(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowDate(enabled)
        }
    }

    fun setDateFormatPattern(pattern: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setDateFormatPattern(pattern)
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

    fun setFontSize(size: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setFontSize(size)
        }
    }

    fun setShowAppIcons(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowAppIcons(enabled)
        }
    }

    fun setMonochromeIcons(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setMonochromeIcons(enabled)
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

    fun setShowDrawerUsdRate(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowDrawerUsdRate(show)
        }
    }

    fun setShowHomeIdx(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setShowHomeIdx(show)
            if (show) fetchIdxPrices()
        }
    }

    fun setIdxDisplayMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setIdxDisplayMode(mode)
            fetchIdxPrices()
        }
    }

    fun setIdxSymbols(symbols: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setIdxSymbols(symbols)
            fetchIdxPrices()
        }
    }

    fun setMarketCarouselAutoRotate(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setMarketCarouselAutoRotate(enabled)
        }
    }

    fun setMarketCarouselInterval(seconds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setMarketCarouselInterval(seconds)
        }
    }

    fun setMarketCarouselTransition(transition: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setMarketCarouselTransition(transition)
        }
    }

    fun setSeparateMarketViews(separate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setSeparateMarketViews(separate)
        }
    }

    fun setSeparatedMarketLayout(layout: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsStore.setSeparatedMarketLayout(layout)
        }
    }

    override fun onCleared() {
        super.onCleared()
        PackageChangeReceiver.onPackageChangedListener = null
        stopAllPriceUpdates()
    }
}
