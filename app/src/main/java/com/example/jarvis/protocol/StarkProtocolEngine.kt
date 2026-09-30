package com.example.jarvis.protocol

import android.content.Context
import android.os.Build
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.MessageSender
import com.example.jarvis.privacy.PrivacyAuditor
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.voice.CyberneticAudioEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stark Operational Protocols Engine.
 * Formulates and executes comprehensive, charismatic executive protocols
 * inspired by Tony Stark's J.A.R.V.I.S. operating environment.
 */
object StarkProtocolEngine {

    enum class StarkProtocolType(val title: String, val code: String) {
        PROTOCOL_ZERO("Protocol Zero: Supreme Omni-OS Init", "protocol_zero"),
        MORNING("Morning Briefing Protocol", "protocol_morning"),
        NIGHT("Night Standby Protocol", "protocol_night"),
        SECURE_PERIMETER("Secure Perimeter Protocol", "protocol_perimeter"),
        POWER_SURGE("Reactor Overclock Surge", "protocol_surge"),
        STEALTH("Stealth Ghost Protocol", "protocol_stealth"),
        FOCUS_MATRIX("Neural Focus Matrix", "protocol_focus"),
        DIAGNOSTIC_SUITE("Titan Diagnostic Suite", "protocol_diagnostics"),
        EMERGENCY_BEACON("Emergency Tactical Beacon", "protocol_emergency");

        companion object {
            fun fromInput(input: String): StarkProtocolType {
                val lower = input.lowercase()
                return when {
                    lower.contains("zero") || lower.contains("omni") || lower.contains("supreme") || lower.contains("protocol zero") -> PROTOCOL_ZERO
                    lower.contains("stealth") || lower.contains("ghost") || lower.contains("silent") || lower.contains("chup") -> STEALTH
                    lower.contains("focus") || lower.contains("study") || lower.contains("work") || lower.contains("dhyan") -> FOCUS_MATRIX
                    lower.contains("diagnostic") || lower.contains("scan") || lower.contains("check all") || lower.contains("health") -> DIAGNOSTIC_SUITE
                    lower.contains("emergency") || lower.contains("sos") || lower.contains("danger") || lower.contains("khatra") || lower.contains("beacon") -> EMERGENCY_BEACON
                    lower.contains("night") || lower.contains("sleep") || lower.contains("bedtime") || lower.contains("so jao") || lower.contains("goodnight") -> NIGHT
                    lower.contains("secure") || lower.contains("perimeter") || lower.contains("lockdown") || lower.contains("security") -> SECURE_PERIMETER
                    lower.contains("surge") || lower.contains("overclock") || lower.contains("power") || lower.contains("maximum") || lower.contains("beast") -> POWER_SURGE
                    else -> MORNING
                }
            }
        }
    }

    data class ProtocolResult(
        val protocol: StarkProtocolType,
        val speechText: String,
        val summaryBadge: String,
        val telemetryPoints: List<String>,
        val executionTimestamp: Long = System.currentTimeMillis()
    )

    suspend fun executeProtocol(
        protocolType: StarkProtocolType,
        bridge: AndroidBridge,
        repository: JarvisRepository,
        context: Context
    ): ProtocolResult {
        bridge.refreshTelemetry()
        val telemetry = bridge.telemetry.value

        return when (protocolType) {
            StarkProtocolType.PROTOCOL_ZERO -> executeProtocolZero(bridge, repository, context, telemetry)
            StarkProtocolType.MORNING -> executeMorningProtocol(bridge, repository, telemetry)
            StarkProtocolType.NIGHT -> executeNightProtocol(bridge, repository, telemetry)
            StarkProtocolType.SECURE_PERIMETER -> executeSecurePerimeterProtocol(bridge, repository, context)
            StarkProtocolType.POWER_SURGE -> executePowerSurgeProtocol(bridge, repository, telemetry)
            StarkProtocolType.STEALTH -> executeStealthProtocol(bridge, repository, telemetry)
            StarkProtocolType.FOCUS_MATRIX -> executeFocusProtocol(bridge, repository, telemetry)
            StarkProtocolType.DIAGNOSTIC_SUITE -> executeDiagnosticSuite(bridge, repository, telemetry)
            StarkProtocolType.EMERGENCY_BEACON -> executeEmergencyBeacon(bridge, repository, telemetry)
        }
    }

