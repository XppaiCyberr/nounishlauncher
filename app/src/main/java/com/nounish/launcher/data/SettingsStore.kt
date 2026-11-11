package com.nounish.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

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

data class CachedIdxQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long = 0L,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CachedNounsProposal(
    val id: Int,
    val title: String,
    val statusLabel: String,
    val statusIndex: Int,
    val proposer: String,
    val forVotes: Int,
    val againstVotes: Int,
    val abstainVotes: Int = 0,
    val quorum: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CachedNounsCandidate(
    val slug: String,
    val title: String,
    val proposer: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CachedNounsAuction(
    val nounId: Int,
    val amountWei: String,
    val amountEth: Double,
    val startTime: Long,
    val endTime: Long,
    val bidder: String,
    val settled: Boolean,
    val timestamp: Long = System.currentTimeMillis()
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
    val swipeDownAction: String = "notifications", // "notifications", "refresh", "drawer", "none"
    val swipeDownToRefresh: Boolean = false,
    val doubleTapLock: Boolean = false,
    val showRecentApps: Boolean = true,
    val recentApps: List<String> = emptyList(),
    val showWallpaper: Boolean = false,
    val wallpaperDimLevel: Float = 0.80f,
    val accentColor: String = "monochrome", // "monochrome", "nounish_red", "warm_amber", "mint_green", "slate_blue", "lavender"
    val hapticFeedbackLevel: String = "standard", // "standard", "subtle", "firm", "off"
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
    val showDrawerUsdRate: Boolean = true,
    val cachedUsdRate: String = "",
    val cachedUsdChange: String = "",
    val cachedUsdChangePercent: Double = 0.0,
    val cachedUsdTimestamp: Long = 0L,
    val showHomeIdx: Boolean = true,
    val idxDisplayMode: String = "auto_rotate", // "auto_rotate", "watchlist", "indices", "gainers", "losers"
    val idxSymbols: List<String> = listOf("IHSG", "BBCA", "BBRI", "BMRI"),
    val idxAutoRotateIntervalSeconds: Int = 6,
    val marketCarouselAutoRotate: Boolean = true,
    val marketCarouselIntervalSeconds: Int = 7,
    val marketCarouselTransition: String = "slide", // "slide", "crossfade"
    val separateMarketViews: Boolean = true,
    val separatedMarketLayout: String = "stacked", // "stacked", "side_by_side"
    val cachedIdxQuotes: Map<String, CachedIdxQuote> = emptyMap(),
    val cachedIdxCategories: Map<String, List<CachedIdxQuote>> = emptyMap(),
    val cachedIdxTimestamp: Long = 0L,
    val cachedCryptoPrices: Map<String, CachedCoinPrice> = emptyMap(),
    val currentCryptoIndex: Int = 0,
    val alphabetIndexOnRight: Boolean = false,
    val enableIndexAnimation: Boolean = true,
    val searchBarAtBottom: Boolean = false,
    val showCornerText: Boolean = true,
    val cornerText: String = "xppaicyber",
    val marketCurrencyFormat: String = "suffix", // "suffix", "prefix", "none"
    val showHomeScreenNote: Boolean = true,
    val homeScreenNoteDismissed: Boolean = false,
    val homeScreenNoteIndex: Int = 0,
    val showHomeNouns: Boolean = true,
    val nounsStatusFilter: String = "All",
    val nounsDisplayMode: String = "auto_rotate", // "auto_rotate", "proposals", "candidates"
    val nounsMaxDisplayCount: Int = 3,
    val cachedNounsProposals: List<CachedNounsProposal> = emptyList(),
    val cachedNounsTimestamp: Long = 0L,
    val cachedNounsCandidates: List<CachedNounsCandidate> = emptyList(),
    val cachedNounsCandidatesTimestamp: Long = 0L,
    val cachedNounsAuction: CachedNounsAuction? = null,
    val cachedNounsAuctionTimestamp: Long = 0L
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
        private val KEY_SWIPE_DOWN_ACTION = stringPreferencesKey("swipe_down_action")
        private val KEY_SWIPE_DOWN_TO_REFRESH = booleanPreferencesKey("swipe_down_to_refresh")
        private val KEY_DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        private val KEY_CRYPTO_ENABLED = booleanPreferencesKey("crypto_price_enabled")
        private val KEY_CRYPTO_SYMBOLS = stringPreferencesKey("crypto_symbols")
        private val KEY_CRYPTO_QUOTE = stringPreferencesKey("crypto_quote_currency")
        private val KEY_USE_WEBSOCKET_ON_HOME = booleanPreferencesKey("use_websocket_on_home")
        private val KEY_LIGHT_THEME = booleanPreferencesKey("is_light_theme")
        private val KEY_SHOW_WALLPAPER = booleanPreferencesKey("show_wallpaper")
        private val KEY_WALLPAPER_DIM_LEVEL = floatPreferencesKey("wallpaper_dim_level")
        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_HAPTIC_FEEDBACK_LEVEL = stringPreferencesKey("haptic_feedback_level")
        private val KEY_SHOW_RECENT_APPS = booleanPreferencesKey("show_recent_apps")
        private val KEY_RECENT_APPS = stringPreferencesKey("recent_apps")
        private val KEY_USE_MONOSPACE = booleanPreferencesKey("use_monospace")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        private val KEY_MONOCHROME_ICONS = booleanPreferencesKey("monochrome_icons")
        private val KEY_INITIALIZED_PINNED = booleanPreferencesKey("has_initialized_pinned")
        private val KEY_SHOW_DRAWER_USD_RATE = booleanPreferencesKey("show_drawer_usd_rate")
        private val KEY_CACHED_USD_RATE = stringPreferencesKey("cached_usd_rate")
        private val KEY_CACHED_USD_CHANGE = stringPreferencesKey("cached_usd_change")
        private val KEY_CACHED_USD_CHANGE_PERCENT = doublePreferencesKey("cached_usd_change_percent")
        private val KEY_CACHED_USD_TIMESTAMP = longPreferencesKey("cached_usd_timestamp")
        private val KEY_CACHED_PRICES = stringPreferencesKey("cached_crypto_prices")
        private val KEY_CRYPTO_INDEX = intPreferencesKey("current_crypto_index")
        private val KEY_INDEX_ON_RIGHT = booleanPreferencesKey("alphabet_index_on_right")
        private val KEY_ENABLE_INDEX_ANIMATION = booleanPreferencesKey("enable_index_animation")
        private val KEY_SEARCH_BAR_AT_BOTTOM = booleanPreferencesKey("search_bar_at_bottom")
        private val KEY_SHOW_HOME_IDX = booleanPreferencesKey("show_home_idx")
        private val KEY_IDX_DISPLAY_MODE = stringPreferencesKey("idx_display_mode")
        private val KEY_IDX_SYMBOLS = stringPreferencesKey("idx_symbols")
        private val KEY_IDX_AUTO_ROTATE_INTERVAL = intPreferencesKey("idx_auto_rotate_interval")
        private val KEY_MARKET_CAROUSEL_AUTO_ROTATE = booleanPreferencesKey("market_carousel_auto_rotate")
        private val KEY_MARKET_CAROUSEL_INTERVAL = intPreferencesKey("market_carousel_interval")
        private val KEY_MARKET_CAROUSEL_TRANSITION = stringPreferencesKey("market_carousel_transition")
        private val KEY_SEPARATE_MARKET_VIEWS = booleanPreferencesKey("separate_market_views")
        private val KEY_SEPARATED_MARKET_LAYOUT = stringPreferencesKey("separated_market_layout")
        private val KEY_CACHED_IDX_QUOTES = stringPreferencesKey("cached_idx_quotes")
        private val KEY_CACHED_IDX_CATEGORIES = stringPreferencesKey("cached_idx_categories_json")
        private val KEY_CACHED_IDX_TIMESTAMP = longPreferencesKey("cached_idx_timestamp")
        private val KEY_SHOW_CORNER_TEXT = booleanPreferencesKey("show_corner_text")
        private val KEY_CORNER_TEXT = stringPreferencesKey("corner_text")
        private val KEY_MARKET_CURRENCY_FORMAT = stringPreferencesKey("market_currency_format")
        private val KEY_SHOW_HOME_NOTE = booleanPreferencesKey("show_home_note")
        private val KEY_HOME_NOTE_DISMISSED = booleanPreferencesKey("home_note_dismissed")
        private val KEY_HOME_NOTE_INDEX = intPreferencesKey("home_note_index")
        private val KEY_SHOW_HOME_NOUNS = booleanPreferencesKey("show_home_nouns")
        private val KEY_NOUNS_STATUS_FILTER = stringPreferencesKey("nouns_status_filter")
        private val KEY_NOUNS_DISPLAY_MODE = stringPreferencesKey("nouns_display_mode")
        private val KEY_NOUNS_MAX_DISPLAY_COUNT = intPreferencesKey("nouns_max_display_count")
        private val KEY_CACHED_NOUNS_PROPOSALS = stringPreferencesKey("cached_nouns_proposals_json")
        private val KEY_CACHED_NOUNS_TIMESTAMP = longPreferencesKey("cached_nouns_timestamp")
        private val KEY_CACHED_NOUNS_CANDIDATES = stringPreferencesKey("cached_nouns_candidates_json")
        private val KEY_CACHED_NOUNS_CANDIDATES_TIMESTAMP = longPreferencesKey("cached_nouns_candidates_timestamp")
        private val KEY_CACHED_NOUNS_AUCTION = stringPreferencesKey("cached_nouns_auction_json")
        private val KEY_CACHED_NOUNS_AUCTION_TIMESTAMP = longPreferencesKey("cached_nouns_auction_timestamp")
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
        val savedSwipeDownAction = prefs[KEY_SWIPE_DOWN_ACTION]
        val legacySwipeDownToRefresh = prefs[KEY_SWIPE_DOWN_TO_REFRESH]
        val swipeDownAction = when {
            savedSwipeDownAction != null -> savedSwipeDownAction
            legacySwipeDownToRefresh == true -> "refresh"
            else -> "notifications"
        }
        val swipeDownToRefresh = (swipeDownAction == "refresh")
        val doubleTap = prefs[KEY_DOUBLE_TAP_LOCK] ?: false
        val cryptoEnabled = prefs[KEY_CRYPTO_ENABLED] ?: true

        val symbolsRaw = prefs[KEY_CRYPTO_SYMBOLS] ?: "BTC,ETH,SOL"
        val symbolsList = symbolsRaw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("BTC", "ETH", "SOL") }

        val cryptoQuote = prefs[KEY_CRYPTO_QUOTE] ?: "USDT"
        val useWebSocket = prefs[KEY_USE_WEBSOCKET_ON_HOME] ?: false
        val isLight = prefs[KEY_LIGHT_THEME] ?: false
        val showWallpaper = prefs[KEY_SHOW_WALLPAPER] ?: false
        val wallpaperDimLevel = prefs[KEY_WALLPAPER_DIM_LEVEL] ?: 0.80f
        val accentColor = prefs[KEY_ACCENT_COLOR] ?: "monochrome"
        val hapticFeedbackLevel = prefs[KEY_HAPTIC_FEEDBACK_LEVEL] ?: "standard"
        val showRecentApps = prefs[KEY_SHOW_RECENT_APPS] ?: true
        val recentAppsRaw = prefs[KEY_RECENT_APPS] ?: ""
        val recentApps = recentAppsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val useMono = prefs[KEY_USE_MONOSPACE] ?: false
        val fontSize = prefs[KEY_FONT_SIZE] ?: "medium"
        val showIcons = prefs[KEY_SHOW_APP_ICONS] ?: true
        val monoIcons = prefs[KEY_MONOCHROME_ICONS] ?: true
        val initializedPinned = prefs[KEY_INITIALIZED_PINNED] ?: false
        val showDrawerUsd = prefs[KEY_SHOW_DRAWER_USD_RATE] ?: true
        val cachedUsdRate = prefs[KEY_CACHED_USD_RATE] ?: ""
        val cachedUsdChange = prefs[KEY_CACHED_USD_CHANGE] ?: ""
        val cachedUsdChangePercent = prefs[KEY_CACHED_USD_CHANGE_PERCENT] ?: 0.0
        val cachedUsdTimestamp = prefs[KEY_CACHED_USD_TIMESTAMP] ?: 0L

        val cachedMap = parsePricesMap(prefs[KEY_CACHED_PRICES] ?: "")
        val cryptoIndex = prefs[KEY_CRYPTO_INDEX] ?: 0
        val indexOnRight = prefs[KEY_INDEX_ON_RIGHT] ?: false
        val indexAnimation = prefs[KEY_ENABLE_INDEX_ANIMATION] ?: true
        val searchBarAtBottom = prefs[KEY_SEARCH_BAR_AT_BOTTOM] ?: false

        val showHomeIdx = prefs[KEY_SHOW_HOME_IDX] ?: true
        val idxDisplayMode = prefs[KEY_IDX_DISPLAY_MODE] ?: "auto_rotate"
        val idxSymbolsRaw = prefs[KEY_IDX_SYMBOLS] ?: "IHSG,BBCA,BBRI,BMRI"
        val idxSymbols = idxSymbolsRaw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("IHSG", "BBCA", "BBRI", "BMRI") }
        val idxAutoRotateInterval = prefs[KEY_IDX_AUTO_ROTATE_INTERVAL] ?: 6
        val carouselAutoRotate = prefs[KEY_MARKET_CAROUSEL_AUTO_ROTATE] ?: true
        val carouselInterval = prefs[KEY_MARKET_CAROUSEL_INTERVAL] ?: 7
        val carouselTransition = prefs[KEY_MARKET_CAROUSEL_TRANSITION] ?: "slide"
        val separateMarketViews = prefs[KEY_SEPARATE_MARKET_VIEWS] ?: true
        val separatedMarketLayout = prefs[KEY_SEPARATED_MARKET_LAYOUT] ?: "stacked"
        val cachedIdxQuotes = parseIdxMap(prefs[KEY_CACHED_IDX_QUOTES] ?: "")
        val cachedIdxCategories = parseIdxCategoriesJson(prefs[KEY_CACHED_IDX_CATEGORIES] ?: "")
        val cachedIdxTimestamp = prefs[KEY_CACHED_IDX_TIMESTAMP] ?: 0L

        val showCornerText = prefs[KEY_SHOW_CORNER_TEXT] ?: true
        val cornerText = prefs[KEY_CORNER_TEXT] ?: "xppaicyber"
        val marketCurrencyFormat = prefs[KEY_MARKET_CURRENCY_FORMAT] ?: "suffix"
        val showHomeScreenNote = prefs[KEY_SHOW_HOME_NOTE] ?: true
        val homeNoteDismissed = prefs[KEY_HOME_NOTE_DISMISSED] ?: false
        val homeNoteIndex = prefs[KEY_HOME_NOTE_INDEX] ?: 0
        val showHomeNouns = prefs[KEY_SHOW_HOME_NOUNS] ?: true
        val nounsStatusFilter = prefs[KEY_NOUNS_STATUS_FILTER] ?: "All"
        val nounsDisplayMode = prefs[KEY_NOUNS_DISPLAY_MODE] ?: "auto_rotate"
        val nounsMaxDisplayCount = prefs[KEY_NOUNS_MAX_DISPLAY_COUNT] ?: 3
        val cachedNounsProposals = parseNounsProposalsJson(prefs[KEY_CACHED_NOUNS_PROPOSALS] ?: "")
        val cachedNounsTimestamp = prefs[KEY_CACHED_NOUNS_TIMESTAMP] ?: 0L
        val cachedNounsCandidates = parseNounsCandidatesJson(prefs[KEY_CACHED_NOUNS_CANDIDATES] ?: "")
        val cachedNounsCandidatesTimestamp = prefs[KEY_CACHED_NOUNS_CANDIDATES_TIMESTAMP] ?: 0L
        val cachedNounsAuction = parseNounsAuctionJson(prefs[KEY_CACHED_NOUNS_AUCTION] ?: "")
        val cachedNounsAuctionTimestamp = prefs[KEY_CACHED_NOUNS_AUCTION_TIMESTAMP] ?: 0L

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
            swipeDownAction = swipeDownAction,
            swipeDownToRefresh = swipeDownToRefresh,
            doubleTapLock = doubleTap,
            showRecentApps = showRecentApps,
            recentApps = recentApps,
            showWallpaper = showWallpaper,
            wallpaperDimLevel = wallpaperDimLevel,
            accentColor = accentColor,
            hapticFeedbackLevel = hapticFeedbackLevel,
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
            showDrawerUsdRate = showDrawerUsd,
            cachedUsdRate = cachedUsdRate,
            cachedUsdChange = cachedUsdChange,
            cachedUsdChangePercent = cachedUsdChangePercent,
            cachedUsdTimestamp = cachedUsdTimestamp,
            showHomeIdx = showHomeIdx,
            idxDisplayMode = idxDisplayMode,
            idxSymbols = idxSymbols,
            idxAutoRotateIntervalSeconds = idxAutoRotateInterval,
            marketCarouselAutoRotate = carouselAutoRotate,
            marketCarouselIntervalSeconds = carouselInterval,
            marketCarouselTransition = carouselTransition,
            separateMarketViews = separateMarketViews,
            separatedMarketLayout = separatedMarketLayout,
            cachedIdxQuotes = cachedIdxQuotes,
            cachedIdxCategories = cachedIdxCategories,
            cachedIdxTimestamp = cachedIdxTimestamp,
            cachedCryptoPrices = cachedMap,
            currentCryptoIndex = cryptoIndex,
            alphabetIndexOnRight = indexOnRight,
            enableIndexAnimation = indexAnimation,
            searchBarAtBottom = searchBarAtBottom,
            showCornerText = showCornerText,
            cornerText = cornerText,
            marketCurrencyFormat = marketCurrencyFormat,
            showHomeScreenNote = showHomeScreenNote,
            homeScreenNoteDismissed = homeNoteDismissed,
            homeScreenNoteIndex = homeNoteIndex,
            showHomeNouns = showHomeNouns,
            nounsStatusFilter = nounsStatusFilter,
            nounsDisplayMode = nounsDisplayMode,
            nounsMaxDisplayCount = nounsMaxDisplayCount,
            cachedNounsProposals = cachedNounsProposals,
            cachedNounsTimestamp = cachedNounsTimestamp,
            cachedNounsCandidates = cachedNounsCandidates,
            cachedNounsCandidatesTimestamp = cachedNounsCandidatesTimestamp,
            cachedNounsAuction = cachedNounsAuction,
            cachedNounsAuctionTimestamp = cachedNounsAuctionTimestamp
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

    suspend fun setSwipeDownAction(action: String) {
        context.dataStore.edit {
            it[KEY_SWIPE_DOWN_ACTION] = action
            it[KEY_SWIPE_DOWN_TO_REFRESH] = (action == "refresh")
        }
    }

    suspend fun setSwipeDownToRefresh(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_SWIPE_DOWN_TO_REFRESH] = enabled
            it[KEY_SWIPE_DOWN_ACTION] = if (enabled) "refresh" else "notifications"
        }
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

    suspend fun setShowWallpaper(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_WALLPAPER] = enabled }
    }

    suspend fun setWallpaperDimLevel(level: Float) {
        context.dataStore.edit { it[KEY_WALLPAPER_DIM_LEVEL] = level.coerceIn(0.2f, 0.98f) }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { it[KEY_ACCENT_COLOR] = color }
    }

    suspend fun setHapticFeedbackLevel(level: String) {
        context.dataStore.edit { it[KEY_HAPTIC_FEEDBACK_LEVEL] = level }
    }

    suspend fun setShowRecentApps(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_RECENT_APPS] = enabled }
    }

    suspend fun recordRecentApp(appKeyOrPackage: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_APPS]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val updated = (listOf(appKeyOrPackage) + current.filter { it != appKeyOrPackage }).take(8)
            prefs[KEY_RECENT_APPS] = updated.joinToString(",")
        }
    }

    suspend fun clearRecentApps() {
        context.dataStore.edit { it[KEY_RECENT_APPS] = "" }
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

    suspend fun setShowDrawerUsdRate(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_DRAWER_USD_RATE] = show }
    }

    suspend fun setShowHomeIdx(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_HOME_IDX] = show }
    }

    suspend fun setIdxDisplayMode(mode: String) {
        context.dataStore.edit { it[KEY_IDX_DISPLAY_MODE] = mode }
    }

    suspend fun setIdxSymbols(symbols: List<String>) {
        context.dataStore.edit { it[KEY_IDX_SYMBOLS] = symbols.joinToString(",") }
    }

    suspend fun setIdxAutoRotateInterval(seconds: Int) {
        context.dataStore.edit { it[KEY_IDX_AUTO_ROTATE_INTERVAL] = seconds }
    }

    suspend fun setMarketCarouselAutoRotate(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MARKET_CAROUSEL_AUTO_ROTATE] = enabled }
    }

    suspend fun setMarketCarouselInterval(seconds: Int) {
        context.dataStore.edit { it[KEY_MARKET_CAROUSEL_INTERVAL] = seconds }
    }

    suspend fun setMarketCarouselTransition(transition: String) {
        context.dataStore.edit { it[KEY_MARKET_CAROUSEL_TRANSITION] = transition }
    }

    suspend fun setSeparateMarketViews(separate: Boolean) {
        context.dataStore.edit { it[KEY_SEPARATE_MARKET_VIEWS] = separate }
    }

    suspend fun setSeparatedMarketLayout(layout: String) {
        context.dataStore.edit { it[KEY_SEPARATED_MARKET_LAYOUT] = layout }
    }

    suspend fun updateCachedIdxQuotes(quotes: Map<String, CachedIdxQuote>, timestamp: Long) {
        context.dataStore.edit { prefs ->
            val encoded = quotes.values.joinToString(";") {
                "${it.symbol}|${it.name}|${it.price}|${it.change}|${it.changePercent}|${it.volume}|${it.high}|${it.low}|${it.timestamp}"
            }
            prefs[KEY_CACHED_IDX_QUOTES] = encoded
            prefs[KEY_CACHED_IDX_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateCachedIdxCategories(categories: Map<String, List<CachedIdxQuote>>, timestamp: Long) {
        context.dataStore.edit { prefs ->
            val root = JSONObject()
            val flatQuotes = mutableMapOf<String, CachedIdxQuote>()
            categories.forEach { (cat, list) ->
                val arr = JSONArray()
                list.forEach { q ->
                    val obj = JSONObject()
                    obj.put("symbol", q.symbol)
                    obj.put("name", q.name)
                    obj.put("price", q.price)
                    obj.put("change", q.change)
                    obj.put("changePercent", q.changePercent)
                    obj.put("volume", q.volume)
                    obj.put("high", q.high)
                    obj.put("low", q.low)
                    obj.put("timestamp", q.timestamp)
                    arr.put(obj)
                    flatQuotes[q.symbol.uppercase(Locale.US)] = q
                }
                root.put(cat, arr)
            }
            prefs[KEY_CACHED_IDX_CATEGORIES] = root.toString()
            prefs[KEY_CACHED_IDX_TIMESTAMP] = timestamp

            if (flatQuotes.isNotEmpty()) {
                val encoded = flatQuotes.values.joinToString(";") {
                    "${it.symbol}|${it.name}|${it.price}|${it.change}|${it.changePercent}|${it.volume}|${it.high}|${it.low}|${it.timestamp}"
                }
                prefs[KEY_CACHED_IDX_QUOTES] = encoded
            }
        }
    }

    suspend fun updateCachedUsdRate(rate: String, change: String, changePercent: Double, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CACHED_USD_RATE] = rate
            prefs[KEY_CACHED_USD_CHANGE] = change
            prefs[KEY_CACHED_USD_CHANGE_PERCENT] = changePercent
            prefs[KEY_CACHED_USD_TIMESTAMP] = timestamp
        }
    }

    suspend fun setAlphabetIndexOnRight(onRight: Boolean) {
        context.dataStore.edit { it[KEY_INDEX_ON_RIGHT] = onRight }
    }

    suspend fun setIndexAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLE_INDEX_ANIMATION] = enabled }
    }

    suspend fun setSearchBarAtBottom(atBottom: Boolean) {
        context.dataStore.edit { it[KEY_SEARCH_BAR_AT_BOTTOM] = atBottom }
    }

    suspend fun setShowCornerText(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_CORNER_TEXT] = show }
    }

    suspend fun setCornerText(text: String) {
        context.dataStore.edit { it[KEY_CORNER_TEXT] = text }
    }

    suspend fun setMarketCurrencyFormat(format: String) {
        context.dataStore.edit { it[KEY_MARKET_CURRENCY_FORMAT] = format }
    }

    suspend fun setShowHomeScreenNote(show: Boolean) {
        context.dataStore.edit {
            it[KEY_SHOW_HOME_NOTE] = show
            if (show) {
                it[KEY_HOME_NOTE_DISMISSED] = false
            }
        }
    }

    suspend fun setHomeScreenNoteDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[KEY_HOME_NOTE_DISMISSED] = dismissed }
    }

    suspend fun setHomeScreenNoteIndex(index: Int) {
        context.dataStore.edit { it[KEY_HOME_NOTE_INDEX] = index }
    }

    suspend fun setShowHomeNouns(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_HOME_NOUNS] = show }
    }

    suspend fun setNounsStatusFilter(filter: String) {
        context.dataStore.edit { it[KEY_NOUNS_STATUS_FILTER] = filter }
    }

    suspend fun setNounsDisplayMode(mode: String) {
        context.dataStore.edit { it[KEY_NOUNS_DISPLAY_MODE] = mode }
    }

    suspend fun setNounsMaxDisplayCount(count: Int) {
        context.dataStore.edit { it[KEY_NOUNS_MAX_DISPLAY_COUNT] = count }
    }

    suspend fun updateCachedNounsCandidates(candidates: List<CachedNounsCandidate>, timestamp: Long) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            candidates.forEach { c ->
                val obj = JSONObject()
                obj.put("slug", c.slug)
                obj.put("title", c.title)
                obj.put("proposer", c.proposer)
                obj.put("timestamp", c.timestamp)
                arr.put(obj)
            }
            prefs[KEY_CACHED_NOUNS_CANDIDATES] = arr.toString()
            prefs[KEY_CACHED_NOUNS_CANDIDATES_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateCachedNounsProposals(proposals: List<CachedNounsProposal>, timestamp: Long) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            proposals.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("title", p.title)
                obj.put("statusLabel", p.statusLabel)
                obj.put("statusIndex", p.statusIndex)
                obj.put("proposer", p.proposer)
                obj.put("forVotes", p.forVotes)
                obj.put("againstVotes", p.againstVotes)
                obj.put("abstainVotes", p.abstainVotes)
                obj.put("quorum", p.quorum)
                obj.put("timestamp", p.timestamp)
                arr.put(obj)
            }
            prefs[KEY_CACHED_NOUNS_PROPOSALS] = arr.toString()
            prefs[KEY_CACHED_NOUNS_TIMESTAMP] = timestamp
        }
    }

    suspend fun updateCachedNounsAuction(auction: CachedNounsAuction?, timestamp: Long) {
        context.dataStore.edit { prefs ->
            if (auction != null) {
                val obj = JSONObject().apply {
                    put("nounId", auction.nounId)
                    put("amountWei", auction.amountWei)
                    put("amountEth", auction.amountEth)
                    put("startTime", auction.startTime)
                    put("endTime", auction.endTime)
                    put("bidder", auction.bidder)
                    put("settled", auction.settled)
                    put("timestamp", auction.timestamp)
                }
                prefs[KEY_CACHED_NOUNS_AUCTION] = obj.toString()
                prefs[KEY_CACHED_NOUNS_AUCTION_TIMESTAMP] = timestamp
            } else {
                prefs.remove(KEY_CACHED_NOUNS_AUCTION)
                prefs.remove(KEY_CACHED_NOUNS_AUCTION_TIMESTAMP)
            }
        }
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
        root.put("swipeDownAction", settings.swipeDownAction)
        root.put("swipeDownToRefresh", settings.swipeDownToRefresh)
        root.put("doubleTapLock", settings.doubleTapLock)

        root.put("cryptoPriceEnabled", settings.cryptoPriceEnabled)
        root.put("showDrawerUsdRate", settings.showDrawerUsdRate)
        root.put("cachedUsdRate", settings.cachedUsdRate)
        root.put("cachedUsdChange", settings.cachedUsdChange)
        root.put("cachedUsdChangePercent", settings.cachedUsdChangePercent)
        root.put("cachedUsdTimestamp", settings.cachedUsdTimestamp)
        root.put("showHomeIdx", settings.showHomeIdx)
        root.put("idxDisplayMode", settings.idxDisplayMode)
        root.put("idxSymbols", JSONArray(settings.idxSymbols))
        root.put("idxAutoRotateIntervalSeconds", settings.idxAutoRotateIntervalSeconds)
        root.put("marketCarouselAutoRotate", settings.marketCarouselAutoRotate)
        root.put("marketCarouselIntervalSeconds", settings.marketCarouselIntervalSeconds)
        root.put("marketCarouselTransition", settings.marketCarouselTransition)
        root.put("separateMarketViews", settings.separateMarketViews)
        root.put("separatedMarketLayout", settings.separatedMarketLayout)
        root.put("cryptoSymbols", JSONArray(settings.cryptoSymbols))
        root.put("cryptoQuoteCurrency", settings.cryptoQuoteCurrency)
        root.put("useWebSocketOnHome", settings.useWebSocketOnHome)

        root.put("isLightTheme", settings.isLightTheme)
        root.put("showWallpaper", settings.showWallpaper)
        root.put("wallpaperDimLevel", settings.wallpaperDimLevel.toDouble())
        root.put("accentColor", settings.accentColor)
        root.put("hapticFeedbackLevel", settings.hapticFeedbackLevel)
        root.put("showRecentApps", settings.showRecentApps)
        root.put("recentApps", JSONArray(settings.recentApps))
        root.put("useMonospace", settings.useMonospace)
        root.put("fontSize", settings.fontSize)
        root.put("showAppIcons", settings.showAppIcons)
        root.put("monochromeIcons", settings.monochromeIcons)
        root.put("alphabetIndexOnRight", settings.alphabetIndexOnRight)
        root.put("enableIndexAnimation", settings.enableIndexAnimation)
        root.put("searchBarAtBottom", settings.searchBarAtBottom)
        root.put("showCornerText", settings.showCornerText)
        root.put("cornerText", settings.cornerText)
        root.put("marketCurrencyFormat", settings.marketCurrencyFormat)
        root.put("showHomeScreenNote", settings.showHomeScreenNote)
        root.put("homeScreenNoteDismissed", settings.homeScreenNoteDismissed)
        root.put("homeScreenNoteIndex", settings.homeScreenNoteIndex)
        root.put("showHomeNouns", settings.showHomeNouns)
        root.put("nounsStatusFilter", settings.nounsStatusFilter)
        root.put("nounsDisplayMode", settings.nounsDisplayMode)
        root.put("nounsMaxDisplayCount", settings.nounsMaxDisplayCount)

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
                if (root.has("swipeDownAction")) {
                    val action = root.getString("swipeDownAction")
                    prefs[KEY_SWIPE_DOWN_ACTION] = action
                    prefs[KEY_SWIPE_DOWN_TO_REFRESH] = (action == "refresh")
                } else if (root.has("swipeDownToRefresh")) {
                    val isRefresh = root.getBoolean("swipeDownToRefresh")
                    prefs[KEY_SWIPE_DOWN_TO_REFRESH] = isRefresh
                    prefs[KEY_SWIPE_DOWN_ACTION] = if (isRefresh) "refresh" else "notifications"
                }
                if (root.has("doubleTapLock")) prefs[KEY_DOUBLE_TAP_LOCK] = root.getBoolean("doubleTapLock")

                if (root.has("cryptoPriceEnabled")) prefs[KEY_CRYPTO_ENABLED] = root.getBoolean("cryptoPriceEnabled")
                if (root.has("showDrawerUsdRate")) prefs[KEY_SHOW_DRAWER_USD_RATE] = root.getBoolean("showDrawerUsdRate")
                if (root.has("cachedUsdRate")) prefs[KEY_CACHED_USD_RATE] = root.getString("cachedUsdRate")
                if (root.has("cachedUsdChange")) prefs[KEY_CACHED_USD_CHANGE] = root.getString("cachedUsdChange")
                if (root.has("cachedUsdChangePercent")) prefs[KEY_CACHED_USD_CHANGE_PERCENT] = root.getDouble("cachedUsdChangePercent")
                if (root.has("cachedUsdTimestamp")) prefs[KEY_CACHED_USD_TIMESTAMP] = root.getLong("cachedUsdTimestamp")
                if (root.has("showHomeIdx")) prefs[KEY_SHOW_HOME_IDX] = root.getBoolean("showHomeIdx")
                if (root.has("idxDisplayMode")) prefs[KEY_IDX_DISPLAY_MODE] = root.getString("idxDisplayMode")
                if (root.has("idxSymbols")) {
                    val arr = root.getJSONArray("idxSymbols")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    prefs[KEY_IDX_SYMBOLS] = list.joinToString(",")
                }
                if (root.has("idxAutoRotateIntervalSeconds")) prefs[KEY_IDX_AUTO_ROTATE_INTERVAL] = root.getInt("idxAutoRotateIntervalSeconds")
                if (root.has("marketCarouselAutoRotate")) prefs[KEY_MARKET_CAROUSEL_AUTO_ROTATE] = root.getBoolean("marketCarouselAutoRotate")
                if (root.has("marketCarouselIntervalSeconds")) prefs[KEY_MARKET_CAROUSEL_INTERVAL] = root.getInt("marketCarouselIntervalSeconds")
                if (root.has("marketCarouselTransition")) prefs[KEY_MARKET_CAROUSEL_TRANSITION] = root.getString("marketCarouselTransition")
                if (root.has("separateMarketViews")) prefs[KEY_SEPARATE_MARKET_VIEWS] = root.getBoolean("separateMarketViews")
                if (root.has("separatedMarketLayout")) prefs[KEY_SEPARATED_MARKET_LAYOUT] = root.getString("separatedMarketLayout")
                if (root.has("cryptoSymbols")) {
                    val arr = root.getJSONArray("cryptoSymbols")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    prefs[KEY_CRYPTO_SYMBOLS] = list.joinToString(",")
                }
                if (root.has("cryptoQuoteCurrency")) prefs[KEY_CRYPTO_QUOTE] = root.getString("cryptoQuoteCurrency")
                if (root.has("useWebSocketOnHome")) prefs[KEY_USE_WEBSOCKET_ON_HOME] = root.getBoolean("useWebSocketOnHome")

                if (root.has("isLightTheme")) prefs[KEY_LIGHT_THEME] = root.getBoolean("isLightTheme")
                if (root.has("showWallpaper")) prefs[KEY_SHOW_WALLPAPER] = root.getBoolean("showWallpaper")
                if (root.has("wallpaperDimLevel")) prefs[KEY_WALLPAPER_DIM_LEVEL] = root.getDouble("wallpaperDimLevel").toFloat()
                if (root.has("accentColor")) prefs[KEY_ACCENT_COLOR] = root.getString("accentColor")
                if (root.has("hapticFeedbackLevel")) prefs[KEY_HAPTIC_FEEDBACK_LEVEL] = root.getString("hapticFeedbackLevel")
                if (root.has("showRecentApps")) prefs[KEY_SHOW_RECENT_APPS] = root.getBoolean("showRecentApps")
                if (root.has("recentApps")) {
                    val arr = root.getJSONArray("recentApps")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    prefs[KEY_RECENT_APPS] = list.joinToString(",")
                }
                if (root.has("useMonospace")) prefs[KEY_USE_MONOSPACE] = root.getBoolean("useMonospace")
                if (root.has("fontSize")) prefs[KEY_FONT_SIZE] = root.getString("fontSize")
                if (root.has("showAppIcons")) prefs[KEY_SHOW_APP_ICONS] = root.getBoolean("showAppIcons")
                if (root.has("monochromeIcons")) prefs[KEY_MONOCHROME_ICONS] = root.getBoolean("monochromeIcons")
                if (root.has("alphabetIndexOnRight")) prefs[KEY_INDEX_ON_RIGHT] = root.getBoolean("alphabetIndexOnRight")
                if (root.has("enableIndexAnimation")) prefs[KEY_ENABLE_INDEX_ANIMATION] = root.getBoolean("enableIndexAnimation")
                if (root.has("searchBarAtBottom")) prefs[KEY_SEARCH_BAR_AT_BOTTOM] = root.getBoolean("searchBarAtBottom")
                if (root.has("showCornerText")) prefs[KEY_SHOW_CORNER_TEXT] = root.getBoolean("showCornerText")
                if (root.has("cornerText")) prefs[KEY_CORNER_TEXT] = root.getString("cornerText")
                if (root.has("marketCurrencyFormat")) prefs[KEY_MARKET_CURRENCY_FORMAT] = root.getString("marketCurrencyFormat")
                if (root.has("showHomeScreenNote")) prefs[KEY_SHOW_HOME_NOTE] = root.getBoolean("showHomeScreenNote")
                if (root.has("homeScreenNoteDismissed")) prefs[KEY_HOME_NOTE_DISMISSED] = root.getBoolean("homeScreenNoteDismissed")
                if (root.has("homeScreenNoteIndex")) prefs[KEY_HOME_NOTE_INDEX] = root.getInt("homeScreenNoteIndex")
                if (root.has("showHomeNouns")) prefs[KEY_SHOW_HOME_NOUNS] = root.getBoolean("showHomeNouns")
                if (root.has("nounsStatusFilter")) prefs[KEY_NOUNS_STATUS_FILTER] = root.getString("nounsStatusFilter")
                if (root.has("nounsDisplayMode")) prefs[KEY_NOUNS_DISPLAY_MODE] = root.getString("nounsDisplayMode")
                if (root.has("nounsMaxDisplayCount")) prefs[KEY_NOUNS_MAX_DISPLAY_COUNT] = root.getInt("nounsMaxDisplayCount")
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

    private fun parseIdxMap(raw: String): Map<String, CachedIdxQuote> {
        if (raw.isBlank()) return emptyMap()
        return try {
            raw.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 9) {
                    parts[0] to CachedIdxQuote(
                        symbol = parts[0],
                        name = parts[1],
                        price = parts[2].toDoubleOrNull() ?: 0.0,
                        change = parts[3].toDoubleOrNull() ?: 0.0,
                        changePercent = parts[4].toDoubleOrNull() ?: 0.0,
                        volume = parts[5].toLongOrNull() ?: 0L,
                        high = parts[6].toDoubleOrNull() ?: 0.0,
                        low = parts[7].toDoubleOrNull() ?: 0.0,
                        timestamp = parts[8].toLongOrNull() ?: System.currentTimeMillis()
                    )
                } else null
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseIdxCategoriesJson(raw: String): Map<String, List<CachedIdxQuote>> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, List<CachedIdxQuote>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val cat = keys.next()
                val arr = root.getJSONArray(cat)
                val list = mutableListOf<CachedIdxQuote>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        CachedIdxQuote(
                            symbol = obj.optString("symbol"),
                            name = obj.optString("name"),
                            price = obj.optDouble("price", 0.0),
                            change = obj.optDouble("change", 0.0),
                            changePercent = obj.optDouble("changePercent", 0.0),
                            volume = obj.optLong("volume", 0L),
                            high = obj.optDouble("high", 0.0),
                            low = obj.optDouble("low", 0.0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                result[cat] = list
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun parseNounsProposalsJson(raw: String): List<CachedNounsProposal> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CachedNounsProposal>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CachedNounsProposal(
                        id = obj.optInt("id", 0),
                        title = obj.optString("title", ""),
                        statusLabel = obj.optString("statusLabel", "Unknown"),
                        statusIndex = obj.optInt("statusIndex", 0),
                        proposer = obj.optString("proposer", ""),
                        forVotes = obj.optInt("forVotes", 0),
                        againstVotes = obj.optInt("againstVotes", 0),
                        abstainVotes = obj.optInt("abstainVotes", 0),
                        quorum = obj.optInt("quorum", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNounsCandidatesJson(raw: String): List<CachedNounsCandidate> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CachedNounsCandidate>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CachedNounsCandidate(
                        slug = obj.optString("slug", ""),
                        title = obj.optString("title", ""),
                        proposer = obj.optString("proposer", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNounsAuctionJson(raw: String): CachedNounsAuction? {
        if (raw.isBlank()) return null
        return try {
            val obj = JSONObject(raw)
            CachedNounsAuction(
                nounId = obj.optInt("nounId", 0),
                amountWei = obj.optString("amountWei", "0"),
                amountEth = obj.optDouble("amountEth", 0.0),
                startTime = obj.optLong("startTime", 0L),
                endTime = obj.optLong("endTime", 0L),
                bidder = obj.optString("bidder", ""),
                settled = obj.optBoolean("settled", false),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }
}
