package com.jarvis.ai.voice

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Base64
import android.util.Log
import com.jarvis.ai.BuildConfig
import com.jarvis.ai.bridge.AndroidBridge
import com.jarvis.ai.model.ProviderSettings
import com.jarvis.ai.model.VoiceSynthesisEngine
import com.jarvis.ai.security.EncryptedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Human Voice Synthesis Engine for J.A.R.V.I.S.
 * Ultra-natural conversational speech model comparable to ChatGPT Voice Mode.
 * Bridges Gemini Studio High-Fidelity Neural Speech Generation with on-device Neural WaveNet TTS.
 */
object HumanVoiceEngine {
    private const val TAG = "HumanVoiceEngine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeSpeechJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var activeAudioFile: File? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastVoiceUsed = MutableStateFlow("On-Device Neural")
    val lastVoiceUsed: StateFlow<String> = _lastVoiceUsed.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var audioFocusRequest: AudioFocusRequest? = null

    // Supported Gemini prebuilt voices
    private val validGeminiVoices = setOf("Puck", "Charon", "Kore", "Fenrir", "Aoede")

    /**
     * Primary entry point for speaking text with human realism.
     */
    fun speak(
        context: Context,
        text: String,
        speechRate: Float,
        pitch: Float,
        locale: Locale?,
        voiceProfile: VoiceProfileType,
        settings: ProviderSettings?,
        bridge: AndroidBridge,
        onDone: (() -> Unit)? = null
    ) {
        val cleanedText = TtsSanitizer.cleanForHumanSpeech(text)
        if (cleanedText.isBlank()) {
            onDone?.invoke()
            return
        }

        // Cancel any ongoing speech
        stop(bridge)

        val effectiveSettings = settings ?: loadSettingsFallback(context)
        val engine = effectiveSettings.voiceSynthesisEngine
        val apiKey = effectiveSettings.customApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }

        val canUseGeminiStudio = (engine == VoiceSynthesisEngine.GEMINI_STUDIO || engine == VoiceSynthesisEngine.HYBRID_AUTO) &&
                apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                isNetworkAvailable(context)

