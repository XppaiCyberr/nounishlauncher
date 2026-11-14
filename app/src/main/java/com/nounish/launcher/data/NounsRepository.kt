package com.nounish.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class NounsAuction(
    val nounId: Int,
    val amountWei: String,
    val amountEth: Double,
    val startTime: Long,
    val endTime: Long,
    val bidder: String,
    val settled: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class NounsAuctionUi(
    val nounId: Int,
    val amountWei: String,
    val amountEth: Double,
    val amountFormatted: String,
    val startTime: Long,
    val endTime: Long,
    val bidder: String,
    val bidderFormatted: String,
    val settled: Boolean,
    val timeLeftFormatted: String,
    val isStale: Boolean = false,
    val ageMarker: String = ""
)

data class NounsAuctionClient(
    val name: String,
    val urlTemplate: String
) {
    fun buildUrl(nounId: Int): String {
        return if (urlTemplate.contains("{id}")) {
            urlTemplate.replace("{id}", nounId.toString())
        } else {
            urlTemplate
        }
    }
}

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
    val proposerFormatted: String = "",
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
    val proposerFormatted: String = "",
    val statusLabel: String = "Candidate",
    val isStale: Boolean = false,
    val ageMarker: String = ""
)

data class RawProposalStruct(
    val id: Int,
    val proposer: String,
    val quorum: Int,
    val forVotes: Int,
    val againstVotes: Int,
    val abstainVotes: Int,
    val creationBlock: Long
)

class NounsRepository {

