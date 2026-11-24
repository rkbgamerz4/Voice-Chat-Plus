package com.voicechatplus.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PcmFormatTest {

    @Test
    fun `voice format is 48 kilohertz mono with 20 millisecond frames`() {
        assertEquals(48_000, PcmFormat.VOICE.sampleRate)
        assertEquals(1, PcmFormat.VOICE.channels)
        assertEquals(2, PcmFormat.VOICE.bytesPerSample)
        assertEquals(960, PcmFormat.VOICE.samplesPerFrame(20))
    }

    @Test
    fun `frame size accounts for stereo`() {
        assertEquals(4, PcmFormat(48_000, 2).frameSizeBytes)
    }

    @Test
    fun `rejects invalid formats`() {
        assertThrows(IllegalArgumentException::class.java) { PcmFormat(0, 1) }
        assertThrows(IllegalArgumentException::class.java) { PcmFormat(48_000, 3) }
        assertThrows(IllegalArgumentException::class.java) { PcmFormat(48_000, 1, 8) }
    }
}