    private suspend fun executeProtocolZero(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        context: Context,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playReactorSurge()
        bridge.vibrate(150L)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val now = Date()
        val currentTime = timeFormat.format(now)

        val pendingTasks = repository.tasks.value.filter { !it.isCompleted }
        val memories = repository.memories.value.size
        val settings = repository.settings.value

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("⚡ PROTOCOL ZERO: All Quantum Subsystems Synchronized")
        telemetryPoints.add("🧠 Active AI Core: ${settings.providerType.name} (${settings.selectedModel})")
        telemetryPoints.add("🔋 Power Cell: ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Optimal Discharge"})")
        telemetryPoints.add("📡 Telemetry Uplink: ${telemetry.networkType} / Latency Nominal")
        telemetryPoints.add("💾 Neural Memories Stored: $memories knowledge nodes")
        telemetryPoints.add("📋 Directives Queue: ${pendingTasks.size} pending tasks")

        val speech = "Protocol Zero initialized, Sir. " +
                "Omni-OS neural framework synchronized across all device hardware layers. " +
                "Main power core is at ${telemetry.batteryPercent} percent, network uplink is steady at ${telemetry.networkType}, " +
                "and ${memories} contextual memory nodes are primed. " +
                "I am fully online, operating at absolute maximum capacity, and standing by for your commands."

