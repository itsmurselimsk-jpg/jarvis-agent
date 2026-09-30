package com.example.jarvis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SmartBatteryMonitor: Stark-themed real-time battery and power alerts.
 * 
 * Provides automated voice warnings when:
 * 1. Charger connected / disconnected
 * 2. Battery reaches 100% (or >= 99%) while charging to protect battery health
 * 3. Battery drops to critical levels (<= 15%) while discharging
 */
class SmartBatteryMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onVoiceAlert: (String) -> Unit
) {

    data class BatteryState(
        val percent: Int = 100,
        val isCharging: Boolean = false,
        val isFull: Boolean = false
    )

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private var wasCharging: Boolean? = null
    private var hasAlertedFullForCycle = false
    private var hasAlertedLowForCycle = false
    private var isRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent ?: return
            val action = intent.action ?: return

            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    wasCharging = true
                    hasAlertedFullForCycle = false
                    _batteryState.value = _batteryState.value.copy(isCharging = true)
                    onVoiceAlert("Auxiliary power connected, recharging arc reactor cells.")
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    wasCharging = false
                    hasAlertedFullForCycle = false
                    _batteryState.value = _batteryState.value.copy(isCharging = false)
                    onVoiceAlert("Auxiliary power disconnected. Running on internal battery reserves.")
                }

                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                    val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100
                    val isFull = pct >= 99

                    _batteryState.value = BatteryState(
                        percent = pct,
                        isCharging = isCharging,
                        isFull = isFull
                    )

                    // Full charge alert (overcharge protection warning)
                    if (isCharging && isFull && !hasAlertedFullForCycle) {
                        hasAlertedFullForCycle = true
                        onVoiceAlert("Main power cell fully charged at $pct percent, Sir. Please disconnect charger to preserve battery longevity.")
                    }

                    // Low battery alert
                    if (!isCharging && pct <= 15 && !hasAlertedLowForCycle) {
                        hasAlertedLowForCycle = true
                        onVoiceAlert("Warning Sir, power reserves critical at $pct percent. Engaging power conservation protocols.")
                    } else if (pct > 20) {
                        hasAlertedLowForCycle = false
                    }
                }
            }
        }
    }

    fun startMonitoring() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(batteryReceiver, filter)
        isRegistered = true
    }

    fun stopMonitoring() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        isRegistered = false
    }
}
