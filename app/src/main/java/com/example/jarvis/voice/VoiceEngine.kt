package com.example.jarvis.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

interface WakeWordEngine {
    val name: String
    val isSupported: Boolean
    fun startListening(onWakeWordDetected: () -> Unit)
    fun stopListening()
}

class DefaultWakeWordEngine(private val context: Context) : WakeWordEngine {
    override val name = "Continuous Acoustic Wake Engine"
    override val isSupported = true

    private var engine: ContinuousWakeEngine? = null

    override fun startListening(onWakeWordDetected: () -> Unit) {
        if (!VoicePermissions.hasRecordAudioPermission(context)) return
        engine = ContinuousWakeEngine(
            context = context,
            onWakeWordDetected = { onWakeWordDetected() },
            onCommandReceived = { _, _ -> }
        ).apply {
            startMonitoring()
        }
    }

    override fun stopListening() {
        engine?.stop()
        engine = null
    }
}

object VoicePermissions {
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
