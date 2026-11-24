package com.voicechatplus.audio

/**
 * Fixed-capacity ring buffer for PCM samples.
 *
 * When more samples are written than the buffer can hold, the oldest samples
 * are discarded. This provides back-pressure without blocking the capture
 * thread and without allocating per frame.
 */
class PcmRingBuffer(capacitySamples: Int) {
    private val buffer = ShortArray(capacitySamples)
    private var head = 0
    private var count = 0

    val capacity: Int = capacitySamples
    val size: Int get() = count

    init {
        require(capacitySamples > 0) { "capacitySamples must be positive" }
    }

    fun write(samples: ShortArray, offset: Int = 0, length: Int = samples.size - offset) {
        require(offset >= 0) { "offset must be non-negative" }
        require(length >= 0) { "length must be non-negative" }
        require(offset + length <= samples.size) { "read range exceeds sample array" }

        if (length > capacity) {
            // Keep only the newest samples rather than blocking the producer.
            write(samples, offset + length - capacity, capacity)
            return
        }

        for (i in 0 until length) {
            buffer[(head + count) % capacity] = samples[offset + i]
            if (count == capacity) {
                head = (head + 1) % capacity
            } else {
                count++
            }
        }
    }

    /** Reads up to [length] samples. Returns the number actually read. */
    fun read(destination: ShortArray, offset: Int = 0, length: Int = destination.size - offset): Int {
        require(offset >= 0) { "offset must be non-negative" }
        require(length >= 0) { "length must be non-negative" }
        require(offset + length <= destination.size) { "write range exceeds destination array" }

        val amount = minOf(length, count)
        for (i in 0 until amount) {
            destination[offset + i] = buffer[(head + i) % capacity]
        }
        head = (head + amount) % capacity
        count -= amount
        return amount
    }

    fun clear() {
        head = 0
        count = 0
    }
}
