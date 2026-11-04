package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class NounsProposal(
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

data class NounsProposalUi(
    val id: Int,
    val title: String,
    val statusLabel: String,
    val statusIndex: Int,
    val proposer: String,
    val forVotes: Int,
    val againstVotes: Int,
    val abstainVotes: Int = 0,
    val quorum: Int = 0,
    val isStale: Boolean = false,
    val ageMarker: String = ""
)

class NounsRepository {

    private val baseUrl = "https://xpp.wtf/nouns"

    suspend fun fetchProposals(limit: Int = 10, status: String = "All"): Result<List<NounsProposal>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString = if (status.equals("All", ignoreCase = true)) {
                "$baseUrl/api.php?endpoint=proposals&limit=$limit"
            } else {
                "$baseUrl/api.php?endpoint=proposals&limit=$limit&status=$status"
            }
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(RuntimeException("HTTP ${connection.responseCode}"))
            }

            val jsonString = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val root = JSONObject(jsonString)
            if (!root.optBoolean("success", false)) {
                return@withContext Result.failure(RuntimeException(root.optString("error", "Unknown error")))
            }

            val dataArray = root.getJSONArray("data")
            val proposals = mutableListOf<NounsProposal>()
            val now = System.currentTimeMillis()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val stateObj = item.optJSONObject("state")
                val statusLabel = stateObj?.optString("label") ?: "Unknown"
                val statusIndex = stateObj?.optInt("index", 0) ?: 0
                val votesObj = item.optJSONObject("votes")
                val forVotes = votesObj?.optInt("for", 0) ?: 0
                val againstVotes = votesObj?.optInt("against", 0) ?: 0
                val abstainVotes = votesObj?.optInt("abstain", 0) ?: 0
                val quorum = votesObj?.optInt("quorum", 0) ?: 0

                proposals.add(
                    NounsProposal(
                        id = item.optInt("id", 0),
                        title = item.optString("title", "Proposal #${item.optInt("id", 0)}"),
                        statusLabel = statusLabel,
                        statusIndex = statusIndex,
                        proposer = item.optString("proposer", ""),
                        forVotes = forVotes,
                        againstVotes = againstVotes,
                        abstainVotes = abstainVotes,
                        quorum = quorum,
                        timestamp = now
                    )
                )
            }

            Result.success(proposals)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
