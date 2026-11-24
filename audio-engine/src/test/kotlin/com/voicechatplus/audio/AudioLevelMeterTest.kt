package com.voicechatplus.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sin

class AudioLevelMeterTest {

    @Test
    fun `silence produces zero level`() {
        val meter = AudioLevelMeter()
        assertEquals(0f, meter.update(ShortArray(960)))
        assertEquals(0f, meter.peak)
    }

    @Test
    fun `real sine samples produce a nonzero level`() {
        val meter = AudioLevelMeter()
        val level = meter.update(sine(amplitude = 16_000, frames = 960))
        assertTrue(level > 0.2f, "level was $level")
        assertTrue(meter.peak > 0.4f, "peak was ${meter.peak}")
    }

    @Test
    fun `level decays faster than peak holds`() {
        val meter = AudioLevelMeter()
        meter.update(sine(16_000, 960))
        val loud = meter.level
        meter.update(ShortArray(960))
        assertTrue(meter.level < loud)
        assertTrue(meter.peak >= meter.level)
    }

    @Test
    fun `reset clears level and peak`() {
        val meter = AudioLevelMeter()
        meter.update(sine(16_000, 960))
        meter.reset()
        assertEquals(0f, meter.level)
        assertEquals(0f, meter.peak)
    }

    private fun sine(amplitude: Int, frames: Int): ShortArray = ShortArray(frames) { index ->
        (amplitude * sin(index * 2.0 * Math.PI / 32.0)).toInt().toShort()
    }
}
