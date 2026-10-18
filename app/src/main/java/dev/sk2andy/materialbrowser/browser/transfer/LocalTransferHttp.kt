package dev.sk2andy.materialbrowser.browser.transfer

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * The few bytes of HTTP/1.1 the local transfer speaks: one request, one answer, then the
 * connection closes. Written by hand on plain sockets so both phones bound every read (line
 * length, header count, body size) and no general HTTP stack sits on the Wi-Fi.
 */
internal object LocalTransferHttp {
    private const val MAX_LINE_BYTES = 1_024
    private const val MAX_HEADERS = 32

    class Request(val method: String, val path: String, val body: String)

    class Response(val status: Int, val body: String)

    /** The request on [input], or null when it is not a bounded HTTP/1.1 request. */
    fun readRequest(input: InputStream, maxBodyBytes: Int): Request? {
        val requestLine = readLine(input) ?: return null
        val parts = requestLine.split(' ')
        if (parts.size != 3 || parts[2] != "HTTP/1.1") return null
        val body = readHeadersAndBody(input, maxBodyBytes) ?: return null
        return Request(parts[0], parts[1], body)
    }

    /** The answer on [input], or null when it is not a bounded HTTP/1.1 answer. */
    fun readResponse(input: InputStream, maxBodyBytes: Int): Response? {
        val statusLine = readLine(input) ?: return null
        val parts = statusLine.split(' ', limit = 3)
        if (parts.size < 2 || parts[0] != "HTTP/1.1") return null
        val status = parts[1].toIntOrNull()?.takeIf { it in 100..599 } ?: return null
        val body = readHeadersAndBody(input, maxBodyBytes) ?: return null
        return Response(status, body)
    }

    fun writeRequest(output: OutputStream, host: String, port: Int, path: String, body: String) {
        val bytes = body.encodeToByteArray()
        val head = "POST $path HTTP/1.1\r\nHost: $host:$port\r\nContent-Type: application/json\r\n" +
            "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        output.write(head.encodeToByteArray())
        output.write(bytes)
        output.flush()
    }

    fun writeResponse(output: OutputStream, status: Int, body: String = "") {
        val bytes = body.encodeToByteArray()
        val head = "HTTP/1.1 $status ${reason(status)}\r\nContent-Type: application/json\r\n" +
            "Content-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
        output.write(head.encodeToByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun readHeadersAndBody(input: InputStream, maxBodyBytes: Int): String? {
        var length: Int? = null
        repeat(MAX_HEADERS + 1) {
            val line = readLine(input) ?: return null
            if (line.isEmpty()) {
                val size = length ?: return null
                val body = ByteArray(size)
                var read = 0
                while (read < size) {
                    val count = input.read(body, read, size - read)
                    if (count < 0) return null
                    read += count
                }
                return runCatching { body.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
            }
            val name = line.substringBefore(':', missingDelimiterValue = "").trim()
            if (name.isEmpty()) return null
            when {
                name.equals("Content-Length", ignoreCase = true) -> {
                    if (length != null) return null
                    val value = line.substringAfter(':').trim()
                    if (value.isEmpty() || !value.all(Char::isDigit) || value.length > 9) return null
                    length = value.toInt().takeIf { it <= maxBodyBytes } ?: return null
                }
                name.equals("Transfer-Encoding", ignoreCase = true) -> return null
            }
        }
        return null
    }

    /** One CRLF-terminated ASCII line without its terminator; null past [MAX_LINE_BYTES] or at the end. */
    private fun readLine(input: InputStream): String? {
        val line = ByteArrayOutputStream()
        while (line.size() <= MAX_LINE_BYTES) {
            when (val byte = input.read()) {
                -1 -> return null
                '\r'.code -> return if (input.read() == '\n'.code) String(line.toByteArray(), Charsets.US_ASCII) else null
                '\n'.code -> return null
                else -> if (byte < 0x20 && byte != '\t'.code || byte > 0x7E) return null else line.write(byte)
            }
        }
        return null
    }

    private fun reason(status: Int): String = when (status) {
        200 -> "OK"
        400 -> "Bad Request"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        410 -> "Gone"
        429 -> "Too Many Requests"
        else -> "Error"
    }
}

/**
 * What [socket] sends, for [budgetMillis] in all. A socket timeout covers one read, so a peer that
 * sends a byte every few seconds never trips it and, on a server that serves one connection at a
 * time, holds everyone else out; here every read gets only the time that is left, and the budget
 * ending is a [SocketTimeoutException].
 */
internal class DeadlineSocketInput(
    private val socket: Socket,
    budgetMillis: Int,
    private val nanoTime: () -> Long = System::nanoTime,
) : InputStream() {
    private val input = socket.getInputStream()
    private val deadline = nanoTime() + budgetMillis * NANOS_PER_MILLI

    override fun read(): Int {
        arm()
        return input.read()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        arm()
        return input.read(buffer, offset, length)
    }

    /** A socket timeout of 0 would wait forever, so the time left is at least one millisecond. */
    private fun arm() {
        val left = (deadline - nanoTime()) / NANOS_PER_MILLI
        if (left <= 0) throw SocketTimeoutException("The request took longer than its budget")
        socket.soTimeout = left.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
