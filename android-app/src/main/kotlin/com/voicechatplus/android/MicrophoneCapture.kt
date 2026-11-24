package com.voicechatplus.android

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.voicechatplus.audio.AudioLevelMeter
import com.voicechatplus.audio.PcmFormat
import com.voicechatplus.audio.PcmFrameProcessor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real microphone capture using AudioRecord.
 *
 * All AudioRecord lifecycle operations happen on one dedicated high-priority
 * thread; start/stop are idempotent and release every acquired resource.
 * Platform noise suppression, echo cancellation and AGC are enabled only when
 * the device reports them as available.
 */
class MicrophoneCapture(
    private val onLevel: (Float, Float) -> Unit,
    private val onState: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    /** Optional consumer of exact 20 ms PCM frames (used by the codec later). */
    var onFrame: ((ShortArray) -> Unit)? = null

    private val running = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val processor = PcmFrameProcessor()
    private val meter = AudioLevelMeter()
    private val effects = mutableListOf<AudioEffect>()
    private val enabledEffectNames = mutableListOf<String>()

    private var record: AudioRecord? = null
    private var thread: Thread? = null

    val isRunning: Boolean get() = running.get()
    val sampleRate: Int get() = PcmFormat.VOICE.sampleRate
    val channelCount: Int get() = PcmFormat.VOICE.channels
    val level: Float get() = meter.level
    val enabledEffects: List<String> get() = enabledEffectNames.toList()

    fun start(device: AudioDeviceInfo? = null): Boolean {
        if (!running.compareAndSet(false, true)) return false

        try {
            val format = PcmFormat.VOICE
            val channelMask = AudioFormat.CHANNEL_IN_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minimum = AudioRecord.getMinBufferSize(format.sampleRate, channelMask, encoding)
            if (minimum <= 0) {
                throw IllegalStateException("No microphone reports the requested PCM format")
            }
            val bufferSize = maxOf(minimum, format.frameSizeBytes * 4)

            val recorder = if (device != null) {
                val built = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(encoding)
                            .setSampleRate(format.sampleRate)
                            .setChannelMask(channelMask)
                            .build(),
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()
                // AudioRecord exposes device routing through setPreferredDevice,
                // not through its Builder. A false result falls back to system routing.
                built.setPreferredDevice(device)
                built
            } else {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    format.sampleRate,
                    channelMask,
                    encoding,
                    bufferSize,
                )
            }

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                throw IllegalStateException("AudioRecord could not initialize (microphone may be busy)")
            }
            record = recorder
            enablePlatformEffects(recorder.audioSessionId)
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("AudioRecord did not enter the recording state")
            }

            meter.reset()
            processor.clear()
            onState("Recording")
            startCaptureThread()
            return true
        } catch (e: SecurityException) {
            cleanup()
            onError("Microphone permission was denied. Allow RECORD_AUDIO for Voice Chat+ in Android settings.")
            return false
        } catch (e: Exception) {
            cleanup()
            onError("Microphone start failed: ${e.message ?: e.javaClass.simpleName}")
            return false
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        val worker = thread
        worker?.interrupt()
        try {
            worker?.join(JOIN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
        cleanup()
        mainHandler.post { onState("Stopped") }
    }

    private fun startCaptureThread() {
        val worker = Thread(
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                val scratch = ShortArray(PcmFormat.VOICE.samplesPerFrame(20) * 2)
                var lastUiUpdate = 0L

                while (running.get()) {
                    val recorder = record ?: break
                    val read = try {
                        recorder.read(scratch, 0, scratch.size)
                    } catch (e: Exception) {
                        mainHandler.post {
                            onError("Microphone read failed: ${e.message ?: e.javaClass.simpleName}")
                            stop()
                        }
                        break
                    }

                    if (read > 0) {
                        processor.feed(scratch, 0, read)
                        meter.update(scratch, 0, read)
                        while (processor.availableFrames > 0) {
                            val frame = processor.nextFrame() ?: break
                            onFrame?.invoke(frame)
                        }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastUiUpdate >= UI_UPDATE_INTERVAL_MS) {
                            lastUiUpdate = now
                            val current = meter.level
                            val peak = meter.peak
                            mainHandler.post { onLevel(current, peak) }
                        }
                    } else if (read < 0) {
                        val code = read
                        mainHandler.post {
                            onError("Microphone read error ($code). Stopping capture.")
                            stop()
                        }
                        break
                    }
                }
            },
            "voice-chat-plus-capture",
        )
        thread = worker
        worker.start()
    }

    private fun enablePlatformEffects(sessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(sessionId)?.let { effect ->
                    effect.enabled = true
                    effects.add(effect)
                    enabledEffectNames.add("Echo cancellation")
                }
            }
        } catch (e: Exception) {
            // Device-specific effects are optional; never fail capture because of them.
        }
        try {
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(sessionId)?.let { effect ->
                    effect.enabled = true
                    effects.add(effect)
                    enabledEffectNames.add("Noise suppression")
                }
            }
        } catch (e: Exception) {
            // Optional effect.
        }
        try {
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(sessionId)?.let { effect ->
                    effect.enabled = true
                    effects.add(effect)
                    enabledEffectNames.add("Automatic gain control")
                }
            }
        } catch (e: Exception) {
            // Optional effect.
        }
    }

    private fun cleanup() {
        running.set(false)
        record?.let { recorder ->
            try {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            } catch (e: Exception) {
                // Release is still attempted below.
            }
            try {
                recorder.release()
            } catch (e: Exception) {
                // Already released.
            }
        }
        record = null
        effects.forEach { effect ->
            try {
                effect.release()
            } catch (e: Exception) {
                // Already released.
            }
        }
        effects.clear()
        enabledEffectNames.clear()
        meter.reset()
        processor.clear()
    }

    companion object {
        private const val UI_UPDATE_INTERVAL_MS = 80L
        private const val JOIN_TIMEOUT_MS = 1_000L
    }
}
