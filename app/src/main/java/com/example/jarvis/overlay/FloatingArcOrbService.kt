package com.example.jarvis.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.protocol.StarkProtocolEngine
import com.example.jarvis.service.JarvisVoiceService
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.voice.CyberneticAudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Floating Holographic Arc Reactor Orb Service.
 * Provides a persistent system-wide sci-fi floating controller over all Android applications.
 */
class FloatingArcOrbService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_orb_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_START = "com.example.jarvis.START_ORB"
        const val ACTION_STOP = "com.example.jarvis.STOP_ORB"

        private val _isOrbRunning = MutableStateFlow(false)
        val isOrbRunning: StateFlow<Boolean> = _isOrbRunning.asStateFlow()

        fun isOverlayPermitted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        }

        fun start(context: Context) {
            if (!isOverlayPermitted(context)) return
            val intent = Intent(context, FloatingArcOrbService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingArcOrbService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var orbRootView: FrameLayout? = null
    private var hudCardView: LinearLayout? = null

    private lateinit var orbParams: WindowManager.LayoutParams
    private lateinit var hudParams: WindowManager.LayoutParams

    private var isHudOpen = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = JarvisRepository(applicationContext)
        bridge = AndroidBridge(applicationContext).apply {
            this.repository = this@FloatingArcOrbService.repository
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundWithNotification()
                _isOrbRunning.value = true
                showOrb()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeViews()
        _isOrbRunning.value = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Floating Arc Reactor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "System-wide persistent Arc Reactor overlay orb"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS Arc Reactor Active")
            .setContentText("System-wide floating holographic orb online")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOrb() {
        if (orbRootView != null || !isOverlayPermitted(this)) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        orbParams = WindowManager.LayoutParams(
            dpToPx(56),
            dpToPx(56),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(140)
        }

        // Create Custom Sci-Fi Arc Reactor View
        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EE0B1220"))
                setStroke(dpToPx(2), Color.parseColor("#00E5FF"))
            }
            background = bg
            elevation = dpToPx(10).toFloat()
        }

        // Inner glowing core
        val innerCore = View(this).apply {
            val size = dpToPx(26)
            val lp = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#8000E5FF"))
                setStroke(dpToPx(1), Color.parseColor("#FFFFFF"))
            }
        }
        root.addView(innerCore)

        // Center Triangle / Arc Symbol
        val symbol = TextView(this).apply {
            text = "▲"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        root.addView(symbol)

        // Dragging & Click Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = orbParams.x
                    initialY = orbParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        isClick = false
                    }
                    orbParams.x = initialX + deltaX
                    orbParams.y = initialY + deltaY
                    windowManager?.updateViewLayout(orbRootView, orbParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        CyberneticAudioEngine.playOrbBeep()
                        toggleHudCard()
                    } else {
                        // Snap to nearest screen edge (left or right)
                        val screenWidth = resources.displayMetrics.widthPixels
                        val targetX = if (orbParams.x + dpToPx(28) < screenWidth / 2) {
                            dpToPx(12)
                        } else {
                            screenWidth - dpToPx(68)
                        }
                        orbParams.x = targetX
                        windowManager?.updateViewLayout(orbRootView, orbParams)
                    }
                    true
                }
                else -> false
            }
        }

        orbRootView = root
        try {
            windowManager?.addView(orbRootView, orbParams)
        } catch (_: Exception) {}
    }

    private fun toggleHudCard() {
        if (isHudOpen) {
            hideHudCard()
        } else {
            showHudCard()
        }
    }

    private fun showHudCard() {
        if (hudCardView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        hudParams = WindowManager.LayoutParams(
            dpToPx(260),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (orbParams.y + dpToPx(65)).coerceAtMost(resources.displayMetrics.heightPixels - dpToPx(280))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F5090F1C"))
                setStroke(dpToPx(1), Color.parseColor("#00E5FF"))
                cornerRadius = dpToPx(16).toFloat()
            }
            background = bg
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
            elevation = dpToPx(12).toFloat()
        }

        // Header with Close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S. TACTICAL HUD"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#90CAF9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener { hideHudCard() }
        }
        header.addView(title)
        header.addView(close)
        card.addView(header)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
            setBackgroundColor(Color.parseColor("#2600E5FF"))
        }
        card.addView(divider)

        // Actions List
        fun createActionBtn(label: String, icon: String, colorHex: String, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#142238"))
                    cornerRadius = dpToPx(8).toFloat()
                }
                background = bg
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(6)
                }

                val iconTv = TextView(context).apply {
                    text = icon
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    layoutParams = LinearLayout.LayoutParams(dpToPx(24), LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val labelTv = TextView(context).apply {
                    text = label
                    setTextColor(Color.parseColor(colorHex))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                addView(iconTv)
                addView(labelTv)

                setOnClickListener {
                    onClick()
                    hideHudCard()
                }
            }
        }

        // 1. Voice Command
        card.addView(createActionBtn("Voice Command Input", "🎙️", "#00E5FF") {
            val intent = Intent(applicationContext, JarvisVoiceService::class.java).apply {
                action = JarvisVoiceService.ACTION_TRIGGER_WAKE
            }
            startService(intent)
        })

        // 2. Morning Protocol
        card.addView(createActionBtn("Morning Briefing Protocol", "🌅", "#FFD700") {
            serviceScope.launch {
                val res = StarkProtocolEngine.executeProtocol(
                    StarkProtocolEngine.StarkProtocolType.MORNING,
                    bridge,
                    repository,
                    applicationContext
                )
                bridge.speak(res.speechText)
            }
        })

        // 3. Night Protocol
        card.addView(createActionBtn("Night Standby Protocol", "🌙", "#90CAF9") {
            serviceScope.launch {
                val res = StarkProtocolEngine.executeProtocol(
                    StarkProtocolEngine.StarkProtocolType.NIGHT,
                    bridge,
                    repository,
                    applicationContext
                )
                bridge.speak(res.speechText)
            }
        })

        // 4. Secure Perimeter
        card.addView(createActionBtn("Secure Perimeter Scan", "🛡️", "#00E676") {
            serviceScope.launch {
                val res = StarkProtocolEngine.executeProtocol(
                    StarkProtocolEngine.StarkProtocolType.SECURE_PERIMETER,
                    bridge,
                    repository,
                    applicationContext
                )
                bridge.speak(res.speechText)
            }
        })

        // 5. Open Main Console
        card.addView(createActionBtn("Launch JARVIS Mainframe", "⚡", "#FFFFFF") {
            val appIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(appIntent)
        })

        hudCardView = card
        try {
            windowManager?.addView(hudCardView, hudParams)
            isHudOpen = true
        } catch (_: Exception) {}
    }

    private fun hideHudCard() {
        if (hudCardView != null) {
            try {
                windowManager?.removeView(hudCardView)
            } catch (_: Exception) {}
            hudCardView = null
            isHudOpen = false
        }
    }

    private fun removeViews() {
        hideHudCard()
        if (orbRootView != null) {
            try {
                windowManager?.removeView(orbRootView)
            } catch (_: Exception) {}
            orbRootView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
