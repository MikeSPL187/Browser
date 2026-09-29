package dev.sk2andy.materialbrowser

import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val READ_UP_TO_BUFFER_BYTES = 8 * 1_024

/**
 * Reads at most [maxBytes], stopping early at the end of the stream.
 *
 * Same contract as `InputStream.readNBytes(int)`, which Android only ships from API 33.
 */
internal fun InputStream.readUpTo(maxBytes: Int): ByteArray {
    require(maxBytes >= 0) { "maxBytes must not be negative" }
    val output = ByteArrayOutputStream(minOf(maxBytes, READ_UP_TO_BUFFER_BYTES))
    val buffer = ByteArray(READ_UP_TO_BUFFER_BYTES)
    var remaining = maxBytes
    while (remaining > 0) {
        val count = read(buffer, 0, minOf(buffer.size, remaining))
        if (count < 0) break
        output.write(buffer, 0, count)
        remaining -= count
    }
    return output.toByteArray()
}
