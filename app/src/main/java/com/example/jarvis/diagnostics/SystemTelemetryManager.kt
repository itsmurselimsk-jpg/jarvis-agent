package com.example.jarvis.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

data class BatteryTelemetry(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 25f,
    val voltageMv: Int = 4000,
    val health: String = "Good",
    val powerSource: String = "Battery"
)

data class MemoryTelemetry(
    val totalRamGb: Float = 8f,
    val availRamGb: Float = 4f,
    val usedRamGb: Float = 4f,
    val ramUsagePercent: Int = 50,
    val isLowMemory: Boolean = false
)

data class StorageTelemetry(
    val totalStorageGb: Float = 128f,
    val freeStorageGb: Float = 64f,
    val usedStorageGb: Float = 64f,
    val usagePercent: Int = 50
)

data class NetworkTelemetry(
    val connectionType: String = "Wi-Fi",
    val isConnected: Boolean = true,
    val linkSpeedMbps: Int = 0,
    val signalDbm: Int = 0,
    val ipAddress: String = "127.0.0.1"
)

data class SensorTelemetry(
    val compassAzimuthDegrees: Float = 0f,
    val lightLux: Float = 0f,
    val isCompassAvailable: Boolean = false,
    val isLightSensorAvailable: Boolean = false
)

class SystemTelemetryManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val gravityData = FloatArray(3)
    private val geomagneticData = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val _batteryState = MutableStateFlow(BatteryTelemetry())
    val batteryState: StateFlow<BatteryTelemetry> = _batteryState.asStateFlow()

    private val _memoryState = MutableStateFlow(MemoryTelemetry())
    val memoryState: StateFlow<MemoryTelemetry> = _memoryState.asStateFlow()

    private val _storageState = MutableStateFlow(StorageTelemetry())
    val storageState: StateFlow<StorageTelemetry> = _storageState.asStateFlow()

    private val _networkState = MutableStateFlow(NetworkTelemetry())
    val networkState: StateFlow<NetworkTelemetry> = _networkState.asStateFlow()

    private val _sensorState = MutableStateFlow(SensorTelemetry())
    val sensorState: StateFlow<SensorTelemetry> = _sensorState.asStateFlow()

    fun startListening() {
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        lightSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        refreshSnapshot()
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun refreshSnapshot() {
        refreshBattery()
        refreshMemory()
        refreshStorage()
        refreshNetwork()
    }

    private fun refreshBattery() {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, intentFilter) ?: return

            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 100

            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val tempTenths = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)
            val tempC = tempTenths / 10f

            val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)

            val healthCode = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            val healthStr = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Critical/Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                else -> "Nominal (Good)"
            }

            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val powerSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Arc Power"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Bus"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Inductive"
                else -> "Battery Internal"
            }

            _batteryState.value = BatteryTelemetry(
                levelPercent = percent,
                isCharging = isCharging,
                temperatureCelsius = tempC,
                voltageMv = voltage,
                health = healthStr,
                powerSource = powerSource
            )
        } catch (_: Exception) {
        }
    }

    private fun refreshMemory() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val totalGb = memInfo.totalMem / (1024f * 1024f * 1024f)
            val availGb = memInfo.availMem / (1024f * 1024f * 1024f)
            val usedGb = totalGb - availGb
            val usagePct = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 0

            _memoryState.value = MemoryTelemetry(
                totalRamGb = totalGb,
                availRamGb = availGb,
                usedRamGb = usedGb,
                ramUsagePercent = usagePct,
                isLowMemory = memInfo.lowMemory
            )
        } catch (_: Exception) {
        }
    }

    private fun refreshStorage() {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = totalBytes / (1024f * 1024f * 1024f)
            val freeGb = freeBytes / (1024f * 1024f * 1024f)
            val usedGb = usedBytes / (1024f * 1024f * 1024f)
            val usagePct = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 0

            _storageState.value = StorageTelemetry(
                totalStorageGb = totalGb,
                freeStorageGb = freeGb,
                usedStorageGb = usedGb,
                usagePercent = usagePct
            )
        } catch (_: Exception) {
        }
    }

    private fun refreshNetwork() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)

            val isConnected = caps != null && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            val connType = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi Subspace"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 4G/5G"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet Hardwire"
                else -> "Offline / Isolated"
            }

            var linkSpeed = 0
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            if (wifiInfo != null && caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                linkSpeed = wifiInfo.linkSpeed
            }

            var ip = "127.0.0.1"
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        ip = addr.hostAddress ?: ip
                        break
                    }
                }
            }

            _networkState.value = NetworkTelemetry(
                connectionType = connType,
                isConnected = isConnected,
                linkSpeedMbps = linkSpeed,
                signalDbm = wifiInfo?.rssi ?: 0,
                ipAddress = ip
            )
        } catch (_: Exception) {
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityData, 0, 3)
                hasGravity = true
                calculateAzimuth()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticData, 0, 3)
                hasGeomagnetic = true
                calculateAzimuth()
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                _sensorState.value = _sensorState.value.copy(
                    lightLux = lux,
                    isLightSensorAvailable = true
                )
            }
        }
    }

    private fun calculateAzimuth() {
        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityData, geomagneticData)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val azimuthRad = orientation[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

                _sensorState.value = _sensorState.value.copy(
                    compassAzimuthDegrees = azimuthDeg,
                    isCompassAvailable = true
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