        if (canUseGeminiStudio) {
            _isSpeaking.value = true
            activeSpeechJob = scope.launch {
                val candidateVoice = effectiveSettings.geminiVoiceName.ifBlank { voiceProfile.geminiVoiceName }
                val targetVoice = if (validGeminiVoices.contains(candidateVoice)) candidateVoice else "Puck"

                val success = synthesizeWithGeminiStudio(
                    context = context,
                    text = cleanedText,
                    apiKey = apiKey,
                    voiceName = targetVoice,
                    bridge = bridge,
                    onDone = {
                        _isSpeaking.value = false
                        onDone?.invoke()
                    }
                )

                if (!success) {
                    // Fallback seamlessly to calibrated on-device neural voice
                    Log.i(TAG, "Gemini Studio voice unavailable, fallback to Neural on-device TTS")
                    _lastVoiceUsed.value = "Neural Device (${voiceProfile.profileName})"
                    bridge.speakDeviceNeural(
                        sanitizedText = cleanedText,
                        speechRate = speechRate,
                        pitch = pitch,
                        locale = locale,
                        voiceProfile = voiceProfile,
                        onDone = onDone
                    )
                }
            }
        } else {
            // Direct On-Device Neural Synthesis
            _lastVoiceUsed.value = "Neural Device (${voiceProfile.profileName})"
            bridge.speakDeviceNeural(
                sanitizedText = cleanedText,
                speechRate = speechRate,
                pitch = pitch,
                locale = locale,
                voiceProfile = voiceProfile,
                onDone = onDone
            )
        }
    }

    /**
     * Call Gemini TTS API with AUDIO response modality and prebuilt voice configuration.
     * Uses approved gemini-2.5-flash-preview-tts and gemini-2.5-flash models.
     */
    private suspend fun synthesizeWithGeminiStudio(
        context: Context,
        text: String,
        apiKey: String,
        voiceName: String,
        bridge: AndroidBridge,
        onDone: () -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val modelsToTry = listOf("gemini-2.5-flash-preview-tts", "gemini-2.5-flash")

        val isBengali = text.any { it in '\u0980'..'\u09FF' } ||
                LanguageDetector.detectLanguage(text) == LanguageDetector.DetectedLanguage.BENGALI

        val prompt = if (isBengali) {
            "Say in ultra-natural conversational human warmth with clear Bengali pronunciation: $text"
        } else {
            "Say in ultra-natural conversational human warmth and expressive inflection: $text"
        }

        val rootJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                val modalities = JSONArray().apply { put("AUDIO") }
                put("responseModalities", modalities)

                val speechConfig = JSONObject().apply {
                    val voiceConfig = JSONObject().apply {
                        val prebuilt = JSONObject().put("voiceName", voiceName)
                        put("prebuiltVoiceConfig", prebuilt)
                    }
                    put("voiceConfig", voiceConfig)
                }
                put("speechConfig", speechConfig)
            }
            put("generationConfig", genConfig)
        }

        val requestBodyString = rootJson.toString()

        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val body = requestBodyString.toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val err = response.body?.string()
                    Log.w(TAG, "Gemini Studio model $model returned HTTP ${response.code}: $err")
                    continue
                }

                val responseBody = response.body?.string() ?: continue
                val parsed = JSONObject(responseBody)
                val parts = parsed.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")

                var base64Audio: String? = null
                var mimeType = "audio/wav"

                if (parts != null) {
                    for (idx in 0 until parts.length()) {
                        val part = parts.optJSONObject(idx) ?: continue
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            base64Audio = inlineData.optString("data")
                            mimeType = inlineData.optString("mimeType", "audio/wav")
                            break
                        }
                    }
                }

                if (base64Audio.isNullOrBlank()) {
                    Log.w(TAG, "No audio inlineData found in model $model response")
                    continue
                }

                val rawAudioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                if (rawAudioBytes == null || rawAudioBytes.isEmpty()) {
                    continue
                }

                val playableBytes = ensurePlayableAudio(rawAudioBytes, mimeType)

                // Write to cache file
                val extension = if (mimeType.contains("mp3")) "mp3" else "wav"
                val tempFile = File(context.cacheDir, "jarvis_human_voice_${System.currentTimeMillis()}.$extension")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(playableBytes)
                    fos.flush()
                }

                withContext(Dispatchers.Main) {
                    playAudioFile(context, tempFile, voiceName, bridge, onDone)
                }
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error synthesizing with model $model", e)
            }
        }
        return@withContext false
    }

    /**
     * Guarantees raw PCM or WAV audio bytes can be played seamlessly by Android MediaPlayer.
     */
    private fun ensurePlayableAudio(bytes: ByteArray, mimeType: String): ByteArray {
        if (bytes.size < 4) return bytes

        // Already has RIFF WAV header
        if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            return bytes
        }

        // Already has MP3 header (ID3 or sync word)
        if ((bytes.size >= 3 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) ||
            (bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0)) {
            return bytes
        }

        // Raw PCM: prepend 44-byte standard RIFF header at 24000Hz (or 16000Hz) mono 16-bit
        val sampleRate = if (mimeType.contains("16000")) 16000 else 24000
        return wrapPcmInWav(bytes, sampleRate = sampleRate, channels = 1)
    }

    private fun wrapPcmInWav(pcmData: ByteArray, sampleRate: Int = 24000, channels: Int = 1): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16 // 16-bit
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val out = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, out, 0, 44)
        System.arraycopy(pcmData, 0, out, 44, pcmData.size)
        return out
    }

    /**
     * Plays generated audio file via MediaPlayer with proper audio focus and barge-in monitoring.
     */
    private fun playAudioFile(
        context: Context,
        file: File,
        voiceName: String,
        bridge: AndroidBridge,
        onDone: () -> Unit
    ) {
        try {
            stopAudio(bridge)
            activeAudioFile = file
            _lastVoiceUsed.value = "Gemini Ultra Voice ($voiceName)"

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            requestAudioFocus(audioManager)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    bridge.stopBargeInMonitoring()
                    abandonAudioFocus(audioManager)
                    cleanActiveAudioFile()
                    _isSpeaking.value = false
                    onDone()
                }
                setOnErrorListener { _, _, _ ->
                    bridge.stopBargeInMonitoring()
                    abandonAudioFocus(audioManager)
                    cleanActiveAudioFile()
                    _isSpeaking.value = false
                    onDone()
                    true
                }
                start()
                _isSpeaking.value = true
                // Start acoustic barge-in detector so user can interrupt JARVIS speaking at any millisecond
                bridge.startBargeInMonitoring()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            bridge.stopBargeInMonitoring()
            cleanActiveAudioFile()
            _isSpeaking.value = false
            onDone()
        }
    }

    fun stop(bridge: AndroidBridge? = null) {
        activeSpeechJob?.cancel()
        activeSpeechJob = null
        stopAudio(bridge)
        _isSpeaking.value = false
        bridge?.stopDeviceTts()
    }

    private fun stopAudio(bridge: AndroidBridge? = null) {
        bridge?.stopBargeInMonitoring()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (_: Exception) {}
        cleanActiveAudioFile()
    }

    private fun cleanActiveAudioFile() {
        try {
            activeAudioFile?.delete()
            activeAudioFile = null
        } catch (_: Exception) {}
    }

    private fun requestAudioFocus(audioManager: AudioManager?) {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioFocus(audioManager: AudioManager?) {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun loadSettingsFallback(context: Context): ProviderSettings {
        return try {
            val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            val encKey = prefs.getString("encrypted_custom_api_key", "") ?: ""
            val decKey = if (encKey.isNotEmpty()) EncryptedStorage.decrypt(encKey) else ""
            val engineId = prefs.getString("voice_synthesis_engine", VoiceSynthesisEngine.HYBRID_AUTO.id) ?: VoiceSynthesisEngine.HYBRID_AUTO.id
            val voiceName = prefs.getString("gemini_voice_name", "Puck") ?: "Puck"
            ProviderSettings(
                customApiKey = decKey,
                voiceSynthesisEngine = VoiceSynthesisEngine.fromId(engineId),
                geminiVoiceName = voiceName
            )
        } catch (_: Exception) {
            ProviderSettings()
        }
    }
}
