package com.voicechatplus.audio

import kotlin.math.abs

/**
 * Computes a smoothed peak level in the range 0..1 from real PCM samples.
 *
 * Attack is faster than release so the meter responds quickly to speech but
 * does not flicker between syllables. [peak] is a decaying hold marker.
 */
class AudioLevelMeter(
    private val attack: Float = DEFAULT_ATTACK,
    private val release: Float = DEFAULT_RELEASE,
    private val peakHoldRelease: Float = DEFAULT_PEAK_RELEASE,
) {
    var level: Float = 0f
        private set

    var peak: Float = 0f
        private set

    init {
        require(attack in 0f..1f) { "attack must be between 0 and 1" }
        require(release in 0f..1f) { "release must be between 0 and 1" }
        require(peakHoldRelease in 0f..1f) { "peakHoldRelease must be between 0 and 1" }
    }

    /** Updates and returns the smoothed level for one captured chunk. */
    fun update(samples: ShortArray, offset: Int = 0, length: Int = samples.size - offset): Float {
        require(offset >= 0) { "offset must be non-negative" }
        require(length >= 0) { "length must be non-negative" }
        require(offset + length <= samples.size) { "range exceeds sample array" }

        if (length == 0) return level

        var maxMagnitude = 0
        for (i in offset until offset + length) {
            val magnitude = abs(samples[i].toInt())
            if (magnitude > maxMagnitude) maxMagnitude = magnitude
        }
        val instantaneous = (maxMagnitude / 32768f).coerceIn(0f, 1f)

        val coefficient = if (instantaneous > level) attack else release
        level += (instantaneous - level) * coefficient

        peak = if (instantaneous > peak) {
            instantaneous
        } else {
            (peak - peakHoldRelease).coerceAtLeast(level)
        }
        return level
    }

    fun reset() {
        level = 0f
        peak = 0f
    }

    companion object {
        const val DEFAULT_ATTACK: Float = 0.5f
        const val DEFAULT_RELEASE: Float = 0.12f
        const val DEFAULT_PEAK_RELEASE: Float = 0.004f
    }
}