        repository.logActivity("Stark Protocol", "Protocol Zero: Supreme OS Online", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.PROTOCOL_ZERO,
            speechText = speech,
            summaryBadge = "PROTOCOL ZERO SUPREME",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executeStealthProtocol(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playShieldEngage()
        bridge.setVolume(5)
        bridge.toggleFlashlight(false)
        bridge.vibrate(50L)

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("👻 Acoustic Profile: Silent Ghost (5%)")
        telemetryPoints.add("💡 Optical Output: Flashlight Disabled")
        telemetryPoints.add("🛡️ Perimeter Signature: Low Profile")
        telemetryPoints.add("🔋 Power Reserve: ${telemetry.batteryPercent}%")

        val speech = "Stealth Protocol engaged, Sir. Acoustic emissions silenced to minimum, optical emitter terminated, and stealth sensors primed."
        repository.logActivity("Stark Protocol", "Stealth Protocol Active", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.STEALTH,
            speechText = speech,
            summaryBadge = "STEALTH PROTOCOL ACTIVE",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executeFocusProtocol(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playScanPing()
        bridge.setVolume(25)
        bridge.vibrate(80L)

        val pendingTasks = repository.tasks.value.filter { !it.isCompleted }
        val topTask = pendingTasks.firstOrNull()?.title ?: "Deep Work Session"

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("🎯 Neural Focus Matrix: Locked On")
        telemetryPoints.add("🔇 Distractions: Filtered")
        telemetryPoints.add("📌 Priority Objective: $topTask")
        telemetryPoints.add("⏱️ Focus Timer: 25-minute Pomodoro Block Suggested")

        val speech = "Neural Focus Matrix locked, Sir. Distraction filters activated, acoustic levels balanced. Your current primary objective is '$topTask'. Let us maintain uninterrupted momentum."
        repository.logActivity("Stark Protocol", "Neural Focus Matrix Engaged", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.FOCUS_MATRIX,
            speechText = speech,
            summaryBadge = "NEURAL FOCUS ENGAGED",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executeDiagnosticSuite(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playScanPing()
        bridge.vibrate(100L)

        val rt = Runtime.getRuntime()
        val usedMemMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val maxMemMB = rt.maxMemory() / (1024 * 1024)

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("🔬 CPU Telemetry: Nominal Architecture (${Build.HARDWARE})")
        telemetryPoints.add("💾 JVM Heap Allocation: ${usedMemMB}MB / ${maxMemMB}MB")
        telemetryPoints.add("🔋 Power Cell Health: ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Stable"})")
        telemetryPoints.add("📡 Uplink Bandwidth: ${telemetry.networkType}")
        telemetryPoints.add("🔊 Audio Output Level: ${telemetry.volumePercent}%")

        val speech = "Complete Titan Diagnostic Scan concluded, Sir. " +
                "Hardware processor ${Build.HARDWARE} is responsive, JVM heap at ${usedMemMB} megabytes out of ${maxMemMB}, " +
                "power cell holding at ${telemetry.batteryPercent} percent, and uplink network telemetry is stable. All systems nominal."

        repository.logActivity("Stark Protocol", "Titan Diagnostic Scan Complete", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.DIAGNOSTIC_SUITE,
            speechText = speech,
            summaryBadge = "ALL DIAGNOSTICS NOMINAL",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executeEmergencyBeacon(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playReactorSurge()
        bridge.toggleFlashlight(true)
        bridge.vibrate(500L)

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("🚨 EMERGENCY TACTICAL BEACON: ACTIVATED")
        telemetryPoints.add("💡 Optical Distress Emitter: Strobe/Torch ONLINE")
        telemetryPoints.add("🔋 Battery Reserve: ${telemetry.batteryPercent}%")
        telemetryPoints.add("⚠️ Security Advisory: Keep device accessible")

        val speech = "Emergency tactical beacon activated, Sir! Optical emitter illuminated and tactical alert broadcasted. Standing by for SOS dispatch directives."
        repository.logActivity("Stark Protocol", "Emergency Tactical Beacon Activated", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.EMERGENCY_BEACON,
            speechText = speech,
            summaryBadge = "EMERGENCY BEACON ACTIVE",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executeMorningProtocol(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playWakeChime()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val now = Date()
        val currentTime = timeFormat.format(now)
        val currentDate = dateFormat.format(now)

        val pendingTasks = repository.tasks.value.filter { !it.isCompleted }
        val highPriorityTasks = pendingTasks.filter { it.priority.equals("High", ignoreCase = true) }

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("Temporal Coordinates: $currentDate, $currentTime")
        telemetryPoints.add("Main Power Cell: ${telemetry.batteryPercent}% (${if (telemetry.isCharging) "Charging" else "Discharging"})")
        telemetryPoints.add("Uplink Matrix: ${telemetry.networkType} / Wi-Fi Active")
        telemetryPoints.add("Pending Directives: ${pendingTasks.size} active (${highPriorityTasks.size} high priority)")

        val speechBuilder = StringBuilder()
        speechBuilder.append("Good morning, Sir. ")
        speechBuilder.append("The time is $currentTime on $currentDate. ")
        speechBuilder.append("Main power is at ${telemetry.batteryPercent} percent. ")
        speechBuilder.append("Network telemetry indicates an optimal ${telemetry.networkType} uplink. ")

        if (pendingTasks.isNotEmpty()) {
            val taskMention = if (highPriorityTasks.isNotEmpty()) {
                "You have ${pendingTasks.size} operational directives scheduled, with ${highPriorityTasks.first().title} prioritized. "
            } else {
                "You have ${pendingTasks.size} pending tasks logged in the queue. "
            }
            speechBuilder.append(taskMention)
        } else {
            speechBuilder.append("Your agenda is currently clear of pending conflicts. ")
        }

        val upcomingEvents = bridge.queryUpcomingCalendarEvents()
        if (upcomingEvents.isNotEmpty()) {
            telemetryPoints.add("Calendar Schedule: ${upcomingEvents.size} appointment(s) registered today")
            speechBuilder.append("Calendar scan reveals ${upcomingEvents.size} scheduled item${if (upcomingEvents.size > 1) "s" else ""}: ${upcomingEvents.take(2).joinToString("; ")}. ")
        }

        speechBuilder.append("All primary diagnostics are nominal. I remain online and awaiting your directives.")

        val speech = speechBuilder.toString()
        repository.logActivity("Stark Protocol", "Morning Briefing Executed", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.MORNING,
            speechText = speech,
            summaryBadge = "MORNING PROTOCOL ACTIVE",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executeNightProtocol(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playShieldEngage()

        // Softly balance audio volume for night hours
        bridge.setVolume(20)

        val completedToday = repository.tasks.value.filter { it.isCompleted }

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("Acoustic Output: Calibrated to Night Standby (20%)")
        telemetryPoints.add("Power Cell: ${telemetry.batteryPercent}%")
        telemetryPoints.add("Accomplished Objectives: ${completedToday.size} tasks completed")
        telemetryPoints.add("Security Perimeter: Vigilant Standby")

        val speech = "All core systems are switching to auxiliary standby mode, Sir. " +
                "Audio levels have been reduced for evening comfort. " +
                "Battery is at ${telemetry.batteryPercent} percent. " +
                (if (completedToday.isNotEmpty()) "You have accomplished ${completedToday.size} operational goals today. " else "") +
                "I will monitor perimeter telemetry and alarms in background. Have a restful evening, Sir."

        repository.logActivity("Stark Protocol", "Night Standby Protocol Engaged", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.NIGHT,
            speechText = speech,
            summaryBadge = "NIGHT STANDBY ENGAGED",
            telemetryPoints = telemetryPoints
        )
    }

    private suspend fun executeSecurePerimeterProtocol(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        context: Context
    ): ProtocolResult {
        CyberneticAudioEngine.playScanPing()

        val auditor = PrivacyAuditor(context, repository)
        val telemetry = auditor.getPrivacyTelemetry()

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("Acoustic Sensor (Microphone): ${if (telemetry.hasMicPermission) "Protected & Monitored" else "Access Restricted"}")
        telemetryPoints.add("Optical Sensor (Camera): ${if (telemetry.hasCameraPermission) "Secured" else "Shielded"}")
        telemetryPoints.add("Accessibility Shield: ${if (telemetry.isAccessibilityServiceEnabled) "Active & Guarding" else "Disabled"}")
        telemetryPoints.add("Notification Intercept: ${if (telemetry.isNotificationAccessEnabled) "Vigilant" else "Offline"}")
        telemetryPoints.add("Active AI Core: ${telemetry.activeProviderName}")

        val speech = "Perimeter security scan complete, Sir. " +
                "Hardware sensors verified: Microphone is ${if (telemetry.hasMicPermission) "operational" else "restricted"}, and optical sensors are secured. " +
                "Accessibility defense matrix is ${if (telemetry.isAccessibilityServiceEnabled) "online" else "in standby"}. " +
                "Zero unauthorized data leaks or perimeter breaches detected. Security posture remains fortified."

        repository.logActivity("Stark Protocol", "Perimeter Security Lockdown Verified", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.SECURE_PERIMETER,
            speechText = speech,
            summaryBadge = "PERIMETER SECURED",
            telemetryPoints = telemetryPoints
        )
    }

    private fun executePowerSurgeProtocol(
        bridge: AndroidBridge,
        repository: JarvisRepository,
        telemetry: com.example.jarvis.model.DeviceTelemetry
    ): ProtocolResult {
        CyberneticAudioEngine.playReactorSurge()
        bridge.vibrate(250L)

        val telemetryPoints = mutableListOf<String>()
        telemetryPoints.add("Arc Reactor Output: 100% MAXIMUM OVERCLOCK")
        telemetryPoints.add("Acoustic Resonator: Online")
        telemetryPoints.add("Neural Bus Frequency: Peak Operating Capacity")
        telemetryPoints.add("Battery Reserve: ${telemetry.batteryPercent}%")

        val speech = "Arc Reactor core overclocked to maximum yield, Sir. " +
                "All quantum neural channels and responsive relays operating at peak operational velocity. Systems primed."

        repository.logActivity("Stark Protocol", "Arc Reactor Overclock Surge", ActivityType.SYSTEM_EVENT)

        return ProtocolResult(
            protocol = StarkProtocolType.POWER_SURGE,
            speechText = speech,
            summaryBadge = "OVERCLOCK POWER SURGE",
            telemetryPoints = telemetryPoints
        )
    }
}

