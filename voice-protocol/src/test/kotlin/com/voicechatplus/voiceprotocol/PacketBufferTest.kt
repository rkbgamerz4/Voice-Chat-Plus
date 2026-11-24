package com.voicechatplus.voiceprotocol

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PacketBufferTest {

    @Test
    fun `round trips primitive values`() {
        val buffer = PacketBuffer.allocate(1)
        buffer.writeByte(0x7F)
        buffer.writeShort(0x1234)
        buffer.writeInt(0x01020304)
        buffer.writeLong(0x0102030405060708L)
        buffer.writeBytes(byteArrayOf(0x0A, 0x0B))

        val reader = PacketBuffer.wrap(buffer.writtenArray())
        assertEquals(0x7F, reader.readByte())
        assertEquals(0x1234, reader.readShort())
        assertEquals(0x01020304, reader.readInt())
        assertEquals(0x0102030405060708L, reader.readLong())
        assertArrayEquals(byteArrayOf(0x0A, 0x0B), reader.readBytes(2))
        assertEquals(0, reader.readableBytes)
    }

    @Test
    fun `uses big endian byte order`() {
        val buffer = PacketBuffer.allocate()
        buffer.writeInt(0x01020304)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), buffer.writtenArray())
    }

    @Test
    fun `grows beyond initial capacity`() {
        val buffer = PacketBuffer.allocate(2)
        buffer.writeBytes(ByteArray(100) { it.toByte() })
        assertEquals(100, buffer.writtenBytes)
        assertEquals(100, PacketBuffer.wrap(buffer.writtenArray()).readBytes(100).size)
    }

    @Test
    fun `rejects reads beyond available data`() {
        val reader = PacketBuffer.wrap(byteArrayOf(1, 2))
        assertThrows(IllegalArgumentException::class.java) { reader.readInt() }
        assertThrows(IllegalArgumentException::class.java) { reader.readBytes(-1) }
    }

    @Test
    fun `wrap copies its input`() {
        val source = byteArrayOf(1, 2, 3)
        val reader = PacketBuffer.wrap(source)
        source[0] = 9
        assertEquals(1, reader.readByte())
    }
}
