package com.voicechatplus.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Requests and abandons Android audio focus for voice communication.
 *
 * API 26+ AudioFocusRequest is used throughout; focus changes are delivered on
 * the main thread so the UI can suspend and resume capture safely.
 */
class AudioFocusManager(
    context: Context,
    private val onFocusChange: (Boolean) -> Unit,
) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null
    private var held = false

    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        val granted = change == AudioManager.AUDIOFOCUS_GAIN ||
            change == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        onFocusChange(granted)
    }

    val isHeld: Boolean get() = held

    fun request(): Boolean {
        if (held) return true
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(listener, Handler(Looper.getMainLooper()))
            .build()

        return try {
            val result = manager.requestAudioFocus(focusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                request = focusRequest
                held = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun abandon() {
        if (!held) return
        try {
            request?.let { manager.abandonAudioFocusRequest(it) }
        } catch (e: Exception) {
            // Focus may already have been taken by another app.
        }
        request = null
        held = false
    }
}
