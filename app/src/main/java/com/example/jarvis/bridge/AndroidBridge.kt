package com.example.jarvis.bridge

import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.jarvis.model.DeviceTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidBridge(private val context: Context) {

    private val _telemetry = MutableStateFlow(DeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSpeakerEnabled = MutableStateFlow(true)
    val isSpeakerEnabled: StateFlow<Boolean> = _isSpeakerEnabled.asStateFlow()

    private val _speechSupported = MutableStateFlow(true)
    val speechSupported: StateFlow<Boolean> = _speechSupported.asStateFlow()

    private val _isTorchActive = MutableStateFlow(false)
    val isTorchActive: StateFlow<Boolean> = _isTorchActive.asStateFlow()

    private val _voiceRmsDb = MutableStateFlow(0f)
    val voiceRmsDb: StateFlow<Float> = _voiceRmsDb.asStateFlow()

    private val _deviceTilt = MutableStateFlow(Pair(0f, 0f))
    val deviceTilt: StateFlow<Pair<Float, Float>> = _deviceTilt.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var sensorManager: SensorManager? = null
    private var accelListener: SensorEventListener? = null

    fun isTtsInitialized(): Boolean = isTtsReady
    fun getApplicationContext(): Context = context

    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var onSpeechErrorCallback: ((Int) -> Unit)? = null
    private var onUtteranceDoneCallback: ((String?) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var torchCallback: CameraManager.TorchCallback? = null
    val echoDetector = com.example.jarvis.voice.EchoDetectionEngine()
    private var bargeInDetector: com.example.jarvis.voice.AcousticBargeInDetector? = null
    var onBargeInTriggered: (() -> Unit)? = null

    init {
        com.example.jarvis.memory.KnowledgeGraphEngine.initialize(context)
        initTts()
        initSpeechRecognizer()
        initTorchMonitoring()
        initSensorMonitoring()
        refreshTelemetry()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                var langRes = textToSpeech?.setLanguage(Locale.UK)
                if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    langRes = textToSpeech?.setLanguage(Locale.US)
                    if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                }
                isTtsReady = true
                try {
                    selectBestNeuralVoice(Locale.UK, preferMale = true)
                } catch (_: Exception) {}

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        echoDetector.notifyTtsFinished()
                        bargeInDetector?.stopMonitoring()
                        onUtteranceDoneCallback?.invoke(utteranceId)
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        echoDetector.notifyTtsFinished()
                        bargeInDetector?.stopMonitoring()
                        onUtteranceDoneCallback?.invoke(utteranceId)
                    }
                })
            }
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }
                        override fun onBeginningOfSpeech() {
                            // Immediate Voice Barge-In: Cancel active TTS as soon as user begins speaking
                            if (_isSpeaking.value) {
                                Log.d("AndroidBridge", "Voice Barge-In: onBeginningOfSpeech triggered -> cancelling active TTS immediately")
                                stopSpeaking()
                                onBargeInTriggered?.invoke()
                            }
                        }
                        override fun onRmsChanged(rmsdB: Float) {
                            _voiceRmsDb.value = (rmsdB.coerceIn(0f, 15f))
                            // Voice Barge-In on acoustic energy onset during TTS
                            if (_isSpeaking.value && rmsdB > 4.0f) {
                                Log.d("AndroidBridge", "Voice Barge-In: onRmsChanged ($rmsdB dB) -> cancelling active TTS immediately")
                                stopSpeaking()
                                onBargeInTriggered?.invoke()
                            }
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isListening.value = false
                            _voiceRmsDb.value = 0f
                        }
                        override fun onError(error: Int) {
                            _isListening.value = false
                            _voiceRmsDb.value = 0f
                            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                                try {
                                    speechRecognizer?.cancel()
                                } catch (_: Exception) {}
                            }
                            onSpeechErrorCallback?.invoke(error)
                        }
                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull() ?: ""
                            val cleaned = echoDetector.cleanupLeadingEcho(recognized)
                            if (cleaned.isNotBlank()) {
                                if (echoDetector.shouldRejectAsEcho(cleaned)) {
                                    Log.d("AndroidBridge", "Echo Shield: Ignored self-feedback echo: $cleaned")
                                    onSpeechErrorCallback?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
                                    return
                                }
                                _liveTranscript.value = cleaned
                                onSpeechResultCallback?.invoke(cleaned)
                            } else {
                                onSpeechErrorCallback?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            if (_isSpeaking.value) {
                                Log.d("AndroidBridge", "Voice Barge-In: onPartialResults -> cancelling active TTS immediately")
                                stopSpeaking()
                                onBargeInTriggered?.invoke()
                            }
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            matches?.firstOrNull()?.let {
                                _liveTranscript.value = it
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                _speechSupported.value = true
            } else {
                _speechSupported.value = false
            }
        } catch (_: Exception) {
            _speechSupported.value = false
        }
    }

    private fun initTorchMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cb = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    _isTorchActive.value = enabled
                    _telemetry.value = _telemetry.value.copy(isFlashlightOn = enabled)
                }
            }
            torchCallback = cb
            try {
                cameraManager.registerTorchCallback(cb, null)
            } catch (_: Exception) {}
        }
    }

    private fun initSensorMonitoring() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accel != null) {
                accelListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event != null && event.values.size >= 2) {
                            val x = event.values[0]
                            val y = event.values[1]
                            _deviceTilt.value = Pair(-x / 9.81f, y / 9.81f)
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager?.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (_: Exception) {}
    }

    fun toggleMicMute() {
        _isMicMuted.value = !_isMicMuted.value
        if (_isMicMuted.value && _isListening.value) {
            stopListening()
        }
    }

    fun toggleSpeaker() {
        _isSpeakerEnabled.value = !_isSpeakerEnabled.value
        if (!_isSpeakerEnabled.value && _isSpeaking.value) {
            stopSpeaking()
        }
    }

    fun interruptAndListen(onResult: (String) -> Unit) {
        interruptAndListen(onResult, null)
    }

    fun interruptAndListen(onResult: (String) -> Unit, onError: ((Int) -> Unit)?) {
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        startListening(onResult, onError)
    }

    fun startListening(onResult: (String) -> Unit) {
        startListening(onResult, null)
    }

    fun startListening(onResult: (String) -> Unit, onError: ((Int) -> Unit)?) {
        if (_isMicMuted.value) {
            _isListening.value = false
            return
        }
        if (_isSpeaking.value) {
            stopSpeaking()
        }
        bargeInDetector?.stopMonitoring()
        onSpeechResultCallback = onResult
        onSpeechErrorCallback = onError
        _liveTranscript.value = ""

        mainHandler.post {
            try {
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}

                if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
                    initSpeechRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.w("AndroidBridge", "startListening failed: ${e.message}")
                _isListening.value = false
                onError?.invoke(SpeechRecognizer.ERROR_CLIENT)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
            _voiceRmsDb.value = 0f
        }
    }

    fun setUtteranceDoneListener(listener: ((String?) -> Unit)?) {
        onUtteranceDoneCallback = listener
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopSpeaking()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    var repository: com.example.jarvis.storage.JarvisRepository? = null

    fun startBargeInMonitoring() {
        if (bargeInDetector == null) {
            bargeInDetector = com.example.jarvis.voice.AcousticBargeInDetector(context) {
                Log.i("AndroidBridge", "Acoustic barge-in detected, stopping audio output immediately")
                stopSpeaking()
                onBargeInTriggered?.invoke()
            }
        }
        bargeInDetector?.startMonitoring()
    }

    fun stopBargeInMonitoring() {
        bargeInDetector?.stopMonitoring()
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        locale: Locale? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!_isSpeakerEnabled.value || text.isBlank()) {
            onDone?.invoke()
            return
        }

        // Cancel any currently playing TTS to strictly prevent overlapping playback
        stopDeviceTts()

        val settings = repository?.settings?.value
        val effectiveRate = speechRate * (settings?.speechRate ?: 1.0f)
        val effectivePitch = pitch * (settings?.speechPitch ?: 1.0f)

        speakDeviceNeural(
            sanitizedText = text,
            speechRate = effectiveRate,
            pitch = effectivePitch,
            locale = locale,
            onDone = onDone
        )
    }

    /**
     * Speaks using local calibrated on-device Neural WaveNet TTS.
     */
    fun speakDeviceNeural(
        sanitizedText: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        locale: Locale? = null,
        voiceProfile: com.example.jarvis.voice.VoiceProfileType? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (sanitizedText.isBlank() || !_isSpeakerEnabled.value) {
            onDone?.invoke()
            return
        }

        if (textToSpeech == null || !isTtsReady) {
            initTts()
            mainHandler.postDelayed({
                speakDeviceNeural(sanitizedText, speechRate, pitch, locale, voiceProfile, onDone)
            }, 350)
            return
        }

        requestAudioFocus()
        val targetLocale = locale ?: (voiceProfile?.preferredLocaleTag?.let { Locale.forLanguageTag(it) } ?: Locale.UK)
        try {
            var langRes = textToSpeech?.setLanguage(targetLocale)
            if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                langRes = textToSpeech?.setLanguage(Locale.US)
                if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.getDefault())
                }
            }
            selectBestNeuralVoice(targetLocale, preferMale = voiceProfile != com.example.jarvis.voice.VoiceProfileType.FRIDAY)
        } catch (_: Exception) {}

        textToSpeech?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        textToSpeech?.setPitch(pitch.coerceIn(0.5f, 2.0f))
        val utteranceId = "JARVIS_${System.currentTimeMillis()}"
        if (onDone != null) {
            val previousDone = onUtteranceDoneCallback
            onUtteranceDoneCallback = { id ->
                previousDone?.invoke(id)
                if (id == utteranceId) {
                    abandonAudioFocus()
                    bargeInDetector?.stopMonitoring()
                    onDone()
                }
            }
        }
        _isSpeaking.value = true
        echoDetector.notifyTtsStarted(sanitizedText)

        // Real-time acoustic barge-in detector: monitors microphone for user interruption during TTS
        startBargeInMonitoring()

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        val result = textToSpeech?.speak(sanitizedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
            echoDetector.notifyTtsFinished()
            bargeInDetector?.stopMonitoring()
            abandonAudioFocus()
            onDone?.invoke()
        }
    }

    /**
     * Selects the highest quality Neural / Network / WaveNet voice available on device.
     */
    fun selectBestNeuralVoice(targetLocale: Locale, preferMale: Boolean = true): Boolean {
        val tts = textToSpeech ?: return false
        return try {
            val availableVoices = tts.voices ?: return false
            val matchingVoices = availableVoices.filter { voice ->
                voice.locale.language.equals(targetLocale.language, ignoreCase = true)
            }
            if (matchingVoices.isEmpty()) return false

            val bestVoice = matchingVoices.maxByOrNull { voice ->
                var score = 0
                val name = voice.name.lowercase()
                if (name.contains("network")) score += 50
                if (name.contains("neural") || name.contains("wavenet")) score += 40
                if (name.contains("natural")) score += 30
                if (voice.quality >= 400) score += 20
                if (preferMale && (name.contains("male") || name.contains("rjs") || name.contains("g-network") || name.contains("en-gb-x-rjs"))) score += 20
                if (!preferMale && (name.contains("female") || name.contains("sfg") || name.contains("woman"))) score += 20
                score
            }

            if (bestVoice != null) {
                tts.voice = bestVoice
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun getInstalledTtsVoices(): List<String> {
        return try {
            textToSpeech?.voices?.map { "${it.name} (${it.locale.displayLanguage})" }?.take(15) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun stopDeviceTts() {
        textToSpeech?.stop()
        abandonAudioFocus()
        _isSpeaking.value = false
    }

    fun stopSpeaking() {
        bargeInDetector?.stopMonitoring()
        stopDeviceTts()
    }

    // FLASH LIGHT CONTROLLER & VERIFIER
    fun toggleFlashlight(enable: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            try {
                val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return false
                cameraManager.setTorchMode(cameraId, enable)
                _isTorchActive.value = enable
                _telemetry.value = _telemetry.value.copy(isFlashlightOn = enable)
                return true
            } catch (_: CameraAccessException) {
                return false
            } catch (_: Exception) {
                return false
            }
        }
        return false
    }

    fun isFlashlightOn(): Boolean = _isTorchActive.value

    // BATTERY TELEMETRY & VERIFICATION
    fun getBatteryDetailedStatus(): Map<String, Any> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Adapter"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Qi Wireless Induction"
            else -> "Not Plugged"
        }

        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val voltage = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0
        val health = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good / Optimal"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Depleted"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        return mapOf(
            "percent" to batteryPct,
            "isCharging" to isCharging,
            "plugType" to plugType,
            "temperatureC" to temperature,
            "voltageV" to voltage,
            "health" to health
        )
    }

    // NETWORK & WI-FI TELEMETRY
    fun getNetworkStatus(): Map<String, Any> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(network)

        val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
        val isMetered = caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        val transport = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "Encrypted VPN"
            else -> "Other"
        }

        return mapOf(
            "isConnected" to isConnected,
            "isValidated" to isValidated,
            "isMetered" to isMetered,
            "transport" to transport
        )
    }

    fun getWifiDetails(): Map<String, Any> {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiEnabled = wm?.isWifiEnabled ?: false
        val info = wm?.connectionInfo
        val ssid = info?.ssid?.replace("\"", "") ?: "Unknown"
        val linkSpeed = info?.linkSpeed ?: 0
        val rssi = info?.rssi ?: -100

        return mapOf(
            "isWifiEnabled" to isWifiEnabled,
            "ssid" to if (ssid == "<unknown ssid>") "Connected (Masked by OS)" else ssid,
            "linkSpeedMbps" to linkSpeed,
            "signalStrengthDbm" to rssi
        )
    }

    // BLUETOOTH TELEMETRY
    fun getBluetoothStatus(): Map<String, Any> {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = bm?.adapter
        val isSupported = adapter != null
        val isEnabled = adapter?.isEnabled ?: false
        val name = try { adapter?.name ?: "Local Bluetooth" } catch (_: SecurityException) { "Local Bluetooth" }
        val bondedCount = try { adapter?.bondedDevices?.size ?: 0 } catch (_: SecurityException) { 0 }

        return mapOf(
            "isSupported" to isSupported,
            "isEnabled" to isEnabled,
            "name" to name,
            "pairedDevicesCount" to bondedCount
        )
    }

    // AUDIO & VOLUME CONTROLS
    fun getVolumeInfo(): Map<String, Int> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return emptyMap()
        val musicVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val musicMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val ringVol = am.getStreamVolume(AudioManager.STREAM_RING)
        val ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING)
        val alarmVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        return mapOf(
            "musicCurrent" to musicVol,
            "musicMax" to musicMax,
            "ringCurrent" to ringVol,
            "ringMax" to ringMax,
            "alarmCurrent" to alarmVol,
            "alarmMax" to alarmMax
        )
    }

    fun setMusicVolume(volumePercent: Int): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (volumePercent.coerceIn(0, 100) * max) / 100
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        refreshTelemetry()
        return true
    }

    fun setVolume(volumePercent: Int): Boolean = setMusicVolume(volumePercent)

    fun vibrate(durationMs: Long = 100): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                v?.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                v?.vibrate(durationMs)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // MEDIA CONTROLS
    fun dispatchMediaControl(action: String): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val keyCode = when (action.lowercase()) {
            "play", "pause", "play_pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return false
        }
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(eventDown)
        am.dispatchMediaKeyEvent(eventUp)
        return true
    }

    // DISPLAY & BRIGHTNESS
    fun getScreenBrightness(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            -1
        }
    }

    fun openDisplaySettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // APP LAUNCHER
    fun getInstalledAppsList(): List<Pair<String, String>> {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            resolveInfos.mapNotNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm)?.toString()?.trim()
                val pkg = resolveInfo.activityInfo?.packageName
                if (!label.isNullOrEmpty() && !pkg.isNullOrEmpty()) {
                    Pair(label, pkg)
                } else null
            }.distinctBy { it.second }.sortedBy { it.first }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun cleanAppQuery(input: String): String {
        var text = input.trim()
        val removePhrases = listOf(
            "open app", "launch app", "start app", "run app",
            "open karo", "launch karo", "start karo", "open kr", "open koro",
            "open de", "open do", "launch kr", "launch koro",
            "open", "launch", "start", "run",
            "kholna", "khol de", "khol do", "kholo", "khol", "khole",
            "खोलो", "खोल", "খুলুন", "খোল", "চালু করো", "চালু কর",
            "open app named", "open app called", "app"
        )
        for (phrase in removePhrases) {
            text = text.replace(Regex("""(?i)\b${Regex.escape(phrase)}\b"""), " ")
        }
        return text.replace(Regex("""\s+"""), " ").trim()
    }

    fun launchAppByNameOrPackage(query: String): Pair<Boolean, String> {
        val pm = context.packageManager
        val cleanQuery = cleanAppQuery(query)
        if (cleanQuery.isBlank()) {
            return Pair(false, "Ye app installed nahi hai bhai.")
        }

        // 1. Direct package check
        if (cleanQuery.contains(".")) {
            val directIntent = pm.getLaunchIntentForPackage(cleanQuery)
            if (directIntent != null) {
                directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(directIntent)
                    Pair(true, "Successfully opened $cleanQuery.")
                } catch (e: Exception) {
                    Pair(false, "Failed to launch $cleanQuery: ${e.message}")
                }
            }
        }

        // 2. Query all installed launcher apps
        val allApps = getInstalledAppsList()
        val lowerQuery = cleanQuery.lowercase()

        // a. Exact Label Match (case-insensitive)
        val exactMatch = allApps.find { it.first.equals(cleanQuery, ignoreCase = true) }
        if (exactMatch != null) {
            return launchPackage(pm, exactMatch.second, exactMatch.first)
        }

        // b. Normalized Label Match (ignoring spaces & special chars e.g. "play store" vs "google play store")
        val queryNorm = lowerQuery.replace(Regex("""[^a-z0-9]"""), "")
        if (queryNorm.isNotEmpty()) {
            val normExact = allApps.filter { app ->
                val appNorm = app.first.lowercase().replace(Regex("""[^a-z0-9]"""), "")
                appNorm == queryNorm
            }
            if (normExact.size == 1) {
                return launchPackage(pm, normExact[0].second, normExact[0].first)
            }
        }

        // c. Partial / Substring Matches
        val partialMatches = allApps.filter { app ->
            val appLower = app.first.lowercase()
            val pkgLower = app.second.lowercase()
            appLower.contains(lowerQuery) || lowerQuery.contains(appLower) || pkgLower.contains(lowerQuery)
        }

        if (partialMatches.size == 1) {
            return launchPackage(pm, partialMatches[0].second, partialMatches[0].first)
        }

        if (partialMatches.size > 1) {
            val wordMatches = partialMatches.filter { app ->
                val words = app.first.lowercase().split(" ", "-", "_")
                words.contains(lowerQuery)
            }
            if (wordMatches.size == 1) {
                return launchPackage(pm, wordMatches[0].second, wordMatches[0].first)
            }
            return Pair(false, "Kaunsa app kholun?")
        }

        return Pair(false, "Ye app installed nahi hai bhai.")
    }

    private fun launchPackage(pm: PackageManager, packageName: String, appLabel: String): Pair<Boolean, String> {
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            return Pair(false, "Failed to launch $appLabel: No launch activity found.")
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            Pair(true, "Successfully opened $appLabel.")
        } catch (e: Exception) {
            Pair(false, "Failed to launch $appLabel: ${e.message}")
        }
    }

    // YOUTUBE SEARCH & PLAYBACK (REAL INTENT ACTION)
    fun searchYouTube(query: String): Pair<Boolean, String> {
        val pm = context.packageManager
        val cleanQuery = query.trim()
        val encodedQuery = try {
            java.net.URLEncoder.encode(cleanQuery, "UTF-8")
        } catch (_: Exception) { cleanQuery }

        val youtubePackage = "com.google.android.youtube"
        val isAppInstalled = try {
            pm.getPackageInfo(youtubePackage, 0)
            true
        } catch (_: Exception) { false }

        if (isAppInstalled) {
            try {
                val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(youtubePackage)
                    putExtra("query", cleanQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appIntent)
                return Pair(true, "Dispatched query '$cleanQuery' directly to YouTube application.")
            } catch (_: Exception) {
                try {
                    val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                        setPackage(youtubePackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(viewIntent)
                    return Pair(true, "Opened YouTube search results for '$cleanQuery'.")
                } catch (_: Exception) {}
            }
        }

        // Fallback: Web browser search for YouTube
        return try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Pair(true, "Dispatched YouTube search for '$cleanQuery' via web browser.")
        } catch (e: Exception) {
            Pair(false, "Could not launch YouTube query: ${e.message}")
        }
    }

    // PHONE CALL & CONTACT RESOLVER
    fun makePhoneCall(target: String): Pair<Boolean, String> {
        val clean = target.trim()
        var phoneNumber: String? = null
        var contactName: String = clean

        // 1. If target is already numeric phone digits
        val digitOnly = clean.filter { it.isDigit() || it == '+' }
        if (digitOnly.length >= 3 && digitOnly.any { it.isDigit() }) {
            phoneNumber = digitOnly
        } else {
            // 2. Resolve from contacts if permission granted
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val cursor: Cursor? = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                        arrayOf("%$clean%"),
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIdx >= 0) contactName = it.getString(nameIdx)
                            if (numIdx >= 0) phoneNumber = it.getString(numIdx)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        val targetNum = phoneNumber ?: clean
        val hasCallPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        return try {
            if (hasCallPermission && phoneNumber != null) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$targetNum")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                Pair(true, "Initiated direct voice call to $contactName ($targetNum).")
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetNum")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                Pair(true, "Opened system phone dialer for $contactName ($targetNum).")
            }
        } catch (e: Exception) {
            Pair(false, "Failed to initiate call: ${e.message}")
        }
    }

    // ==========================================
    // WHATSAPP & SMS AUTOMATION
    // ==========================================
    fun resolveContactPhoneNumber(nameOrNumber: String): Pair<String, String?> {
        val clean = nameOrNumber.trim()
        val digitOnly = clean.filter { it.isDigit() || it == '+' }
        if (digitOnly.length >= 3 && digitOnly.any { it.isDigit() }) {
            return Pair(clean, digitOnly)
        }
        var contactName = clean
        var phoneNumber: String? = null
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val cursor: Cursor? = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                    arrayOf("%$clean%"),
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nameIdx >= 0) contactName = it.getString(nameIdx)
                        if (numIdx >= 0) phoneNumber = it.getString(numIdx)
                    }
                }
            } catch (_: Exception) {}
        }
        return Pair(contactName, phoneNumber)
    }

    fun sendWhatsAppMessage(recipient: String, messageText: String): Pair<Boolean, String> {
        val (contactName, phone) = resolveContactPhoneNumber(recipient)
        val cleanNumber = phone?.filter { it.isDigit() }

        return try {
            if (!cleanNumber.isNullOrBlank()) {
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(messageText)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Dispatched WhatsApp message draft to $contactName ($cleanNumber): \"$messageText\"")
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Opened WhatsApp with drafted text: \"$messageText\"")
            }
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Send Message via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                Pair(true, "WhatsApp not reachable directly; opened system message share dialog.")
            } catch (ex: Exception) {
                Pair(false, "Failed to send WhatsApp message: ${ex.message}")
            }
        }
    }

    fun sendSmsMessage(recipient: String, messageText: String): Pair<Boolean, String> {
        val (contactName, phone) = resolveContactPhoneNumber(recipient)
        val targetNum = phone ?: recipient.trim()

        return try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$targetNum")
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            Pair(true, "Initiated SMS dispatch to $contactName ($targetNum): \"$messageText\"")
        } catch (e: Exception) {
            Pair(false, "Failed to initiate SMS: ${e.message}")
        }
    }

    // ==========================================
    // CALENDAR & APPOINTMENT SYNC
    // ==========================================
    fun addCalendarEvent(
        title: String,
        description: String = "",
        location: String = "",
        beginTimeMillis: Long = System.currentTimeMillis() + 3600000L,
        endTimeMillis: Long = beginTimeMillis + 3600000L
    ): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Created Calendar event draft for \"$title\".")
        } catch (e: Exception) {
            Pair(false, "Failed to create calendar event: ${e.message}")
        }
    }

    fun queryUpcomingCalendarEvents(maxDays: Int = 1): List<String> {
        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return emptyList()

        val results = mutableListOf<String>()
        val startMillis = System.currentTimeMillis()
        val endMillis = startMillis + (maxDays * 24 * 60 * 60 * 1000L)

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.EVENT_LOCATION
        )
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )
            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val locIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val timeFormat = SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

                while (it.moveToNext()) {
                    val title = if (titleIdx >= 0) it.getString(titleIdx) else "Event"
                    val dt = if (startIdx >= 0) it.getLong(startIdx) else 0L
                    val loc = if (locIdx >= 0) it.getString(locIdx) else null
                    val timeStr = if (dt > 0) timeFormat.format(dt) else "Today"
                    val locStr = if (!loc.isNullOrBlank()) " at $loc" else ""
                    results.add("• $timeStr: $title$locStr")
                }
            }
        } catch (_: Exception) {}
        return results
    }

    fun openCalendar(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Opened system calendar.")
        } catch (e: Exception) {
            Pair(false, "Failed to open calendar: ${e.message}")
        }
    }

    // BATTERY OPTIMIZATION & OVERLAY PERMISSIONS
    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    fun requestIgnoreBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                    true
                } catch (_: Exception) { false }
            }
        }
        return false
    }

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    fun openOverlaySettings(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) { false }
        } else true
    }

    // CLIPBOARD
    fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readFromClipboard(): String? {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        } catch (_: Exception) {
            null
        }
    }

    // OPEN URL
    fun openUrl(url: String): Boolean {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // SETTINGS LAUNCHERS
    fun openAndroidSettings(targetScreen: String = "general"): Boolean {
        val action = when (targetScreen.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notifications", "notification_listener" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            "app_details" -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                openUrl("https://play.google.com/store/apps/details?id=$packageName")
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openCameraApp(): Boolean {
        return try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openMapsApp(query: String = ""): Boolean {
        return try {
            val uri = if (query.isNotBlank()) {
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            } else {
                Uri.parse("geo:0,0")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openUrl("https://maps.google.com")
        }
    }

    // HARDWARE & SYSTEM DIAGNOSTICS
    fun getHardwareDiagnostics(): Map<String, String> {
        val runtime = Runtime.getRuntime()
        val maxHeapMB = runtime.maxMemory() / (1024 * 1024)
        val totalHeapMB = runtime.totalMemory() / (1024 * 1024)
        val freeHeapMB = runtime.freeMemory() / (1024 * 1024)
        val availableHeapMB = maxHeapMB - totalHeapMB + freeHeapMB

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val storageAvailableGB = String.format("%.2f", bytesAvailable / (1024.0 * 1024.0 * 1024.0))

        return mapOf(
            "manufacturer" to Build.MANUFACTURER.uppercase(),
            "model" to Build.MODEL,
            "device" to Build.DEVICE,
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkLevel" to Build.VERSION.SDK_INT.toString(),
            "board" to Build.BOARD,
            "supportedAbis" to Build.SUPPORTED_ABIS.joinToString(", "),
            "availableHeap" to "$availableHeapMB MB",
            "freeStorage" to "$storageAvailableGB GB"
        )
    }

    fun refreshTelemetry() {
        val batteryDetails = getBatteryDetailedStatus()
        val batteryLevel = (batteryDetails["percent"] as? Int) ?: 100
        val isCharging = (batteryDetails["isCharging"] as? Boolean) ?: false

        val netStatus = getNetworkStatus()
        val networkType = (netStatus["transport"] as? String) ?: "Online"

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val volumePercent = if (maxVol > 0) ((currentVol.toFloat() / maxVol) * 100).toInt() else 50

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTimeString = timeFormat.format(Date())

        val runtime = Runtime.getRuntime()
        val availableMem = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (1024 * 1024)

        _telemetry.value = _telemetry.value.copy(
            batteryPercent = batteryLevel.coerceIn(0, 100),
            isCharging = isCharging,
            networkType = networkType,
            volumePercent = volumePercent,
            memoryAvailableMB = availableMem,
            currentTimeString = currentTimeString
        )
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && torchCallback != null) {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cameraManager?.unregisterTorchCallback(torchCallback!!)
            }
        } catch (_: Exception) {}
    }
}
