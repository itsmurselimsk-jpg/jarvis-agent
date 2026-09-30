package com.example.jarvis.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time Cybernetic Sci-Fi Sound Synthesizer.
 * Generates low-latency procedural audio cues using Android's native AudioTrack PCM streaming.
 * Requires 0 external audio files or MP3 assets, guaranteeing offline, instant sound playback.
 */
object CyberneticAudioEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private const val SAMPLE_RATE = 44100
    var isMuted: Boolean = false

    /**
     * Plays a high-tech UI chirp/beep when user touches or spins the 3D Holographic Orb.
     */
    fun playOrbBeep(frequencyHz: Float = 1400f, durationMs: Int = 50) {
        if (isMuted) return
        scope.launch {
            synthesizeTone(
                startFreq = frequencyHz,
                endFreq = frequencyHz * 1.3f,
                durationMs = durationMs,
                amplitude = 0.25f,
                decay = true
            )
        }
    }

    /**
     * Plays a powerful charging surge sound when overclocking or boosting the core reactor.
     */
    fun playReactorSurge(durationMs: Int = 380) {
        if (isMuted) return
        scope.launch {
            synthesizeTone(
                startFreq = 180f,
                endFreq = 920f,
                durationMs = durationMs,
                amplitude = 0.35f,
                decay = false
            )
        }
    }

    /**
     * Plays a dual-tone radar ping for diagnostics, optical gesture locking, or sensor scans.
     */
    fun playScanPing() {
        if (isMuted) return
        scope.launch {
            synthesizeTone(startFreq = 1600f, endFreq = 2200f, durationMs = 60, amplitude = 0.22f, decay = true)
            synthesizeTone(startFreq = 2200f, endFreq = 2800f, durationMs = 90, amplitude = 0.18f, decay = true)
        }
    }

    /**
     * Plays a deep harmonic lock sound when switching to J.A.R.V.I.S. Combat Crimson or engaging defensive protocols.
     */
    fun playShieldEngage(durationMs: Int = 260) {
        if (isMuted) return
        scope.launch {
            synthesizeTone(
                startFreq = 740f,
                endFreq = 180f,
                durationMs = durationMs,
                amplitude = 0.40f,
                decay = true
            )
        }
    }

    /**
     * Classic ascending 3-tone JARVIS chime (Iron Man HUD activation).
     */
    fun playWakeChime() {
        if (isMuted) return
        scope.launch {
            // A4 (440Hz), C#5 (554Hz), E5 (659Hz)
            val chord = listOf(440f to 50, 554f to 50, 659f to 100)
            for ((freq, dur) in chord) {
                synthesizeTone(startFreq = freq, endFreq = freq, durationMs = dur, amplitude = 0.28f, decay = true)
            }
        }
    }

    /**
     * Harmonious high-tech confirmation pulse for completed Stark protocols.
     */
    fun playProtocolSuccess() {
        if (isMuted) return
        scope.launch {
            val chord = listOf(587.33f to 60, 880f to 80, 1174.66f to 120)
            for ((freq, dur) in chord) {
                synthesizeTone(startFreq = freq, endFreq = freq * 1.05f, durationMs = dur, amplitude = 0.24f, decay = true)
            }
        }
    }

    /**
     * Subtle holographic particle pulse chime.
     */
    fun playHologramPulse() {
        if (isMuted) return
        scope.launch {
            synthesizeTone(startFreq = 2400f, endFreq = 3200f, durationMs = 40, amplitude = 0.15f, decay = true)
        }
    }

    /**
     * Low-level procedural PCM tone synthesizer writing directly to AudioTrack.
     */
    private fun synthesizeTone(
        startFreq: Float,
        endFreq: Float,
        durationMs: Int,
        amplitude: Float,
        decay: Boolean
    ) {
        try {
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val time = i.toDouble() / SAMPLE_RATE
                val wave = sin(2.0 * PI * currentFreq * time)

                val envelope = if (decay) {
                    (1.0 - progress) * (1.0 - progress)
                } else {
                    1.0
                }

                val sample = (wave * amplitude * envelope * Short.MAX_VALUE).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

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
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            // Release after completion
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Ignored if device audio channel is briefly congested
        }
    }
}
