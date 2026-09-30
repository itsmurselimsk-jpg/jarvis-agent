package com.example.jarvis.tactical

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlashlightController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isStrobeActive = MutableStateFlow(false)
    val isStrobeActive: StateFlow<Boolean> = _isStrobeActive.asStateFlow()

    private var strobeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            val cameraIds = cameraManager?.cameraIdList ?: return
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }
            if (cameraId == null && cameraIds.isNotEmpty()) {
                cameraId = cameraIds[0]
            }
        } catch (_: Exception) {
        }
    }

    fun toggleTorch(): Boolean {
        stopStrobe()
        val newState = !_isTorchOn.value
        setTorchState(newState)
        return newState
    }

    fun setTorchState(enable: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager?.setTorchMode(id, enable)
            _isTorchOn.value = enable
        } catch (_: CameraAccessException) {
            _isTorchOn.value = false
        } catch (_: Exception) {
            _isTorchOn.value = false
        }
    }

    fun toggleStrobe(hz: Int = 5) {
        if (_isStrobeActive.value) {
            stopStrobe()
        } else {
            startStrobe(hz)
        }
    }

    private fun startStrobe(hz: Int) {
        stopStrobe()
        val intervalMs = (1000 / (hz * 2)).toLong().coerceAtLeast(40L)
        _isStrobeActive.value = true

        strobeJob = scope.launch {
            var state = false
            try {
                while (_isStrobeActive.value) {
                    state = !state
                    cameraId?.let { id ->
                        try {
                            cameraManager?.setTorchMode(id, state)
                        } catch (_: Exception) {}
                    }
                    _isTorchOn.value = state
                    delay(intervalMs)
                }
            } finally {
                setTorchState(false)
            }
        }
    }

    fun stopStrobe() {
        _isStrobeActive.value = false
        strobeJob?.cancel()
        strobeJob = null
        setTorchState(false)
    }
}
