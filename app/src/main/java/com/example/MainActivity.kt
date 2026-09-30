package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.jarvis.service.JarvisVoiceService
import com.example.jarvis.ui.JarvisApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize background voice wake service if explicitly enabled by user
    val prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
    val wakeEnabled = prefs.getBoolean("continuous_wake_enabled", false)
    if (wakeEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      JarvisVoiceService.start(this)
    }

    setContent {
      MyApplicationTheme {
        JarvisApp()
      }
    }
  }
}

