package com.example.jarvis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.MessageSender
import com.example.jarvis.provider.JarvisUnifiedAIProvider
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.voice.ContinuousWakeEngine
import com.example.jarvis.voice.CyberneticAudioEngine
import com.example.jarvis.voice.JarvisOverlayHud
import com.example.jarvis.voice.LanguageDetector
import com.example.jarvis.voice.SupportedLanguage
import com.example.jarvis.voice.VoiceProfileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisVoiceService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_voice_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.jarvis.START_VOICE_SERVICE"
        const val ACTION_STOP = "com.example.jarvis.STOP_VOICE_SERVICE"
        const val ACTION_TRIGGER_WAKE = "com.example.jarvis.TRIGGER_WAKE"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeEngine: ContinuousWakeEngine? = null
    private var overlayHud: JarvisOverlayHud? = null
    private var smartBatteryMonitor: SmartBatteryMonitor? = null

    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge
    private lateinit var brain: AgentBrain

    override fun onCreate() {
        super.onCreate()
        repository = JarvisRepository(applicationContext)
        bridge = AndroidBridge(applicationContext).apply {
            this.repository = this@JarvisVoiceService.repository
        }
        val aiProvider = JarvisUnifiedAIProvider(repository)
        brain = AgentBrain(
            repository = repository,
            bridge = bridge,
            aiProvider = aiProvider,
            onConfirmationRequired = { /* Safety confirmations logged */ }
        )
        overlayHud = JarvisOverlayHud(applicationContext)

        acquireWakeLock()
        createNotificationChannel()
        initWakeEngine()

        // Smart Battery & Power Alerts
        smartBatteryMonitor = SmartBatteryMonitor(
            context = applicationContext,
            scope = serviceScope,
            onVoiceAlert = { alertText ->
                serviceScope.launch {
                    val settings = repository.settings.value
                    if (settings.autoSpeakResponses) {
                        bridge.speak(
                            text = alertText,
                            speechRate = settings.speechRate,
                            pitch = settings.speechPitch
                        )
                    }
                    overlayHud?.show(status = "BATTERY ALERT", transcript = alertText, isListening = false)
                    repository.logActivity("Battery Alert", alertText, ActivityType.SYSTEM_EVENT)
                }
            }
        ).apply {
            startMonitoring()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_WAKE -> {
                triggerWakeSession(LanguageDetector.WakeWordCheck(isWakeWordPresent = true))
            }
            else -> {
                startForegroundWithNotification()
                _isServiceRunning.value = true
                wakeEngine?.startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun initWakeEngine() {
        wakeEngine = ContinuousWakeEngine(
            context = applicationContext,
            onWakeWordDetected = { wakeCheck ->
                triggerWakeSession(wakeCheck)
            },
            onCommandReceived = { command, detectedLang ->
                executeSpokenCommand(command, detectedLang)
            },
            onTranscriptUpdated = { partial ->
                overlayHud?.updateTranscript(partial)
            }
        )
    }

    private fun triggerWakeSession(wakeCheck: LanguageDetector.WakeWordCheck) {
        vibrateHapticFeedback()
        CyberneticAudioEngine.playWakeChime()

        val settings = repository.settings.value
        val detectedLang = wakeCheck.language

        // If user already spoke command (e.g. "Hey JARVIS open Facebook")
        if (wakeCheck.commandAfterWake.isNotBlank()) {
            overlayHud?.show(
                status = "EXECUTING DIRECTIVE",
                transcript = wakeCheck.commandAfterWake,
                isListening = false
            )
            executeSpokenCommand(wakeCheck.commandAfterWake, detectedLang)
            return
        }

        // Otherwise: Wake word only detected
        val greeting = when (SupportedLanguage.fromCode(settings.languageCode)) {
            SupportedLanguage.BENGALI -> LanguageDetector.getWakeGreeting(LanguageDetector.DetectedLanguage.BENGALI)
            SupportedLanguage.HINDI -> LanguageDetector.getWakeGreeting(LanguageDetector.DetectedLanguage.HINDI)
            SupportedLanguage.ENGLISH -> LanguageDetector.getWakeGreeting(LanguageDetector.DetectedLanguage.ENGLISH)
            SupportedLanguage.HINGLISH -> "Haan Sir, main sun raha hoon. How can I help you?"
            SupportedLanguage.BANGLISH -> "Hae Sir, ami shunchi. Kibaabe help korte pari?"
            SupportedLanguage.AUTO -> LanguageDetector.getWakeGreeting(detectedLang)
        }

        overlayHud?.show(
            status = "JARVIS LISTENING",
            transcript = greeting,
            isListening = true
        )

        val profile = VoiceProfileType.fromName(settings.voiceProfileName)
        val locale = LanguageDetector.getLocaleForLanguage(detectedLang)

        wakeEngine?.pauseForSpeaking()
        bridge.speak(
            text = greeting,
            speechRate = profile.defaultSpeed * settings.speechRate,
            pitch = profile.defaultPitch * settings.speechPitch,
            locale = locale,
            onDone = {
                // Begin active command capture
                wakeEngine?.enterActiveCommandListening()
                overlayHud?.updateStatus("LISTENING FOR COMMAND...", isListening = true)
            }
        )
    }

    private fun executeSpokenCommand(command: String, detectedLang: LanguageDetector.DetectedLanguage) {
        serviceScope.launch {
            overlayHud?.updateStatus("PROCESSING DIRECTIVE...", isListening = false)
            overlayHud?.updateTranscript(command)

            // Log to conversation
            val userMsg = ChatMessage(sender = MessageSender.USER, text = command)
            repository.addMessage(userMsg)
            repository.logActivity("Voice Wake Directive", command, ActivityType.VOICE_EVENT)

            val settings = repository.settings.value
            val profile = VoiceProfileType.fromName(settings.voiceProfileName)
            val locale = LanguageDetector.getLocaleForLanguage(detectedLang)

            wakeEngine?.pauseForSpeaking()

            // Run through AgentBrain loop
            brain.processUserInput(
                input = command,
                onThinking = {
                    overlayHud?.updateStatus("SYNTHESIZING PLAN...", isListening = false)
                },
                onSpeaking = {
                    overlayHud?.updateStatus("JARVIS RESPONDING", isListening = false)
                },
                onIdle = {
                    val messages = repository.messages.value
                    val lastJarvis = messages.lastOrNull { it.sender == MessageSender.JARVIS }?.text ?: "Completed, Sir."
                    overlayHud?.updateTranscript(lastJarvis)

                    if (settings.autoSpeakResponses) {
                        bridge.speak(
                            text = lastJarvis,
                            speechRate = profile.defaultSpeed * settings.speechRate,
                            pitch = profile.defaultPitch * settings.speechPitch,
                            locale = locale,
                            onDone = {
                                if (settings.continuousConversationEnabled) {
                                    overlayHud?.updateStatus("LISTENING FOR FOLLOW-UP...", isListening = true)
                                    wakeEngine?.resumeAfterSpeaking(listenForFollowUp = true)
                                } else {
                                    overlayHud?.hide()
                                    wakeEngine?.resumeAfterSpeaking(listenForFollowUp = false)
                                }
                            }
                        )
                    } else {
                        if (settings.continuousConversationEnabled) {
                            wakeEngine?.resumeAfterSpeaking(listenForFollowUp = true)
                        } else {
                            overlayHud?.hide()
                            wakeEngine?.resumeAfterSpeaking(listenForFollowUp = false)
                        }
                    }
                }
            )
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildServiceNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS Neural Acoustic Grid Active")
            .setContentText("Listening for 'Hey JARVIS' / 'জারভিস' / 'जार्विस'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deactivate", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Acoustic Voice Grid",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors background microphone for 'Hey JARVIS' wake activation."
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JARVIS:AcousticWakeLock")?.apply {
                acquire(10 * 60 * 1000L) // 10 min sliding window
            }
        } catch (_: Exception) {}
    }

    private fun vibrateHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        smartBatteryMonitor?.stopMonitoring()
        wakeEngine?.stop()
        overlayHud?.hide()
        bridge.destroy()
        serviceScope.cancel()
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
