package com.voicechatplus.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PcmFrameProcessorTest {

    @Test
    fun `default frame is 960 samples`() {
        assertEquals(960, PcmFrameProcessor().frameSizeSamples)
    }

    @Test
    fun `combines partial chunks into one frame`() {
        val processor = PcmFrameProcessor(frameSizeSamples = 4, ringCapacityFrames = 4)
        val first = shortArrayOf(1, 2)
        val second = shortArrayOf(3, 4)

        assertEquals(0, processor.feed(first))
        assertEquals(1, processor.feed(second))
        val frame = processor.nextFrame()
        assertNotNull(frame)
        assertArrayEquals(shortArrayOf(1, 2, 3, 4), frame)
        assertNull(processor.nextFrame())
    }

    @Test
    fun `each frame is an independent copy`() {
        val processor = PcmFrameProcessor(frameSizeSamples = 2, ringCapacityFrames = 4)
        processor.feed(shortArrayOf(5, 6, 7, 8))
        val first = processor.nextFrame()!!
        val second = processor.nextFrame()!!
        assertNotSame(first, second)
        assertArrayEquals(shortArrayOf(5, 6), first)
        assertArrayEquals(shortArrayOf(7, 8), second)
    }

    @Test
    fun `overflow drops the oldest samples without blocking`() {
        val processor = PcmFrameProcessor(frameSizeSamples = 4, ringCapacityFrames = 2)
        val samples = ShortArray(12) { (it + 1).toShort() }
        processor.feed(samples)
        assertEquals(2, processor.availableFrames)
        assertArrayEquals(shortArrayOf(5, 6, 7, 8), processor.nextFrame())
        assertArrayEquals(shortArrayOf(9, 10, 11, 12), processor.nextFrame())
    }

    @Test
    fun `drain keeps an incomplete tail`() {
        val processor = PcmFrameProcessor(frameSizeSamples = 4, ringCapacityFrames = 4)
        processor.feed(ShortArray(10) { 7 })
        val frames = mutableListOf<ShortArray>()
        assertEquals(2, processor.drainAll(frames))
        assertEquals(2, frames.size)
        assertEquals(2, processor.bufferedSamples)
    }

    @Test
    fun `clear discards partial data`() {
        val processor = PcmFrameProcessor(frameSizeSamples = 4)
        processor.feed(shortArrayOf(1, 2))
        processor.clear()
        assertEquals(0, processor.bufferedSamples)
        assertNull(processor.nextFrame())
    }
}
