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

data class LilDigestEvent(
    val id: String,
    val kind: String,
    val proposalId: String = "",
    val proposalTitle: String = "",
    val actor: String = "",
    val counterpart: String = "",
    val undelegated: Boolean = false,
    val nounIds: List<String> = emptyList(),
    val support: Int? = null,
    val voteWeight: Int? = null,
    val timestamp: Long = 0L,
    val href: String = "",
    val endsAt: Long? = null,
    val nounIdsCount: Int = 0
)

data class LilDigestPage(
    val events: List<LilDigestEvent>,
    val nextCursor: String? = null
)

data class LilDigestEventUi(
    val id: String,
    val kind: String,
    val proposalId: String,
    val proposalTitle: String,
    val actor: String,
    val actorFormatted: String,
    val counterpart: String = "",
    val counterpartFormatted: String = "",
    val undelegated: Boolean = false,
    val nounIds: List<String> = emptyList(),
    val support: Int?,
    val voteWeight: Int?,
    val timestamp: Long,
    val timeAgoFormatted: String,
    val href: String,
    val endsAt: Long?,
    val nounIdsCount: Int
)

class LilNounsRepository {

    companion object {
        const val SUBGRAPH_URL =
            "https://api.goldsky.com/api/public/project_cldjvjgtylso13swq3dre13sf/subgraphs/lil-nouns-subgraph/1.0.10/gn"

        const val DIGEST_API_URL = "https://db.lilnouns.wtf/api/digest"

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

    suspend fun fetchDigestPage(
        limit: Int = 30,
        cursor: String? = null
    ): Result<LilDigestPage> = withContext(Dispatchers.IO) {
        // 1. Primary: Lil Nouns Digest REST API (https://db.lilnouns.wtf/api/digest)
        try {
            val urlBuilder = StringBuilder(DIGEST_API_URL).append("?limit=").append(limit.coerceIn(5, 50))
            if (!cursor.isNullOrBlank()) {
                urlBuilder.append("&cursor=").append(cursor)
            }
            val respStr = getJson(urlBuilder.toString())
            if (!respStr.isNullOrBlank()) {
                val root = JSONObject(respStr)
                val eventsArr = root.optJSONArray("events")
                val nextCursor = root.optString("nextCursor", "").takeIf { it.isNotBlank() }
                if (eventsArr != null) {
                    val list = mutableListOf<LilDigestEvent>()
                    for (i in 0 until eventsArr.length()) {
                        val item = eventsArr.optJSONObject(i) ?: continue
                        val id = item.optString("id", "")
                        val kind = item.optString("kind", "event")
                        val proposalId = item.optString("proposalId", "")
                        val proposalTitle = item.optString("proposalTitle", "")
                        val actor = item.optString("actor", "")
                        val counterpart = item.optString("counterpart", "")
                        val undelegated = item.optBoolean("undelegated", false)
                        val support = if (item.has("support")) item.optInt("support") else null
                        val voteWeight = if (item.has("voteWeight")) item.optInt("voteWeight") else null
                        val timestamp = item.optLong("timestamp", 0L)
                        val href = item.optString("href", "")
                        val endsAt = if (item.has("endsAt")) item.optLong("endsAt") else null
                        val nounIdsJson = item.optJSONArray("nounIds")
                        val nounIds = mutableListOf<String>()
                        if (nounIdsJson != null) {
                            for (n in 0 until nounIdsJson.length()) {
                                val nId = nounIdsJson.optString(n, "").trim()
                                if (nId.isNotEmpty()) nounIds.add(nId)
                            }
                        }
                        val nounIdsCount = if (nounIds.isNotEmpty()) nounIds.size else item.optInt("nounIdsCount", 0)

                        if (id.isNotEmpty()) {
                            list.add(
                                LilDigestEvent(
                                    id = id,
                                    kind = kind,
                                    proposalId = proposalId,
                                    proposalTitle = proposalTitle,
                                    actor = actor,
                                    counterpart = counterpart,
                                    undelegated = undelegated,
                                    nounIds = nounIds,
                                    support = support,
                                    voteWeight = voteWeight,
                                    timestamp = timestamp,
                                    href = href,
                                    endsAt = endsAt,
                                    nounIdsCount = nounIdsCount
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) {
                        return@withContext Result.success(LilDigestPage(events = list, nextCursor = nextCursor))
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: Goldsky Subgraph GetDigestCore
        try {
            val queryPayload = JSONObject().apply {
                put(
                    "query",
                    """
                    query GetDigestCore(${'$'}first: Int!) {
                      votes(first: ${'$'}first, orderBy: blockTimestamp, orderDirection: desc) {
                        id
                        voter { id }
                        supportDetailed
                        votes
                        blockTimestamp
                        proposal { id title }
                      }
                      proposals(first: ${'$'}first, orderBy: createdTimestamp, orderDirection: desc) {
                        id
                        title
                        proposer { id }
                        createdTimestamp
                      }
                    }
                    """.trimIndent()
                )
                put("variables", JSONObject().apply {
                    put("first", limit.coerceIn(5, 30))
                })
            }
            val respStr = postJson(SUBGRAPH_URL, queryPayload.toString())
            if (!respStr.isNullOrBlank()) {
                val root = JSONObject(respStr)
                val data = root.optJSONObject("data")
                val list = mutableListOf<LilDigestEvent>()
                val votesArr = data?.optJSONArray("votes")
                if (votesArr != null) {
                    for (i in 0 until votesArr.length()) {
                        val item = votesArr.optJSONObject(i) ?: continue
                        val vId = item.optString("id", "")
                        val voter = item.optJSONObject("voter")?.optString("id", "") ?: ""
                        val support = item.optInt("supportDetailed", 1)
                        val votes = item.optString("votes", "0").toIntOrNull() ?: 0
                        val ts = item.optLong("blockTimestamp", 0L)
                        val propObj = item.optJSONObject("proposal")
                        val pId = propObj?.optString("id", "") ?: ""
                        val pTitle = propObj?.optString("title", "") ?: ""

                        list.add(
                            LilDigestEvent(
                                id = "vote-$vId",
                                kind = "vote",
                                proposalId = pId,
                                proposalTitle = pTitle,
                                actor = voter,
                                support = support,
                                voteWeight = votes,
                                timestamp = ts,
                                href = "/vote/$pId"
                            )
                        )
                    }
                }
                val propsArr = data?.optJSONArray("proposals")
                if (propsArr != null) {
                    for (i in 0 until propsArr.length()) {
                        val item = propsArr.optJSONObject(i) ?: continue
                        val pId = item.optString("id", "")
                        val title = item.optString("title", "")
                        val proposer = item.optJSONObject("proposer")?.optString("id", "") ?: ""
                        val ts = item.optLong("createdTimestamp", 0L)

                        list.add(
                            LilDigestEvent(
                                id = "proposal-created-$pId",
                                kind = "proposal-created",
                                proposalId = pId,
                                proposalTitle = title,
                                actor = proposer,
                                timestamp = ts,
                                href = "/vote/$pId"
                            )
                        )
                    }
                }
                list.sortByDescending { it.timestamp }
                if (list.isNotEmpty()) {
                    return@withContext Result.success(LilDigestPage(events = list, nextCursor = null))
                }
            }
        } catch (_: Exception) {}

        Result.failure(RuntimeException("Failed to fetch Lil Nouns digest"))
    }

    private fun getJson(urlStr: String, timeoutMs: Int = 8_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NounishLauncher/1.0")
            }
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
