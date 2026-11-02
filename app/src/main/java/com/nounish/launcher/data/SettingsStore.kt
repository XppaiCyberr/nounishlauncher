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
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

data class CachedCoinPrice(
    val price: String,
    val changeText: String = "",
    val changePercent: Double = 0.0,
    val highPrice: String = "",
    val lowPrice: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sparkline: String = ""
)

data class LauncherSettings(
    val pinnedApps: List<String> = emptyList(),
    val pinnedAppLabels: Map<String, String> = emptyMap(),
    val hiddenApps: Set<String> = emptySet(),
    val customAppLabels: Map<String, String> = emptyMap(),
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val clockTimeFormat: String = "system", // "system", "24h", "12h", "12h_no_am_pm"
    val showClockSeconds: Boolean = false,
    val dateFormatPattern: String = "EEEE, d MMMM",
    val showHomeCrypto: Boolean = true,
    val autoLaunchSingleMatch: Boolean = false,
    val swipeLeftPackage: String? = null,
    val swipeRightPackage: String? = null,
    val doubleTapLock: Boolean = false,
    val cryptoPriceEnabled: Boolean = true,
    val cryptoSymbols: List<String> = listOf("BTC", "ETH", "SOL"),
    val cryptoQuoteCurrency: String = "USDT",
    val useWebSocketOnHome: Boolean = false,
    val isLightTheme: Boolean = false,
    val useMonospace: Boolean = false,
    val fontSize: String = "medium", // "micro", "extra_small", "small", "compact", "medium", "large", "extra_large"
    val showAppIcons: Boolean = true,
    val monochromeIcons: Boolean = true,
    val hasInitializedPinned: Boolean = false,
    val cachedCryptoPrices: Map<String, CachedCoinPrice> = emptyMap(),
    val currentCryptoIndex: Int = 0,
    val alphabetIndexOnRight: Boolean = false,
    val enableIndexAnimation: Boolean = true
)

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_PINNED_APPS = stringPreferencesKey("pinned_apps")
        private val KEY_PINNED_APP_LABELS = stringPreferencesKey("pinned_app_labels")
        private val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        private val KEY_CUSTOM_LABELS = stringPreferencesKey("custom_app_labels")
        private val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        private val KEY_SHOW_DATE = booleanPreferencesKey("show_date")
        private val KEY_CLOCK_TIME_FORMAT = stringPreferencesKey("clock_time_format")
        private val KEY_SHOW_CLOCK_SECONDS = booleanPreferencesKey("show_clock_seconds")
        private val KEY_DATE_FORMAT_PATTERN = stringPreferencesKey("date_format_pattern")
        private val KEY_SHOW_HOME_CRYPTO = booleanPreferencesKey("show_home_crypto")
        private val KEY_AUTO_LAUNCH = booleanPreferencesKey("auto_launch_single_match")
        private val KEY_SWIPE_LEFT = stringPreferencesKey("swipe_left_package")
        private val KEY_SWIPE_RIGHT = stringPreferencesKey("swipe_right_package")
        private val KEY_DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        private val KEY_CRYPTO_ENABLED = booleanPreferencesKey("crypto_price_enabled")
        private val KEY_CRYPTO_SYMBOLS = stringPreferencesKey("crypto_symbols")
        private val KEY_CRYPTO_QUOTE = stringPreferencesKey("crypto_quote_currency")
        private val KEY_USE_WEBSOCKET_ON_HOME = booleanPreferencesKey("use_websocket_on_home")
        private val KEY_LIGHT_THEME = booleanPreferencesKey("is_light_theme")
        private val KEY_USE_MONOSPACE = booleanPreferencesKey("use_monospace")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        private val KEY_MONOCHROME_ICONS = booleanPreferencesKey("monochrome_icons")
        private val KEY_INITIALIZED_PINNED = booleanPreferencesKey("has_initialized_pinned")
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

        val pinnedLabels = parseLabelsMap(prefs[KEY_PINNED_APP_LABELS] ?: "")
        val hiddenSet = prefs[KEY_HIDDEN_APPS] ?: emptySet()
        val customLabels = parseLabelsMap(prefs[KEY_CUSTOM_LABELS] ?: "")
        val showClock = prefs[KEY_SHOW_CLOCK] ?: true
        val showDate = prefs[KEY_SHOW_DATE] ?: true
        val clockFormat = prefs[KEY_CLOCK_TIME_FORMAT] ?: "system"
        val showSeconds = prefs[KEY_SHOW_CLOCK_SECONDS] ?: false
        val datePattern = prefs[KEY_DATE_FORMAT_PATTERN] ?: "EEEE, d MMMM"
        val showHomeCrypto = prefs[KEY_SHOW_HOME_CRYPTO] ?: true
        val autoLaunch = prefs[KEY_AUTO_LAUNCH] ?: false
        val swipeLeft = prefs[KEY_SWIPE_LEFT]?.takeIf { it.isNotBlank() }
        val swipeRight = prefs[KEY_SWIPE_RIGHT]?.takeIf { it.isNotBlank() }
        val doubleTap = prefs[KEY_DOUBLE_TAP_LOCK] ?: false
        val cryptoEnabled = prefs[KEY_CRYPTO_ENABLED] ?: true

        val symbolsRaw = prefs[KEY_CRYPTO_SYMBOLS] ?: "BTC,ETH,SOL"
        val symbolsList = symbolsRaw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("BTC", "ETH", "SOL") }

        val cryptoQuote = prefs[KEY_CRYPTO_QUOTE] ?: "USDT"
        val useWebSocket = prefs[KEY_USE_WEBSOCKET_ON_HOME] ?: false
        val isLight = prefs[KEY_LIGHT_THEME] ?: false
        val useMono = prefs[KEY_USE_MONOSPACE] ?: false
        val fontSize = prefs[KEY_FONT_SIZE] ?: "medium"
        val showIcons = prefs[KEY_SHOW_APP_ICONS] ?: true
        val monoIcons = prefs[KEY_MONOCHROME_ICONS] ?: true
        val initializedPinned = prefs[KEY_INITIALIZED_PINNED] ?: false

        val cachedMap = parsePricesMap(prefs[KEY_CACHED_PRICES] ?: "")
        val cryptoIndex = prefs[KEY_CRYPTO_INDEX] ?: 0
        val indexOnRight = prefs[KEY_INDEX_ON_RIGHT] ?: false
        val indexAnimation = prefs[KEY_ENABLE_INDEX_ANIMATION] ?: true

        LauncherSettings(
            pinnedApps = pinnedList,
            pinnedAppLabels = pinnedLabels,
            hiddenApps = hiddenSet,
            customAppLabels = customLabels,
            showClock = showClock,
            showDate = showDate,
            clockTimeFormat = clockFormat,
            showClockSeconds = showSeconds,
            dateFormatPattern = datePattern,
            showHomeCrypto = showHomeCrypto,
            autoLaunchSingleMatch = autoLaunch,
            swipeLeftPackage = swipeLeft,
            swipeRightPackage = swipeRight,
            doubleTapLock = doubleTap,
            cryptoPriceEnabled = cryptoEnabled,
            cryptoSymbols = symbolsList,
            cryptoQuoteCurrency = cryptoQuote,
            useWebSocketOnHome = useWebSocket,
            isLightTheme = isLight,
            useMonospace = useMono,
            fontSize = fontSize,
            showAppIcons = showIcons,
            monochromeIcons = monoIcons,
            hasInitializedPinned = initializedPinned,
            cachedCryptoPrices = cachedMap,
            currentCryptoIndex = cryptoIndex,
            alphabetIndexOnRight = indexOnRight,
            enableIndexAnimation = indexAnimation
        )
    }

    suspend fun pinApp(packageName: String, label: String? = null) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_PINNED_APPS] ?: "").split(",").filter { it.isNotBlank() }.toMutableList()
            if (!current.contains(packageName)) {
                current.add(packageName)
                prefs[KEY_PINNED_APPS] = current.joinToString(",")
            }
            if (!label.isNullOrBlank()) {
                val currentLabels = parseLabelsMap(prefs[KEY_PINNED_APP_LABELS] ?: "").toMutableMap()
                currentLabels[packageName] = label.trim()
                prefs[KEY_PINNED_APP_LABELS] = JSONObject(currentLabels as Map<*, *>).toString()
            }
        }
    }

    suspend fun unpinApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_PINNED_APPS] ?: "").split(",").filter { it.isNotBlank() }.toMutableList()
            if (current.remove(packageName)) {
                prefs[KEY_PINNED_APPS] = current.joinToString(",")
            }
            val currentLabels = parseLabelsMap(prefs[KEY_PINNED_APP_LABELS] ?: "").toMutableMap()
            if (currentLabels.remove(packageName) != null) {
                prefs[KEY_PINNED_APP_LABELS] = JSONObject(currentLabels as Map<*, *>).toString()
            }
        }
    }

    suspend fun updatePinnedAppLabels(labels: Map<String, String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PINNED_APP_LABELS] = JSONObject(labels as Map<*, *>).toString()
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

    suspend fun setAppCustomLabel(packageName: String, customLabel: String?) {
        context.dataStore.edit { prefs ->
            val current = parseLabelsMap(prefs[KEY_CUSTOM_LABELS] ?: "").toMutableMap()
            if (customLabel.isNullOrBlank()) {
                current.remove(packageName)
            } else {
                current[packageName] = customLabel.trim()
            }
            prefs[KEY_CUSTOM_LABELS] = JSONObject(current as Map<*, *>).toString()
        }
    }

    suspend fun setShowClock(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_CLOCK] = show }
    }

    suspend fun setShowDate(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_DATE] = show }
    }

    suspend fun setClockTimeFormat(format: String) {
        context.dataStore.edit { it[KEY_CLOCK_TIME_FORMAT] = format }
    }

    suspend fun setShowClockSeconds(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_CLOCK_SECONDS] = show }
    }

    suspend fun setDateFormatPattern(pattern: String) {
        context.dataStore.edit { it[KEY_DATE_FORMAT_PATTERN] = pattern }
    }

    suspend fun setShowHomeCrypto(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_HOME_CRYPTO] = show }
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

    suspend fun setUseWebSocketOnHome(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_WEBSOCKET_ON_HOME] = enabled }
    }

    suspend fun setLightTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIGHT_THEME] = enabled }
    }

    suspend fun setUseMonospace(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_MONOSPACE] = enabled }
    }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setShowAppIcons(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_APP_ICONS] = show }
    }

    suspend fun setMonochromeIcons(mono: Boolean) {
        context.dataStore.edit { it[KEY_MONOCHROME_ICONS] = mono }
    }

    suspend fun setHasInitializedPinned(initialized: Boolean) {
        context.dataStore.edit { it[KEY_INITIALIZED_PINNED] = initialized }
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
                "${it.key}:${it.value.price}:${it.value.changeText}:${it.value.changePercent}:${it.value.highPrice}:${it.value.lowPrice}:${it.value.timestamp}:${it.value.sparkline}"
            }
            prefs[KEY_CACHED_PRICES] = encoded
        }
    }

    suspend fun setCurrentCryptoIndex(index: Int) {
        context.dataStore.edit { it[KEY_CRYPTO_INDEX] = index }
    }

    fun exportSettingsJson(settings: LauncherSettings): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        root.put("pinnedApps", JSONArray(settings.pinnedApps))
        root.put("pinnedAppLabels", JSONObject(settings.pinnedAppLabels))
        root.put("hiddenApps", JSONArray(settings.hiddenApps))
        root.put("customAppLabels", JSONObject(settings.customAppLabels))

        root.put("showClock", settings.showClock)
        root.put("showDate", settings.showDate)
        root.put("clockTimeFormat", settings.clockTimeFormat)
        root.put("showClockSeconds", settings.showClockSeconds)
        root.put("dateFormatPattern", settings.dateFormatPattern)
        root.put("showHomeCrypto", settings.showHomeCrypto)
        root.put("autoLaunchSingleMatch", settings.autoLaunchSingleMatch)

        root.put("swipeLeftPackage", settings.swipeLeftPackage ?: "")
        root.put("swipeRightPackage", settings.swipeRightPackage ?: "")
        root.put("doubleTapLock", settings.doubleTapLock)

        root.put("cryptoPriceEnabled", settings.cryptoPriceEnabled)
        root.put("cryptoSymbols", JSONArray(settings.cryptoSymbols))
        root.put("cryptoQuoteCurrency", settings.cryptoQuoteCurrency)
        root.put("useWebSocketOnHome", settings.useWebSocketOnHome)

        root.put("isLightTheme", settings.isLightTheme)
        root.put("useMonospace", settings.useMonospace)
        root.put("fontSize", settings.fontSize)
        root.put("showAppIcons", settings.showAppIcons)
        root.put("monochromeIcons", settings.monochromeIcons)
        root.put("alphabetIndexOnRight", settings.alphabetIndexOnRight)
        root.put("enableIndexAnimation", settings.enableIndexAnimation)

        return root.toString(2)
    }

    suspend fun importSettingsJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            context.dataStore.edit { prefs ->
                if (root.has("pinnedApps")) {
                    val arr = root.getJSONArray("pinnedApps")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    prefs[KEY_PINNED_APPS] = list.joinToString(",")
                }

                if (root.has("pinnedAppLabels")) {
                    val obj = root.getJSONObject("pinnedAppLabels")
                    prefs[KEY_PINNED_APP_LABELS] = obj.toString()
                }

                if (root.has("hiddenApps")) {
                    val arr = root.getJSONArray("hiddenApps")
                    val set = mutableSetOf<String>()
                    for (i in 0 until arr.length()) set.add(arr.getString(i))
                    prefs[KEY_HIDDEN_APPS] = set
                }

                if (root.has("customAppLabels")) {
                    val obj = root.getJSONObject("customAppLabels")
                    prefs[KEY_CUSTOM_LABELS] = obj.toString()
                }

                if (root.has("showClock")) prefs[KEY_SHOW_CLOCK] = root.getBoolean("showClock")
                if (root.has("showDate")) prefs[KEY_SHOW_DATE] = root.getBoolean("showDate")
                if (root.has("clockTimeFormat")) prefs[KEY_CLOCK_TIME_FORMAT] = root.getString("clockTimeFormat")
                if (root.has("showClockSeconds")) prefs[KEY_SHOW_CLOCK_SECONDS] = root.getBoolean("showClockSeconds")
                if (root.has("dateFormatPattern")) prefs[KEY_DATE_FORMAT_PATTERN] = root.getString("dateFormatPattern")

                if (root.has("showHomeCrypto")) prefs[KEY_SHOW_HOME_CRYPTO] = root.getBoolean("showHomeCrypto")
                if (root.has("autoLaunchSingleMatch")) prefs[KEY_AUTO_LAUNCH] = root.getBoolean("autoLaunchSingleMatch")

                if (root.has("swipeLeftPackage")) prefs[KEY_SWIPE_LEFT] = root.getString("swipeLeftPackage")
                if (root.has("swipeRightPackage")) prefs[KEY_SWIPE_RIGHT] = root.getString("swipeRightPackage")
                if (root.has("doubleTapLock")) prefs[KEY_DOUBLE_TAP_LOCK] = root.getBoolean("doubleTapLock")

                if (root.has("cryptoPriceEnabled")) prefs[KEY_CRYPTO_ENABLED] = root.getBoolean("cryptoPriceEnabled")
                if (root.has("cryptoSymbols")) {
                    val arr = root.getJSONArray("cryptoSymbols")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    prefs[KEY_CRYPTO_SYMBOLS] = list.joinToString(",")
                }
                if (root.has("cryptoQuoteCurrency")) prefs[KEY_CRYPTO_QUOTE] = root.getString("cryptoQuoteCurrency")
                if (root.has("useWebSocketOnHome")) prefs[KEY_USE_WEBSOCKET_ON_HOME] = root.getBoolean("useWebSocketOnHome")

                if (root.has("isLightTheme")) prefs[KEY_LIGHT_THEME] = root.getBoolean("isLightTheme")
                if (root.has("useMonospace")) prefs[KEY_USE_MONOSPACE] = root.getBoolean("useMonospace")
                if (root.has("fontSize")) prefs[KEY_FONT_SIZE] = root.getString("fontSize")
                if (root.has("showAppIcons")) prefs[KEY_SHOW_APP_ICONS] = root.getBoolean("showAppIcons")
                if (root.has("monochromeIcons")) prefs[KEY_MONOCHROME_ICONS] = root.getBoolean("monochromeIcons")
                if (root.has("alphabetIndexOnRight")) prefs[KEY_INDEX_ON_RIGHT] = root.getBoolean("alphabetIndexOnRight")
                if (root.has("enableIndexAnimation")) prefs[KEY_ENABLE_INDEX_ANIMATION] = root.getBoolean("enableIndexAnimation")
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseLabelsMap(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parsePricesMap(raw: String): Map<String, CachedCoinPrice> {
        if (raw.isBlank()) return emptyMap()
        return try {
            raw.split(";").mapNotNull { pair ->
                val parts = pair.split(":")
                when {
                    parts.size >= 8 -> {
                        parts[0] to CachedCoinPrice(
                            price = parts[1],
                            changeText = parts[2],
                            changePercent = parts[3].toDoubleOrNull() ?: 0.0,
                            highPrice = parts[4],
                            lowPrice = parts[5],
                            timestamp = parts[6].toLongOrNull() ?: System.currentTimeMillis(),
                            sparkline = parts[7]
                        )
                    }
                    parts.size >= 7 -> {
                        parts[0] to CachedCoinPrice(
                            price = parts[1],
                            changeText = parts[2],
                            changePercent = parts[3].toDoubleOrNull() ?: 0.0,
                            highPrice = parts[4],
                            lowPrice = parts[5],
                            timestamp = parts[6].toLongOrNull() ?: System.currentTimeMillis()
                        )
                    }
                    parts.size >= 6 -> {
                        parts[0] to CachedCoinPrice(
                            price = parts[1],
                            changeText = parts[2],
                            changePercent = parts[3].toDoubleOrNull() ?: 0.0,
                            highPrice = parts[4],
                            lowPrice = parts[5]
                        )
                    }
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