    suspend fun fetchProposals(limit: Int = 10, status: String = "All"): Result<List<NounsProposal>> = withContext(Dispatchers.IO) {
        val targetLimit = limit.coerceIn(1, 20)
        val isAllStatus = status.equals("All", ignoreCase = true)
        val batchWindow = if (isAllStatus) targetLimit else (targetLimit * 3).coerceIn(12, 30)

        for (rpcUrl in RPC_ENDPOINTS) {
            try {
                // 1. Query proposalCount() -> selector 0xda35c664 on DAO Proxy
                val countPayload = JSONObject().apply {
                    put("jsonrpc", "2.0")
                    put("id", 1)
                    put("method", "eth_call")
                    val callObj = JSONObject().apply {
                        put("to", DAO_PROXY_CONTRACT)
                        put("data", PROPOSAL_COUNT_SELECTOR)
                    }
                    put("params", org.json.JSONArray(listOf(callObj, "latest")))
                }

                val countRespStr = postJsonRpc(rpcUrl, countPayload.toString()) ?: continue
                val countRoot = JSONObject(countRespStr)
                val countHex = countRoot.optString("result", "").removePrefix("0x").trim()
                if (countHex.isEmpty() || countHex.startsWith("null")) continue

                val totalCount = BigInteger(countHex, 16).toInt()
                if (totalCount <= 0) continue

                val lowestId = (totalCount - batchWindow + 1).coerceAtLeast(1)
                val ids = (totalCount downTo lowestId).toList()

                // 2. Batch query proposals(id) [0x013cf08b] and state(id) [0x3e4f49e6]
                val structBatch = org.json.JSONArray()
                ids.forEachIndexed { idx, propId ->
                    val paddedId = propId.toString(16).padStart(64, '0')
                    structBatch.put(JSONObject().apply {
                        put("jsonrpc", "2.0")
                        put("id", idx * 2 + 1)
                        put("method", "eth_call")
                        val pObj = JSONObject().apply {
                            put("to", DAO_PROXY_CONTRACT)
                            put("data", PROPOSALS_SELECTOR + paddedId)
                        }
                        put("params", org.json.JSONArray(listOf(pObj, "latest")))
                    })
                    structBatch.put(JSONObject().apply {
                        put("jsonrpc", "2.0")
                        put("id", idx * 2 + 2)
                        put("method", "eth_call")
                        val sObj = JSONObject().apply {
                            put("to", DAO_PROXY_CONTRACT)
                            put("data", PROPOSAL_STATE_SELECTOR + paddedId)
                        }
                        put("params", org.json.JSONArray(listOf(sObj, "latest")))
                    })
                }

                val batchRespStr = postJsonRpc(rpcUrl, structBatch.toString()) ?: continue
                val batchArr = org.json.JSONArray(batchRespStr)
                val resultById = HashMap<Int, String>(batchArr.length())
                for (i in 0 until batchArr.length()) {
                    val item = batchArr.optJSONObject(i) ?: continue
                    val rpcId = item.optInt("id", -1)
                    val res = item.optString("result", "")
                    if (rpcId != -1 && res.isNotBlank() && res != "0x") {
                        resultById[rpcId] = res
                    }
                }

                data class PendingProposal(
                    val struct: RawProposalStruct,
                    val statusIndex: Int,
                    val statusLabel: String
                )

                val matchedProposals = mutableListOf<PendingProposal>()
                for (idx in ids.indices) {
                    val propId = ids[idx]
                    val structHex = resultById[idx * 2 + 1] ?: continue
                    val stateHex = resultById[idx * 2 + 2] ?: continue

                    val struct = decodeProposalStruct(propId, structHex) ?: continue
                    val stateClean = stateHex.removePrefix("0x").trim()
                    val stateIndex = if (stateClean.isNotEmpty()) BigInteger(stateClean, 16).toInt() else 0
                    val stateLabel = stateLabelForIndex(stateIndex)

                    if (isAllStatus || stateLabel.equals(status, ignoreCase = true)) {
                        matchedProposals.add(PendingProposal(struct, stateIndex, stateLabel))
                        if (matchedProposals.size >= targetLimit) break
                    }
                }

                if (matchedProposals.isEmpty() && isAllStatus) continue

                // 3. Fetch missing proposal titles from ProposalCreated event logs at exact creationBlock
                val missingTitles = matchedProposals.filter { !proposalTitleCache.containsKey(it.struct.id) && it.struct.creationBlock > 0L }
                if (missingTitles.isNotEmpty()) {
                    val logsBatch = org.json.JSONArray()
                    for (pending in missingTitles) {
                        val blockHex = "0x" + pending.struct.creationBlock.toString(16)
                        logsBatch.put(JSONObject().apply {
                            put("jsonrpc", "2.0")
                            put("id", pending.struct.id)
                            put("method", "eth_getLogs")
                            val filterObj = JSONObject().apply {
                                put("address", DAO_PROXY_CONTRACT)
                                put("fromBlock", blockHex)
                                put("toBlock", blockHex)
                                put("topics", org.json.JSONArray(listOf(PROPOSAL_CREATED_TOPIC)))
                            }
                            put("params", org.json.JSONArray(listOf(filterObj)))
                        })
                    }

                    // Try current RPC first, fallback to Alchemy if eth_getLogs is restricted on public node
                    val logsRespStr = postJsonRpc(rpcUrl, logsBatch.toString())
                        ?: if (rpcUrl != RPC_ENDPOINTS.first()) postJsonRpc(RPC_ENDPOINTS.first(), logsBatch.toString()) else null

                    if (logsRespStr != null && logsRespStr.trim().startsWith("[")) {
                        try {
                            val logsArr = org.json.JSONArray(logsRespStr)
                            for (i in 0 until logsArr.length()) {
                                val rpcItem = logsArr.optJSONObject(i) ?: continue
                                val propId = rpcItem.optInt("id", 0)
                                val logsResult = rpcItem.optJSONArray("result") ?: continue
                                for (j in 0 until logsResult.length()) {
                                    val logObj = logsResult.optJSONObject(j) ?: continue
                                    val dataHex = logObj.optString("data", "")
                                    val decoded = decodeProposalCreatedDescription(dataHex)
                                    if (decoded != null && (decoded.first == propId || logsResult.length() == 1)) {
                                        val title = extractProposalTitle(decoded.second, propId)
                                        proposalTitleCache[propId] = title
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val now = System.currentTimeMillis()
                val proposals = matchedProposals.map { pending ->
                    val id = pending.struct.id
                    NounsProposal(
                        id = id,
                        title = proposalTitleCache[id] ?: "Proposal #$id",
                        statusLabel = pending.statusLabel,
                        statusIndex = pending.statusIndex,
                        proposer = pending.struct.proposer,
                        forVotes = pending.struct.forVotes,
                        againstVotes = pending.struct.againstVotes,
                        abstainVotes = pending.struct.abstainVotes,
                        quorum = pending.struct.quorum,
                        timestamp = now
                    )
                }

                return@withContext Result.success(proposals)
            } catch (_: Exception) {
                // Try next RPC endpoint
            }
        }

        Result.failure(RuntimeException("Failed to fetch Nouns proposals from Ethereum RPC endpoints"))
    }

    suspend fun fetchCandidates(limit: Int = 10): Result<List<NounsCandidate>> = withContext(Dispatchers.IO) {
        try {
            val candidates = fetchCandidatesViaRpc(limit)
            if (candidates.isNotEmpty()) {
                return@withContext Result.success(candidates)
            }
            Result.failure(RuntimeException("No candidates found on-chain"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postJsonRpc(rpcUrl: String, payloadJson: String, timeoutMs: Int = 7_000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(rpcUrl).openConnection() as HttpURLConnection).apply {
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

    suspend fun fetchCurrentAuction(): Result<NounsAuction> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "eth_call")
            val paramsObj = JSONObject().apply {
                put("to", AUCTION_CONTRACT)
                put("data", AUCTION_METHOD_DATA)
            }
            put("params", org.json.JSONArray(listOf(paramsObj, "latest")))
        }.toString().toByteArray()

        for (rpcUrl in RPC_ENDPOINTS) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(rpcUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "NounishLauncher/1.0")
                }
                conn.outputStream.use { it.write(payload) }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val respJson = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val root = JSONObject(respJson)
                    val resultHex = root.optString("result", "")
                    if (resultHex.isNotBlank() && !resultHex.startsWith("0xnull") && resultHex != "0x") {
                        val auction = decodeAuction(resultHex)
                        if (auction != null) {
                            return@withContext Result.success(auction)
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next RPC endpoint in list
            } finally {
                conn?.disconnect()
            }
        }

        Result.failure(RuntimeException("Failed to fetch Nouns auction from all RPC endpoints"))
    }

    suspend fun resolveEnsNames(addresses: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        val cleanAddresses = addresses.map { it.lowercase(Locale.US).trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanAddresses.isEmpty()) return@withContext result

        val uncached = mutableListOf<String>()
        for (addr in cleanAddresses) {
            if (addr == "0x0000000000000000000000000000000000000000" || addr.length != 42) {
                result[addr] = ""
                ensCache[addr] = ""
            } else {
                val cached = ensCache[addr]
                if (cached != null) {
                    result[addr] = cached
                } else {
                    uncached.add(addr)
                }
            }
        }

        if (uncached.isEmpty()) {
            return@withContext result
        }

        // Call ReverseRecords getNames(address[])
        val offsetHex = "0000000000000000000000000000000000000000000000000000000000000020"
        val countHex = uncached.size.toString(16).padStart(64, '0')
        val addrsHex = StringBuilder()
        for (addr in uncached) {
            addrsHex.append(addr.removePrefix("0x").padStart(64, '0'))
        }
        val calldata = ENS_GET_NAMES_SELECTOR + offsetHex + countHex + addrsHex.toString()

        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", 1)
            put("method", "eth_call")
            val callObj = JSONObject().apply {
                put("to", REVERSE_RECORDS_CONTRACT)
                put("data", calldata)
            }
            put("params", org.json.JSONArray(listOf(callObj, "latest")))
        }.toString()

        for (rpcUrl in RPC_ENDPOINTS) {
            val respStr = postJsonRpc(rpcUrl, payload, timeoutMs = 5_000) ?: continue
            try {
                val root = JSONObject(respStr)
                val resultHex = root.optString("result", "")
                if (resultHex.isNotBlank() && !resultHex.startsWith("0xnull") && resultHex != "0x") {
                    val decodedNames = decodeEnsNames(resultHex)
                    for (i in uncached.indices) {
                        val addr = uncached[i]
                        val name = decodedNames.getOrNull(i) ?: ""
                        ensCache[addr] = name
                        result[addr] = name
                    }
                    return@withContext result
                }
            } catch (_: Exception) {
                // Try next RPC
            }
        }

        for (addr in uncached) {
            if (!result.containsKey(addr)) {
                result[addr] = ""
            }
        }
        result
    }

    companion object {
        const val DAO_PROXY_CONTRACT = "0x6f3E6272A167e8AcCb32072d08E0957F9c79223d"
        const val CANDIDATE_CONTRACT = "0xf790A5f59678dd733fb3De93493A91f472ca1365"
        const val AUCTION_CONTRACT = "0x830bd73e4184cef73443c15111a1df14e495c706"
        const val REVERSE_RECORDS_CONTRACT = "0x3671aE578E63FdF66ad4F3E12CC0c0d71Ac7510C"
        const val PROPOSAL_COUNT_SELECTOR = "0xda35c664"
        const val PROPOSALS_SELECTOR = "0x013cf08b"
        const val PROPOSAL_STATE_SELECTOR = "0x3e4f49e6"
        const val AUCTION_METHOD_DATA = "0x7d9f6db5"
        const val PROPOSAL_CREATED_TOPIC = "0x7d84a6263ae0d98d3329bd7b46bb4e8d6f98cd35a7adb45c274c8b7fd5ebd5e0"
        const val ENS_GET_NAMES_SELECTOR = "0xcbf8b66c"

        val STATE_LABELS = mapOf(
            0 to "Pending",
            1 to "Active",
            2 to "Canceled",
            3 to "Defeated",
            4 to "Succeeded",
            5 to "Queued",
            6 to "Expired",
            7 to "Executed",
            8 to "Vetoed",
            9 to "Objection Period",
            10 to "Updatable"
        )

        val RPC_ENDPOINTS = listOf(
            "https://eth-mainnet.g.alchemy.com/v2/K0Oex8ponc-8nl61tthZj",
            "https://ethereum.publicnode.com",
            "https://cloudflare-eth.com"
        )

        private val proposalTitleCache = java.util.concurrent.ConcurrentHashMap<Int, String>()
        private val ensCache = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun getCachedEns(address: String): String? {
            val clean = address.lowercase(Locale.US).trim()
            val name = ensCache[clean]
            return if (!name.isNullOrBlank()) name else null
        }

        fun cacheEnsName(address: String, ensName: String) {
            val clean = address.lowercase(Locale.US).trim()
            ensCache[clean] = ensName
        }

        fun decodeEnsNames(hex: String): List<String> {
            val clean = hex.removePrefix("0x").trim()
            if (clean.length < 128) return emptyList()
            val slots = clean.chunked(64)
            if (slots.size < 2) return emptyList()
            return try {
                val count = BigInteger(slots[1], 16).toInt()
                val names = ArrayList<String>(count)
                for (i in 0 until count) {
                    if (2 + i >= slots.size) break
                    val offset = BigInteger(slots[2 + i], 16).toInt()
                    val lenSlot = 2 + (offset / 32)
                    if (lenSlot >= slots.size) {
                        names.add("")
                        continue
                    }
                    val strLen = BigInteger(slots[lenSlot], 16).toInt()
                    if (strLen <= 0) {
                        names.add("")
                        continue
                    }
                    val numDataSlots = (strLen + 31) / 32
                    val sb = StringBuilder()
                    for (s in 1..numDataSlots) {
                        if (lenSlot + s < slots.size) {
                            sb.append(slots[lenSlot + s])
                        }
                    }
                    val dataHex = sb.toString()
                    val byteLen = strLen.coerceAtMost(dataHex.length / 2)
                    val bytes = ByteArray(byteLen)
                    var b = 0
                    while (b < byteLen) {
                        bytes[b] = ((Character.digit(dataHex[b * 2], 16) shl 4) + Character.digit(dataHex[b * 2 + 1], 16)).toByte()
                        b++
                    }
                    names.add(String(bytes, Charsets.UTF_8))
                }
                names
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun cacheProposalTitle(id: Int, title: String) {
            if (id > 0 && title.isNotBlank() && title != "Proposal #$id") {
                proposalTitleCache[id] = title
            }
        }

        fun stateLabelForIndex(stateIndex: Int): String {
            return STATE_LABELS[stateIndex] ?: "State $stateIndex"
        }

        fun decodeProposalStruct(id: Int, hex: String): RawProposalStruct? {
            val clean = hex.removePrefix("0x").trim()
            if (clean.length < 15 * 64) return null
            return try {
                val proposer = "0x" + clean.substring(64 + 24, 128).lowercase(Locale.US)
                val quorum = BigInteger(clean.substring(3 * 64, 4 * 64), 16).toInt()
                val forVotes = BigInteger(clean.substring(7 * 64, 8 * 64), 16).toInt()
                val againstVotes = BigInteger(clean.substring(8 * 64, 9 * 64), 16).toInt()
                val abstainVotes = BigInteger(clean.substring(9 * 64, 10 * 64), 16).toInt()
                val creationBlock = BigInteger(clean.substring(14 * 64, 15 * 64), 16).toLong()
                RawProposalStruct(
                    id = id,
                    proposer = proposer,
                    quorum = quorum,
                    forVotes = forVotes,
                    againstVotes = againstVotes,
                    abstainVotes = abstainVotes,
                    creationBlock = creationBlock
                )
            } catch (_: Exception) {
                null
            }
        }

        fun decodeProposalCreatedDescription(logDataHex: String): Pair<Int, String>? {
            val clean = logDataHex.removePrefix("0x").trim()
            if (clean.length < 9 * 64) return null
            return try {
                val id = BigInteger(clean.substring(0, 64), 16).toInt()
                val descByteOffset = BigInteger(clean.substring(8 * 64, 9 * 64), 16).toInt()
                val descCharOffset = descByteOffset * 2
                if (descCharOffset + 64 > clean.length) return id to ""
                val descByteLength = BigInteger(clean.substring(descCharOffset, descCharOffset + 64), 16).toInt()
                val descDataStart = descCharOffset + 64
                val descDataEnd = (descDataStart + descByteLength * 2).coerceAtMost(clean.length)
                if (descByteLength <= 0 || descDataStart >= descDataEnd) return id to ""
                val descHex = clean.substring(descDataStart, descDataEnd)
                val bytes = ByteArray(descHex.length / 2)
                var i = 0
                while (i < descHex.length - 1) {
                    bytes[i / 2] = ((Character.digit(descHex[i], 16) shl 4) + Character.digit(descHex[i + 1], 16)).toByte()
                    i += 2
                }
                val description = String(bytes, Charsets.UTF_8)
                id to description
            } catch (_: Exception) {
                null
            }
        }

        fun extractProposalTitle(description: String, id: Int): String {
            if (description.isBlank()) return "Proposal #$id"
            for (line in description.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val cleaned = trimmed.replace(Regex("^#+\\s*"), "").trim()
                    if (cleaned.isNotEmpty()) {
                        return if (cleaned.length > 140) cleaned.take(139) + "…" else cleaned
                    }
                }
            }
            return "Proposal #$id"
        }

        fun decodeAuction(hex: String, now: Long = System.currentTimeMillis()): NounsAuction? {
            val cleanHex = hex.removePrefix("0x").trim()
            if (cleanHex.length < 384) return null
            return try {
                val nounId = BigInteger(cleanHex.substring(0, 64), 16).toInt()
                val amountBigInt = BigInteger(cleanHex.substring(64, 128), 16)
                val amountEth = if (amountBigInt == BigInteger.ZERO) {
                    0.0
                } else {
                    BigDecimal(amountBigInt)
                        .divide(BigDecimal("1000000000000000000"), 4, RoundingMode.HALF_UP)
                        .toDouble()
                }
                val startTime = BigInteger(cleanHex.substring(128, 192), 16).toLong()
                val endTime = BigInteger(cleanHex.substring(192, 256), 16).toLong()
                val bidderWord = cleanHex.substring(256, 320)
                val bidder = "0x" + bidderWord.takeLast(40).lowercase(Locale.US)
                val settled = BigInteger(cleanHex.substring(320, 384), 16).toInt() != 0

                NounsAuction(
                    nounId = nounId,
                    amountWei = amountBigInt.toString(),
                    amountEth = amountEth,
                    startTime = startTime,
                    endTime = endTime,
                    bidder = bidder,
                    settled = settled,
                    timestamp = now
                )
            } catch (_: Exception) {
                null
            }
        }

        fun formatAmount(amountEth: Double): String {
            return if (amountEth <= 0.0) {
                "0.00 ETH"
            } else {
                String.format(Locale.US, "%.2f ETH", amountEth)
            }
        }

        fun formatBidder(bidder: String, ensName: String? = null): String {
            if (bidder.isBlank() || bidder.equals("0x0000000000000000000000000000000000000000", ignoreCase = true)) {
                return "No bids"
            }
            if (!ensName.isNullOrBlank()) {
                return ensName
            }
            return if (bidder.length >= 10) {
                "${bidder.take(6)}...${bidder.takeLast(4)}"
            } else {
                bidder
            }
        }

        fun formatAddress(address: String, ensName: String? = null): String {
            if (address.isBlank() || address.equals("0x0000000000000000000000000000000000000000", ignoreCase = true)) {
                return ""
            }
            if (!ensName.isNullOrBlank()) {
                return ensName
            }
            return if (address.length >= 10) {
                "${address.take(6)}...${address.takeLast(4)}"
            } else {
                address
            }
        }

        fun formatTimeLeft(endTimeSeconds: Long, settled: Boolean, nowMillis: Long = System.currentTimeMillis()): String {
            if (settled) return "Settled"
            val nowSeconds = nowMillis / 1000L
            val diff = endTimeSeconds - nowSeconds
            if (diff <= 0L) return "Auction ended"

            val hours = diff / 3600L
            val minutes = (diff % 3600L) / 60L
            val seconds = diff % 60L

            return when {
                hours > 0L -> "${hours}h ${minutes}m left"
                minutes > 0L -> "${minutes}m ${seconds}s left"
                else -> "${seconds}s left"
            }
        }

        fun toUiModel(
            auction: CachedNounsAuction,
            isStale: Boolean = false,
            ageMarker: String = "",
            nowMillis: Long = System.currentTimeMillis(),
            ensName: String? = null
        ): NounsAuctionUi {
            val resolvedEns = ensName ?: getCachedEns(auction.bidder)
            return NounsAuctionUi(
                nounId = auction.nounId,
                amountWei = auction.amountWei,
                amountEth = auction.amountEth,
                amountFormatted = formatAmount(auction.amountEth),
                startTime = auction.startTime,
                endTime = auction.endTime,
                bidder = auction.bidder,
                bidderFormatted = formatBidder(auction.bidder, resolvedEns),
                settled = auction.settled,
                timeLeftFormatted = formatTimeLeft(auction.endTime, auction.settled, nowMillis),
                isStale = isStale,
                ageMarker = ageMarker
            )
        }

        fun toUiModel(
            auction: NounsAuction,
            isStale: Boolean = false,
            ageMarker: String = "",
            nowMillis: Long = System.currentTimeMillis(),
            ensName: String? = null
        ): NounsAuctionUi {
            val resolvedEns = ensName ?: getCachedEns(auction.bidder)
            return NounsAuctionUi(
                nounId = auction.nounId,
                amountWei = auction.amountWei,
                amountEth = auction.amountEth,
                amountFormatted = formatAmount(auction.amountEth),
                startTime = auction.startTime,
                endTime = auction.endTime,
                bidder = auction.bidder,
                bidderFormatted = formatBidder(auction.bidder, resolvedEns),
                settled = auction.settled,
                timeLeftFormatted = formatTimeLeft(auction.endTime, auction.settled, nowMillis),
                isStale = isStale,
                ageMarker = ageMarker
            )
        }

        val AUCTION_CLIENTS = listOf(
            NounsAuctionClient("nouns.wtf", "https://nouns.wtf"),
            NounsAuctionClient("noun.wtf", "https://noun.wtf/noun/{id}"),
            NounsAuctionClient("nouns.game", "https://nouns.game/auction")
        )

        fun getRandomAuctionClient(): NounsAuctionClient {
            return AUCTION_CLIENTS.random()
        }
    }
}
