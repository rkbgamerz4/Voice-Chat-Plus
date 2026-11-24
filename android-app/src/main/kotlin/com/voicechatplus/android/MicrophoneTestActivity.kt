package com.voicechatplus.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.voicechatplus.audio.AudioDeviceDirection
import com.voicechatplus.audio.AudioDeviceInfo as DeviceInfo
import com.voicechatplus.audio.AudioSessionState
import com.voicechatplus.audio.AudioSessionStateMachine
import com.voicechatplus.audio.PcmFormat
import kotlin.math.sin

/**
 * Functional microphone test screen. Every displayed level comes from real
 * AudioRecord samples; no simulated or hardcoded values are used.
 */
class MicrophoneTestActivity : Activity() {

    private lateinit var permissionStatus: TextView
    private lateinit var recordingStatus: TextView
    private lateinit var meter: LevelMeterView
    private lateinit var sampleRateValue: TextView
    private lateinit var channelValue: TextView
    private lateinit var deviceValue: TextView
    private lateinit var effectsValue: TextView
    private lateinit var devicesValue: TextView
    private lateinit var messageValue: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var deviceSpinner: Spinner

    private val session = AudioSessionStateMachine()
    private var capture: MicrophoneCapture? = null
    private var playback: SpeakerPlayback? = null
    private var focusManager: AudioFocusManager? = null
    private var androidInputDevices: List<AudioDeviceInfo> = emptyList()
    private var selectedDevice: AudioDeviceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Voice Chat+ — Microphone Test"
        buildUi()

        capture = MicrophoneCapture(
            onLevel = { level, peak -> meter.update(level, peak) },
            onState = { status ->
                recordingStatus.text = status
                updateButtons()
            },
            onError = { message -> showError(message) },
        )
        playback = SpeakerPlayback { showError(it) }
        focusManager = AudioFocusManager(this) { granted -> onFocusChanged(granted) }

