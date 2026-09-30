package com.example.jarvis.voice

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Floating JARVIS Overlay HUD that renders over other apps, games, or the lock screen.
 * Requires SYSTEM_ALERT_WINDOW permission. Gracefully bypassed if not granted.
 */
class JarvisOverlayHud(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hudView: View? = null
    private var isShowing = false
    private var autoDismissRunnable: Runnable? = null

    fun show(
        status: String = "JARVIS ACTIVE",
        transcript: String = "Awaiting directive...",
        isListening: Boolean = true
    ) {
        mainHandler.post {
            if (!canDrawOverlays()) return@post

            if (hudView == null) {
                createHudView()
            }

            updateContent(status, transcript, isListening)

            if (!isShowing && hudView != null) {
                try {
                    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }

                    @Suppress("DEPRECATION")
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        y = dpToPx(48)
                    }

                    windowManager?.addView(hudView, params)
                    isShowing = true
                } catch (_: Exception) {}
            }

            resetAutoDismiss(6000L)
        }
    }

    fun updateTranscript(transcript: String) {
        mainHandler.post {
            hudView?.findViewById<TextView>(1002)?.text = transcript
            resetAutoDismiss(6000L)
        }
    }

    fun updateStatus(status: String, isListening: Boolean) {
        mainHandler.post {
            val statusTv = hudView?.findViewById<TextView>(1001)
            statusTv?.text = status
            val orb = hudView?.findViewById<View>(1000)
            val orbBg = orb?.background as? GradientDrawable
            if (isListening) {
                orbBg?.setColor(Color.parseColor("#00E5FF"))
            } else {
                orbBg?.setColor(Color.parseColor("#76FF03"))
            }
            resetAutoDismiss(6000L)
        }
    }

    fun hide() {
        mainHandler.post {
            cancelAutoDismiss()
            if (isShowing && hudView != null) {
                try {
                    windowManager?.removeView(hudView)
                } catch (_: Exception) {}
                isShowing = false
            }
        }
    }

    private fun createHudView() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE0B1220")) // Sleek futuristic dark translucent
                setStroke(dpToPx(1), Color.parseColor("#00E5FF"))
                cornerRadius = dpToPx(24).toFloat()
            }
            background = bg
            elevation = dpToPx(8).toFloat()
        }

        // 1. Glowing Core Orb
        val orb = View(context).apply {
            id = 1000
            val size = dpToPx(14)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dpToPx(12)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E5FF"))
            }
        }
        root.addView(orb)

        // 2. Text Container
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dpToPx(12)
            }
        }

        val statusTv = TextView(context).apply {
            id = 1001
            text = "JARVIS LISTENING"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }
        textContainer.addView(statusTv)

        val transcriptTv = TextView(context).apply {
            id = 1002
            text = "Say a command..."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 2
        }
        textContainer.addView(transcriptTv)

        root.addView(textContainer)

        // 3. Close Button
        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            setOnClickListener { hide() }
        }
        root.addView(closeBtn)

        hudView = root
    }

    private fun updateContent(status: String, transcript: String, isListening: Boolean) {
        val statusTv = hudView?.findViewById<TextView>(1001)
        val transcriptTv = hudView?.findViewById<TextView>(1002)
        val orb = hudView?.findViewById<View>(1000)

        statusTv?.text = status.uppercase()
        transcriptTv?.text = transcript

        val orbBg = orb?.background as? GradientDrawable
        if (isListening) {
            orbBg?.setColor(Color.parseColor("#00E5FF"))
        } else {
            orbBg?.setColor(Color.parseColor("#76FF03"))
        }
    }

    private fun resetAutoDismiss(delayMs: Long) {
        cancelAutoDismiss()
        autoDismissRunnable = Runnable { hide() }
        mainHandler.postDelayed(autoDismissRunnable!!, delayMs)
    }

    private fun cancelAutoDismiss() {
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = null
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
