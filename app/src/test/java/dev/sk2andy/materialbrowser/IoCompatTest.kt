package dev.sk2andy.materialbrowser

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class IoCompatTest {
    @Test
    fun `reads whole stream when it is shorter than the limit`() {
        val bytes = ByteArray(10) { it.toByte() }

        assertArrayEquals(bytes, ByteArrayInputStream(bytes).readUpTo(64))
    }

    @Test
    fun `stops at the limit and leaves the rest unread`() {
        val input = ByteArrayInputStream(ByteArray(20_000) { (it % 251).toByte() })

        val read = input.readUpTo(10_001)

        assertEquals(10_001, read.size)
        assertEquals((10_000 % 251).toByte(), read.last())
        assertEquals(9_999, input.available())
    }

    @Test
    fun `keeps reading short chunks until the limit`() {
        val source = ByteArray(100) { it.toByte() }
        val oneByteAtATime = object : InputStream() {
            private var position = 0

            override fun read(): Int = if (position < source.size) source[position++].toInt() and 0xFF else -1

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (len == 0) return 0
                val next = read()
                if (next < 0) return -1
                b[off] = next.toByte()
                return 1
            }
        }

        assertArrayEquals(source.copyOf(40), oneByteAtATime.readUpTo(40))
    }

    @Test
    fun `zero limit reads nothing`() {
        val input = ByteArrayInputStream(byteArrayOf(1, 2, 3))

        assertEquals(0, input.readUpTo(0).size)
        assertEquals(3, input.available())
    }
}
