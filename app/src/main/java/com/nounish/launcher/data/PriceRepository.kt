package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

data class CoinTicker(
    val price: String,
    val changePercent: Double,
    val formattedChange: String,
    val highPrice: String = "",
    val lowPrice: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sparkline: String = ""
)

data class PriceUiState(
    val coinSymbol: String = "",
    val priceText: String = "",
    val changeText: String = "",
    val changePercent: Double = 0.0,
    val isStale: Boolean = false,
    val ageMarker: String = "",
    val isLoading: Boolean = false,
    val sparkline: String = ""
) {
    val isNotEmpty: Boolean
        get() = coinSymbol.isNotEmpty() && priceText.isNotEmpty()
}

class RateLimitException(val retryAfterSeconds: Long = 60L) : RuntimeException("Rate limit reached (HTTP 429)")

class PriceRepository {

    private val hosts = listOf(
        "data-api.binance.vision",
        "api.binance.com",
        "api.binance.me"
    )

    private val commonQuotes = listOf(
        "USDT", "USDC", "BUSD", "BIDR", "IDR", "EUR", "TRY", "BRL", "GBP", "AUD", "BTC", "ETH", "BNB"
    )

    fun resolvePair(symbol: String, quoteCurrency: String): String {
        val clean = symbol.trim().uppercase(Locale.US)
        val cleanQuote = quoteCurrency.trim().uppercase(Locale.US).ifEmpty { "USDT" }
        return if (commonQuotes.any { clean.endsWith(it) && clean.length > it.length }) {
            clean
        } else {
            "${clean}${cleanQuote}"
        }
    }

    suspend fun fetchPrices(
        symbols: List<String>,
        quoteCurrency: String,
        cachedSparklines: Map<String, String> = emptyMap(),
        fetchSparklines: Boolean = false
    ): Result<Map<String, CoinTicker>> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext Result.success(emptyMap())

        // Map original symbol to Binance pair
        val pairMap = symbols.associateWith { sym ->
            resolvePair(sym, quoteCurrency)
        }

        val tradingPairs = pairMap.values.distinct()
        val symbolsJson = JSONArray(tradingPairs).toString()
        val encodedSymbols = URLEncoder.encode(symbolsJson, "UTF-8")

        var lastException: Exception? = null

        for (host in hosts) {
            val urlString = "https://$host/api/v3/ticker/24hr?symbols=$encodedSymbols"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "NounishLauncher/1.0")
                }

                val responseCode = connection.responseCode
                if (responseCode == 429 || responseCode == 418) {
                    val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 60L
                    throw RateLimitException(retryAfter)
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val priceMap = mutableMapOf<String, CoinTicker>()
                    val now = System.currentTimeMillis()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val pair = item.getString("symbol")
                        val priceRaw = item.optString("lastPrice", item.optString("price", "0")).toDoubleOrNull() ?: 0.0
                        val changePercentRaw = item.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0
                        val highRaw = item.optString("highPrice", "0").toDoubleOrNull() ?: 0.0
                        val lowRaw = item.optString("lowPrice", "0").toDoubleOrNull() ?: 0.0

                        val formattedPrice = formatPrice(priceRaw)
                        val formattedChange = formatPercent(changePercentRaw)
                        val formattedHigh = formatPrice(highRaw)
                        val formattedLow = formatPrice(lowRaw)

                        pairMap.filter { it.value == pair }.forEach { (origSym, _) ->
                            val cleanSym = origSym.trim().uppercase(Locale.US)
                            val sparkline = cachedSparklines[cleanSym] ?: ""
                            priceMap[cleanSym] = CoinTicker(
                                price = formattedPrice,
                                changePercent = changePercentRaw,
                                formattedChange = formattedChange,
                                highPrice = formattedHigh,
                                lowPrice = formattedLow,
                                timestamp = now,
                                sparkline = sparkline
                            )
                        }
                    }

                    // Optionally fetch 24h klines sparklines if requested
                    if (fetchSparklines) {
                        try {
                            coroutineScope {
                                val klineJobs = tradingPairs.map { pair ->
                                    async {
                                        pair to fetchKlines(pair)
                                    }
                                }.awaitAll()

                                for ((pair, klinePrices) in klineJobs) {
                                    val spark = generateSparkline(klinePrices)
                                    pairMap.filter { it.value == pair }.forEach { (origSym, _) ->
                                        val cleanSym = origSym.trim().uppercase(Locale.US)
                                        priceMap[cleanSym]?.let { current ->
                                            priceMap[cleanSym] = current.copy(sparkline = spark)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    return@withContext Result.success(priceMap)
                } else {
                    lastException = RuntimeException("HTTP $responseCode")
                }
            } catch (rle: RateLimitException) {
                return@withContext Result.failure(rle)
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastException ?: RuntimeException("Network error"))
    }

    suspend fun fetchKlines(pair: String): List<Double> = withContext(Dispatchers.IO) {
        val urlString = "https://data-api.binance.vision/api/v3/klines?symbol=$pair&interval=2h&limit=12"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val json = JSONArray(reader.use { it.readText() })
                val closes = mutableListOf<Double>()
                for (i in 0 until json.length()) {
                    val bar = json.getJSONArray(i)
                    val closePrice = bar.getString(4).toDoubleOrNull() ?: 0.0
                    closes.add(closePrice)
                }
                closes
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        fun formatPrice(price: Double): String {
            return if (price >= 100.0) {
                NumberFormat.getIntegerInstance(Locale.US).format(price.toLong())
            } else if (price >= 1.0) {
                String.format(Locale.US, "%.2f", price)
            } else {
                String.format(Locale.US, "%.4f", price)
            }
        }

        fun formatPercent(percent: Double): String {
            val sign = if (percent > 0) "+" else ""
            return "$sign${String.format(Locale.US, "%.2f", percent)}%"
        }

        fun formatAgeMarker(timestamp: Long): String {
            if (timestamp <= 0L) return ""
            val ageMs = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
            val mins = ageMs / (60 * 1000)
            return when {
                mins < 1 -> "·now"
                mins < 60 -> "·${mins}m"
                mins < 1440 -> "·${mins / 60}h"
                else -> "·${mins / 1440}d"
            }
        }

        /**
         * Generates an 8-level text sparkline from numerical price points:
         * Block characters: ' ', '▂', '▃', '▄', '▅', '▆', '▇', '█'
         */
        fun generateSparkline(prices: List<Double>): String {
            if (prices.isEmpty()) return ""
            val min = prices.minOrNull() ?: return ""
            val max = prices.maxOrNull() ?: return ""
            val blocks = charArrayOf(' ', '▂', '▃', '▄', '▅', '▆', '▇', '█')
            val range = max - min
            return prices.map { p ->
                if (range <= 0.0) {
                    '▄'
                } else {
                    val ratio = ((p - min) / range).coerceIn(0.0, 1.0)
                    val index = (ratio * (blocks.size - 1)).toInt().coerceIn(0, blocks.size - 1)
                    blocks[index]
                }
            }.joinToString("")
        }
    }
}
