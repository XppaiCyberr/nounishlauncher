package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

data class PriceUiState(
    val displayText: String = "",
    val isStale: Boolean = false,
    val isLoading: Boolean = false
)

class PriceRepository {

    private val hosts = listOf(
        "api.binance.com",
        "api.binance.me",
        "data-api.binance.vision"
    )

    suspend fun fetchPrices(
        symbols: List<String>,
        quoteCurrency: String
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext Result.success(emptyMap())

        val tradingPairs = symbols.map { "${it.uppercase()}${quoteCurrency.uppercase()}" }
        val symbolsJson = JSONArray(tradingPairs).toString()
        val encodedSymbols = URLEncoder.encode(symbolsJson, "UTF-8")

        var lastException: Exception? = null

        for (host in hosts) {
            val urlString = "https://$host/api/v3/ticker/price?symbols=$encodedSymbols"
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
                    val priceMap = mutableMapOf<String, String>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val pair = item.getString("symbol")
                        val priceRaw = item.getString("price").toDoubleOrNull() ?: 0.0
                        val formattedPrice = formatPrice(priceRaw)

                        // Map back to base coin symbol
                        symbols.find { "${it.uppercase()}${quoteCurrency.uppercase()}" == pair }?.let { coin ->
                            priceMap[coin.uppercase()] = formattedPrice
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
}
