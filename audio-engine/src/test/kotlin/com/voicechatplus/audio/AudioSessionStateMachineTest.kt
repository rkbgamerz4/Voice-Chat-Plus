package com.voicechatplus.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AudioSessionStateMachineTest {

    @Test
    fun `happy path acquires then releases once`() {
        val session = AudioSessionStateMachine()
        assertTrue(session.requestPermission())
        assertTrue(session.permissionGranted())
        assertTrue(session.resourcesAcquired)
        assertTrue(session.onPrepared())
        assertEquals(AudioSessionState.Running, session.state)
        assertTrue(session.stop())
        assertEquals(AudioSessionState.Idle, session.state)
        assertFalse(session.resourcesAcquired)
        assertEquals(1, session.releaseCount)
    }

    @Test
    fun `permission denied releases resources and records permanence`() {
        val session = AudioSessionStateMachine()
        session.requestPermission()
        session.permissionGranted()
        assertTrue(session.permissionDenied(permanent = true))
        assertEquals(AudioSessionState.PermissionDenied(true), session.state)
        assertFalse(session.resourcesAcquired)
        assertEquals(1, session.releaseCount)
        assertTrue(session.requestPermission(), "user must be able to retry")
    }

    @Test
    fun `denial before grant does not count as a release`() {
        val session = AudioSessionStateMachine()
        session.requestPermission()
        assertTrue(session.permissionDenied(false))
        assertEquals(0, session.releaseCount)
    }

    @Test
    fun `start stop can repeat without leaking resources`() {
        val session = AudioSessionStateMachine()
        repeat(5) {
            assertTrue(session.requestPermission())
            assertTrue(session.permissionGranted())
            assertTrue(session.onPrepared())
            assertTrue(session.stop())
        }
        assertEquals(5, session.releaseCount)
        assertFalse(session.resourcesAcquired)
    }

    @Test
    fun `resource release is idempotent`() {
        val session = AudioSessionStateMachine()
        session.requestPermission()
        session.permissionGranted()
        assertTrue(session.releaseResources())
        assertFalse(session.releaseResources())
        assertEquals(1, session.releaseCount)
    }

    @Test
    fun `focus loss suspends and focus gain resumes`() {
        val session = AudioSessionStateMachine()
        session.requestPermission()
        session.permissionGranted()
        session.onPrepared()
        assertTrue(session.onFocusLost())
        assertEquals(AudioSessionState.Suspended, session.state)
        assertTrue(session.resourcesAcquired, "suspension must keep AudioRecord alive")
        assertTrue(session.onFocusGained())
        assertEquals(AudioSessionState.Running, session.state)
    }

    @Test
    fun `failure releases resources and allows retry`() {
        val session = AudioSessionStateMachine()
        session.requestPermission()
        session.permissionGranted()
        session.onPrepared()
        assertTrue(session.fail("AudioRecord init failed"))
        assertEquals(AudioSessionState.Failed("AudioRecord init failed"), session.state)
        assertEquals(1, session.releaseCount)
        assertTrue(session.requestPermission())
    }

    @Test
    fun `invalid transitions are rejected`() {
        val session = AudioSessionStateMachine()
        assertFalse(session.onPrepared())
        assertFalse(session.onFocusLost())
        assertFalse(session.stop())
        assertTrue(session.requestPermission())
        assertFalse(session.requestPermission())
    }
}
