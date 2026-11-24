package com.voicechatplus.network

/** Lifecycle states of a Voice Chat+ voice connection. */
sealed class ConnectionState {

    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Authenticating : ConnectionState()
    data object Connected : ConnectionState()

    /** Terminal-for-this-attempt failure carrying a human-readable reason. */
    data class Failed(val reason: String) : ConnectionState()

    /** Returns whether a transition to [next] is legal from this state. */
    fun canTransitionTo(next: ConnectionState): Boolean = when (this) {
        is Disconnected -> next is Connecting || next is Failed
        is Connecting -> next is Authenticating || next is Failed || next is Disconnected
        is Authenticating -> next is Connected || next is Failed || next is Disconnected
        is Connected -> next is Disconnected || next is Failed
        is Failed -> next is Connecting || next is Disconnected
    }
}
