package com.example.jarvis.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object StarkSoundEngine {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a rising Arc Reactor charge-up acoustic tone
     */
    fun playArcCharge() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 280
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 350.0 + (800.0 * progress)
                val angle = 2.0 * PI * i * (freq / sampleRate)
                val envelope = sin(progress * PI) // smooth attack & decay
                val sampleValue = (sin(angle) * 0.45 * envelope * Short.MAX_VALUE).toInt()
                samples[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playPcmBuffer(samples, sampleRate)
        }
    }

    /**
     * Plays a crisp holographic sci-fi UI blip
     */
    fun playCyberBeep() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 65
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 1320.0
                val angle = 2.0 * PI * i * (freq / sampleRate)
                val envelope = 1.0 - progress
                val sampleValue = (sin(angle) * 0.35 * envelope * Short.MAX_VALUE).toInt()
                samples[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playPcmBuffer(samples, sampleRate)
        }
    }

    /**
     * Plays a Stark JARVIS dual-tone chime confirmation
     */
    fun playConfirmationChime() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 180
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)

            val half = numSamples / 2
            for (i in 0 until numSamples) {
                val freq = if (i < half) 659.25 else 880.0 // E5 -> A5
                val localProgress = if (i < half) i.toDouble() / half else (i - half).toDouble() / half
                val envelope = 1.0 - localProgress
                val angle = 2.0 * PI * i * (freq / sampleRate)
                val sampleValue = (sin(angle) * 0.40 * envelope * Short.MAX_VALUE).toInt()
                samples[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playPcmBuffer(samples, sampleRate)
        }
    }

    /**
     * Plays a low-warning tactical alert tone
     */
    fun playWarningTone() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 220
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 320.0
                val angle = 2.0 * PI * i * (freq / sampleRate)
                val envelope = sin(progress * PI)
                val sampleValue = (sin(angle) * 0.40 * envelope * Short.MAX_VALUE).toInt()
                samples[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playPcmBuffer(samples, sampleRate)
        }
    }

    private fun playPcmBuffer(samples: ShortArray, sampleRate: Int) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufSize, samples.size * 2)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            Thread.sleep((samples.size * 1000L / sampleRate) + 20L)
            audioTrack.release()
        } catch (_: Exception) {
            // AudioTrack failure handled gracefully
        }
    }
}
