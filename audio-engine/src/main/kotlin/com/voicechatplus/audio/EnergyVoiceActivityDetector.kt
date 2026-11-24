package com.voicechatplus.audio

import kotlin.math.sqrt

/**
 * RMS-energy voice activity detector with attack/release hysteresis.
 *
 * This is an original, dependency-free gate intended as a baseline. It can be
 * replaced by a more advanced detector without changing callers. Sensitivity
 * is expressed as a normalised RMS threshold in the range (0, 1).
 */
class EnergyVoiceActivityDetector(
    private val threshold: Float = DEFAULT_THRESHOLD,
    private val attackFrames: Int = 1,
    private val releaseFrames: Int = 5,
) {
    private var consecutiveLoud = 0
    private var consecutiveQuiet = 0

    var isVoiceActive: Boolean = false
        private set

    var lastRms: Float = 0f
        private set

    init {
        require(threshold > 0f && threshold < 1f) { "threshold must be between 0 and 1" }
        require(attackFrames > 0) { "attackFrames must be positive" }
        require(releaseFrames > 0) { "releaseFrames must be positive" }
    }

    /** Feeds one PCM frame and returns whether voice is currently active. */
    fun process(samples: ShortArray, offset: Int = 0, length: Int = samples.size - offset): Boolean {
        require(offset >= 0) { "offset must be non-negative" }
        require(length >= 0) { "length must be non-negative" }
        require(offset + length <= samples.size) { "range exceeds sample array" }

        if (length == 0) return isVoiceActive

        var sumOfSquares = 0.0
        for (i in offset until offset + length) {
            val normalised = samples[i].toFloat() / 32768f
            sumOfSquares += normalised * normalised
        }
        lastRms = sqrt(sumOfSquares / length).toFloat()

        if (lastRms >= threshold) {
            consecutiveLoud++
            consecutiveQuiet = 0
        } else {
            consecutiveQuiet++
            consecutiveLoud = 0
        }

        when {
            !isVoiceActive && consecutiveLoud >= attackFrames -> isVoiceActive = true
            isVoiceActive && consecutiveQuiet >= releaseFrames -> isVoiceActive = false
        }
        return isVoiceActive
    }

    fun reset() {
        isVoiceActive = false
        lastRms = 0f
        consecutiveLoud = 0
        consecutiveQuiet = 0
    }

    companion object {
        const val DEFAULT_THRESHOLD: Float = 0.01f
    }
}
