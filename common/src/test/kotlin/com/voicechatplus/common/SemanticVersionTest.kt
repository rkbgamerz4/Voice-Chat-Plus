package com.voicechatplus.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticVersionTest {

    @Test
    fun `parses a valid version`() {
        assertEquals(SemanticVersion(1, 2, 3), SemanticVersion.parse("1.2.3"))
    }

    @Test
    fun `ignores surrounding whitespace`() {
        assertEquals(SemanticVersion(0, 1, 0), SemanticVersion.parse(" 0.1.0 "))
    }

    @Test
    fun `rejects malformed input`() {
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion.parse("1.2") }
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion.parse("v1.2.3") }
        assertThrows(IllegalArgumentException::class.java) { SemanticVersion.parse("1.2.x") }
    }

    @Test
    fun `orders by major minor patch`() {
        assertTrue(SemanticVersion.parse("2.0.0") > SemanticVersion.parse("1.99.99"))
        assertTrue(SemanticVersion.parse("1.10.0") > SemanticVersion.parse("1.9.9"))
        assertTrue(SemanticVersion.parse("1.2.4") > SemanticVersion.parse("1.2.3"))
    }

    @Test
    fun `renders as dotted decimal`() {
        assertEquals("1.2.3", SemanticVersion(1, 2, 3).toString())
    }
}
