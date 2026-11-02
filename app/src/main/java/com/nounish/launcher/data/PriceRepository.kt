package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
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
    val timestamp: Long = System.currentTimeMillis()
)

data class PriceUiState(
    val coinSymbol: String = "",
    val priceText: String = "",
    val changeText: String = "",
    val changePercent: Double = 0.0,
    val isStale: Boolean = false,
    val ageMarker: String = "",
    val isLoading: Boolean = false
) {
    val isNotEmpty: Boolean
        get() = coinSymbol.isNotEmpty() && priceText.isNotEmpty()
}

class PriceRepository {

    private val hosts = listOf(
        "data-api.binance.vision",
        "api.binance.com",
        "api.binance.me"
    )

    private val commonQuotes = listOf(
        "USDT", "USDC", "BUSD", "BIDR", "IDR", "EUR", "TRY", "BRL", "GBP", "AUD", "BTC", "ETH", "BNB"
    )

    suspend fun fetchPrices(
        symbols: List<String>,
        quoteCurrency: String
    ): Result<Map<String, CoinTicker>> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext Result.success(emptyMap())

        val cleanQuote = quoteCurrency.trim().uppercase().ifEmpty { "USDT" }

        // Map original symbol to Binance pair
        // If symbol already ends with a known quote (e.g. BTCUSDT, ETHBIDR), use it directly
        val pairMap = symbols.associateWith { sym ->
            val clean = sym.trim().uppercase()
            if (commonQuotes.any { clean.endsWith(it) && clean.length > it.length }) {
                clean
            } else {
                "${clean}${cleanQuote}"
            }
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

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
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

                        // Find all original symbols that mapped to this trading pair
                        pairMap.filter { it.value == pair }.forEach { (origSym, _) ->
                            priceMap[origSym.trim().uppercase()] = CoinTicker(
                                price = formattedPrice,
                                changePercent = changePercentRaw,
                                formattedChange = formattedChange,
                                highPrice = formattedHigh,
                                lowPrice = formattedLow,
                                timestamp = now
                            )
                        }
                    }

                    return@withContext Result.success(priceMap)
                } else {
                    lastException = RuntimeException("HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastException ?: RuntimeException("Unknown network error"))
    }

    private fun formatPrice(price: Double): String {
        return if (price >= 100.0) {
            NumberFormat.getIntegerInstance(Locale.US).format(price.toLong())
        } else if (price >= 1.0) {
            String.format(Locale.US, "%.2f", price)
        } else {
            String.format(Locale.US, "%.4f", price)
        }
    }

    private fun formatPercent(percent: Double): String {
        val sign = if (percent > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", percent)}%"
    }

    companion object {
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
    }
}
