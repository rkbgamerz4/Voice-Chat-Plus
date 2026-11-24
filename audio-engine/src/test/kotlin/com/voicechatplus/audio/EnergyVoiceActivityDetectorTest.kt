package com.voicechatplus.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sin

class EnergyVoiceActivityDetectorTest {

    @Test
    fun `silence is not voice`() {
        val vad = EnergyVoiceActivityDetector()
        assertFalse(vad.process(ShortArray(960)))
        assertEquals(0f, vad.lastRms)
    }

    @Test
    fun `loud sine wave is voice`() {
        val vad = EnergyVoiceActivityDetector()
        assertTrue(vad.process(sine(amplitude = 16_000, frames = 960)))
        assertTrue(vad.lastRms > 0.01f)
    }

    @Test
    fun `release hysteresis prevents instant toggling`() {
        val vad = EnergyVoiceActivityDetector(releaseFrames = 3)
        assertTrue(vad.process(sine(16_000, 960)))
        assertTrue(vad.process(ShortArray(960)))
        assertTrue(vad.isVoiceActive)
        vad.process(ShortArray(960))
        assertTrue(vad.isVoiceActive)
        vad.process(ShortArray(960))
        assertFalse(vad.isVoiceActive)
    }

    @Test
    fun `sensitivity threshold is configurable and validated`() {
        val sensitive = EnergyVoiceActivityDetector(threshold = 0.001f)
        assertTrue(sensitive.process(sine(amplitude = 200, frames = 960)))

        val strict = EnergyVoiceActivityDetector(threshold = 0.5f)
        assertFalse(strict.process(sine(amplitude = 200, frames = 960)))

        assertThrows(IllegalArgumentException::class.java) {
            EnergyVoiceActivityDetector(threshold = 0f)
        }
    }

    @Test
    fun `reset returns detector to initial state`() {
        val vad = EnergyVoiceActivityDetector()
        vad.process(sine(16_000, 960))
        vad.reset()
        assertFalse(vad.isVoiceActive)
        assertEquals(0f, vad.lastRms)
    }

    private fun sine(amplitude: Int, frames: Int): ShortArray = ShortArray(frames) { index ->
        (amplitude * sin(index * 2.0 * Math.PI / 16.0)).toInt().toShort()
    }
}
