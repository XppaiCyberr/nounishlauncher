package com.nounish.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nounish.launcher.data.AppRepository
import com.nounish.launcher.data.CachedCoinPrice
import com.nounish.launcher.data.CoinTicker
import com.nounish.launcher.data.LauncherSettings
import com.nounish.launcher.data.PriceRepository
import com.nounish.launcher.data.PriceUiState
import com.nounish.launcher.data.SettingsStore
import com.nounish.launcher.model.AppItem
import com.nounish.launcher.service.PackageChangeReceiver
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

data class CoinTickerUi(
    val symbol: String,
    val price: String,
    val changeText: String,
    val changePercent: Double,
    val highPrice: String = "",
    val lowPrice: String = "",
    val isStale: Boolean = false,
    val ageMarker: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

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

    val searchQuery = MutableStateFlow("")

    val pinnedApps: StateFlow<List<AppItem>> = combine(allApps, settings) { apps, pref ->
        pref.pinnedApps.mapNotNull { pkg ->
            apps.find { it.packageName == pkg }
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
                    // Hidden apps only appear when searched by exact or close name
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
            val cached = pref.cachedCryptoPrices[sym]
            val timestamp = cached?.timestamp ?: 0L
            val isStale = timestamp <= 0L || (now - timestamp) > 60_000L
            val ageMarker = if (cached != null && isStale) PriceRepository.formatAgeMarker(timestamp) else ""
            CoinTickerUi(
                symbol = sym,
                price = cached?.price ?: "--",
                changeText = cached?.changeText ?: "",
                changePercent = cached?.changePercent ?: 0.0,
                highPrice = cached?.highPrice ?: "",
                lowPrice = cached?.lowPrice ?: "",
                isStale = isStale,
                ageMarker = ageMarker
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var priceJob: Job? = null

    init {
        PackageChangeReceiver.onPackageChangedListener = {
            appRepository.reloadApps()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun startPriceRefresh() {
        val currentSettings = settings.value
        if (!currentSettings.cryptoPriceEnabled) {
            _priceState.value = PriceUiState()
            return
        }

        updatePriceDisplayFromCache(currentSettings)

        priceJob?.cancel()
        priceJob = viewModelScope.launch {
            while (isActive) {
                fetchFreshPrices()
                delay(15_000L)
            }
        }
    }

    fun stopPriceRefresh() {
        priceJob?.cancel()
        priceJob = null
    }

    private suspend fun fetchFreshPrices() {
        val currentSettings = settings.value
        if (!currentSettings.cryptoPriceEnabled || currentSettings.cryptoSymbols.isEmpty()) return

        _priceState.value = _priceState.value.copy(isLoading = true)

        val result = priceRepository.fetchPrices(
            symbols = currentSettings.cryptoSymbols,
            quoteCurrency = currentSettings.cryptoQuoteCurrency
        )

        result.onSuccess { newPrices ->
            val updatedCache = currentSettings.cachedCryptoPrices.toMutableMap()
            newPrices.forEach { (sym, ticker) ->
                updatedCache[sym] = CachedCoinPrice(
                    price = ticker.price,
                    changeText = ticker.formattedChange,
                    changePercent = ticker.changePercent,
                    highPrice = ticker.highPrice,
                    lowPrice = ticker.lowPrice,
                    timestamp = ticker.timestamp
                )
            }
            settingsStore.updateCachedPrices(updatedCache)

            val symbols = currentSettings.cryptoSymbols
            val index = currentSettings.currentCryptoIndex.coerceIn(0, (symbols.size - 1).coerceAtLeast(0))
            val currentSymbol = symbols.getOrNull(index) ?: symbols.firstOrNull() ?: "BTC"
            val ticker = newPrices[currentSymbol] ?: updatedCache[currentSymbol]?.let {
                CoinTicker(it.price, it.changePercent, it.changeText, it.highPrice, it.lowPrice, it.timestamp)
            }

            _priceState.value = PriceUiState(
                coinSymbol = currentSymbol,
                priceText = ticker?.price ?: "--",
                changeText = ticker?.formattedChange ?: "",
                changePercent = ticker?.changePercent ?: 0.0,
                isStale = false,
                ageMarker = "",
                isLoading = false
            )
        }.onFailure {
            val symbols = currentSettings.cryptoSymbols
            val index = currentSettings.currentCryptoIndex.coerceIn(0, (symbols.size - 1).coerceAtLeast(0))
            val currentSymbol = symbols.getOrNull(index) ?: symbols.firstOrNull() ?: "BTC"
            val cached = currentSettings.cachedCryptoPrices[currentSymbol]
            val ageMarker = PriceRepository.formatAgeMarker(cached?.timestamp ?: 0L)

            _priceState.value = PriceUiState(
                coinSymbol = currentSymbol,
                priceText = cached?.price ?: "--",
                changeText = cached?.changeText ?: "",
                changePercent = cached?.changePercent ?: 0.0,
                isStale = true,
                ageMarker = ageMarker,
                isLoading = false
            )
        }
    }

    private fun updatePriceDisplayFromCache(currentSettings: LauncherSettings) {
        val symbols = currentSettings.cryptoSymbols
        if (symbols.isEmpty()) return

        val index = currentSettings.currentCryptoIndex.coerceIn(0, symbols.size - 1)
        val symbol = symbols[index]
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
                isLoading = false
            )
        }
    }

    fun cycleNextCryptoCoin() {
        val currentSettings = settings.value
        val symbols = currentSettings.cryptoSymbols
        if (symbols.isEmpty()) return

        val nextIndex = (currentSettings.currentCryptoIndex + 1) % symbols.size
        viewModelScope.launch {
            settingsStore.setCurrentCryptoIndex(nextIndex)
            val symbol = symbols[nextIndex]
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
                ageMarker = ageMarker
            )
            // Fetch fresh
            fetchFreshPrices()
        }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch {
            settingsStore.pinApp(packageName)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            settingsStore.unpinApp(packageName)
        }
    }

    fun reorderPinnedApps(list: List<String>) {
        viewModelScope.launch {
            settingsStore.reorderPinnedApps(list)
        }
    }

    fun toggleHideApp(packageName: String) {
        viewModelScope.launch {
            val isHidden = settings.value.hiddenApps.contains(packageName)
            settingsStore.setAppHidden(packageName, !isHidden)
        }
    }

    fun setAppCustomLabel(packageName: String, customLabel: String?) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            settingsStore.setSwipeLeftPackage(packageName)
        }
    }

    fun setSwipeRight(packageName: String?) {
        viewModelScope.launch {
            settingsStore.setSwipeRightPackage(packageName)
        }
    }

    fun setDoubleTapLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDoubleTapLock(enabled)
        }
    }

    fun setShowClock(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setShowClock(enabled)
        }
    }

    fun setShowDate(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setShowDate(enabled)
        }
    }

    fun setShowHomeCrypto(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setShowHomeCrypto(enabled)
        }
    }

    fun setAutoLaunchSingleMatch(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoLaunchSingleMatch(enabled)
        }
    }

    fun setCryptoPriceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setCryptoPriceEnabled(enabled)
            if (enabled) {
                startPriceRefresh()
            } else {
                stopPriceRefresh()
                _priceState.value = PriceUiState()
            }
        }
    }

    fun setCryptoSymbols(symbols: List<String>) {
        viewModelScope.launch {
            settingsStore.setCryptoSymbols(symbols)
            fetchFreshPrices()
        }
    }

    fun setCryptoQuoteCurrency(quote: String) {
        viewModelScope.launch {
            settingsStore.setCryptoQuoteCurrency(quote)
            fetchFreshPrices()
        }
    }

    fun setLightTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setLightTheme(enabled)
        }
    }

    fun setUseMonospace(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setUseMonospace(enabled)
        }
    }

    fun setAlphabetIndexOnRight(onRight: Boolean) {
        viewModelScope.launch {
            settingsStore.setAlphabetIndexOnRight(onRight)
        }
    }

    fun setIndexAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setIndexAnimationEnabled(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        PackageChangeReceiver.onPackageChangedListener = null
        stopPriceRefresh()
    }
}
