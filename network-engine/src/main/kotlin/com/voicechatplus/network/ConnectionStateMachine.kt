package com.voicechatplus.network

/**
 * Enforces the connection lifecycle so that UI and network code cannot drive
 * the socket into an impossible state. No socket IO lives here.
 */
class ConnectionStateMachine(initial: ConnectionState = ConnectionState.Disconnected) {

    var state: ConnectionState = initial
        private set

    /** Applies [next] if legal; returns false and leaves state unchanged otherwise. */
    fun transition(next: ConnectionState): Boolean {
        if (!state.canTransitionTo(next)) return false
        state = next
        return true
    }

    fun fail(reason: String): Boolean = transition(ConnectionState.Failed(reason))
}
