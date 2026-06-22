package com.antispy.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.util.Random

class AntiSpyOverlayService : Service() {

    companion object {
        const val ACTION_UPDATE = "com.antispy.app.ACTION_UPDATE"
        const val ACTION_STOP = "com.antispy.app.ACTION_STOP"
        
        const val EXTRA_OPACITY = "extra_opacity"
        const val EXTRA_PATTERN = "extra_pattern"

        const val PATTERN_DIM = "dim"
        const val PATTERN_LINES = "lines"
        const val PATTERN_CROSSHATCH = "crosshatch"
        const val PATTERN_NOISE = "noise"

        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "AntiSpyServiceChannel"
        
        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: OverlayView
    private var opacity: Float = 0.5f
    private var pattern: String = PATTERN_LINES

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == ACTION_UPDATE) {
                    opacity = it.getFloatExtra(EXTRA_OPACITY, opacity)
                    pattern = it.getStringExtra(EXTRA_PATTERN) ?: pattern
                    savePreferences()
                    overlayView.updateSettings(opacity, pattern)
                } else if (it.action == ACTION_STOP) {
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Load initial preferences
        val sharedPref = getSharedPreferences("AntiSpyPrefs", Context.MODE_PRIVATE)
        opacity = sharedPref.getFloat("opacity", 0.5f)
        pattern = sharedPref.getString("pattern", PATTERN_LINES) ?: PATTERN_LINES

        overlayView = OverlayView(this)
        overlayView.updateSettings(opacity, pattern)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Make overlay cover notch area on Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE)
            addAction(ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }
        
        // Notify tile and activity about status change
        sendBroadcast(Intent("com.antispy.app.STATUS_CHANGED"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_STOP) {
                stopSelf()
                return START_NOT_STICKY
            }
            // Update settings if starting with extras
            if (it.hasExtra(EXTRA_OPACITY) || it.hasExtra(EXTRA_PATTERN)) {
                opacity = it.getFloatExtra(EXTRA_OPACITY, opacity)
                pattern = it.getStringExtra(EXTRA_PATTERN) ?: pattern
                savePreferences()
                overlayView.updateSettings(opacity, pattern)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        unregisterReceiver(updateReceiver)
        
        if (::windowManager.isInitialized && ::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Notify tile and activity about status change
        sendBroadcast(Intent("com.antispy.app.STATUS_CHANGED"))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun savePreferences() {
        val sharedPref = getSharedPreferences("AntiSpyPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putFloat("opacity", opacity)
            putString("pattern", pattern)
            apply()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Anti-Spy Service"
            val descriptionText = "Layanan aktif untuk filter privasi layar"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AntiSpyOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Filter Anti-Spy Aktif")
            .setContentText("Menjaga privasi layar dari sudut pandang samping.")
            .setSmallIcon(android.R.drawable.ic_lock_lock) // Standard system icon
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    // Inner class representing the overlay filter view
    private class OverlayView(context: Context) : View(context) {
        private var opacity: Float = 0.5f
        private var patternType: String = PATTERN_LINES
        private var noiseBitmap: Bitmap? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun updateSettings(opacity: Float, patternType: String) {
            this.opacity = opacity
            this.patternType = patternType
            if (patternType != PATTERN_NOISE) {
                noiseBitmap?.recycle()
                noiseBitmap = null
            } else {
                generateNoiseIfNecessary()
            }
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (patternType == PATTERN_NOISE) {
                generateNoiseBitmap(w, h)
            }
        }

        private fun generateNoiseIfNecessary() {
            if (width > 0 && height > 0 && noiseBitmap == null) {
                generateNoiseBitmap(width, height)
            }
        }

        private fun generateNoiseBitmap(w: Int, h: Int) {
            try {
                // Generate a smaller bitmap first for performance, then scale it up
                val scale = 4
                val bw = (w / scale).coerceAtLeast(1)
                val bh = (h / scale).coerceAtLeast(1)
                
                val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val random = Random()
                val dotPaint = Paint()

                for (x in 0 until bw) {
                    for (y in 0 until bh) {
                        if (random.nextBoolean()) {
                            dotPaint.color = Color.BLACK
                            canvas.drawPoint(x.toFloat(), y.toFloat(), dotPaint)
                        }
                    }
                }
                
                noiseBitmap = Bitmap.createScaledBitmap(bmp, w, h, false)
                bmp.recycle()
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            val alphaInt = (opacity * 255).toInt().coerceIn(0, 255)

            when (patternType) {
                PATTERN_DIM -> {
                    canvas.drawColor(Color.argb(alphaInt, 0, 0, 0))
                }
                
                PATTERN_LINES -> {
                    val density = resources.displayMetrics.density
                    val pitch = 6 * density
                    val lineWidth = 2.5f * density
                    
                    paint.style = Paint.Style.FILL
                    paint.color = Color.BLACK
                    paint.alpha = alphaInt
                    
                    var x = 0f
                    while (x < width) {
                        canvas.drawRect(x, 0f, x + lineWidth, height.toFloat(), paint)
                        x += pitch
                    }
                }
                
                PATTERN_CROSSHATCH -> {
                    val density = resources.displayMetrics.density
                    val pitch = 12 * density
                    
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f * density
                    paint.color = Color.BLACK
                    paint.alpha = alphaInt
                    
                    val maxDim = Math.max(width, height).toFloat()
                    var offset = -maxDim
                    while (offset < maxDim) {
                        // Diagonal /
                        canvas.drawLine(offset, 0f, offset + height, height.toFloat(), paint)
                        // Diagonal \
                        canvas.drawLine(offset + height, 0f, offset, height.toFloat(), paint)
                        offset += pitch
                    }
                }
                
                PATTERN_NOISE -> {
                    noiseBitmap?.let {
                        paint.alpha = alphaInt
                        canvas.drawBitmap(it, 0f, 0f, paint)
                    } ?: run {
                        // Fallback to simple dim if noise is not generated yet
                        canvas.drawColor(Color.argb(alphaInt, 0, 0, 0))
                    }
                }
            }
        }
    }
}
