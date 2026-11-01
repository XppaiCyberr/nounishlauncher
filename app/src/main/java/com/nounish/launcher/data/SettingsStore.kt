package com.nounish.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

data class CachedCoinPrice(
    val price: String,
    val changeText: String = "",
    val changePercent: Double = 0.0
)

data class LauncherSettings(
    val pinnedApps: List<String> = emptyList(),
    val hiddenApps: Set<String> = emptySet(),
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val autoLaunchSingleMatch: Boolean = false,
    val swipeLeftPackage: String? = null,
    val swipeRightPackage: String? = null,
    val doubleTapLock: Boolean = false,
    val cryptoPriceEnabled: Boolean = false,
    val cryptoSymbols: List<String> = listOf("BTC", "ETH", "SOL"),
    val cryptoQuoteCurrency: String = "USDT",
    val isLightTheme: Boolean = false,
    val useMonospace: Boolean = false,
    val cachedCryptoPrices: Map<String, CachedCoinPrice> = emptyMap(),
    val currentCryptoIndex: Int = 0,
    val alphabetIndexOnRight: Boolean = false,
    val enableIndexAnimation: Boolean = true
)

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_PINNED_APPS = stringPreferencesKey("pinned_apps")
        private val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        private val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        private val KEY_SHOW_DATE = booleanPreferencesKey("show_date")
        private val KEY_AUTO_LAUNCH = booleanPreferencesKey("auto_launch_single_match")
        private val KEY_SWIPE_LEFT = stringPreferencesKey("swipe_left_package")
        private val KEY_SWIPE_RIGHT = stringPreferencesKey("swipe_right_package")
        private val KEY_DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        private val KEY_CRYPTO_ENABLED = booleanPreferencesKey("crypto_price_enabled")
        private val KEY_CRYPTO_SYMBOLS = stringPreferencesKey("crypto_symbols")
        private val KEY_CRYPTO_QUOTE = stringPreferencesKey("crypto_quote_currency")
        private val KEY_LIGHT_THEME = booleanPreferencesKey("is_light_theme")
        private val KEY_USE_MONOSPACE = booleanPreferencesKey("use_monospace")
        private val KEY_CACHED_PRICES = stringPreferencesKey("cached_crypto_prices")
        private val KEY_CRYPTO_INDEX = intPreferencesKey("current_crypto_index")
        private val KEY_INDEX_ON_RIGHT = booleanPreferencesKey("alphabet_index_on_right")
        private val KEY_ENABLE_INDEX_ANIMATION = booleanPreferencesKey("enable_index_animation")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        val pinnedRaw = prefs[KEY_PINNED_APPS] ?: ""
        val pinnedList = if (pinnedRaw.isBlank()) {
            emptyList()
        } else {
            pinnedRaw.split(",").filter { it.isNotBlank() }
        }

        val hiddenSet = prefs[KEY_HIDDEN_APPS] ?: emptySet()
        val showClock = prefs[KEY_SHOW_CLOCK] ?: true
        val showDate = prefs[KEY_SHOW_DATE] ?: true
        val autoLaunch = prefs[KEY_AUTO_LAUNCH] ?: false
        val swipeLeft = prefs[KEY_SWIPE_LEFT]?.takeIf { it.isNotBlank() }
        val swipeRight = prefs[KEY_SWIPE_RIGHT]?.takeIf { it.isNotBlank() }
        val doubleTap = prefs[KEY_DOUBLE_TAP_LOCK] ?: false
        val cryptoEnabled = prefs[KEY_CRYPTO_ENABLED] ?: false

        val symbolsRaw = prefs[KEY_CRYPTO_SYMBOLS] ?: "BTC,ETH,SOL"
        val symbolsList = symbolsRaw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("BTC", "ETH", "SOL") }

        val cryptoQuote = prefs[KEY_CRYPTO_QUOTE] ?: "USDT"
        val isLight = prefs[KEY_LIGHT_THEME] ?: false
        val useMono = prefs[KEY_USE_MONOSPACE] ?: false

        val cachedMap = parsePricesMap(prefs[KEY_CACHED_PRICES] ?: "")
        val cryptoIndex = prefs[KEY_CRYPTO_INDEX] ?: 0
        val indexOnRight = prefs[KEY_INDEX_ON_RIGHT] ?: false
        val indexAnimation = prefs[KEY_ENABLE_INDEX_ANIMATION] ?: true

        LauncherSettings(
            pinnedApps = pinnedList,
            hiddenApps = hiddenSet,
            showClock = showClock,
            showDate = showDate,
            autoLaunchSingleMatch = autoLaunch,
            swipeLeftPackage = swipeLeft,
            swipeRightPackage = swipeRight,
            doubleTapLock = doubleTap,
            cryptoPriceEnabled = cryptoEnabled,
            cryptoSymbols = symbolsList,
            cryptoQuoteCurrency = cryptoQuote,
            isLightTheme = isLight,
            useMonospace = useMono,
            cachedCryptoPrices = cachedMap,
            currentCryptoIndex = cryptoIndex,
            alphabetIndexOnRight = indexOnRight,
            enableIndexAnimation = indexAnimation
        )
    }

    suspend fun pinApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_PINNED_APPS] ?: "").split(",").filter { it.isNotBlank() }.toMutableList()
            if (!current.contains(packageName)) {
                current.add(packageName)
                prefs[KEY_PINNED_APPS] = current.joinToString(",")
            }
        }
    }

    suspend fun unpinApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_PINNED_APPS] ?: "").split(",").filter { it.isNotBlank() }.toMutableList()
            if (current.remove(packageName)) {
                prefs[KEY_PINNED_APPS] = current.joinToString(",")
            }
        }
    }

    suspend fun reorderPinnedApps(newOrder: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PINNED_APPS] = newOrder.joinToString(",")
        }
    }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_HIDDEN_APPS] ?: emptySet()).toMutableSet()
            if (hidden) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            prefs[KEY_HIDDEN_APPS] = current
        }
    }

    suspend fun setShowClock(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_CLOCK] = show }
    }

    suspend fun setShowDate(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_DATE] = show }
    }

    suspend fun setAutoLaunchSingleMatch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_LAUNCH] = enabled }
    }

    suspend fun setSwipeLeftPackage(packageName: String?) {
        context.dataStore.edit { it[KEY_SWIPE_LEFT] = packageName ?: "" }
    }

    suspend fun setSwipeRightPackage(packageName: String?) {
        context.dataStore.edit { it[KEY_SWIPE_RIGHT] = packageName ?: "" }
    }

    suspend fun setDoubleTapLock(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DOUBLE_TAP_LOCK] = enabled }
    }

    suspend fun setCryptoPriceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CRYPTO_ENABLED] = enabled }
    }

    suspend fun setCryptoSymbols(symbols: List<String>) {
        context.dataStore.edit { it[KEY_CRYPTO_SYMBOLS] = symbols.joinToString(",") }
    }

    suspend fun setCryptoQuoteCurrency(quote: String) {
        context.dataStore.edit { it[KEY_CRYPTO_QUOTE] = quote.trim().uppercase() }
    }

    suspend fun setLightTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIGHT_THEME] = enabled }
    }

    suspend fun setUseMonospace(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_MONOSPACE] = enabled }
    }

    suspend fun setAlphabetIndexOnRight(onRight: Boolean) {
        context.dataStore.edit { it[KEY_INDEX_ON_RIGHT] = onRight }
    }

    suspend fun setIndexAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLE_INDEX_ANIMATION] = enabled }
    }

    suspend fun updateCachedPrices(prices: Map<String, CachedCoinPrice>) {
        context.dataStore.edit { prefs ->
            val encoded = prices.entries.joinToString(";") {
                "${it.key}:${it.value.price}:${it.value.changeText}:${it.value.changePercent}"
            }
            prefs[KEY_CACHED_PRICES] = encoded
        }
    }

    suspend fun setCurrentCryptoIndex(index: Int) {
        context.dataStore.edit { it[KEY_CRYPTO_INDEX] = index }
    }

    private fun parsePricesMap(raw: String): Map<String, CachedCoinPrice> {
        if (raw.isBlank()) return emptyMap()
        return try {
            raw.split(";").mapNotNull { pair ->
                val parts = pair.split(":")
                when {
                    parts.size >= 4 -> {
                        parts[0] to CachedCoinPrice(
                            price = parts[1],
                            changeText = parts[2],
                            changePercent = parts[3].toDoubleOrNull() ?: 0.0
                        )
                    }
                    parts.size == 2 -> {
                        parts[0] to CachedCoinPrice(price = parts[1])
                    }
                    else -> null
                }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
