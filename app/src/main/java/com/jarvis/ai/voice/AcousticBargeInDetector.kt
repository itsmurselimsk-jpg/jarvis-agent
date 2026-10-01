package com.jarvis.ai.voice

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Low-latency acoustic energy monitor used exclusively during JARVIS speech synthesis
 * to detect user barge-in (interruption) without conflicting with Android's SpeechRecognizer.
 */
class AcousticBargeInDetector(
    private val context: Context,
    private val onBargeInDetected: () -> Unit
) {
    companion object {
        private const val TAG = "AcousticBargeIn"
        private const val SAMPLE_RATE = 16000
        private const val RMS_THRESHOLD = 1400.0 // Conversational speech energy threshold
        private const val REQUIRED_CONSECUTIVE_FRAMES = 2
        private const val STARTUP_GRACE_PERIOD_MS = 400L // Prevent TTS startup pop from false-triggering
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isMonitoring = false
    private var monitorThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startMonitoring() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        stopMonitoring()

        try {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
            if (minBufSize <= 0) return

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                minBufSize * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return
            }

            audioRecord = record
            record.startRecording()
            isMonitoring = true

            monitorThread = Thread {
                val buffer = ShortArray(1024)
                var consecutiveSpeechFrames = 0
                val startTime = System.currentTimeMillis()

                while (isMonitoring && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0 && (System.currentTimeMillis() - startTime > STARTUP_GRACE_PERIOD_MS)) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = Math.sqrt(sum / read)

                        if (rms > RMS_THRESHOLD) {
                            consecutiveSpeechFrames++
                            if (consecutiveSpeechFrames >= REQUIRED_CONSECUTIVE_FRAMES) {
                                isMonitoring = false
                                Log.i(TAG, "Acoustic barge-in detected (RMS: $rms), interrupting JARVIS TTS")
                                mainHandler.post {
                                    onBargeInDetected()
                                }
                                break
                            }
                        } else {
                            consecutiveSpeechFrames = 0
                        }
                    }
                }
            }.apply {
                name = "AcousticBargeInDetectorThread"
                isDaemon = true
                priority = Thread.NORM_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start acoustic barge-in detector: ${e.message}")
            stopMonitoring()
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        try {
            monitorThread?.interrupt()
            monitorThread = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (_: Exception) {}
    }
}
