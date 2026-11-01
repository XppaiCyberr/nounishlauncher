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
    val formattedChange: String
)

data class PriceUiState(
    val coinSymbol: String = "",
    val priceText: String = "",
    val changeText: String = "",
    val changePercent: Double = 0.0,
    val isStale: Boolean = false,
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

    suspend fun fetchPrices(
        symbols: List<String>,
        quoteCurrency: String
    ): Result<Map<String, CoinTicker>> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext Result.success(emptyMap())

        val tradingPairs = symbols.map { "${it.uppercase()}${quoteCurrency.uppercase()}" }
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

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val pair = item.getString("symbol")
                        val priceRaw = item.optString("lastPrice", item.optString("price", "0")).toDoubleOrNull() ?: 0.0
                        val changePercentRaw = item.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0

                        val formattedPrice = formatPrice(priceRaw)
                        val formattedChange = formatPercent(changePercentRaw)

                        // Map back to base coin symbol
                        symbols.find { "${it.uppercase()}${quoteCurrency.uppercase()}" == pair }?.let { coin ->
                            priceMap[coin.uppercase()] = CoinTicker(
                                price = formattedPrice,
                                changePercent = changePercentRaw,
                                formattedChange = formattedChange
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
}
