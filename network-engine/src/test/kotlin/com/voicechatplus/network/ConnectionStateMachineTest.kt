package com.voicechatplus.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConnectionStateMachineTest {

    @Test
    fun `follows the happy path`() {
        val machine = ConnectionStateMachine()
        assertEquals(ConnectionState.Disconnected, machine.state)
        assertTrue(machine.transition(ConnectionState.Connecting))
        assertTrue(machine.transition(ConnectionState.Authenticating))
        assertTrue(machine.transition(ConnectionState.Connected))
        assertTrue(machine.transition(ConnectionState.Disconnected))
    }

    @Test
    fun `rejects skipped authentication`() {
        val machine = ConnectionStateMachine()
        assertTrue(machine.transition(ConnectionState.Connecting))
        assertFalse(machine.transition(ConnectionState.Connected))
        assertEquals(ConnectionState.Connecting, machine.state)
    }

    @Test
    fun `can fail from any active state`() {
        for (state in listOf(
            ConnectionState.Disconnected,
            ConnectionState.Connecting,
            ConnectionState.Authenticating,
            ConnectionState.Connected,
        )) {
            assertTrue(state.canTransitionTo(ConnectionState.Failed("test")), "$state should allow failure")
        }
    }

    @Test
    fun `failed state can retry or disconnect`() {
        val machine = ConnectionStateMachine()
        assertTrue(machine.fail("no route"))
        assertEquals(ConnectionState.Failed("no route"), machine.state)
        assertTrue(machine.transition(ConnectionState.Connecting))
        assertTrue(machine.fail("again"))
        assertTrue(machine.transition(ConnectionState.Disconnected))
    }

    @Test
    fun `cannot fail from failed without reconnecting`() {
        val machine = ConnectionStateMachine()
        machine.fail("first")
        assertFalse(machine.fail("second"))
        assertEquals(ConnectionState.Failed("first"), machine.state)
    }
}
