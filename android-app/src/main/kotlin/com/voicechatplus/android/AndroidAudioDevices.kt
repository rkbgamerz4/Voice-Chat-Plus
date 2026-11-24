package com.voicechatplus.android

import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.voicechatplus.audio.AudioDeviceDirection
import com.voicechatplus.audio.AudioDeviceInfo as DeviceInfo
import com.voicechatplus.audio.AudioDeviceKind

/** Maps Android's audio device enum to platform-independent diagnostics. */
object AndroidAudioDevices {

    fun enumerate(manager: AudioManager): List<DeviceInfo> {
        val result = mutableListOf<DeviceInfo>()
        manager.getDevices(AudioManager.GET_DEVICES_INPUTS).forEach {
            result.add(map(it, AudioDeviceDirection.INPUT))
        }
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach {
            result.add(map(it, AudioDeviceDirection.OUTPUT))
        }
        return result
    }

    private fun map(device: AudioDeviceInfo, direction: AudioDeviceDirection): DeviceInfo {
        val kind = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_TELEPHONY,
            -> AudioDeviceKind.BUILT_IN

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            -> AudioDeviceKind.WIRED

            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceKind.BLUETOOTH_SCO
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceKind.BLUETOOTH_A2DP

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            -> AudioDeviceKind.USB

            else -> AudioDeviceKind.UNKNOWN
        }
        return DeviceInfo(
            id = device.id,
            kind = kind,
            direction = direction,
            productName = device.productName?.toString(),
            isBuiltIn = kind == AudioDeviceKind.BUILT_IN,
        )
    }
}
