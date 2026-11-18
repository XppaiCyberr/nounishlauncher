package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class LilNounsRepository {

    companion object {
        const val SUBGRAPH_URL =
            "https://api.goldsky.com/api/public/project_cldjvjgtylso13swq3dre13sf/subgraphs/lil-nouns-subgraph/1.0.10/gn"

        const val LIL_DAO_PROXY_CONTRACT = "0x5d2C31ce16924C2a71D317e5BbFd5ce387854039"

        val RPC_ENDPOINTS = listOf(
            "https://lilnouns-goldsky-rpc.lilnouns.workers.dev/",
            "https://eth-mainnet.g.alchemy.com/v2/tEAmLPls4-IajaZM2nyTIfG6CqK_uAb0",
            "https://cloudflare-eth.com",
            "https://ethereum-rpc.publicnode.com"
        )
    }

    suspend fun fetchProposals(
        limit: Int = 15,
        skip: Int = 0
    ): Result<List<NounsProposal>> = withContext(Dispatchers.IO) {
        // 1. Primary: Query Lil Nouns Subgraph on Goldsky
        try {
            val queryPayload = JSONObject().apply {
                put(
                    "query",
                    """
                    query GetLilProposals(${'$'}first: Int!, ${'$'}skip: Int!) {
                      proposals(first: ${'$'}first, skip: ${'$'}skip, orderBy: createdTimestamp, orderDirection: desc) {
                        id
                        title
                        status
                        forVotes
                        againstVotes
                        abstainVotes
                        quorumVotes
                        proposer {
                          id
                        }
                        createdTimestamp
                      }
                    }
                    """.trimIndent()
                )
                put("variables", JSONObject().apply {
                    put("first", limit.coerceIn(1, 30))
                    put("skip", skip.coerceAtLeast(0))
                })
            }

            val respStr = postJson(SUBGRAPH_URL, queryPayload.toString())
            if (!respStr.isNullOrBlank()) {
                val root = JSONObject(respStr)
                val data = root.optJSONObject("data")
                val proposalsArr = data?.optJSONArray("proposals")
                if (proposalsArr != null && proposalsArr.length() > 0) {
                    val now = System.currentTimeMillis()
                    val resultList = mutableListOf<NounsProposal>()

                    for (i in 0 until proposalsArr.length()) {
                        val item = proposalsArr.optJSONObject(i) ?: continue
                        val id = item.optString("id", "0").toIntOrNull() ?: continue
                        val rawTitle = item.optString("title", "").trim()
                        val title = if (rawTitle.isNotEmpty()) rawTitle else "Proposal #$id"
                        val statusRaw = item.optString("status", "ACTIVE").uppercase(Locale.US)
                        val statusLabel = formatStatusLabel(statusRaw)
                        val statusIndex = statusIndexForLabel(statusLabel)
                        val forVotes = item.optString("forVotes", "0").toIntOrNull() ?: 0
                        val againstVotes = item.optString("againstVotes", "0").toIntOrNull() ?: 0
                        val abstainVotes = item.optString("abstainVotes", "0").toIntOrNull() ?: 0
                        val quorum = item.optString("quorumVotes", "0").toIntOrNull() ?: 0
                        val proposer = item.optJSONObject("proposer")?.optString("id", "") ?: ""

                        resultList.add(
                            NounsProposal(
                                id = id,
                                title = title,
                                statusLabel = statusLabel,
                                statusIndex = statusIndex,
                                proposer = proposer,
                                forVotes = forVotes,
                                againstVotes = againstVotes,
                                abstainVotes = abstainVotes,
                                quorum = quorum,
                                timestamp = now
                            )
                        )
                    }

                    if (resultList.isNotEmpty()) {
                        return@withContext Result.success(resultList)
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: Query Lil Nouns DAO contract on Ethereum RPC
        for (rpcUrl in RPC_ENDPOINTS) {
            try {
                val proposals = fetchProposalsViaRpc(rpcUrl, limit, skip)
                if (proposals.isNotEmpty()) {
                    return@withContext Result.success(proposals)
                }
            } catch (_: Exception) {}
        }

        Result.failure(RuntimeException("Failed to fetch Lil Nouns proposals from Subgraph and RPC"))
    }

    private fun fetchProposalsViaRpc(rpcUrl: String, limit: Int, skip: Int): List<NounsProposal> {
        // Query proposalCount() -> 0xda35c664 on Lil Nouns Governor Proxy
        val countPayload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "eth_call")
            val callObj = JSONObject().apply {
                put("to", LIL_DAO_PROXY_CONTRACT)
                put("data", "0xda35c664")
            }
            put("params", JSONArray(listOf(callObj, "latest")))
        }

        val countRespStr = postJson(rpcUrl, countPayload.toString()) ?: return emptyList()
        val countRoot = JSONObject(countRespStr)
        val countHex = countRoot.optString("result", "").removePrefix("0x").trim()
        if (countHex.isEmpty()) return emptyList()

        val totalCount = BigInteger(countHex, 16).toInt()
        if (totalCount <= 0) return emptyList()

        val startId = totalCount - skip
        if (startId <= 0) return emptyList()
        val lowestId = (startId - limit + 1).coerceAtLeast(1)
        val ids = (startId downTo lowestId).toList()
        if (ids.isEmpty()) return emptyList()

        // Batch query state(id) -> 0x3e4f49e6
        val stateBatch = JSONArray()
        ids.forEachIndexed { idx, propId ->
            val paddedId = propId.toString(16).padStart(64, '0')
            stateBatch.put(JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", idx + 1)
                put("method", "eth_call")
                val sObj = JSONObject().apply {
                    put("to", LIL_DAO_PROXY_CONTRACT)
                    put("data", "0x3e4f49e6$paddedId")
                }
                put("params", JSONArray(listOf(sObj, "latest")))
            })
        }

        val batchRespStr = postJson(rpcUrl, stateBatch.toString()) ?: return emptyList()
        val batchArr = JSONArray(batchRespStr)
        val statesById = HashMap<Int, Int>()
        for (i in 0 until batchArr.length()) {
            val item = batchArr.optJSONObject(i) ?: continue
            val idIdx = item.optInt("id", 0) - 1
            if (idIdx in ids.indices) {
                val propId = ids[idIdx]
                val resHex = item.optString("result", "").removePrefix("0x").trim()
                if (resHex.isNotEmpty()) {
                    statesById[propId] = BigInteger(resHex, 16).toInt()
                }
            }
        }

        val now = System.currentTimeMillis()
        return ids.map { propId ->
            val stateIdx = statesById[propId] ?: 1
            val label = stateLabelForIndex(stateIdx)
            NounsProposal(
                id = propId,
                title = "Lil Nouns Proposal #$propId",
                statusLabel = label,
                statusIndex = stateIdx,
                proposer = "",
                forVotes = 0,
                againstVotes = 0,
                abstainVotes = 0,
                quorum = 0,
                timestamp = now
            )
        }
    }

    private fun formatStatusLabel(rawStatus: String): String {
        return when (rawStatus) {
            "ACTIVE" -> "Active"
            "EXECUTED" -> "Executed"
            "DEFEATED" -> "Defeated"
            "CANCELLED", "CANCELED" -> "Canceled"
            "VETOED" -> "Vetoed"
            "PENDING" -> "Pending"
            "SUCCEEDED" -> "Succeeded"
            "QUEUED" -> "Queued"
            else -> rawStatus.lowercase(Locale.US).replaceFirstChar { it.uppercase() }
        }
    }

    private fun statusIndexForLabel(label: String): Int {
        return when (label) {
            "Pending" -> 0
            "Active" -> 1
            "Canceled" -> 2
            "Defeated" -> 3
            "Succeeded" -> 4
            "Queued" -> 5
            "Vetoed" -> 6
            "Executed" -> 7
            else -> 1
        }
    }

    private fun stateLabelForIndex(index: Int): String {
        return when (index) {
            0 -> "Pending"
            1 -> "Active"
            2 -> "Canceled"
            3 -> "Defeated"
            4 -> "Succeeded"
            5 -> "Queued"
            6 -> "Vetoed"
            7 -> "Executed"
            else -> "Active"
        }
    }

    private fun postJson(urlStr: String, payloadJson: String, timeoutMs: Int = 8_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }
            conn.outputStream.use { it.write(payloadJson.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
