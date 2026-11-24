package com.voicechatplus.audio

/**
 * Accumulates a continuous PCM sample stream into fixed-size frames.
 *
 * AudioRecord delivers arbitrary chunk sizes; Opus and the voice pipeline need
 * exact 20 ms frames. A [PcmRingBuffer] provides bounded back-pressure: if the
 * consumer falls behind, the oldest samples are dropped instead of blocking
 * the capture thread.
 */
class PcmFrameProcessor(
    val frameSizeSamples: Int = PcmFormat.VOICE.samplesPerFrame(PcmFormat.DEFAULT_FRAME_DURATION_MS),
    ringCapacityFrames: Int = DEFAULT_RING_CAPACITY_FRAMES,
) {
    private val ring = PcmRingBuffer(frameSizeSamples * ringCapacityFrames)
    private val scratch = ShortArray(frameSizeSamples)

    val availableFrames: Int get() = ring.size / frameSizeSamples
    val bufferedSamples: Int get() = ring.size
    val capacitySamples: Int get() = ring.capacity

    init {
        require(frameSizeSamples > 0) { "frameSizeSamples must be positive" }
        require(ringCapacityFrames > 0) { "ringCapacityFrames must be positive" }
    }

    /** Appends captured samples. Returns the number of complete frames available. */
    fun feed(samples: ShortArray, offset: Int = 0, length: Int = samples.size - offset): Int {
        ring.write(samples, offset, length)
        return availableFrames
    }

    /**
     * Removes the next complete frame and returns a copy.
     * Returns null when fewer than [frameSizeSamples] samples are buffered.
     */
    fun nextFrame(): ShortArray? {
        if (availableFrames == 0) return null
        ring.read(scratch, 0, frameSizeSamples)
        return scratch.copyOf()
    }

    /** Removes all complete frames, oldest first. Incomplete tail samples are kept. */
    fun drainAll(destination: MutableList<ShortArray>): Int {
        var count = 0
        while (true) {
            val frame = nextFrame() ?: break
            destination.add(frame)
            count++
        }
        return count
    }

    fun clear() = ring.clear()

    companion object {
        const val DEFAULT_RING_CAPACITY_FRAMES: Int = 8
    }
}
