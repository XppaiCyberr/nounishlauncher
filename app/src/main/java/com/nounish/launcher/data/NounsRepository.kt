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

data class NounsCandidate(
    val slug: String,
    val title: String,
    val proposer: String,
    val statusLabel: String = "Candidate",
    val timestamp: Long = System.currentTimeMillis()
)

data class NounsCandidateUi(
    val slug: String,
    val title: String,
    val proposer: String,
    val statusLabel: String = "Candidate",
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

    suspend fun fetchCandidates(limit: Int = 10): Result<List<NounsCandidate>> = withContext(Dispatchers.IO) {
        // First try xpp.wtf endpoint in case server has it
        try {
            val url = URL("$baseUrl/api.php?endpoint=candidates&limit=$limit")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()
                val root = JSONObject(jsonStr)
                if (root.optBoolean("success", false)) {
                    val arr = root.optJSONArray("data")
                    if (arr != null && arr.length() > 0) {
                        val list = mutableListOf<NounsCandidate>()
                        val now = System.currentTimeMillis()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                NounsCandidate(
                                    slug = obj.optString("slug", ""),
                                    title = obj.optString("title", obj.optString("slug")),
                                    proposer = obj.optString("proposer", ""),
                                    timestamp = now
                                )
                            )
                        }
                        if (list.isNotEmpty()) {
                            return@withContext Result.success(list)
                        }
                    }
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {
            // Fall through to Alchemy RPC
        }

        // Direct Alchemy RPC fallback for candidates
        try {
            val candidates = fetchCandidatesViaRpc(limit)
            if (candidates.isNotEmpty()) {
                return@withContext Result.success(candidates)
            }
            Result.failure(RuntimeException("No candidates found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchCandidatesViaRpc(limit: Int): List<NounsCandidate> {
        val rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/K0Oex8ponc-8nl61tthZj"
        val candidateContract = "0xf790A5f59678dd733fb3De93493A91f472ca1365"
        val candidateCreatedTopic = "0x092e72bcc568a2ad800b1d7bbd7cc3f305e4a7b596092c69d10da00e38d705e6"

        val transferPayload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "alchemy_getAssetTransfers")
            val paramsObj = JSONObject().apply {
                put("toAddress", candidateContract)
                put("category", org.json.JSONArray(listOf("external")))
                put("excludeZeroValue", false)
                put("maxCount", "0x" + Integer.toHexString(limit * 2))
                put("order", "desc")
            }
            put("params", org.json.JSONArray(listOf(paramsObj)))
        }

        val transfersConn = (URL(rpcUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7_000
            readTimeout = 7_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "NounishLauncher/1.0")
        }
        transfersConn.outputStream.use { it.write(transferPayload.toString().toByteArray()) }

        if (transfersConn.responseCode != HttpURLConnection.HTTP_OK) {
            transfersConn.disconnect()
            return emptyList()
        }

        val transfersJson = BufferedReader(InputStreamReader(transfersConn.inputStream)).use { it.readText() }
        transfersConn.disconnect()

        val transfersRoot = JSONObject(transfersJson)
        val resultObj = transfersRoot.optJSONObject("result") ?: return emptyList()
        val transfersArr = resultObj.optJSONArray("transfers") ?: return emptyList()

        val hashes = mutableListOf<String>()
        for (i in 0 until minOf(transfersArr.length(), limit * 2)) {
            val h = transfersArr.getJSONObject(i).optString("hash")
            if (h.isNotBlank()) hashes.add(h)
        }
        if (hashes.isEmpty()) return emptyList()

        val batchArr = org.json.JSONArray()
        hashes.forEachIndexed { idx, h ->
            val callObj = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", idx + 1)
                put("method", "eth_getTransactionReceipt")
                put("params", org.json.JSONArray(listOf(h)))
            }
            batchArr.put(callObj)
        }

        val batchConn = (URL(rpcUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "NounishLauncher/1.0")
        }
        batchConn.outputStream.use { it.write(batchArr.toString().toByteArray()) }

        if (batchConn.responseCode != HttpURLConnection.HTTP_OK) {
            batchConn.disconnect()
            return emptyList()
        }

        val batchJson = BufferedReader(InputStreamReader(batchConn.inputStream)).use { it.readText() }
        batchConn.disconnect()

        val receiptsArr = org.json.JSONArray(batchJson)
        val candidates = mutableListOf<NounsCandidate>()
        val seenSlugs = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        for (i in 0 until receiptsArr.length()) {
            val rObj = receiptsArr.getJSONObject(i)
            val receipt = rObj.optJSONObject("result") ?: continue
            val logs = receipt.optJSONArray("logs") ?: continue

            for (j in 0 until logs.length()) {
                val log = logs.getJSONObject(j)
                val topics = log.optJSONArray("topics") ?: continue
                if (topics.length() < 2) continue
                val t0 = topics.optString(0)
                if (t0.equals(candidateCreatedTopic, ignoreCase = true)) {
                    val t1 = topics.optString(1)
                    val proposer = if (t1.length >= 26) "0x" + t1.substring(26).lowercase() else t1
                    val hexData = log.optString("data").removePrefix("0x")
                    if (hexData.length < 384) continue

                    try {
                        val raw = hexStringToByteArray(hexData)
                        val descOffset = readBigInt(raw, 128, 32)
                        val slugOffset = readBigInt(raw, 160, 32)

                        if (slugOffset + 32 <= raw.size) {
                            val slugLen = readBigInt(raw, slugOffset, 32)
                            val slugStart = slugOffset + 32
                            if (slugStart + slugLen <= raw.size && slugLen > 0) {
                                val slug = String(raw, slugStart, slugLen, Charsets.UTF_8).trim()
                                if (slug.isNotBlank() && !seenSlugs.contains(slug)) {
                                    seenSlugs.add(slug)

                                    var desc = ""
                                    if (descOffset + 32 <= raw.size) {
                                        val descLen = readBigInt(raw, descOffset, 32)
                                        val descStart = descOffset + 32
                                        if (descStart + descLen <= raw.size && descLen > 0) {
                                            desc = String(raw, descStart, descLen, Charsets.UTF_8)
                                        }
                                    }

                                    var title = slug
                                    for (line in desc.lines()) {
                                        val trimmed = line.trim().trimStart('#').trim()
                                        if (trimmed.isNotBlank()) {
                                            title = trimmed
                                            break
                                        }
                                    }

                                    candidates.add(
                                        NounsCandidate(
                                            slug = slug,
                                            title = title,
                                            proposer = proposer,
                                            timestamp = now
                                        )
                                    )
                                    if (candidates.size >= limit) return candidates
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        return candidates
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len - 1) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun readBigInt(bytes: ByteArray, offset: Int, length: Int): Int {
        var result = 0
        val start = (offset + length - 4).coerceAtLeast(offset)
        for (i in start until offset + length) {
            result = (result shl 8) or (bytes[i].toInt() and 0xFF)
        }
        return result
    }
}
