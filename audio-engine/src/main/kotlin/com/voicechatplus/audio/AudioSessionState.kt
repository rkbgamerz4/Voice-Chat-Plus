package com.voicechatplus.audio

/** Lifecycle of a microphone session, including permission and audio focus. */
sealed class AudioSessionState {
    data object Idle : AudioSessionState()
    data object AwaitingPermission : AudioSessionState()

    /** [permanent] means the user selected "Don't ask again" and must use Settings. */
    data class PermissionDenied(val permanent: Boolean) : AudioSessionState()

    data object Preparing : AudioSessionState()
    data object Running : AudioSessionState()

    /** Audio focus was lost temporarily; capture is suspended but resources are kept. */
    data object Suspended : AudioSessionState()

    data class Failed(val reason: String) : AudioSessionState()
}
