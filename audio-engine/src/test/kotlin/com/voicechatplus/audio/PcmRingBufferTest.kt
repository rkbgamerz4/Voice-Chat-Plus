package com.voicechatplus.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PcmRingBufferTest {

    @Test
    fun `round trips samples in order`() {
        val buffer = PcmRingBuffer(8)
        buffer.write(shortArrayOf(1, 2, 3, 4))
        val out = ShortArray(4)
        assertEquals(4, buffer.read(out))
        assertArrayEquals(shortArrayOf(1, 2, 3, 4), out)
        assertEquals(0, buffer.size)
    }

    @Test
    fun `wraps around without corrupting order`() {
        val buffer = PcmRingBuffer(4)
        buffer.write(shortArrayOf(1, 2))
        buffer.read(ShortArray(2))
        buffer.write(shortArrayOf(3, 4, 5))
        val out = ShortArray(3)
        buffer.read(out)
        assertArrayEquals(shortArrayOf(3, 4, 5), out)
    }

    @Test
    fun `overflow discards the oldest samples`() {
        val buffer = PcmRingBuffer(3)
        buffer.write(shortArrayOf(1, 2, 3, 4, 5))
        assertEquals(3, buffer.size)
        val out = ShortArray(3)
        buffer.read(out)
        assertArrayEquals(shortArrayOf(3, 4, 5), out)
    }

    @Test
    fun `read returns available amount when buffer has less data`() {
        val buffer = PcmRingBuffer(8)
        buffer.write(shortArrayOf(7, 8))
        val out = ShortArray(8)
        assertEquals(2, buffer.read(out))
        assertArrayEquals(shortArrayOf(7, 8, 0, 0, 0, 0, 0, 0), out)
    }

    @Test
    fun `clear empties the buffer`() {
        val buffer = PcmRingBuffer(4)
        buffer.write(shortArrayOf(1, 2, 3))
        buffer.clear()
        assertEquals(0, buffer.size)
    }

    @Test
    fun `rejects invalid configuration and ranges`() {
        assertThrows(IllegalArgumentException::class.java) { PcmRingBuffer(0) }
        val buffer = PcmRingBuffer(4)
        assertThrows(IllegalArgumentException::class.java) {
            buffer.write(shortArrayOf(1, 2), offset = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buffer.read(ShortArray(2), length = 5)
        }
    }
}
