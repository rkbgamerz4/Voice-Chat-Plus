package com.voicechatplus.voiceprotocol

/**
 * Small big-endian byte buffer for building and parsing Voice Chat+ packets.
 *
 * This is our own internal wire format. It is not derived from, and makes no
 * claim to implement, any third-party protocol. See `docs/RESEARCH.md`.
 */
class PacketBuffer private constructor(
    private var data: ByteArray,
    private var writerIndex: Int,
) {
    private var readerIndex: Int = 0

    val readableBytes: Int get() = writerIndex - readerIndex
    val writtenBytes: Int get() = writerIndex

    fun writtenArray(): ByteArray = data.copyOf(writerIndex)

    fun writeByte(value: Int) {
        ensureCapacity(1)
        data[writerIndex++] = (value and 0xFF).toByte()
    }

    fun writeShort(value: Int) {
        ensureCapacity(2)
        data[writerIndex++] = ((value ushr 8) and 0xFF).toByte()
        data[writerIndex++] = (value and 0xFF).toByte()
    }

    fun writeInt(value: Int) {
        ensureCapacity(4)
        data[writerIndex++] = ((value ushr 24) and 0xFF).toByte()
        data[writerIndex++] = ((value ushr 16) and 0xFF).toByte()
        data[writerIndex++] = ((value ushr 8) and 0xFF).toByte()
        data[writerIndex++] = (value and 0xFF).toByte()
    }

    fun writeLong(value: Long) {
        ensureCapacity(8)
        for (shift in 56 downTo 0 step 8) {
            data[writerIndex++] = ((value ushr shift) and 0xFFL).toByte()
        }
    }

    fun writeBytes(bytes: ByteArray) {
        ensureCapacity(bytes.size)
        bytes.copyInto(data, writerIndex)
        writerIndex += bytes.size
    }

    fun readByte(): Int {
        require(readableBytes >= 1) { "no bytes left to read" }
        return data[readerIndex++].toInt() and 0xFF
    }

    fun readShort(): Int = (readByte() shl 8) or readByte()

    fun readInt(): Int =
        (readByte() shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()

    fun readLong(): Long {
        var result = 0L
        repeat(8) { result = (result shl 8) or readByte().toLong() }
        return result
    }

    fun readBytes(length: Int): ByteArray {
        require(length >= 0) { "length must be non-negative" }
        require(readableBytes >= length) { "not enough bytes to read" }
        val result = data.copyOfRange(readerIndex, readerIndex + length)
        readerIndex += length
        return result
    }

    private fun ensureCapacity(additional: Int) {
        val required = writerIndex + additional
        if (required <= data.size) return
        var newSize = data.size.coerceAtLeast(16)
        while (newSize < required) newSize *= 2
        data = data.copyOf(newSize)
    }

    companion object {
        fun allocate(initialCapacity: Int = 256): PacketBuffer {
            require(initialCapacity > 0) { "initialCapacity must be positive" }
            return PacketBuffer(ByteArray(initialCapacity), 0)
        }

        /** Wraps a defensive copy of [data] in a fully-written readable buffer. */
        fun wrap(data: ByteArray): PacketBuffer = PacketBuffer(data.copyOf(), data.size)
    }
}
