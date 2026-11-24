package com.voicechatplus.audio

/**
 * Enforces the microphone session lifecycle and tracks resource ownership.
 *
 * The Android adapter drives this machine and releases the real AudioRecord /
 * audio-effect objects when [releaseResources] is required. Keeping the
 * ownership rules here makes permission denial, repeated start/stop and
 * cleanup behaviour unit-testable without an Android device.
 */
class AudioSessionStateMachine {

    var state: AudioSessionState = AudioSessionState.Idle
        private set

    /** True between a successful permission grant and the next resource release. */
    var resourcesAcquired: Boolean = false
        private set

    /** Number of successful resource releases; used to verify no double-release. */
    var releaseCount: Int = 0
        private set

    fun requestPermission(): Boolean {
        if (state !is AudioSessionState.Idle &&
            state !is AudioSessionState.PermissionDenied &&
            state !is AudioSessionState.Failed
        ) return false
        state = AudioSessionState.AwaitingPermission
        return true
    }

    /** Permission granted: from here until stop/failure the adapter owns resources. */
    fun permissionGranted(): Boolean {
        if (state !is AudioSessionState.AwaitingPermission) return false
        resourcesAcquired = true
        state = AudioSessionState.Preparing
        return true
    }

    fun permissionDenied(permanent: Boolean): Boolean {
        if (state !is AudioSessionState.AwaitingPermission && state !is AudioSessionState.Preparing) return false
        releaseResources()
        state = AudioSessionState.PermissionDenied(permanent)
        return true
    }

    fun onPrepared(): Boolean {
        if (state !is AudioSessionState.Preparing) return false
        state = AudioSessionState.Running
        return true
    }

    fun onFocusLost(): Boolean {
        if (state !is AudioSessionState.Running) return false
        state = AudioSessionState.Suspended
        return true
    }

    fun onFocusGained(): Boolean {
        if (state !is AudioSessionState.Suspended) return false
        state = AudioSessionState.Running
        return true
    }

    fun stop(): Boolean {
        if (state !is AudioSessionState.Preparing &&
            state !is AudioSessionState.Running &&
            state !is AudioSessionState.Suspended &&
            state !is AudioSessionState.PermissionDenied &&
            state !is AudioSessionState.Failed
        ) return false
        releaseResources()
        state = AudioSessionState.Idle
        return true
    }

    fun fail(reason: String): Boolean {
        releaseResources()
        state = AudioSessionState.Failed(reason)
        return true
    }

    /** Idempotent: releases once per acquisition. */
    fun releaseResources(): Boolean {
        if (!resourcesAcquired) return false
        resourcesAcquired = false
        releaseCount++
        return true
    }
}
