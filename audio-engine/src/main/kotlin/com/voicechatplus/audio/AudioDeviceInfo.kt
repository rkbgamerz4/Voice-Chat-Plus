package com.voicechatplus.audio

/** Platform-independent classification of an audio endpoint. */
enum class AudioDeviceKind {
    BUILT_IN,
    WIRED,
    BLUETOOTH_SCO,
    BLUETOOTH_A2DP,
    USB,
    UNKNOWN,
}

enum class AudioDeviceDirection {
    INPUT,
    OUTPUT,
}

/**
 * A snapshot of one Android audio device. Platform mapping happens in the
 * android-app module; this type keeps diagnostics testable and UI-independent.
 */
data class AudioDeviceInfo(
    val id: Int,
    val kind: AudioDeviceKind,
    val direction: AudioDeviceDirection,
    val productName: String?,
    val isBuiltIn: Boolean,
) {
    val displayName: String
        get() = productName?.takeIf { it.isNotBlank() }
            ?: when (kind) {
                AudioDeviceKind.BUILT_IN -> "Built-in ${direction.name.lowercase()}"
                AudioDeviceKind.WIRED -> "Wired headset"
                AudioDeviceKind.BLUETOOTH_SCO -> "Bluetooth headset (SCO)"
                AudioDeviceKind.BLUETOOTH_A2DP -> "Bluetooth audio (A2DP)"
                AudioDeviceKind.USB -> "USB audio"
                AudioDeviceKind.UNKNOWN -> "Unknown device $id"
            }
}
