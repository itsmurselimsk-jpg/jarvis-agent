package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * State machine for continuous background acoustic wake-word detection and command listening.
 */
class ContinuousWakeEngine(
    private val context: Context,
    private val onWakeWordDetected: (wakeResult: LanguageDetector.WakeWordCheck) -> Unit,
    private val onCommandReceived: (commandText: String, detectedLang: LanguageDetector.DetectedLanguage) -> Unit,
    private val onTranscriptUpdated: (partial: String) -> Unit = {}
) {
    companion object {
        private const val TAG = "ContinuousWakeEngine"
    }

    enum class EngineState {
        PASSIVE_WAKE_MONITORING,
        ACTIVE_COMMAND_LISTENING,
        SPEAKING_OR_PROCESSING,
        STOPPED
    }

    private val _engineState = MutableStateFlow(EngineState.STOPPED)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _isMicrophoneActive = MutableStateFlow(false)
    val isMicrophoneActive: StateFlow<Boolean> = _isMicrophoneActive.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isDestroyed = false
    private var restartRunnable: Runnable? = null
    private var consecutiveSilences = 0

    fun startMonitoring() {
        isDestroyed = false
        mainHandler.post {
            _engineState.value = EngineState.PASSIVE_WAKE_MONITORING
            recreateRecognizer()
            startListeningInternal()
        }
    }

    fun enterActiveCommandListening() {
        mainHandler.post {
            _engineState.value = EngineState.ACTIVE_COMMAND_LISTENING
            recreateRecognizer()
            startListeningInternal()
        }
    }

    fun pauseForSpeaking() {
        mainHandler.post {
            _engineState.value = EngineState.SPEAKING_OR_PROCESSING
            cancelRestart()
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isMicrophoneActive.value = false
        }
    }

    fun resumeAfterSpeaking(listenForFollowUp: Boolean) {
        mainHandler.post {
            if (isDestroyed) return@post
            if (listenForFollowUp) {
                _engineState.value = EngineState.ACTIVE_COMMAND_LISTENING
            } else {
                _engineState.value = EngineState.PASSIVE_WAKE_MONITORING
            }
            recreateRecognizer()
            startListeningInternal()
        }
    }

    fun stop() {
        isDestroyed = true
        mainHandler.post {
            _engineState.value = EngineState.STOPPED
            cancelRestart()
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            _isMicrophoneActive.value = false
        }
    }

    private fun recreateRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device platform.")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
    }

    private fun startListeningInternal() {
        if (isDestroyed || _engineState.value == EngineState.STOPPED || _engineState.value == EngineState.SPEAKING_OR_PROCESSING) {
            return
        }

        cancelRestart()

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                // Support mixed multilingual speech
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }
            speechRecognizer?.startListening(intent)
            _isMicrophoneActive.value = true
        } catch (e: Exception) {
            Log.w(TAG, "startListening error: ${e.message}")
            scheduleRestart(1000L)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isMicrophoneActive.value = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isMicrophoneActive.value = false
            }

            override fun onError(error: Int) {
                _isMicrophoneActive.value = false
                Log.d(TAG, "RecognitionListener onError code: $error")
                consecutiveSilences++

                val delay = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (consecutiveSilences > 3) 2500L else 1200L
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_AUDIO -> 3500L
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_NETWORK -> 4000L
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 8000L
                    else -> 2000L
                }

                if (_engineState.value == EngineState.ACTIVE_COMMAND_LISTENING &&
                    (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH)
                ) {
                    // Revert to passive monitoring on silence timeout
                    _engineState.value = EngineState.PASSIVE_WAKE_MONITORING
                }

                scheduleRestart(delay)
            }

            override fun onResults(results: Bundle?) {
                _isMicrophoneActive.value = false
                consecutiveSilences = 0
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                val topMatch = matches.firstOrNull()?.trim() ?: ""

                if (topMatch.isNotBlank()) {
                    handleRecognizedText(topMatch)
                } else {
                    scheduleRestart(1200L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                val partialText = partials.firstOrNull()?.trim() ?: ""
                if (partialText.isNotBlank()) {
                    onTranscriptUpdated(partialText)

                    // Fast-path wake word trigger from partial results
                    if (_engineState.value == EngineState.PASSIVE_WAKE_MONITORING) {
                        val wakeCheck = LanguageDetector.inspectForWakeWord(partialText)
                        if (wakeCheck.isWakeWordPresent) {
                            try {
                                speechRecognizer?.stopListening()
                            } catch (_: Exception) {}
                            onWakeWordDetected(wakeCheck)
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleRecognizedText(text: String) {
        val detectedLang = LanguageDetector.detectLanguage(text)

        when (_engineState.value) {
            EngineState.PASSIVE_WAKE_MONITORING -> {
                val wakeCheck = LanguageDetector.inspectForWakeWord(text)
                if (wakeCheck.isWakeWordPresent) {
                    onWakeWordDetected(wakeCheck)
                } else {
                    scheduleRestart(1200L)
                }
            }

            EngineState.ACTIVE_COMMAND_LISTENING -> {
                if (LanguageDetector.isTerminationCommand(text)) {
                    _engineState.value = EngineState.PASSIVE_WAKE_MONITORING
                    scheduleRestart(500L)
                } else {
                    onCommandReceived(text, detectedLang)
                }
            }

            else -> {
                scheduleRestart(500L)
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (isDestroyed || _engineState.value == EngineState.STOPPED || _engineState.value == EngineState.SPEAKING_OR_PROCESSING) {
            return
        }
        cancelRestart()
        restartRunnable = Runnable {
            if (!isDestroyed && _engineState.value != EngineState.STOPPED && _engineState.value != EngineState.SPEAKING_OR_PROCESSING) {
                recreateRecognizer()
                startListeningInternal()
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMs)
    }

    private fun cancelRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }
}
