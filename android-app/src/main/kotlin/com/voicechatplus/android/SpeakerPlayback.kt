package com.voicechatplus.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.voicechatplus.audio.PcmFormat
import com.voicechatplus.audio.PcmRingBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real PCM playback using AudioTrack in streaming mode with voice
 * communication attributes. Callers enqueue PCM frames; the playback thread
 * drains a bounded queue so callers never block on AudioTrack.write().
 */
class SpeakerPlayback(private val onError: (String) -> Unit) {

    private val running = AtomicBoolean(false)
    private val queue = PcmRingBuffer(QUEUE_CAPACITY_SAMPLES)
    private var track: AudioTrack? = null
    private var thread: Thread? = null

    val isRunning: Boolean get() = running.get()

    fun start(): Boolean {
        if (!running.compareAndSet(false, true)) return false
        try {
            val format = PcmFormat.VOICE
            val minimum = AudioTrack.getMinBufferSize(
                format.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minimum <= 0) throw IllegalStateException("No output reports the requested PCM format")

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(format.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val player = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(maxOf(minimum, format.frameSizeBytes * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            player.play()
            track = player

            val worker = Thread(
                {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                    val buffer = ShortArray(format.samplesPerFrame(20))
                    while (running.get()) {
                        val output = track ?: break
                        val read = queue.read(buffer)
                        if (read > 0) {
                            val written = try {
                                output.write(buffer, 0, read)
                            } catch (e: Exception) {
                                onError("Speaker write failed: ${e.message ?: e.javaClass.simpleName}")
                                break
                            }
                            if (written < 0) {
                                onError("AudioTrack returned write error $written")
                                break
                            }
                        } else {
                            try {
                                Thread.sleep(IDLE_SLEEP_MS)
                            } catch (e: InterruptedException) {
                                break
                            }
                        }
                    }
                },
                "voice-chat-plus-playback",
            )
            thread = worker
            worker.start()
            return true
        } catch (e: Exception) {
            running.set(false)
            track?.release()
            track = null
            onError("Speaker start failed: ${e.message ?: e.javaClass.simpleName}")
            return false
        }
    }

    /** Queues PCM samples for playback. Overflow drops the oldest audio. */
    fun enqueue(samples: ShortArray) {
        queue.write(samples)
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
        track?.let { player ->
            try {
                player.stop()
            } catch (e: Exception) {
                // Already stopped.
            }
            try {
                player.release()
            } catch (e: Exception) {
                // Already released.
            }
        }
        track = null
        queue.clear()
    }

    companion object {
        private const val QUEUE_CAPACITY_SAMPLES = 48_000
        private const val IDLE_SLEEP_MS = 10L
        private const val JOIN_TIMEOUT_MS = 1_000L
    }
}
