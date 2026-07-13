package com.nihaw.translate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showBubble()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBubble() {
        if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) return
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        val params = WindowManager.LayoutParams(
            dpToPx(56), dpToPx(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dpToPx(100)
        }

        bubbleView.setOnTouchListener { _, event ->
            var initialX = params.x
            var initialY = params.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - event.x).toInt()
                    params.y = initialY + (event.rawY - event.y).toInt()
                    windowManager.updateViewLayout(bubbleView, params)
                }
            }
            false
        }

        bubbleView.setOnClickListener {
            toggleOverlay()
        }

        windowManager.addView(bubbleView, params)
    }

    private fun toggleOverlay() {
        // TODO: show/hide translation overlay panel
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Nihaw Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Nihaw")
            .setContentText("Translation overlay is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleView.isInitialized && bubbleView.isAttachedToWindow) {
            windowManager.removeView(bubbleView)
        }
    }

    companion object {
        private const val CHANNEL_ID = "nihaw_overlay"
    }
}
