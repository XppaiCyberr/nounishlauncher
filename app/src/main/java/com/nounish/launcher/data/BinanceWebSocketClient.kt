package com.nounish.launcher.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class BinanceWebSocketClient(
    private val scope: CoroutineScope,
    private val pairs: List<String>,
    private val onTickerUpdate: (symbol: String, price: String, changePercent: Double, changeText: String, high: String, low: String) -> Unit,
    private val onError: (Throwable) -> Unit
) {

    private var socket: Socket? = null
    private var job: Job? = null
    private val secureRandom = SecureRandom()

    @Volatile
    private var isRunning = false

    fun start() {
        if (pairs.isEmpty() || isRunning) return
        isRunning = true

        job = scope.launch(Dispatchers.IO) {
            var currentSocket: Socket? = null
            try {
                val streamNames = pairs.map { "${it.lowercase(Locale.US)}@ticker" }.joinToString("/")
                val host = "data-stream.binance.vision"
                val ports = listOf(443, 9443)

                var connected = false
                for (port in ports) {
                    if (!isActive || !isRunning) break
                    try {
                        val plainSocket = Socket()
                        plainSocket.connect(InetSocketAddress(host, port), 5000)
                        plainSocket.soTimeout = 30000 // 30 sec read timeout

                        val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                            .createSocket(plainSocket, host, port, true) as SSLSocket
                        sslSocket.startHandshake()
                        currentSocket = sslSocket
                        connected = true
                        break
                    } catch (_: Exception) {
                        currentSocket?.close()
                        currentSocket = null
                    }
                }

                if (!connected || currentSocket == null) {
                    onError(RuntimeException("Failed to connect to Binance WebSocket"))
                    return@launch
                }

                socket = currentSocket
                val outStream = currentSocket.getOutputStream()
                val inStream = BufferedInputStream(currentSocket.getInputStream())

                // Perform WebSocket Upgrade Handshake
                val nonce = ByteArray(16)
                secureRandom.nextBytes(nonce)
                val secKey = Base64.getEncoder().encodeToString(nonce)

                val request = "GET /stream?streams=$streamNames HTTP/1.1\r\n" +
                    "Host: $host\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Key: $secKey\r\n" +
                    "Sec-WebSocket-Version: 13\r\n\r\n"

                outStream.write(request.toByteArray(Charsets.UTF_8))
                outStream.flush()

                // Read Handshake Response Header
                val statusLine = readLine(inStream)
                if (!statusLine.contains("101")) {
                    onError(RuntimeException("WebSocket upgrade failed: $statusLine"))
                    return@launch
                }

                while (true) {
                    val header = readLine(inStream)
                    if (header.isEmpty() || header == "\r") break
                }

                // WebSocket Frame Read Loop
                while (isActive && isRunning) {
                    val b0 = inStream.read()
                    if (b0 == -1) break
                    val b1 = inStream.read()
                    if (b1 == -1) break

                    val opcode = b0 and 0x0F
                    var payloadLen = (b1 and 0x7F).toLong()

                    if (payloadLen == 126L) {
                        val byte1 = inStream.read()
                        val byte2 = inStream.read()
                        if (byte1 == -1 || byte2 == -1) break
                        payloadLen = ((byte1 shl 8) or byte2).toLong()
                    } else if (payloadLen == 127L) {
                        var len = 0L
                        for (i in 0 until 8) {
                            val b = inStream.read()
                            if (b == -1) break
                            len = (len shl 8) or (b.toLong() and 0xFFL)
                        }
                        payloadLen = len
                    }

                    if (payloadLen > 1024 * 1024) break // sanity check

                    val payloadBytes = ByteArray(payloadLen.toInt())
                    var totalRead = 0
                    while (totalRead < payloadBytes.size) {
                        val read = inStream.read(payloadBytes, totalRead, payloadBytes.size - totalRead)
                        if (read == -1) break
                        totalRead += read
                    }

                    when (opcode) {
                        0x1 -> { // Text frame
                            val jsonString = String(payloadBytes, Charsets.UTF_8)
                            handleMessage(jsonString)
                        }
                        0x9 -> { // Ping frame: respond with Pong
                            sendPong(outStream)
                        }
                        0x8 -> { // Close frame
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    onError(e)
                }
            } finally {
                closeSocket()
            }
        }
    }

    private fun handleMessage(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data") ?: root
            val symbol = data.optString("s")
            val priceRaw = data.optString("c", "0").toDoubleOrNull() ?: 0.0
            val changeRaw = data.optString("P", "0").toDoubleOrNull() ?: 0.0
            val highRaw = data.optString("h", "0").toDoubleOrNull() ?: 0.0
            val lowRaw = data.optString("l", "0").toDoubleOrNull() ?: 0.0

            if (symbol.isNotEmpty()) {
                val formattedPrice = PriceRepository.formatPrice(priceRaw)
                val formattedChange = PriceRepository.formatPercent(changeRaw)
                val formattedHigh = PriceRepository.formatPrice(highRaw)
                val formattedLow = PriceRepository.formatPrice(lowRaw)

                onTickerUpdate(symbol, formattedPrice, changeRaw, formattedChange, formattedHigh, formattedLow)
            }
        } catch (_: Exception) {}
    }

    private fun sendPong(outStream: OutputStream) {
        try {
            // Masked pong frame (client frames must be masked in RFC 6455)
            val mask = ByteArray(4)
            secureRandom.nextBytes(mask)
            val pong = byteArrayOf(
                0x8A.toByte(), // FIN = 1, opcode = 0xA (Pong)
                0x80.toByte(), // MASK = 1, len = 0
                mask[0], mask[1], mask[2], mask[3]
            )
            outStream.write(pong)
            outStream.flush()
        } catch (_: Exception) {}
    }

    private fun readLine(input: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val c = input.read()
            if (c == -1 || c == '\n'.code) break
            if (c != '\r'.code) {
                sb.append(c.toChar())
            }
        }
        return sb.toString()
    }

    fun close() {
        isRunning = false
        job?.cancel()
        closeSocket()
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
