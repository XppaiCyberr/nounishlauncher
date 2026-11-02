package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

data class IdxQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long = 0L,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val prevClose: Double = 0.0,
    val currency: String = "IDR",
    val timestamp: Long = System.currentTimeMillis()
)

data class IdxItemUi(
    val symbol: String,
    val name: String,
    val priceText: String,
    val changeText: String,
    val changePercent: Double,
    val highPrice: String = "",
    val lowPrice: String = "",
    val volumeText: String = "",
    val isStale: Boolean = false,
    val ageMarker: String = ""
)

class IdxRepository {

    private val baseUrl = "https://idx.xpp.wtf"

    suspend fun fetchIdxData(
        displayMode: String,
        symbols: List<String>
    ): Result<List<IdxQuote>> = withContext(Dispatchers.IO) {
        try {
            when (displayMode.lowercase(Locale.US)) {
                "gainers" -> fetchTopList("gainers.json")
                "losers" -> fetchTopList("losers.json")
                "indices" -> fetchIndicesOnly()
                else -> fetchWatchlist(symbols)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchWatchlist(symbols: List<String>): Result<List<IdxQuote>> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext Result.success(emptyList())

        val quotesMap = fetchQuotesCache()
        val indicesMap = fetchIndicesCache()
        val results = mutableListOf<IdxQuote>()

        for (sym in symbols) {
            val clean = sym.trim().uppercase(Locale.US).removeSuffix(".JK")
            if (clean.isEmpty()) continue

            // Check indices first (IHSG, LQ45)
            if (indicesMap.containsKey(clean)) {
                indicesMap[clean]?.let { results.add(it) }
                continue
            }

            // Check static quotes cache
            if (quotesMap.containsKey(clean)) {
                quotesMap[clean]?.let { results.add(it) }
                continue
            }

            // Fallback to query API for un-cached stock symbol
            val single = fetchSingleQuote(clean)
            if (single != null) {
                results.add(single)
            }
        }

        Result.success(results)
    }

    private suspend fun fetchTopList(file: String): Result<List<IdxQuote>> = withContext(Dispatchers.IO) {
        val jsonString = httpGet("$baseUrl/cache/$file") ?: return@withContext Result.failure(RuntimeException("Failed to load $file"))
        val root = JSONObject(jsonString)
        if (!root.optBoolean("ok", false)) {
            return@withContext Result.failure(RuntimeException(root.optString("error", "Unknown error")))
        }

        val array = root.getJSONArray("data")
        val now = System.currentTimeMillis()
        val list = mutableListOf<IdxQuote>()

        val limit = minOf(array.length(), 6)
        for (i in 0 until limit) {
            val item = array.getJSONObject(i)
            list.add(parseQuote(item, now))
        }

        Result.success(list)
    }

    private suspend fun fetchIndicesOnly(): Result<List<IdxQuote>> = withContext(Dispatchers.IO) {
        val indicesMap = fetchIndicesCache()
        Result.success(indicesMap.values.toList())
    }

    private suspend fun fetchQuotesCache(): Map<String, IdxQuote> {
        val jsonString = httpGet("$baseUrl/cache/quotes.json") ?: return emptyMap()
        return try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("ok", false)) return emptyMap()
            val data = root.getJSONObject("data")
            val now = System.currentTimeMillis()
            val map = mutableMapOf<String, IdxQuote>()
            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = data.getJSONObject(key)
                map[key.uppercase(Locale.US)] = parseQuote(item, now)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchIndicesCache(): Map<String, IdxQuote> {
        val jsonString = httpGet("$baseUrl/cache/indices.json") ?: return emptyMap()
        return try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("ok", false)) return emptyMap()
            val data = root.getJSONObject("data")
            val now = System.currentTimeMillis()
            val map = mutableMapOf<String, IdxQuote>()
            val keys = data.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = data.getJSONObject(key)
                val name = item.optString("name", key)
                val value = item.optDouble("value", 0.0)
                val change = item.optDouble("change", 0.0)
                val changePercent = item.optDouble("changePercent", 0.0)
                map[key.uppercase(Locale.US)] = IdxQuote(
                    symbol = name,
                    name = name,
                    price = value,
                    change = change,
                    changePercent = changePercent,
                    timestamp = now
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun fetchSingleQuote(symbol: String): IdxQuote? {
        val jsonString = httpGet("$baseUrl/?cmd=quote&symbol=$symbol") ?: return null
        return try {
            val root = JSONObject(jsonString)
            if (!root.optBoolean("ok", false)) return null
            val item = root.getJSONObject("data")
            parseQuote(item, System.currentTimeMillis())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseQuote(item: JSONObject, now: Long): IdxQuote {
        return IdxQuote(
            symbol = item.optString("symbol"),
            name = item.optString("name"),
            price = item.optDouble("price", 0.0),
            change = item.optDouble("change", 0.0),
            changePercent = item.optDouble("changePercent", 0.0),
            volume = item.optLong("volume", 0L),
            high = item.optDouble("high", 0.0),
            low = item.optDouble("low", 0.0),
            open = item.optDouble("open", 0.0),
            prevClose = item.optDouble("prevClose", 0.0),
            currency = item.optString("currency", "IDR"),
            timestamp = now
        )
    }

    private fun httpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        fun formatPrice(symbol: String, price: Double): String {
            return if (symbol.equals("IHSG", ignoreCase = true) || symbol.equals("LQ45", ignoreCase = true)) {
                String.format(Locale.US, "%,.2f", price)
            } else {
                NumberFormat.getIntegerInstance(Locale.US).format(price.toLong())
            }
        }

        fun formatPercent(percent: Double): String {
            val sign = if (percent > 0) "+" else ""
            return "$sign${String.format(Locale.US, "%.2f", percent)}%"
        }

        fun formatVolume(vol: Long): String {
            return when {
                vol >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", vol / 1_000_000_000.0)
                vol >= 1_000_000 -> String.format(Locale.US, "%.1fM", vol / 1_000_000.0)
                vol >= 1_000 -> String.format(Locale.US, "%.1fK", vol / 1_000.0)
                vol > 0 -> vol.toString()
                else -> ""
            }
        }
    }
}