        refreshPermissionStatus()
        refreshDevices()
        updateButtons()
        setMessage("Ready. Tap Start microphone to capture real input.")
    }

    override fun onDestroy() {
        stopCapture()
        playback?.stop()
        super.onDestroy()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#05070C"))
            val padding = dp(20)
            setPadding(padding, padding, padding, padding)
        }

        page.addView(TextView(this).apply {
            text = "MICROPHONE TEST"
            setTextColor(Color.parseColor("#00A8FF"))
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        })
        page.addView(TextView(this).apply {
            text = "Live AudioRecord diagnostics"
            setTextColor(Color.parseColor("#96A8BE"))
            textSize = 13f
        })
        page.addView(spacer(20))

        val statusCard = section("Status")
        permissionStatus = addRow(statusCard, "Permission", "Checking…")
        recordingStatus = addRow(statusCard, "Recording", "Stopped")
        deviceValue = addRow(statusCard, "Input device", "System default")
        sampleRateValue = addRow(statusCard, "Sample rate", "${PcmFormat.VOICE.sampleRate} Hz")
        channelValue = addRow(statusCard, "Channels", "${PcmFormat.VOICE.channels} (mono)")
        effectsValue = addRow(statusCard, "DSP effects", "Not active")
        page.addView(statusCard)

        val meterCard = section("Input level")
        meter = LevelMeterView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28),
            )
        }
        meterCard.addView(meter)
        meterCard.addView(TextView(this).apply {
            text = "Bar = live peak from captured PCM. White marker = decaying peak."
            setTextColor(Color.parseColor("#96A8BE"))
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        })
        page.addView(meterCard)

        val deviceCard = section("Audio devices")
        deviceCard.addView(TextView(this).apply {
            text = "Input device (AudioRecord source override)"
            setTextColor(Color.parseColor("#96A8BE"))
            textSize = 13f
        })
        deviceSpinner = Spinner(this).apply { layoutParams = deviceCardLayoutParams() }
        deviceCard.addView(deviceSpinner)
        devicesValue = TextView(this).apply {
            text = "Enumerating…"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 12f
            setPadding(0, dp(10), 0, 0)
        }
        deviceCard.addView(devicesValue)
        page.addView(deviceCard)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(4) }
        }
        startButton = Button(this).apply {
            text = "Start microphone"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#00A8FF"))
            setOnClickListener { startCapture() }
        }
        stopButton = Button(this).apply {
            text = "Stop"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(8)
            }
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1B60FF"))
            setOnClickListener { stopCapture() }
        }
        controls.addView(startButton)
        controls.addView(stopButton)
        page.addView(controls)

        page.addView(Button(this).apply {
            text = "Play 440 Hz test tone"
            layoutParams = deviceCardLayoutParams().apply { topMargin = dp(8) }
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#09121E"))
            setOnClickListener { playTestTone() }
        })

        messageValue = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#96A8BE"))
            textSize = 13f
            setPadding(0, dp(12), 0, 0)
        }
        page.addView(messageValue)

        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun startCapture() {
        when (session.state) {
            is AudioSessionState.Running,
            is AudioSessionState.Suspended,
            is AudioSessionState.AwaitingPermission,
            -> return

            else -> if (!session.requestPermission()) return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setMessage("Requesting RECORD_AUDIO permission…")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE)
            return
        }

        session.permissionGranted()
        if (focusManager?.request() != true) {
            session.fail("Audio focus was not granted")
            showError("The system did not grant audio focus. Stop other voice apps and try again.")
            updateButtons()
            return
        }

        if (capture?.start(selectedDevice) == true) {
            session.onPrepared()
            recordingStatus.text = "Recording"
            sampleRateValue.text = "${capture?.sampleRate} Hz"
            channelValue.text = "${capture?.channelCount} (mono)"
            effectsValue.text = capture?.enabledEffects?.joinToString(", ")?.ifEmpty { "None available" }
                ?: "None available"
            setMessage("Capturing real microphone samples.")
        } else {
            session.fail("Microphone could not start")
        }
        updateButtons()
    }

    private fun stopCapture() {
        capture?.stop()
        focusManager?.abandon()
        session.stop()
        meter.reset()
        recordingStatus.text = "Stopped"
        updateButtons()
    }

    private fun onFocusChanged(granted: Boolean) {
        if (granted) {
            if (session.state is AudioSessionState.Suspended) {
                session.onFocusGained()
                val restarted = capture?.start(selectedDevice) == true
                recordingStatus.text = if (restarted) {
                    "Recording (focus restored)"
                } else {
                    "Focus restored — press Start microphone"
                }
                updateButtons()
            }
        } else if (session.state is AudioSessionState.Running) {
            session.onFocusLost()
            capture?.stop()
            recordingStatus.text = "Suspended (audio focus lost)"
            updateButtons()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode != REQUEST_MICROPHONE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        refreshPermissionStatus()
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            setMessage("Microphone permission granted.")
            startCapture()
        } else {
            val permanent = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            session.permissionDenied(permanent)
            if (permanent) {
                showError("Microphone access is blocked. Open Android Settings > Apps > Voice Chat+ > Permissions and allow Microphone.")
            } else {
                showError("Microphone permission was denied. Tap Start microphone to try again.")
            }
            updateButtons()
        }
    }

    private fun refreshPermissionStatus() {
        permissionStatus.text = if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            "Granted"
        } else {
            "Not granted"
        }
    }

    private fun refreshDevices() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val all = AndroidAudioDevices.enumerate(manager)
        devicesValue.text = if (all.isEmpty()) {
            "This device reports no audio endpoints."
        } else {
            all.joinToString("\n") { device ->
                "${device.direction.name}: ${device.displayName} (id=${device.id}${if (device.isBuiltIn) ", built-in" else ""})"
            }
        }

        androidInputDevices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList()
        val names = mutableListOf("System default")
        names.addAll(androidInputDevices.mapIndexed { index, device ->
            device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Input ${device.id} ($index)"
        })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter
        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedDevice = if (position == 0) null else androidInputDevices.getOrNull(position - 1)
                deviceValue.text = selectedDevice?.productName?.toString()?.takeIf { it.isNotBlank() }
                    ?: "System default"
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedDevice = null
            }
        }
    }

    private fun playTestTone() {
        val player = playback ?: return
        if (!player.isRunning && !player.start()) {
            showError("AudioTrack could not start playback.")
            return
        }
        val sampleRate = PcmFormat.VOICE.sampleRate
        val samples = ShortArray(sampleRate) { index ->
            (8_000 * sin(index * 2.0 * Math.PI * 440.0 / sampleRate)).toInt().toShort()
        }
        player.enqueue(samples)
        setMessage("Playing a 440 Hz tone through AudioTrack (voice communication stream).")
    }

    private fun updateButtons() {
        val running = capture?.isRunning == true
        startButton.isEnabled = !running
        stopButton.isEnabled = running
    }

    private fun showError(message: String) {
        messageValue.text = message
        messageValue.setTextColor(Color.parseColor("#FF6B6B"))
    }

    private fun setMessage(message: String) {
        messageValue.text = message
        messageValue.setTextColor(Color.parseColor("#96A8BE"))
    }

    private fun section(title: String): LinearLayout {
        val padding = dp(16)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#09121E"))
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(12) }
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.parseColor("#00A8FF"))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun addRow(container: LinearLayout, label: String, initial: String): TextView {
        val value = TextView(this).apply {
            text = initial
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 14f
        }
        container.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) }
            addView(TextView(context).apply {
                text = label
                setTextColor(Color.parseColor("#96A8BE"))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(dp(130), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(value)
        })
        return value
    }

    private fun deviceCardLayoutParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun spacer(dpValue: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(dpValue),
        )
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val REQUEST_MICROPHONE = 1001
    }
}
