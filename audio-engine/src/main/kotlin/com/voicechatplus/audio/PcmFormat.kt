package com.voicechatplus.audio

/**
 * Describes uncompressed PCM audio used inside the audio engine.
 *
 * The engine deliberately works only with 16-bit PCM so that Android
 * `AudioRecord`/`AudioTrack` and JVM-side DSP share one representation.
 */
data class PcmFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int = 16,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(channels in 1..2) { "channels must be 1 (mono) or 2 (stereo)" }
        require(bitsPerSample == 16) { "only 16-bit PCM is supported" }
    }

    val bytesPerSample: Int get() = bitsPerSample / 8
    val frameSizeBytes: Int get() = channels * bytesPerSample

    /** Number of samples per channel in one frame of the given duration. */
    fun samplesPerFrame(frameDurationMs: Int): Int {
        require(frameDurationMs > 0) { "frameDurationMs must be positive" }
        return sampleRate * frameDurationMs / 1000
    }

    companion object {
        const val DEFAULT_FRAME_DURATION_MS: Int = 20

        /** 48 kHz mono 16-bit — the target format for voice transport. */
        val VOICE: PcmFormat = PcmFormat(sampleRate = 48_000, channels = 1)
    }
}
